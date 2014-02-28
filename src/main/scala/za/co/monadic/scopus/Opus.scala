package za.co.monadic.scopus
import org.opuscodec.OpusLibrary._
/**
 *
 */
abstract class Opus {

  def errorString(error: Int) : String = error match {
    case OPUS_BAD_ARG => "One or more invalid/out of range arguments"
    case OPUS_BUFFER_TOO_SMALL => "The mode struct passed is invalid"
    case OPUS_INTERNAL_ERROR => "An internal error was detected"
    case OPUS_INVALID_PACKET => "The compressed data passed is corrupted"
    case OPUS_UNIMPLEMENTED => "Invalid/unsupported request number"
    case OPUS_INVALID_STATE => "An encoder or decoder structure is invalid or already freed"
    case OPUS_ALLOC_FAIL => "Memory allocation has failed"
  }
}
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

