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

import za.co.monadic.scopus._
import za.co.monadic.scopus.dsp.Upsampler

import scala.util.{Success, Try}

private object G711uDecoder {

  // Lookup table for u-law decoder
  val uToLin: Array[Short] = Array[Short](-32124, -31100, -30076, -29052, -28028, -27004, -25980, -24956, -23932,
    -22908, -21884, -20860, -19836, -18812, -17788, -16764, -15996, -15484, -14972, -14460, -13948, -13436, -12924,
    -12412, -11900, -11388, -10876, -10364, -9852, -9340, -8828, -8316, -7932, -7676, -7420, -7164, -6908, -6652, -6396,
    -6140, -5884, -5628, -5372, -5116, -4860, -4604, -4348, -4092, -3900, -3772, -3644, -3516, -3388, -3260, -3132,
    -3004, -2876, -2748, -2620, -2492, -2364, -2236, -2108, -1980, -1884, -1820, -1756, -1692, -1628, -1564, -1500,
    -1436, -1372, -1308, -1244, -1180, -1116, -1052, -988, -924, -876, -844, -812, -780, -748, -716, -684, -652, -620,
    -588, -556, -524, -492, -460, -428, -396, -372, -356, -340, -324, -308, -292, -276, -260, -244, -228, -212, -196,
    -180, -164, -148, -132, -120, -112, -104, -96, -88, -80, -72, -64, -56, -48, -40, -32, -24, -16, -8, 0, 32124,
    31100, 30076, 29052, 28028, 27004, 25980, 24956, 23932, 22908, 21884, 20860, 19836, 18812, 17788, 16764, 15996,
    15484, 14972, 14460, 13948, 13436, 12924, 12412, 11900, 11388, 10876, 10364, 9852, 9340, 8828, 8316, 7932, 7676,
    7420, 7164, 6908, 6652, 6396, 6140, 5884, 5628, 5372, 5116, 4860, 4604, 4348, 4092, 3900, 3772, 3644, 3516, 3388,
    3260, 3132, 3004, 2876, 2748, 2620, 2492, 2364, 2236, 2108, 1980, 1884, 1820, 1756, 1692, 1628, 1564, 1500, 1436,
    1372, 1308, 1244, 1180, 1116, 1052, 988, 924, 876, 844, 812, 780, 748, 716, 684, 652, 620, 588, 556, 524, 492, 460,
    428, 396, 372, 356, 340, 324, 308, 292, 276, 260, 244, 228, 212, 196, 180, 164, 148, 132, 120, 112, 104, 96, 88, 80,
    72, 64, 56, 48, 40, 32, 24, 16, 8, 0)

  // Lookup table returning Float values
  val uToLinF: Array[Float] = new Array[Float](uToLin.length)

  // Just build the Float version
  for (i <- uToLin.indices) {
    uToLinF(i) = uToLin(i) / 32124.0f
  }

}

case class G711uDecoderShort(fs: SampleFrequency, channels: Int) extends DecoderShort with G711uCodec {

  require(channels == 1, s"The $getDetail supports only mono audio")

  import G711uDecoder.uToLin
  import ArrayConversion._

  private val factor = fs match {
    case Sf8000  => 1
    case Sf16000 => 2
    case Sf24000 => 3
    case Sf32000 => 4
    case Sf48000 => 6
    case _       => throw new RuntimeException("Unsupported sample rate conversion")
  }

  private val up = if (factor == 1) None else Some(Upsampler(factor))

  /**
    * Decode an audio packet to an array of Shorts
    *
    * @param compressedAudio The incoming audio packet
    * @return A Try containing decoded audio in Short format
    */
  override def apply(compressedAudio: Array[Byte]): Try[Array[Short]] = {
    val out = new Array[Short](compressedAudio.length)
    var i   = 0
    while (i < compressedAudio.length) {
      out(i) = uToLin(compressedAudio(i) & 0xff)
      i += 1
    }
    up match {
      case Some(u) => Success(floatToShort(u.process(shortToFloat(out))))
      case None => Success(out)
    }
  }

  /**
    * Decode an erased (i.e. not received) audio packet. Note you need to specify
    * how many samples you think you have lost so the decoder can attempt to
    * deal with the erasure appropriately.
    *
    * @return A Try containing decompressed audio in Float format
    */
  override def apply(count: Int): Try[Array[Short]] = Try(new Array[Short](count))

  /**
    * Release all pointers allocated for the encoder. Make every attempt to call this
    * when you are done with the encoder as finalise() is what it is in the JVM
    */
  override def cleanup(): Unit = ()

  /**
    * @return A description of this instance of an encoder or decoder
    */
  override def getDetail: String = "G.711u u-law decoder"

  /**
    * Reset the underlying codec.
    */
  override def reset: Int = 0

  /**
    * @return The sample rate for this codec's instance
    */
  override def getSampleRate: Int = fs()
}

case class G711uDecoderFloat(fs: SampleFrequency, channels: Int) extends DecoderFloat with G711uCodec {
  import G711uDecoder.uToLinF

  require(channels == 1, s"The $getDetail supports only mono audio")

  private val factor = fs match {
    case Sf8000  => 1
    case Sf16000 => 2
    case Sf24000 => 3
    case Sf32000 => 4
    case Sf48000 => 6
    case _       => throw new RuntimeException("Unsupported sample rate conversion")
  }

  private val up = if (factor == 1) None else Some(Upsampler(factor))

  /**
    * Decode an audio packet to an array of Floats
    *
    * @param compressedAudio The incoming audio packet
    * @return A Try containing the decoded audio packet in Float format
    */
  override def apply(compressedAudio: Array[Byte]): Try[Array[Float]] = {
    val out = new Array[Float](compressedAudio.length)
    var i   = 0
    while (i < compressedAudio.length) {
      out(i) = uToLinF(compressedAudio(i) & 0xff)
      i += 1
    }
    up match {
      case Some(u) => Success(u.process(out))
      case None => Success(out)
    }
  }

  /**
    * Decode an erased (i.e. not received) audio packet. Note you need to specify
    * how many samples you think you have lost so the decoder can attempt to
    * deal with the erasure appropriately.
    *
    * @return A Try containing decompressed audio in Float format
    */
  override def apply(count: Int): Try[Array[Float]] = Try(new Array[Float](count))

  /**
    * Release all pointers allocated for the encoder. Make every attempt to call this
    * when you are done with the encoder as finalise() is what it is in the JVM
    */
  override def cleanup(): Unit = ()

  /**
    * @return A discription of this instance of an encoder or decoder
    */
  override def getDetail: String = "G.711u u-law decoder"

  /**
    * Reset the underlying codec.
    */
  override def reset: Int = 0

  /**
    * @return The sample rate for this codec's instance
    */
  override def getSampleRate: Int = fs()
}
