package za.co.monadic.scopus
import za.co.monadic.scopus.OpusLibrary._
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
