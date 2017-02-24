package za.co.monadic.scopus.speex

import za.co.monadic.scopus._

/**
  *
  */
object Speex {

  def getMode(sf: SampleFrequency) = sf match {
    case Sf8000             => SPEEX_MODEID_NB
    case Sf16000            => SPEEX_MODEID_WB
    case Sf32000            => SPEEX_MODEID_UWB
    case s: SampleFrequency => throw new RuntimeException(s"Invalid sampling frequency ($s) for the Speex codec")
  }

  // Ensures that the libraries are loaded as Scala only initialises objects
  // if they are called...
  Libraries()

  // These map closely to the speex C library functions. Consult the speex documentation for more details
  @native
  def encoder_create(mode: Int): Long

  // These map closely to the speex C library functions. Consult the speex documentation for more details
  @native
  def encoder_destroy(state: Long)

  @native
  def encode_short(encoder: Long, input: Array[Short], inSize: Int, output: Array[Byte], outSize: Int): Int

  @native
  def encode_float(encoder: Long, input: Array[Float], inSize: Int, output: Array[Byte], outSize: Int): Int

  @native
  def get_version_string(): String

  /**
    * Create a decoder state. This differs from the C API because we set up both the encoder and
    * the "Bits" structure.
    * @param mode Speex mode
    * @param enhance 1 to enable perceptual enhancement. 0 otherwise.
    * @return
    */
  @native
  def decoder_create(mode: Int, enhance: Int): Long

  @native
  def decoder_destroy(state: Long)

  @native
  def decode_short(decoder: Long, input: Array[Byte], inSize: Int, output: Array[Short], outSize: Int): Int

  @native
  def decode_float(decoder: Long, input: Array[Byte], inSize: Int, output: Array[Float], outSize: Int): Int

  @native
  def encoder_ctl(encoder: Long, command: Int, value: Int): Int

  @native
  def decoder_ctl(decoder: Long, command: Int, value: Int): Int

  // Echo canceller API:
  @native
  def echo_state_init(frame_size: Int, filter_length: Int): Long

  @native
  def echo_state_destroy(SpeexEchoState: Long)

  @native
  def echo_cancellation(SpeexEchoState: Long, rec: Array[Short], play: Array[Short], out: Array[Short])

  @native
  def echo_capture(SpeexEchoState: Long, rec: Array[Short], out: Array[Short])

  @native
  def echo_playback(SpeexEchoState: Long, play: Array[Short])

  @native
  def echo_state_reset(SpeexEchoState: Long)

  @native
  def echo_ctl(SpeexEchoState: Long, request: Int, ptr: Long): Int

  /** Set enhancement on/off (decoder only) */
  final val SPEEX_SET_ENH = 0

  /** Get enhancement state (decoder only) */
  final val SPEEX_GET_ENH = 1

  /** Obtain frame size used by encoder/decoder */
  final val SPEEX_GET_FRAME_SIZE = 3

  /** Set quality value */
  final val SPEEX_SET_QUALITY = 4

  /** Get current quality setting */
  /** Set sub-mode to use */
  final val SPEEX_SET_MODE = 6

  /** Get current sub-mode in use */
  final val SPEEX_GET_MODE = 7

  /** Set low-band sub-mode to use (wideband only) */
  final val SPEEX_SET_LOW_MODE = 8

  /** Get current low-band mode in use (wideband only) */
  final val SPEEX_GET_LOW_MODE = 9

  /** Set high-band sub-mode to use (wideband only) */
  final val SPEEX_SET_HIGH_MODE = 10

  /** Get current high-band mode in use (wideband only) */
  final val SPEEX_GET_HIGH_MODE = 11

  /** Set VBR on (1) or off (0) */
  final val SPEEX_SET_VBR = 12

  /** Get VBR status (1 for on, 0 for off) */
  final val SPEEX_GET_VBR = 13

  /** Set quality value for VBR encoding (0-10) */
  final val SPEEX_SET_VBR_QUALITY = 14

  /** Get current quality value for VBR encoding (0-10) */
  final val SPEEX_GET_VBR_QUALITY = 15

  /** Set complexity of the encoder (0-10) */
  final val SPEEX_SET_COMPLEXITY = 16

  /** Get current complexity of the encoder (0-10) */
  final val SPEEX_GET_COMPLEXITY = 17

  /** Set bit-rate used by the encoder (or lower) */
  final val SPEEX_SET_BITRATE = 18

  /** Get current bit-rate used by the encoder or decoder */
  final val SPEEX_GET_BITRATE = 19

  /** Define a handler function for in-band Speex request */
  final val SPEEX_SET_HANDLER = 20

