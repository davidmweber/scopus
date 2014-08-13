package za.co.monadic.scopus.echo

import za.co.monadic.scopus.speex.Speex._
/**
 * Wrapper for the Speex echo canceller function
 */
class EchoCanceller(frameSize: Int, filterLength: Int) {

  var clean = false

  /**
   * Release all pointers allocated for the decoder. Make every attempt to call this
   * when you are done with the encoder as finalise() is what it is in the JVM
   */
  def cleanup(): Unit = {
    if (!clean) {
      echo_state_destroy(state)
      clean = true
    }
  }

  final override def finalize() = {
    cleanup()
  }


  val state = echo_state_init(frameSize,filterLength)
  /**
   * Call this every time a voice packet is played to the speakers. Use
   * this call in conjunction with the @see capture call if the
   * sound card read and writes (record and playback) are in different threads.
   * @param audio Audio that has just been or about to be played
   */
  def playback(audio: Array[Short]): Unit = {
    echo_playback(state, audio)
  }

  /**
   * Captured audio should be passed to this function after it has been
   * recorded from the sound card. Use this call in conjunction with
   * the @see playback call if the sound card read and writes (record
   * and playback) are in different threads.
   * @param recorded The recorded audio buffer
   * @return The audio with cancelled echo.
   */
  def capture(recorded: Array[Short]): Array[Short] = {
    val out = new Array[Short](recorded.length)
    echo_capture(state,recorded,out)
    out
  }

  /**
   * If recording and playback are synchronised, use this call to cancel the
   * echos.
   * @param rec Recorded audio frame
   * @param play Audio destined for play back
   * @return Echo cancelled recorded audio packet
   */
  def cancel(rec: Array[Short], play: Array[Short] ): Array[Short] = {
    val out = new Array[Short](rec.length)
    echo_cancellation(state,rec,play,out)
    out
  }

}
