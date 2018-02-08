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

package za.co.monadic.scopus

import za.co.monadic.scopus.opus.Opus
import Opus._

/**
  * Base class for supported sample frequencies
  */
sealed trait SampleFrequency {
  def apply(): Int
}

object SampleFrequency {

  def convert(freq: Int): SampleFrequency = freq match {
    case 8000  => Sf8000
    case 12000 => Sf12000
    case 16000 => Sf16000
    case 24000 => Sf24000
    case 32000 => Sf32000
    case 48000 => Sf48000
    case x     => throw new IllegalArgumentException(s"Sampling frequency $x is unsupported.")
  }
}

object Sf8000 extends SampleFrequency {
  def apply(): Int = 8000
}

object Sf12000 extends SampleFrequency {
  def apply(): Int = 12000
}

object Sf16000 extends SampleFrequency {
  def apply(): Int = 16000
}

object Sf24000 extends SampleFrequency {
  def apply(): Int = 24000
}

object Sf32000 extends SampleFrequency {
  def apply(): Int = 32000
}

object Sf48000 extends SampleFrequency {
  def apply(): Int = 48000
}

sealed trait Application {
  def apply(): Int
}

object Voip extends Application {
  def apply(): Int = OPUS_APPLICATION_VOIP
}

object Audio extends Application {
  def apply(): Int = OPUS_APPLICATION_AUDIO
}

object LowDelay extends Application {
  def apply(): Int = OPUS_APPLICATION_RESTRICTED_LOWDELAY
}