  /** Define a handler function for in-band user-defined request */
  final val SPEEX_SET_USER_HANDLER = 22

  /** Set sampling rate used in bit-rate computation */
  final val SPEEX_SET_SAMPLING_RATE = 24

  /** Get sampling rate used in bit-rate computation */
  final val SPEEX_GET_SAMPLING_RATE = 25

  /** Reset the encoder/decoder memories to zero */
  final val SPEEX_RESET_STATE = 26

  /** Get VBR info (mostly used internally) */
  final val SPEEX_GET_RELATIVE_QUALITY = 29

  /** Set VAD status (1 for on, 0 for off) */
  final val SPEEX_SET_VAD = 30

  /** Get VAD status (1 for on, 0 for off) */
  final val SPEEX_GET_VAD = 31

  /** Set Average Bit-Rate (ABR) to n bits per seconds */
  final val SPEEX_SET_ABR = 32

  /** Get Average Bit-Rate (ABR) setting (in bps) */
  final val SPEEX_GET_ABR = 33

  /** Set DTX status (1 for on, 0 for off) */
  final val SPEEX_SET_DTX = 34

  /** Get DTX status (1 for on, 0 for off) */
  final val SPEEX_GET_DTX = 35

  /** Set submode encoding in each frame (1 for yes, 0 for no, setting to no breaks the standard) */
  final val SPEEX_SET_SUBMODE_ENCODING = 36

  /** Get submode encoding in each frame */
  final val SPEEX_GET_SUBMODE_ENCODING = 37

  /** Returns the lookahead used by Speex */
  final val SPEEX_GET_LOOKAHEAD = 39

  /** Sets tuning for packet-loss concealment (expected loss rate) */
  final val SPEEX_SET_PLC_TUNING = 40

  /** Gets tuning for PLC */
  final val SPEEX_GET_PLC_TUNING = 41

  /** Sets the max bit-rate allowed in VBR mode */
  final val SPEEX_SET_VBR_MAX_BITRATE = 42

  /** Gets the max bit-rate allowed in VBR mode */
  final val SPEEX_GET_VBR_MAX_BITRATE = 43

  /** Turn on/off input/output high-pass filtering */
  final val SPEEX_SET_HIGHPASS = 44

  /** Get status of input/output high-pass filtering */
  final val SPEEX_GET_HIGHPASS = 45

  /** Get "activity level" of the last decoded frame, i.e.
    how much damage we cause if we remove the frame */
  final val SPEEX_GET_ACTIVITY = 47

  /* Preserving compatibility:*/
  /** Equivalent to SPEEX_SET_ENH */
  final val SPEEX_SET_PF = 0

  /** Equivalent to SPEEX_GET_ENH */
  final val SPEEX_GET_PF = 1

  /** Query the frame size of a mode */
  final val SPEEX_MODE_FRAME_SIZE = 0

  /** Query the size of an encoded frame for a particular sub-mode */
  final val SPEEX_SUBMODE_BITS_PER_FRAME = 1

  /** Get major Speex version */
  final val SPEEX_LIB_GET_MAJOR_VERSION = 1

  /** Get minor Speex version */
  final val SPEEX_LIB_GET_MINOR_VERSION = 3

  /** Get micro Speex version */
  final val SPEEX_LIB_GET_MICRO_VERSION = 5

  /** Get extra Speex version */
  final val SPEEX_LIB_GET_EXTRA_VERSION = 7

  /** Get Speex version string */
  final val SPEEX_LIB_GET_VERSION_STRING = 9

  /** Number of defined modes in Speex */
  final val SPEEX_NB_MODES = 3

  /** modeID for the defined narrowband mode */
  final val SPEEX_MODEID_NB = 0

  /** modeID for the defined wideband mode */
  final val SPEEX_MODEID_WB = 1

  /** modeID for the defined ultra-wideband mode */
  final val SPEEX_MODEID_UWB = 2

  /** Obtain frame size used by the AEC */
  final val SPEEX_ECHO_GET_FRAME_SIZE = 3

  /** Set sampling rate */
  final val SPEEX_ECHO_SET_SAMPLING_RATE = 24

  /** Get sampling rate */
  final val SPEEX_ECHO_GET_SAMPLING_RATE = 25

  /* Can't set window sizes */
  /** Get size of impulse response (int32) */
  final val SPEEX_ECHO_GET_IMPULSE_RESPONSE_SIZE = 27

  /* Can't set window content */
  /** Get impulse response (int32[]) */
  final val SPEEX_ECHO_GET_IMPULSE_RESPONSE = 29

}
