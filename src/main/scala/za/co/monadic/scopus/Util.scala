/*
 * Copyright David Weber 2014
 * Released under the Creative Commons License (http://creativecommons.org/licenses/by/4.0/legalcode)
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
