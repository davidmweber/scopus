
import org.bridj.Pointer
import za.co.monadic.scopus.Opus
import za.co.monadic.scopus.OpusLibrary._

class Encoder(Fs:Int, channels:Int) extends Opus {

  val error : Pointer[Integer] = Pointer.allocateInts(1)
  val encoder = opus_encoder_create(Fs,channels,OPUS_APPLICATION_VOIP,error)
  if (error.get() != OPUS_OK) {
    throw new RuntimeException(s"Failed to create the Opus encoder: ${errorString(error.get())}")
  }
  val ret = opus_encoder_init(encoder,Fs,channels,OPUS_APPLICATION_VOIP)
  if (ret != OPUS_OK) {
    throw new RuntimeException(s"Failed to initialise the Opus encoder")
  }

  def encode(audio: Array[Short] ): Array[Byte] = {
    val encoded = new Array[Byte](2048)
    val inPtr = Pointer.pointerToArray[java.lang.Short](audio)
    val outPtr = Pointer.pointerToArray[java.lang.Byte](encoded)
    val len = opus_encode(encoder,inPtr,audio.length,outPtr,2048)
    encoded.slice(0,len-1)
  }

  def reset = opus_encoder_ctl(encoder, OPUS_RESET_STATE)

  private def setter(command: Integer, parameter: Integer): Unit = {
    assert(command %2 == 0) // Setter commands are even
    val err = opus_encoder_ctl(encoder, command, parameter)
    if (err != OPUS_OK) throw new RuntimeException(s"opus_encoder_ctl setter failed for command $command: ${errorString(err)}")
  }

  private def getter(command: Int) : Int = {
    assert(command %2 == 1) // Getter commands are even
    val result: Integer = 0
    val err: Int = opus_encoder_ctl(encoder,command,Pointer.pointerToInt(result))
    if (err != OPUS_OK) throw new RuntimeException(s"opus_encoder_ctl getter failed for command $command: ${errorString(err)}")
    result
  }

  def setComplexity(complexity: Integer) = setter(OPUS_SET_COMPLEXITY_REQUEST, complexity)
  def setBitRate(bitRate: Integer) =   opus_encoder_ctl(encoder, OPUS_SET_BITRATE_REQUEST, bitRate)
  def setVbr(useVbr: Integer) = opus_encoder_ctl(encoder, OPUS_SET_VBR_REQUEST, useVbr)
  def setVbrConstraint(cvbr: Integer) = opus_encoder_ctl(encoder, OPUS_SET_VBR_CONSTRAINT_REQUEST, cvbr)
  def setForceChannels(forceChannels: Integer) = opus_encoder_ctl(encoder, OPUS_SET_FORCE_CHANNELS_REQUEST, forceChannels)
  def setMaxBandwidth(bandwidth: Integer) =  opus_encoder_ctl(encoder, OPUS_SET_MAX_BANDWIDTH_REQUEST, bandwidth)
  def setBandWidth(bandwidth: Integer) = opus_encoder_ctl(encoder, OPUS_SET_BANDWIDTH_REQUEST, bandwidth) // No getter
  def setSignal(signal: Integer) = opus_encoder_ctl(encoder, OPUS_SET_SIGNAL_REQUEST, signal)
  def setApplication(appl: Integer) = opus_encoder_ctl(encoder,OPUS_SET_APPLICATION_REQUEST, appl)
  def setInbandFec(useInbandFec: Integer) = opus_encoder_ctl(encoder, OPUS_SET_INBAND_FEC_REQUEST, useInbandFec)
  def setPacketLossPerc(packetLossPerc: Integer) = opus_encoder_ctl(encoder, OPUS_SET_PACKET_LOSS_PERC_REQUEST, packetLossPerc)
  def setUseDtx(useDtx: Integer) = opus_encoder_ctl(encoder, OPUS_SET_DTX_REQUEST, useDtx)
  def setLsbDepth( depth: Integer = 16) = opus_encoder_ctl(encoder, OPUS_SET_LSB_DEPTH_REQUEST, depth)
  def setExpertFrameDuration(duration: Integer) = opus_encoder_ctl(encoder, OPUS_SET_EXPERT_FRAME_DURATION_REQUEST, duration)
  def setPredictionDisable(disable: Integer) = opus_encoder_ctl(encoder,OPUS_SET_PREDICTION_DISABLED_REQUEST, disable)

  def getComplexity = getter(OPUS_GET_COMPLEXITY_REQUEST)
  def getBitRate =   getter(OPUS_GET_BITRATE_REQUEST)
  def getVbr = getter(OPUS_GET_VBR_REQUEST)
  def getVbrConstraint = getter(OPUS_GET_VBR_CONSTRAINT_REQUEST)
  def getForceChannels = getter(OPUS_GET_FORCE_CHANNELS_REQUEST)
  def getMaxBandwidth = getter(OPUS_GET_MAX_BANDWIDTH_REQUEST)
  def getBandwidth = getter(OPUS_GET_BANDWIDTH_REQUEST)
  def getSignal = getter(OPUS_GET_SIGNAL_REQUEST)
  def getApplication = getter(OPUS_GET_APPLICATION_REQUEST)
  def getSampleRate = getter(OPUS_GET_SAMPLE_RATE_REQUEST)// Decoder also
  def getLookahead = getter(OPUS_GET_LOOKAHEAD_REQUEST) // Decoder also
  def getInbandFec = getter(OPUS_GET_INBAND_FEC_REQUEST)
  def getPacketLossPerc = getter(OPUS_GET_PACKET_LOSS_PERC_REQUEST)
  def getUseDtx = getter(OPUS_GET_DTX_REQUEST)
  def getLsbDepth = getter(OPUS_GET_LSB_DEPTH_REQUEST)
  def getExpertFrameDuration = getter(OPUS_GET_EXPERT_FRAME_DURATION_REQUEST)
  def getPredictionDisable = getter(OPUS_GET_PREDICTION_DISABLED_REQUEST)


  override def finalize() {
    opus_encoder_destroy(encoder)
  }

}


