package za.co.monadic.scopus.echo

/**
 * Wrapper for the Speex echo canceller function
 */
trait EchoCanceller {

  /**
   * Call this every time a voice packet is played to the speakers. Use
   * this call in conjunction with the @see capture call if the
   * sound card read and writes (record and playback) are in different threads.
   * @param audio Audio that has just been or about to be played
   */
  def playback(audio: Array[Short])

  /**
   * Captured audio should be passed to this function after it has been
   * recorded from the sound card. Use this call in conjunction with
   * the @see playback call if the sound card read and writes (record
   * and playback) are in different threads.
   * @param recorded The recorded audio buffer
   * @return The audio with cancelled echo.
   */
  def capture(recorded: Array[Short]): Array[Short]

  /**
   * If recording and playback are synchronised, use this call to cancel the
   * echos.
   * @param input Recorded audio frame
   * @param output Audio destined for play back
   * @return Echo cancelled recorded audio packet
   */
  def cancel(input: Array[Short], output: Array[Short] ): Array[Short]

}
