/*
 * Copyright 2014 David Weber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.monadic.scopus.echo

import za.co.monadic.scopus.speex.Speex._

/**
  * Wrapper for the Speex echo canceller function. Note that the playback and capture
  * methods are designed to run in separate threads, hence the synchronization on
  * this. If you use playback/capture with cancel, you deserve what you get.
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

  final override def finalize(): Unit = {
    cleanup()
  }

  private val state = echo_state_init(frameSize, filterLength)

  /**
    * Call this every time a voice packet is played to the speakers. Use
    * this call in conjunction with the @see capture call if the
    * sound card read and writes (record and playback) are in different threads.
    *
    * Note that @see capture and @see playback are not thread safe.
    *
    * @param audio Audio that has just been or about to be played
    */
  def playback(audio: Array[Short]): Unit = {
    this.synchronized(echo_playback(state, audio))
  }

  /**
    * Captured audio should be passed to this function after it has been
    * recorded from the sound card. Use this call in conjunction with
    * the @see playback call if the sound card read and writes (record
    * and playback) are in different threads.
    *
    * Note that @see capture and @see playback are not thread safe.
    *
    * @param recorded The recorded audio buffer
    * @return The audio with cancelled echo.
    */
  def capture(recorded: Array[Short]): Array[Short] = {
    val out = new Array[Short](recorded.length)
    this.synchronized(echo_capture(state, recorded, out))
    out
  }

  /**
    * If recording and playback are synchronised, use this call to cancel the
    * echos.
    * @param rec Recorded audio frame
    * @param play Audio destined for play back
    * @return Echo cancelled recorded audio packet
    */
  def cancel(rec: Array[Short], play: Array[Short]): Array[Short] = {
    val out = new Array[Short](rec.length)
    echo_cancellation(state, rec, play, out)
    out
  }

}
