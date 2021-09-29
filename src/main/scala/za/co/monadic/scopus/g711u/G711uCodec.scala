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

package za.co.monadic.scopus.g711u

import za.co.monadic.scopus.{SampleFrequency, Sf16000, Sf24000, Sf32000, Sf48000, Sf8000}

trait G711uCodec {
  def getCodecName: String = "g711u"

  def channels: Int

  /**
    * Calculates the downsample factor needed for the input sampling frequency
    * @param fs The sample frequecy of the current signal
    * @return An integer indicating the decimation factor to achieve 8kHz sampling required by this codec
    */
  def toFactor(fs: SampleFrequency): Int =
    fs match {
      case Sf8000  => 1
      case Sf16000 => 2
      case Sf24000 => 3
      case Sf32000 => 4
      case Sf48000 => 6
      case _       => throw new RuntimeException("Unsupported sample rate conversion")
    }

  /**
    * When audio codec is configured to use two channels, mix them into one.
    * Otherwise return the source array.
    *
    * @param audio Audio data arranged as a contiguous block interleaved array of floats
    * @return An array containing one mixed audio channel
    */
  def toMono(audio: Array[Float]): Array[Float] = {
    if (channels == 1) {
      audio
    } else {
      val out = new Array[Float](audio.length / 2)
      var i   = 0
      var j   = 0
      while (i < audio.length - 1) {
        out(j) = (audio(i) + audio(i + 1)) / 2f
        i += 2
        j += 1
      }
      out
    }
  }

  /**
    * When audio codec is configured to use two channels, mix them into one.
    * Otherwise return the source array.
    *
    * @param audio Audio data arranged as a contiguous block interleaved array of short integers
    * @return An array containing one mixed audio channel
    */
  def toMono(audio: Array[Short]): Array[Short] = {
    if (channels == 1) {
      audio
    } else {
      val out = new Array[Short](audio.length / 2)
      var i   = 0
      var j   = 0
      while (i < audio.length - 1) {
        out(j) = ((audio(i) + audio(i + 1)) / 2).toShort
        i += 2
        j += 1
      }
      out
    }
  }

  /**
    * When audio codec is configured to use two channels
    * it duplicates one, mono channel into a stream containing left and right channel.
    * Otherwise return the source array.
    *
    * @param audio Audio data arranged as a contiguous block array of floats
    * @return An array containing interleaved array of duplicate audio channel.
    */
  def toStereo(audio: Array[Short]): Array[Short] = {
    if (channels == 1) {
      audio
    } else {
      val out = new Array[Short](audio.length * 2)
      var i   = 0
      var j   = 0
      while (i < audio.length) {
        val elem = audio(i)
        out(j) = elem
        out(j + 1) = elem
        i += 1
        j += 2
      }
      out
    }
  }

  /**
    * When audio codec is configured to use two channels
    * it duplicates one, mono channel into a stream containing left and right channel.
    * Otherwise return the source array.
    *
    * @param audio Audio data arranged as a contiguous block array of short integers
    * @return An array containing interleaved array of duplicate audio channel.
    */
  def toStereo(audio: Array[Float]): Array[Float] = {
    if (channels == 1) {
      audio
    } else {
      val out = new Array[Float](audio.length * 2)
      var i   = 0
      var j   = 0
      while (i < audio.length) {
        val elem = audio(i)
        out(j) = elem
        out(j + 1) = elem
        i += 1
        j += 2
      }
      out
    }
  }

}
