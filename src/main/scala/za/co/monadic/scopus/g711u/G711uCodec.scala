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

}
