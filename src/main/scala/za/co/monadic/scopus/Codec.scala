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

package za.co.monadic.scopus

import scala.util.Try

trait Codec {

  /**
    * Release all pointers allocated for the encoder. Make every attempt to call this
    * when you are done with the encoder as finalise() is what it is in the JVM
    */
  def cleanup(): Unit

  final override def finalize(): Unit = {
    cleanup()
  }

  /**
    * @return A description of this instance of an encoder or decoder
    */
  def getDetail: String

  /**
    * Reset the underlying codec.
    */
  def reset: Int

  /**
    * @return The sample rate for this codec's instance
    */
  def getSampleRate: Int

  /**
    * Returns the canonical name for this codec
    */
  def getCodecName: String

  /**
    * Returns true if the compressed audio packet is DTX. In practice, if
    * this is true then don't transmit this packet.
    * @param compressedAudio A compressed audio packet for this codec
    * @return True if the packet is DTX and should not be transmitted
    */
  def isDTX(compressedAudio: Array[Byte]): Boolean = false
}

trait Decoder {
  val fs: SampleFrequency
  val channels: Int
}

/**
  *  Decoder for float data types
  */
trait DecoderFloat extends Codec with Decoder {

  /**
    * Decode an audio packet to an array of Floats
    * @param compressedAudio The incoming audio packet
    * @return A Try containing the decoded audio packet in Float format
    */
  def apply(compressedAudio: Array[Byte]): Try[Array[Float]]

  /**
    * Decode an erased (i.e. not received) audio packet. Note you need to specify
    * how many samples you think you have lost so the decoder can attempt to
    * deal with the erasure appropriately.
    * @return A Try containing decompressed audio in Float format
    */
  def apply(count: Int): Try[Array[Float]]
}

/**
  * Decoder for float data types
  */
trait DecoderShort extends Codec with Decoder {

  /**
    * Decode an audio packet to an array of Shorts
    * @param compressedAudio The incoming audio packet
    * @return A Try containing decoded audio in Short format
    */
  def apply(compressedAudio: Array[Byte]): Try[Array[Short]]

  /**
    * Decode an erased (i.e. not received) audio packet. Note you need to specify
    * how many samples you think you have lost so the decoder can attempt to
    * deal with the erasure appropriately.
    * @return A Try containing decompressed audio in Float format
    */
  def apply(count: Int): Try[Array[Short]]
}

/**
  * Encoder trait
  */
trait Encoder extends Codec {

  /**
    * Encode a block of raw audio  in integer format using the configured encoder
    * @param audio Audio data arranged as a contiguous block interleaved array of short integers
    * @return An array containing the compressed audio or the exception in case of a failure
    */
  def apply(audio: Array[Short]): Try[Array[Byte]]

  /**
    * Encode a block of raw audio  in float format using the configured encoder
    * @param audio Audio data arranged as a contiguous block interleaved array of floats
    * @return An array containing the compressed audio or the exception in case of a failure
    */
  def apply(audio: Array[Float]): Try[Array[Byte]]

  /**
    * Release all pointers allocated for the encoder. Make every attempt to call this
    * when you are done with the encoder as finalise() is what it is in the JVM
    */
  /**
    * Set the complexity of the encoder. This has no effect if the encoder does not support
    * complexity settings
    * @param c A value between 0 and 10 indicating the encoder complexity.
    * @return A reference to the updated encoder
    */
  def complexity(c: Int): Encoder

}
