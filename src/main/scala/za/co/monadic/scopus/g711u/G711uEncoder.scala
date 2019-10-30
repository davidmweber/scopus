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

package za.co.monadic.scopus.g711u

import za.co.monadic.scopus.dsp.Downsampler
import za.co.monadic.scopus._

import scala.util.{Success, Try}

case class G711uEncoder(sampleFreq: SampleFrequency, channels: Int) extends Encoder with G711uCodec {

  require(channels == 1, s"The $getDetail supports only mono audio")
  import ArrayConversion._

  private val BIAS     = 0x84 /* Bias for linear code. */
  private val CLIP     = 8159
  private val uEnd     = Array[Int](0x3F, 0x7F, 0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF)

  private val factor = sampleFreq match {
    case Sf8000  => 1
    case Sf16000 => 2
    case Sf24000 => 3
    case Sf32000 => 4
    case Sf48000 => 6
    case _       => throw new RuntimeException("Unsupported sample rate conversion")
  }

  private val down = if (factor == 1) None else Some(Downsampler(factor))

  private def searchUEnd(x: Int): Int = {
    var i = 0
    while ((i < uEnd.length) && (x > uEnd(i))) {
      i += 1
    }
    i
  }

  private def toMu(xIn: Short): Byte = {
    val xScale: Int = xIn >> 2
    var (x, mask) = if (xScale < 0) {
      (-xScale - 1, 0x7F)
    } else {
      (xScale, 0xFF)
    }
    if (x > CLIP) x = CLIP /* clip the magnitude */
    x += (BIAS >> 2)

    /* Convert the scaled magnitude to segment number. */
    val seg = searchUEnd(x)

    /*
     * Combine the sign, segment, quantization bits;
     * and complement the code word.
     */
    if (seg >= 8)
      ((0x7F ^ mask) & 0xFF).toByte /* out of range, return maximum value. */
    else {
      val uval = (seg << 4) | ((x >> (seg + 1)) & 0xF)
      ((uval ^ mask) & 0xFF).toByte
    }
  }

  private def toMu(xIn: Float): Byte = toMu((xIn * PCM_NORM).toShort)

  /**
    * Encode a block of raw audio in integer format using the configured encoder
    *
    * @param audio Audio data arranged as a contiguous block interleaved array of short integers
    * @return An array containing the compressed audio or the exception in case of a failure
    */
  override def apply(audio: Array[Short]): Try[Array[Byte]] = {
    val out = new Array[Byte](audio.length / factor)
    val dAudio = down match {
      case Some(d) => floatToShort(d.process(shortToFloat(audio)))
      case None    => audio
    }
    var i = 0
    while (i < dAudio.length) {
      out(i) = toMu(dAudio(i))
      i += 1
    }
    Success(out)
  }

  /**
    * Encode a block of raw audio  in float format using the configured encoder
    *
    * @param audio Audio data arranged as a contiguous block interleaved array of floats
    * @return An array containing the compressed audio or the exception in case of a failure
    */
  override def apply(audio: Array[Float]): Try[Array[Byte]] = {
    val out = new Array[Byte](audio.length / factor)
    val dAudio = down match {
      case Some(d) => d.process(audio)
      case None    => audio
    }
    var i = 0
    while (i < dAudio.length) {
      out(i) = toMu(dAudio(i))
      i += 1
    }
    Success(out)
  }

  /**
    * Set the complexity of the encoder. This has no effect if the encoder does not support
    * complexity settings
    *
    * @param c A value between 0 and 10 indicating the encoder complexity.
    * @return A reference to the updated encoder
    */
  override def complexity(c: Int): Encoder = this

  /**
    * Release all pointers allocated for the encoder. Make every attempt to call this
    * when you are done with the encoder as finalise() is what it is in the JVM
    */
  override def cleanup(): Unit = ()

  /**
    * @return A discription of this instance of an encoder or decoder
    */
  override def getDetail: String = "G.711u u-law encoder"

  /**
    * Reset the underlying codec.
    */
  override def reset: Int = 0

  /**
    * @return The sample rate for this codec's instance
    */
  override def getSampleRate: Int = sampleFreq()
}
