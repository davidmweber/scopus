/*
 * Copyright 2014 David Weber
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

import Opus._
import za.co.monadic.scopus.{Encoder, Application, SampleFrequency, Voip}

import scala.util.{Failure, Success, Try}

/**
  * Wrapper around the Opus codec's encoder subsystem.
  * The C interface is documented at [[http://www.opus-codec.org]] and should
  * be considered definitive. The encoder accepts buffers of duration of
  * 2,5, 5, 10, 20, 40 or 60ms. To calculate the buffer size, multiply your sample
  * frequency by the frame duration. At 8kHz a 20ms packet is 160 samples long.
  * @param sampleFreq The required sampling frequency
  * @param channels The number of channels you intend to encode.
  * @param app The application (Voip, Audio or LowDelay)
  * @param bufferSize The reserved size of the buffer to which compressed data are written.
  *                   The default should be more than sufficient
  */
class OpusEncoder(sampleFreq: SampleFrequency, channels: Int, app: Application, bufferSize: Int = 8192)
    extends Encoder {
  require(bufferSize > 0, "Buffer size must be positive")
  val error: Array[Int] = Array[Int](0)
  val decodePtr         = new Array[Byte](bufferSize)
  val encoder: Long     = encoder_create(sampleFreq(), channels, app(), error)
  if (error(0) != OPUS_OK) {
    throw new RuntimeException(s"Failed to create the Opus encoder: ${error_string(error(0))}")
  }
  var clean = false

  def getDetail = s"Opus encoder with sf= ${sampleFreq()}"

  /**
    * Encode a block of raw audio  in integer format using the configured encoder
    * @param audio Audio data arranged as a contiguous block interleaved array of short integers
    * @return An array containing the compressed audio or the exception in case of a failure
    */
  def apply(audio: Array[Short]): Try[Array[Byte]] = {
    val len: Int =
      encode_short(encoder, audio, audio.length, decodePtr, bufferSize)
    if (len < 0)
      Failure(new IllegalArgumentException(s"opus_encode() failed: ${error_string(len)}"))
    else
      Success(decodePtr.slice(0, len))
  }

  /**
    * Encode a block of raw audio  in float format using the configured encoder
    * @param audio Audio data arranged as a contiguous block interleaved array of floats
    * @return An array containing the compressed audio or the exception in case of a failure
    */
  def apply(audio: Array[Float]): Try[Array[Byte]] = {
    val len = encode_float(encoder, audio, audio.length, decodePtr, bufferSize)
    if (len < 0)
      Failure(new RuntimeException(s"opus_encode_float() failed: ${error_string(len)}"))
    else
      Success(decodePtr.slice(0, len))
  }

  /**
    * Release all pointers allocated for the encoder. Make every attempt to call this
    * when you are done with the encoder as finalise() is what it is in the JVM
    */
  def cleanup(): Unit = {
    if (!clean) {
      encoder_destroy(encoder)
      clean = true
    }
  }

  def reset: Int = encoder_set_ctl(encoder, OPUS_RESET_STATE, 0)

  private def setter(command: Integer, parameter: Integer): Unit = {
    assert(command % 2 == 0) // Setter commands are even
    val err = encoder_set_ctl(encoder, command, parameter)
    if (err != OPUS_OK)
      throw new RuntimeException(s"opus_encoder_ctl setter failed for command $command: ${error_string(err)}")
  }

  private def getter(command: Int): Int = {
    assert(command % 2 == 1) // Getter commands are all odd
    val result   = Array[Int](0)
    val err: Int = encoder_get_ctl(encoder, command, result)
    if (err != OPUS_OK)
      throw new RuntimeException(s"opus_encoder_ctl getter failed for command $command: ${error_string(err)}")
    result(0)
  }

  /**
    * Set the complexity of the encoder. This has no effect if the encoder does not support
    * complexity settings
    * @param c A value between 0 and 10 indicating the encoder complexity.
    * @return A reference to the updated encoder
    */
  override def complexity(c: Int): Encoder = {
    require((c >= 0) && (c <= 10))
    setComplexity(c)
    this
  }

  def setComplexity(complexity: Integer): OpusEncoder = {
    require((complexity >= 0) && (complexity <= 10))
    setter(OPUS_SET_COMPLEXITY_REQUEST, complexity)
    this
  }

  def setBitRate(bitRate: Integer): OpusEncoder = {
    require(bitRate > 500 && bitRate <= 512000)
    setter(OPUS_SET_BITRATE_REQUEST, bitRate)
    this
  }

  def setVbr(useVbr: Integer): OpusEncoder = {
    require(useVbr == 0 || useVbr == 1)
    setter(OPUS_SET_VBR_REQUEST, useVbr)
    this
  }

  def setVbrConstraint(cvbr: Integer): OpusEncoder = {
    require(cvbr == 0 || cvbr == 1)
    setter(OPUS_SET_VBR_CONSTRAINT_REQUEST, cvbr)
    this
  }

  def setForceChannels(forceChannels: Integer): OpusEncoder = {
    require(forceChannels == 0 || forceChannels == 1 || forceChannels == OPUS_AUTO)
    setter(OPUS_SET_FORCE_CHANNELS_REQUEST, forceChannels)
    this
  }

  def setMaxBandwidth(bandwidth: Integer): OpusEncoder = {
    require(
      Set(OPUS_BANDWIDTH_FULLBAND,
          OPUS_BANDWIDTH_SUPERWIDEBAND,
          OPUS_BANDWIDTH_WIDEBAND,
          OPUS_BANDWIDTH_MEDIUMBAND,
          OPUS_BANDWIDTH_NARROWBAND).contains(bandwidth))
    setter(OPUS_SET_MAX_BANDWIDTH_REQUEST, bandwidth)
    this
  }

  def setBandWidth(bandwidth: Integer): OpusEncoder = {
    require(
      Set(OPUS_AUTO,
          OPUS_BANDWIDTH_NARROWBAND,
          OPUS_BANDWIDTH_MEDIUMBAND,
          OPUS_BANDWIDTH_WIDEBAND,
          OPUS_BANDWIDTH_SUPERWIDEBAND,
          OPUS_BANDWIDTH_FULLBAND).contains(bandwidth))
    setter(OPUS_SET_BANDWIDTH_REQUEST, bandwidth)
    this
  }

  def setSignal(signal: Integer): OpusEncoder = {
    require(Set(OPUS_AUTO, OPUS_SIGNAL_VOICE, OPUS_SIGNAL_MUSIC).contains(signal))
    setter(OPUS_SET_SIGNAL_REQUEST, signal)
    this
  }

  def setApplication(appl: Integer): OpusEncoder = {
    require(Set(OPUS_APPLICATION_VOIP, OPUS_APPLICATION_AUDIO, OPUS_APPLICATION_RESTRICTED_LOWDELAY).contains(appl))
    setter(OPUS_SET_APPLICATION_REQUEST, appl)
    this
  }

  def setInbandFec(useInbandFec: Integer): OpusEncoder = {
    require(useInbandFec == 0 || useInbandFec == 1)
    setter(OPUS_SET_INBAND_FEC_REQUEST, useInbandFec)
    this
  }

  def setPacketLossPerc(packetLossPerc: Integer): OpusEncoder = {
    require(packetLossPerc >= 0 && packetLossPerc <= 100)
    setter(OPUS_SET_PACKET_LOSS_PERC_REQUEST, packetLossPerc)
    this
  }

  def setUseDtx(useDtx: Integer): OpusEncoder = {
    require(useDtx == 0 || useDtx == 1)
    setter(OPUS_SET_DTX_REQUEST, useDtx)
    this
  }

  def setLsbDepth(depth: Integer = 24): OpusEncoder = {
    require(depth >= 8 && depth <= 24)
    setter(OPUS_SET_LSB_DEPTH_REQUEST, depth)
    this
  }

  def setExpertFrameDuration(duration: Integer): OpusEncoder = {
    require(
      Set(OPUS_FRAMESIZE_ARG,
          OPUS_FRAMESIZE_2_5_MS,
          OPUS_FRAMESIZE_5_MS,
          OPUS_FRAMESIZE_10_MS,
          OPUS_FRAMESIZE_20_MS,
          OPUS_FRAMESIZE_40_MS,
          OPUS_FRAMESIZE_60_MS).contains(duration))
    setter(OPUS_SET_EXPERT_FRAME_DURATION_REQUEST, duration)
    this
  }

  def setPredictionDisable(disable: Integer): OpusEncoder = {
    require(disable == 0 || disable == 1)
    setter(OPUS_SET_PREDICTION_DISABLED_REQUEST, disable)
    this
  }

  def getComplexity: Int = getter(OPUS_GET_COMPLEXITY_REQUEST)

  def getBitRate: Int = getter(OPUS_GET_BITRATE_REQUEST)

  def getVbr: Int = getter(OPUS_GET_VBR_REQUEST)

  def getVbrConstraint: Int = getter(OPUS_GET_VBR_CONSTRAINT_REQUEST)

  def getForceChannels: Int = getter(OPUS_GET_FORCE_CHANNELS_REQUEST)

  def getMaxBandwidth: Int = getter(OPUS_GET_MAX_BANDWIDTH_REQUEST)

  def getBandwidth: Int = getter(OPUS_GET_BANDWIDTH_REQUEST)

  def getSignal: Int = getter(OPUS_GET_SIGNAL_REQUEST)

  def getApplication: Int = getter(OPUS_GET_APPLICATION_REQUEST)

  def getSampleRate: Int = getter(OPUS_GET_SAMPLE_RATE_REQUEST)

  def getLookahead: Int = getter(OPUS_GET_LOOKAHEAD_REQUEST)

  def getInbandFec: Int = getter(OPUS_GET_INBAND_FEC_REQUEST)

  def getPacketLossPerc: Int = getter(OPUS_GET_PACKET_LOSS_PERC_REQUEST)

  def getUseDtx: Int = getter(OPUS_GET_DTX_REQUEST)

  def getLsbDepth: Int = getter(OPUS_GET_LSB_DEPTH_REQUEST)

  def getExpertFrameDuration: Int = getter(OPUS_GET_EXPERT_FRAME_DURATION_REQUEST)

  def getPredictionDisable: Int = getter(OPUS_GET_PREDICTION_DISABLED_REQUEST)

  /**
    * Test if the packet is an Opus DTX (silent) packet. In practice, if
    * this is true then don't transmit this packet.
    * @param audio Opus compressed packet
    * @return True if it is a DTX packet
    */
  override def isDTX(audio: Array[Byte]): Boolean = audio.length <= 2

}

object OpusEncoder {

  /**
    * Factory for an encoder instance.
    * @param sampleFreq THe required sampling frequency
    * @param channels The number of channels you intend to encode.
    * @param app The application (Voip, Audio or LowDelay). It defaults to Voip.
    * @param bufferSize The reserved size of the buffer to which compressed data are written.
    *                   The default should be more than sufficient
    * @return A Try[Array[Byte]) containing a reference to the encoder object
    */
  def apply(sampleFreq: SampleFrequency, channels: Int, app: Application = Voip, bufferSize: Int = 8192) =
    new OpusEncoder(sampleFreq, channels, app, bufferSize)

}
