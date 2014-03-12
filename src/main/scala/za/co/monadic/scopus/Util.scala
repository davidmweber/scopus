package za.co.monadic.scopus

/**
 * Base class for supported sample frequencies
 */
abstract class SampleFrequency {
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
