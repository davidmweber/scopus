/*
 * Copyright David Weber 2014
 * Released under the Creative Commons License (http://creativecommons.org/licenses/by/4.0/legalcode)
 */
package za.co.monadic.scopus

import za.co.monadic.scopus.Opus._
/**
 * Base class for supported sample frequencies
 */
sealed trait SampleFrequency {
  def apply(): Int
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

object Sf48000 extends SampleFrequency {
  def apply(): Int = 48000
}

sealed trait Application {
  def apply(): Int
}

object Voip extends Application {
  def apply() = OPUS_APPLICATION_VOIP
}

object Audio extends Application {
  def apply() = OPUS_APPLICATION_AUDIO
}

object LowDelay extends Application {
  def apply() = OPUS_APPLICATION_RESTRICTED_LOWDELAY
}


