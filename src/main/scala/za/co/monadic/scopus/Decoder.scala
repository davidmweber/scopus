package za.co.monadic.scopus

import org.opuscodec.OpusLibrary._
import org.bridj.Pointer


class Decoder(Fs:Int, channels:Int) extends Opus {

  val bufferLen: Int = math.round(0.120f*Fs*channels)
  var fec = 1
  val nullBytePtr = 0.asInstanceOf[Pointer[java.lang.Byte]]
  val error : Pointer[Integer] = Pointer.allocateInts(1)
  val decoder = opus_decoder_create(Fs, channels, error)


  if (error.get() != OPUS_OK) {
    throw new RuntimeException(s"Failed to create the Opus encoder: ${errorString(error.get())}")
  }

  val ret = opus_decoder_init(decoder,Fs,channels)
  if (ret != OPUS_OK) {
    throw new RuntimeException(s"Failed to initialise the Opus encoder")
  }

  /**
   * Decode an audio packet to an array of Shorts
   * @param compressedAudio The incoming audio packet
   * @return Decoded audio packet
   */
  def decode(compressedAudio: Array[Byte] ): Array[Short] = {
    val decoded = new Array[Short](bufferLen)
    val inPtr = Pointer.pointerToArray[java.lang.Byte](compressedAudio)
    val outPtr = Pointer.pointerToArray[java.lang.Short](decoded)
    val len = opus_decode(decoder,inPtr,compressedAudio.length,outPtr,bufferLen, fec)
    if (len < 0) throw new RuntimeException(s"opus_encode failed: ${errorString(len)}")
    decoded.slice(0,len-1)
  }

  /**
   * Decode an erased (i.e. not received) audio packet
   * @return The decompressed audio for this packet
   */
  def decode(): Array[Short] = {
    val decoded = new Array[Short](bufferLen)
    val outPtr = Pointer.pointerToArray[java.lang.Short](decoded)
    val len = opus_decode(decoder,nullBytePtr,0,outPtr,bufferLen, fec)
    if (len < 0) throw new RuntimeException(s"opus_encode failed: ${errorString(len)}")
    decoded.slice(0,len-1)
  }


  /**
   * Decode an audio packet to an array of Floats
   * @param compressedAudio The incoming audio packet
   * @return Decoded audio packet
   */
  def decodeFloat(compressedAudio: Array[Byte] ): Array[Float] = {
    val decoded = new Array[Float](bufferLen)
    val inPtr = Pointer.pointerToArray[java.lang.Byte](compressedAudio)
    val outPtr = Pointer.pointerToArray[java.lang.Float](decoded)
    val len = opus_decode_float(decoder,inPtr,compressedAudio.length,outPtr, bufferLen, fec)
    if (len < 0) throw new RuntimeException(s"opus_encode failed: ${errorString(len)}")
    decoded.slice(0,len-1)
  }

  /**
   * Decode an erased (i.e. not received) audio packet
   * @return The decompressed audio for this packet
   */
  def decodeFloat(): Array[Float] = {
    val decoded = new Array[Float](bufferLen)
    val outPtr = Pointer.pointerToArray[java.lang.Float](decoded)
    val len = opus_decode_float(decoder, nullBytePtr,0,outPtr, bufferLen, fec)
    if (len < 0) throw new RuntimeException(s"opus_encode failed: ${errorString(len)}")
    decoded.slice(0,len-1)
  }

  private def getter(command: Int) : Int = {
    assert(command %2 == 1) // Getter commands are all odd
    val result: Integer = 0
    val err: Int = opus_decoder_ctl(decoder,command,Pointer.pointerToInt(result))
    if (err != OPUS_OK) throw new RuntimeException(s"opus_encoder_ctl failed for command $command: ${errorString(err)}")
    result
  }

  private def setter(command: Integer, parameter: Integer): Unit = {
    assert(command %2 == 0) // Setter commands are even
    val err = opus_decoder_ctl(decoder, command, parameter)
    if (err != OPUS_OK) throw new RuntimeException(s"opus_encoder_ctl setter failed for command $command: ${errorString(err)}")
  }

  def reset = opus_decoder_ctl(decoder, OPUS_RESET_STATE)

  def getSampleRate = getter(OPUS_GET_SAMPLE_RATE_REQUEST)
  def getLookahead = getter(OPUS_GET_LOOKAHEAD_REQUEST)
  def getBandwidth = getter(OPUS_GET_BANDWIDTH_REQUEST)
  def getPitch = getter(OPUS_GET_PITCH_REQUEST)
  def getGain = getter(OPUS_GET_GAIN_REQUEST)
  def getLastPacketDuration = getter(OPUS_GET_LAST_PACKET_DURATION_REQUEST)

  def setGain(gain: Int) = setter(OPUS_SET_GAIN_REQUEST,gain)

  /**
   * Custom setter for the FEC mode in the decoder
   * @param useFec If true, employ error correction if it is available in the packet
   */
  def setFec(useFec: Boolean) = {
    fec = if (useFec) 1 else 0
  }

  /**
   * Returns the current FEC decoding status.
   * @return  True if FEC is being decoded
   */
  def getFec(useFec: Boolean) = {
    fec == 1
  }

}


