/*
 * Copyright David Weber 2014
 * Released under the Creative Commons License (http://creativecommons.org/licenses/by/4.0/legalcode)
 */
package za.co.monadic.scopus.opus

import Opus._
import za.co.monadic.scopus.{Encoder, Application, SampleFrequency, Voip}

import scala.util.{Failure, Success, Try}

/**
 * Wrapper around the Opus codec's encoder subsystem.
 * The C interface is documented at [[http://www.opus-codec.org]] and should
 * be considered definitive. The encoder accepts buffers of duration of
 * 2,5, 5, 10, 20, 40 or 60ms. To calculate the buffer size, multiply your sample
 * frequency by the frame duration. At 8kHz a 20ms packet is 160 samples long.
 * @param sampleFreq The required sampling frequency
 * @param channels The number of channels you intend to encode.
 * @param app The application (Voip, Audio or LowDelay)
 * @param bufferSize The reserved size of the buffer to which compressed data are written.
 *                   The default should be more than sufficient
 */
class OpusEncoder(sampleFreq: SampleFrequency, channels: Int, app: Application, bufferSize: Int = 8192) extends Encoder {
  require(bufferSize > 0, "Buffer size must be positive")
  val error = Array[Int](0)
  val decodePtr = new Array[Byte](bufferSize)
  val encoder = encoder_create(sampleFreq(),channels,  app(), error)
  if (error(0) != OPUS_OK) {
    throw new RuntimeException(s"Failed to create the Opus encoder: ${error_string(error(0))}")
  }
  var clean = false

  def getDetail = s"Opus encoder with sf= ${sampleFreq()}"

  /**
   * Encode a block of raw audio  in integer format using the configured encoder
   * @param audio Audio data arranged as a contiguous block interleaved array of short integers
   * @return An array containing the compressed audio or the exception in case of a failure
   */
  def apply(audio: Array[Short]): Try[Array[Byte]] = {
    val len: Int = encode_short(encoder, audio, audio.length, decodePtr, bufferSize)
    if (len < 0)
      Failure(new IllegalArgumentException(s"opus_encode() failed: ${error_string(len)}"))
    else
      Success(decodePtr.slice(0, len))
  }

  /**
   * Encode a block of raw audio  in float format using the configured encoder
   * @param audio Audio data arranged as a contiguous block interleaved array of floats
   * @return An array containing the compressed audio or the exception in case of a failure
   */
  def apply(audio: Array[Float]): Try[Array[Byte]] = {
    val len = encode_float(encoder, audio, audio.length, decodePtr, bufferSize)
    if (len < 0)
      Failure(new RuntimeException(s"opus_encode_float() failed: ${error_string(len)}"))
    else
      Success(decodePtr.slice(0, len))
  }

  /**
   * Release all pointers allocated for the encoder. Make every attempt to call this
   * when you are done with the encoder as finalise() is what it is in the JVM
   */
  def cleanup() = {
    println(s"***** Cleaning opus encoder $encoder")
    if (!clean) {
      encoder_destroy(encoder)
      clean = true
    }
  }

  def reset = encoder_set_ctl(encoder, OPUS_RESET_STATE, 0)

  private def setter(command: Integer, parameter: Integer): Unit = {
    assert(command % 2 == 0) // Setter commands are even
    val err = encoder_set_ctl(encoder, command, parameter)
    if (err != OPUS_OK) throw new RuntimeException(s"opus_encoder_ctl setter failed for command $command: ${error_string(err)}")
  }

  private def getter(command: Int): Int = {
    assert(command % 2 == 1) // Getter commands are all odd
    val result = Array[Int](0)
    val err: Int = encoder_get_ctl(encoder, command, result)
    if (err != OPUS_OK) throw new RuntimeException(s"opus_encoder_ctl getter failed for command $command: ${error_string(err)}")
    result(0)
  }
  /**
   * Set the complexity of the encoder. This has no effect if the encoder does not support
   * complexity settings
   * @param c A value between 0 and 10 indicating the encoder complexity.
   * @return A reference to the updated encoder
   */
  override def complexity(c: Int): Encoder = {
    require((c >= 0) && (c <= 10))
    setComplexity(c)
    this
  }

