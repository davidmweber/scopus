package za.co.monadic.scopus.speex

import za.co.monadic.scopus.speex.Speex._
import za.co.monadic.scopus.{SampleFrequency, DecoderFloat, DecoderShort}

import scala.util.{Success, Failure, Try}

sealed trait SpeexBase {

  val fs: SampleFrequency
  val enhance: Boolean
  val channels = 1

  val en = if (enhance) 1 else 0
  val decoder = decoder_create(getMode(fs), en)
  if (decoder <= 0) throw new RuntimeException("Failed to create Speex decoder state")
  var clean = false

  def reset() = decoder_ctl(decoder,SPEEX_RESET_STATE,0)

  def getSampleRate = decoder_ctl(decoder,SPEEX_GET_SAMPLING_RATE,0)

  /**
   * Something odd occurs in the JVM so we get occasional requests to delete
   * state with a zero pointer. This implies finalize is called on this object
   * after the "decoder" attribute has been set to zero. Ugly.
   */
  def cleanup() = {
    if (decoder == 0) System.err.println("Zero pointer encountered when cleaning SpeexDecoder state")
    if (!clean && (decoder != 0)) {
      decoder_destroy(decoder)
      clean = true
    }
  }
}

class SpeexDecoderShort(val fs: SampleFrequency, val enhance: Boolean) extends SpeexBase with DecoderShort {


  val decodedBuf = new Array[Short](1024)
  /**
   * Decode an audio packet to an array of Shorts
   * @param compressedAudio The incoming audio packet
   * @return A Try containing decoded audio in Short format
   */
  override def apply(compressedAudio: Array[Byte]): Try[Array[Short]] = {
    val len = decode_short(decoder, compressedAudio, compressedAudio.length, decodedBuf, 1024)
    if (len < 0)
      Failure(new RuntimeException(s"speex_decode() failed}"))
    else
      Success(decodedBuf.slice(0, len))
  }

  /**
   * Decode an erased (i.e. not received) audio packet. Note you need to specify
   * how many samples you think you have lost so the decoder can attempt to
   * deal with the erasure appropriately.
   * @return A Try containing decompressed audio in Float format
   */
  override def apply(count: Int): Try[Array[Short]] = {
    val len = decode_short(decoder,null, 0, decodedBuf, 1024)
    if (len < 0)
      Failure(new RuntimeException(s"speex_decode() failed}"))
    else
      Success(decodedBuf.slice(0, len))
  }

  def getDetail = s"Speex decoder to `short' with sf= ${fs()}"

}

object SpeexDecoderShort {
  /**
   * Decode to short
   * @param sampleFreq The required sampling frequency for the decoder
   * @param enhance If true, apply some audio enhancement to the decoded signal
   * @return An instance of a Speex decoder
   */
  def apply(sampleFreq: SampleFrequency, enhance: Boolean = false) = new SpeexDecoderShort(sampleFreq,enhance)
}

class SpeexDecoderFloat(val fs: SampleFrequency, val enhance: Boolean) extends SpeexBase with DecoderFloat {

  val decodedBuf = new Array[Float](1024)
  /**
   * Decode an audio packet to an array of Floats
   * @param compressedAudio The incoming audio packet
   * @return A Try containing the decoded audio packet in Float format
   */
  override def apply(compressedAudio: Array[Byte]): Try[Array[Float]] = {
    val len = decode_float(decoder, compressedAudio, compressedAudio.length, decodedBuf, 1024)
    if (len < 0)
      Failure(new RuntimeException(s"speex_decode() failed}"))
    else
      Success(decodedBuf.slice(0, len))
  }

  /**
   * Decode an erased (i.e. not received) audio packet. Note you need to specify
   * how many samples you think you have lost so the decoder can attempt to
   * deal with the erasure appropriately.
   * @return A Try containing decompressed audio in Float format
   */
  override def apply(count: Int): Try[Array[Float]] = {
    val len = decode_float(decoder,null, 0, decodedBuf, 1024)
    if (len < 0)
      Failure(new RuntimeException(s"speex_decode()short failed}"))
    else
      Success(decodedBuf.slice(0, len))
  }

  def getDetail = s"Speex decoder to `float' with sf= ${fs()}"

}

object SpeexDecoderFloat {
  /**
   * Decode to float
   * @param sampleFreq The required sampling frequency for the decoder
   * @param enhance If true, apply some audio enhancement to the decoded signal
   * @return An instance of a Speex decoder
   */
  def apply(sampleFreq: SampleFrequency, enhance: Boolean = false) = new SpeexDecoderFloat(sampleFreq,enhance)
}
