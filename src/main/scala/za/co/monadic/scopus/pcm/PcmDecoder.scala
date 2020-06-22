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

package za.co.monadic.scopus.pcm

import za.co.monadic.scopus.{DecoderShort, DecoderFloat, SampleFrequency}
import za.co.monadic.scopus.ArrayConversion._

import scala.util.Try

/**
  * Decode array in PCM. Raw types are converted to bytes with the first
  * byte indicating the data type. A "0" indicates Short types and a "1"
  * indicates a Float type
  */
case class PcmDecoderFloat(fs: SampleFrequency, channels: Int) extends DecoderFloat with PcmCodec {

  /**
    * Decode an audio packet to an array of Floats
    * @param ca The incoming "compressed" audio packet
    * @return A Try containing the decoded audio packet in Float format
    */
  override def apply(ca: Array[Byte]): Try[Array[Float]] = {
    Try {
      if (ca.length > 0) {
        ca(0) match {
          case 0 => byteArrayToShortArray(ca.drop(1)).map(_.toFloat / 32768.0f)
          case 1 => byteArrayToFloatArray(ca.drop(1))
          case s => throw new IllegalArgumentException(s"Type $s cannot be decoded")
        }
      } else throw new IllegalArgumentException(s"Byte Array lenght cannot be zero")
    }
  }

  /**
    * Decode an erased (i.e. not received) audio packet. Note you need to specify
    * how many samples you think you have lost so the decoder can attempt to
    * deal with the erasure appropriately.
    * @return A Try containing decompressed audio in Float format
    */
  override def apply(count: Int): Try[Array[Float]] = Try(new Array[Float](count))

  /**
    * @return The sample rate for this codec's instance
    */
  override def getSampleRate: Int = fs()

  /**
    * Release all pointers allocated for the encoder. Make every attempt to call this
    * when you are done with the encoder as finalise() is what it is in the JVM
    */
  override def cleanup(): Unit = {}

  /**
    * @return A discription of this instance of an encoder or decoder
    */
  override def getDetail: String = "PCM codec that does nothing really"

  /**
    * Reset the underlying codec.
    */
  override def reset: Int = 0
}

/**
  * Decode array in PCM. Raw types are converted to bytes with the first
  * byte indicating the data type. A "0" indicates Short types and a "1"
  * indicates a Float type
  */
case class PcmDecoderShort(fs: SampleFrequency, channels: Int) extends DecoderShort with PcmCodec {

  /**
    * Decode an audio packet to an array of Shorts
    * @param ca The incoming audio packet
    * @return A Try containing decoded audio in Short format
    */
  override def apply(ca: Array[Byte]): Try[Array[Short]] = Try {
    if (ca.length > 0) {
      ca(0) match {
        case 0 => byteArrayToShortArray(ca.drop(1))
        case 1 => byteArrayToFloatArray(ca.drop(1)).map((f: Float) => (f * 32768.0f).round.toShort)
        case s => throw new IllegalArgumentException(s"Type $s cannot be decoded")
      }
    } else throw new IllegalArgumentException(s"Byte Array length cannot be zero")
  }

  /**
    * Decode an erased (i.e. not received) audio packet. Note you need to specify
    * how many samples you think you have lost so the decoder can attempt to
    * deal with the erasure appropriately.
    * @return A Try containing decompressed audio in Float format
    */
  override def apply(count: Int): Try[Array[Short]] = Try(new Array[Short](count))

  /**
    * @return The sample rate for this codec's instance
    */
  override def getSampleRate: Int = fs()

  /**
    * Release all pointers allocated for the encoder. Make every attempt to call this
    * when you are done with the encoder as finalise() is what it is in the JVM
    */
  override def cleanup(): Unit = {}

  /**
    * @return A discription of this instance of an encoder or decoder
    */
  override def getDetail: String = "PCM codec that does nothing really"

  /**
    * Reset the underlying codec.
    */
  override def reset: Int = 0
}