  def setComplexity(complexity: Integer) = setter(OPUS_SET_COMPLEXITY_REQUEST, complexity)

  def setBitRate(bitRate: Integer) = setter(OPUS_SET_BITRATE_REQUEST, bitRate)

  def setVbr(useVbr: Integer) = setter(OPUS_SET_VBR_REQUEST, useVbr)

  def setVbrConstraint(cvbr: Integer) = setter(OPUS_SET_VBR_CONSTRAINT_REQUEST, cvbr)

  def setForceChannels(forceChannels: Integer) = setter(OPUS_SET_FORCE_CHANNELS_REQUEST, forceChannels)

  def setMaxBandwidth(bandwidth: Integer) = setter(OPUS_SET_MAX_BANDWIDTH_REQUEST, bandwidth)

  def setBandWidth(bandwidth: Integer) = setter(OPUS_SET_BANDWIDTH_REQUEST, bandwidth)

  def setSignal(signal: Integer) = setter(OPUS_SET_SIGNAL_REQUEST, signal)

  def setApplication(appl: Integer) = setter(OPUS_SET_APPLICATION_REQUEST, appl)

  def setInbandFec(useInbandFec: Integer) = setter(OPUS_SET_INBAND_FEC_REQUEST, useInbandFec)

  def setPacketLossPerc(packetLossPerc: Integer) = setter(OPUS_SET_PACKET_LOSS_PERC_REQUEST, packetLossPerc)

  def setUseDtx(useDtx: Integer) = setter(OPUS_SET_DTX_REQUEST, useDtx)

  def setLsbDepth(depth: Integer = 16) = setter(OPUS_SET_LSB_DEPTH_REQUEST, depth)

  def setExpertFrameDuration(duration: Integer) = setter(OPUS_SET_EXPERT_FRAME_DURATION_REQUEST, duration)

  def setPredictionDisable(disable: Integer) = setter(OPUS_SET_PREDICTION_DISABLED_REQUEST, disable)

  def getComplexity = getter(OPUS_GET_COMPLEXITY_REQUEST)

  def getBitRate = getter(OPUS_GET_BITRATE_REQUEST)

  def getVbr = getter(OPUS_GET_VBR_REQUEST)

  def getVbrConstraint = getter(OPUS_GET_VBR_CONSTRAINT_REQUEST)

  def getForceChannels = getter(OPUS_GET_FORCE_CHANNELS_REQUEST)

  def getMaxBandwidth = getter(OPUS_GET_MAX_BANDWIDTH_REQUEST)

  def getBandwidth = getter(OPUS_GET_BANDWIDTH_REQUEST)

  def getSignal = getter(OPUS_GET_SIGNAL_REQUEST)

  def getApplication = getter(OPUS_GET_APPLICATION_REQUEST)

  def getSampleRate = getter(OPUS_GET_SAMPLE_RATE_REQUEST)

  def getLookahead = getter(OPUS_GET_LOOKAHEAD_REQUEST)

  def getInbandFec = getter(OPUS_GET_INBAND_FEC_REQUEST)

  def getPacketLossPerc = getter(OPUS_GET_PACKET_LOSS_PERC_REQUEST)

  def getUseDtx = getter(OPUS_GET_DTX_REQUEST)

  def getLsbDepth = getter(OPUS_GET_LSB_DEPTH_REQUEST)

  def getExpertFrameDuration = getter(OPUS_GET_EXPERT_FRAME_DURATION_REQUEST)

  def getPredictionDisable = getter(OPUS_GET_PREDICTION_DISABLED_REQUEST)

}

object OpusEncoder {
  /**
   * Factory for an encoder instance.
   * @param sampleFreq THe required sampling frequency
   * @param channels The number of channels you intend to encode.
   * @param app The application (Voip, Audio or LowDelay). It defaults to Voip.
   * @param bufferSize The reserved size of the buffer to which compressed data are written.
   *                   The default should be more than sufficient
   * @return A Try[Array[Byte]) containing a reference to the encoder object
   */
  def apply(sampleFreq: SampleFrequency, channels: Int, app: Application = Voip, bufferSize: Int = 8192) =
    new OpusEncoder(sampleFreq,channels, app, bufferSize)
}
