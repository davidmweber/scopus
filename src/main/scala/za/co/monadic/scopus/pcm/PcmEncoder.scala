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

package za.co.monadic.scopus.pcm

import za.co.monadic.scopus.{SampleFrequency, Encoder}
import za.co.monadic.scopus.ArrayConversion._

import scala.util.Try

/**
  * Encode array in PCM. Raw types are converted to bytes with the first
  * byte indicating the data type. A "0" indicates Short types and a "1"
  * indicates a Float type
  */
case class PcmEncoder(sampleFreq: SampleFrequency, channels: Int) extends Encoder {

  /**
    * Encode a block of raw audio  in integer format using the configured encoder
    * @param audio Audio data arranged as a contiguous block interleaved array of short integers
    * @return An array containing the compressed audio or the exception in case of a failure
    */
  override def apply(audio: Array[Short]): Try[Array[Byte]] =
    Try(Array[Byte](0) ++ shortArrayToByteArray(audio))

  /**
    * Set the complexity of the encoder. This has no effect if the encoder does not support
    * complexity settings
    * @param c A value between 0 and 10 indicating the encoder complexity.
    * @return A reference to the updated encoder
    */
  override def complexity(c: Int): Encoder = this

  /**
    * Encode a block of raw audio  in float format using the configured encoder
    * @param audio Audio data arranged as a contiguous block interleaved array of floats
    * @return An array containing the compressed audio or the exception in case of a failure
    */
  override def apply(audio: Array[Float]): Try[Array[Byte]] =
    Try(Array[Byte](1) ++ floatArrayToByteArray(audio))

  /**
    * @return The sample rate for this codec's instance
    */
  override def getSampleRate: Int = sampleFreq()

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