/*

#define OPUS_SET_COMPLEXITY(x) OPUS_SET_COMPLEXITY_REQUEST, __opus_check_int(x)
#define OPUS_GET_COMPLEXITY(x) OPUS_GET_COMPLEXITY_REQUEST, __opus_check_int_ptr(x)
#define OPUS_SET_BITRATE(x) OPUS_SET_BITRATE_REQUEST, __opus_check_int(x)
#define OPUS_GET_BITRATE(x) OPUS_GET_BITRATE_REQUEST, __opus_check_int_ptr(x)
#define OPUS_SET_VBR(x) OPUS_SET_VBR_REQUEST, __opus_check_int(x)
#define OPUS_GET_VBR(x) OPUS_GET_VBR_REQUEST, __opus_check_int_ptr(x)
#define OPUS_SET_VBR_CONSTRAINT(x) OPUS_SET_VBR_CONSTRAINT_REQUEST, __opus_check_int(x)
#define OPUS_GET_VBR_CONSTRAINT(x) OPUS_GET_VBR_CONSTRAINT_REQUEST, __opus_check_int_ptr(x)
#define OPUS_SET_FORCE_CHANNELS(x) OPUS_SET_FORCE_CHANNELS_REQUEST, __opus_check_int(x)
#define OPUS_GET_FORCE_CHANNELS(x) OPUS_GET_FORCE_CHANNELS_REQUEST, __opus_check_int_ptr(x)
#define OPUS_SET_MAX_BANDWIDTH(x) OPUS_SET_MAX_BANDWIDTH_REQUEST, __opus_check_int(x)
#define OPUS_GET_MAX_BANDWIDTH(x) OPUS_GET_MAX_BANDWIDTH_REQUEST, __opus_check_int_ptr(x)
#define OPUS_SET_BANDWIDTH(x) OPUS_SET_BANDWIDTH_REQUEST, __opus_check_int(x)

#define OPUS_SET_SIGNAL(x) OPUS_SET_SIGNAL_REQUEST, __opus_check_int(x)
#define OPUS_GET_SIGNAL(x) OPUS_GET_SIGNAL_REQUEST, __opus_check_int_ptr(x)
#define OPUS_SET_APPLICATION(x) OPUS_SET_APPLICATION_REQUEST, __opus_check_int(x)
#define OPUS_GET_APPLICATION(x) OPUS_GET_APPLICATION_REQUEST, __opus_check_int_ptr(x)

#define OPUS_GET_SAMPLE_RATE(x) OPUS_GET_SAMPLE_RATE_REQUEST, __opus_check_int_ptr(x) // Getter only. Decoder and encoder

#define OPUS_GET_LOOKAHEAD(x) OPUS_GET_LOOKAHEAD_REQUEST, __opus_check_int_ptr(x)
#define OPUS_SET_INBAND_FEC(x) OPUS_SET_INBAND_FEC_REQUEST, __opus_check_int(x)
#define OPUS_GET_INBAND_FEC(x) OPUS_GET_INBAND_FEC_REQUEST, __opus_check_int_ptr(x)
#define OPUS_SET_PACKET_LOSS_PERC(x) OPUS_SET_PACKET_LOSS_PERC_REQUEST, __opus_check_int(x)
#define OPUS_GET_PACKET_LOSS_PERC(x) OPUS_GET_PACKET_LOSS_PERC_REQUEST, __opus_check_int_ptr(x)
#define OPUS_SET_DTX(x) OPUS_SET_DTX_REQUEST, __opus_check_int(x)
#define OPUS_GET_DTX(x) OPUS_GET_DTX_REQUEST, __opus_check_int_ptr(x)
#define OPUS_SET_LSB_DEPTH(x) OPUS_SET_LSB_DEPTH_REQUEST, __opus_check_int(x)
#define OPUS_GET_LSB_DEPTH(x) OPUS_GET_LSB_DEPTH_REQUEST, __opus_check_int_ptr(x)
#define OPUS_GET_LAST_PACKET_DURATION(x) OPUS_GET_LAST_PACKET_DURATION_REQUEST, __opus_check_int_ptr(x) // Decoder only
#define OPUS_SET_EXPERT_FRAME_DURATION(x) OPUS_SET_EXPERT_FRAME_DURATION_REQUEST, __opus_check_int(x)
#define OPUS_GET_EXPERT_FRAME_DURATION(x) OPUS_GET_EXPERT_FRAME_DURATION_REQUEST, __opus_check_int_ptr(x)
#define OPUS_SET_PREDICTION_DISABLED(x) OPUS_SET_PREDICTION_DISABLED_REQUEST, __opus_check_int(x)
#define OPUS_GET_PREDICTION_DISABLED(x) OPUS_GET_PREDICTION_DISABLED_REQUEST, __opus_check_int_ptr(x)
#define OPUS_RESET_STATE 4028

#define OPUS_GET_PITCH(x) OPUS_GET_PITCH_REQUEST, __opus_check_int_ptr(x)  //Decoder
#define OPUS_GET_BANDWIDTH(x) OPUS_GET_BANDWIDTH_REQUEST, __opus_check_int_ptr(x) // Both (Getter)
#define OPUS_SET_GAIN(x) OPUS_SET_GAIN_REQUEST, __opus_check_int(x)  // Decoder
#define OPUS_GET_GAIN(x) OPUS_GET_GAIN_REQUEST, __opus_check_int_ptr(x) // Decoder

 */
