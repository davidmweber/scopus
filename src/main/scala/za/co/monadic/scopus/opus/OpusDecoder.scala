/*
 * Copyright 2020 David Weber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.monadic.scopus.opus

import za.co.monadic.scopus.opus.Opus._
import za.co.monadic.scopus.{DecoderFloat, DecoderShort, SampleFrequency}

import scala.util.{Failure, Success, Try}

/**
  * Decoder base class which allows specialisations for the different return types offered by
  * the Opus system.
  */
sealed trait OpusBase extends OpusCodec {

  val fs: SampleFrequency
  val channels: Int

  // 120ms of audio is the longest possible buffer we will need for the decoder
  val bufferLen: Int    = math.round(0.120f * fs() * channels)
  var fec               = 0
  val error: Array[Int] = Array[Int](0)
  val decoder: Long     = decoder_create(fs(), channels, error)
  if (error(0) != OPUS_OK) {
    throw new IllegalArgumentException(s"Failed to create the Opus encoder: ${error_string(error(0))}")
  }
  var clean = false

  /**
    * Release all pointers allocated for the decoder. Make every attempt to call this
    * when you are done with the encoder as finalise() is what it is in the JVM
    */
  def cleanup(): Unit = {
    if (!clean) {
      decoder_destroy(decoder)
      clean = true
    }
  }

  private def getter(command: Int): Int = {
    assert(command % 2 == 1) // Getter commands are all odd
    val result   = Array[Int](0)
    val err: Int = decoder_get_ctl(decoder, command, result)
    if (err != OPUS_OK)
      throw new RuntimeException(s"opus_decoder_ctl failed for command $command: ${error_string(err)}")
    result(0)
  }

  private def setter(command: Integer, parameter: Integer): Unit = {
    assert(command % 2 == 0) // Setter commands are even
    val err = decoder_set_ctl(decoder, command, parameter)
    if (err != OPUS_OK)
      throw new RuntimeException(s"opus_decoder_ctl setter failed for command $command: ${error_string(err)}")
  }

  def reset: Int = decoder_set_ctl(decoder, OPUS_RESET_STATE, 0)

  def getSampleRate: Int = getter(OPUS_GET_SAMPLE_RATE_REQUEST)

  def getLookAhead: Int = getter(OPUS_GET_LOOKAHEAD_REQUEST)

  def getBandwidth: Int = getter(OPUS_GET_BANDWIDTH_REQUEST)

  def getPitch: Int = getter(OPUS_GET_PITCH_REQUEST)

  def getGain: Int = getter(OPUS_GET_GAIN_REQUEST)

  def getLastPacketDuration: Int = getter(OPUS_GET_LAST_PACKET_DURATION_REQUEST)

  def setGain(gain: Int): Unit = setter(OPUS_SET_GAIN_REQUEST, gain)

  /**
    * Custom setter for the FEC mode in the decoder
    * @param useFec If true, employ error correction if it is available in the packet
    */
  def setFec(useFec: Boolean): Unit = {
    fec = if (useFec) 1 else 0
  }

  /**
    * Returns the current FEC decoding status.
    * @return  True if FEC is being decoded
    */
  def getFec(useFec: Boolean): Boolean = {
    fec == 1
  }

}

/**
  * Specialisation for Short data return
  * @param fs The sampling frequency required
  * @param channels Number of audio channels required. Must be 1 or 2.
  */
class OpusDecoderShort(val fs: SampleFrequency, val channels: Int) extends DecoderShort with OpusBase {

  val decodedBuf = new Array[Short](bufferLen)

  /**
    * Decode an audio packet to an array of Shorts
    * @param compressedAudio The incoming audio packet
    * @return A Try containing decoded audio in Short format
    */
  def apply(compressedAudio: Array[Byte]): Try[Array[Short]] = {
    val len = decode_short(decoder, compressedAudio, compressedAudio.length, decodedBuf, bufferLen, fec)
    if (len < 0)
      Failure(new RuntimeException(s"opus_decode() failed: ${error_string(len)}"))
    else
      Success(decodedBuf.slice(0, len * channels))
  }

  /**
    * Decode an erased (i.e. not received) audio packet. Note you need to specify
    * how many samples you think you have lost so the decoder can attempt to
    * deal with the erasure appropriately.
    * @return A Try containing decompressed audio in short format
    */
  def apply(count: Int): Try[Array[Short]] = {
    val len = decode_short(decoder, null, 0, decodedBuf, count, fec)
    if (len < 0)
      Failure(new RuntimeException(s"opus_decode() failed: ${error_string(len)}"))
    else
      Success(decodedBuf.slice(0, len))
  }

  def getDetail = s"Opus decoder to `short' with sf= ${fs()}"

  /**
    * Test if the packet is an Opus DTX (silent) packet. In practice, if
    * this is true then don't transmit this packet.
    * @param audio Opus compressed packet
    * @return True if it is a DTX packet
    */
  override def isDTX(audio: Array[Byte]): Boolean = audio.length == 1

}

/**
  * Factory for an Opus decoder that returns Short data
  */
object OpusDecoderShort {

  /**
    * Construct an instance of a decoder that returns audio data as an Array[Short]
    * @param Fs The sample frequency required
    * @param channels The number of channels. Must be 1 or 2
    * @return A Try[] containing a reference to the decoder or an exception if construction fails
    */
  def apply(Fs: SampleFrequency, channels: Int) = new OpusDecoderShort(Fs, channels)
}

/**
  * Specialisation for Float data return
  * @param fs The sampling frequency required
  * @param channels Number of audio channels required. Must be 1 or 2.
  */
class OpusDecoderFloat(val fs: SampleFrequency, val channels: Int) extends DecoderFloat with OpusBase {

  val decodedBuf = new Array[Float](bufferLen)

  def apply(compressedAudio: Array[Byte]): Try[Array[Float]] = {
    val len = decode_float(decoder, compressedAudio, compressedAudio.length, decodedBuf, bufferLen, fec)
    if (len < 0)
      Failure(new RuntimeException(s"opus_decode_float() failed: ${error_string(len)}"))
    else
      Success(decodedBuf.slice(0, len * channels))
  }

  /**
    * Decode an erased (i.e. not received) audio packet. Note you need to specify
    * how many samples you think you have lost so the decoder can attempt to
    * deal with the erasure appropriately.
    * @return A Try containing decompressed audio in Float format
    */
  def apply(count: Int): Try[Array[Float]] = {
    val len = decode_float(decoder, null, 0, decodedBuf, count, fec)
    if (len < 0)
      Failure(new RuntimeException(s"opus_decode_float() failed: ${error_string(len)}"))
    else
      Success(decodedBuf.slice(0, len))
  }

  def getDetail = s"Opus decoder to `float' with sf= ${fs()}"

  /**
    * Test if the packet is an Opus DTX (silent) packet. In practice, if
    * this is true then don't transmit this packet.
    * @param audio Opus compressed packet
    * @return True if it is a DTX packet
    */
  override def isDTX(audio: Array[Byte]): Boolean = audio.length <= 1
}

object OpusDecoderFloat {

  /**
    * Construct an instance of a decoder that returns audio data as an Array[Short]
    * @param Fs The sample frequency required
    * @param channels The number of channels. Must be 1 or 2
    * @return A Try[] containing a reference to the decoder or an exception if construction fails
    */
  def apply(Fs: SampleFrequency, channels: Int) = new OpusDecoderFloat(Fs, channels)
}
