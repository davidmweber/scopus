/*
 * Copyright 2020 David Weber
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

package za.co.monadic.scopus

object FreqUtils {
  import Numeric.Implicits._

  def sqr[A: Numeric](a: A): A = a * a

  /**
    * Measure the amplitude of a sine wave at the specified frequency. This is effectively a DFT
    * computed for a single frequency and calculating the power of the result.
    *
    * @param x          The signal to test
    * @param freq       The frequency to test at
    * @param sampleFreq Sampling frequency used for the signal above
    * @return
    */
  def getAmplitude(x: Array[Float], freq: Double, sampleFreq: SampleFrequency): Double = {
    var re      = 0.0
    var im      = 0.0
    val twiddle = 2.0 * math.Pi * freq / sampleFreq()
    for (idx <- x.indices) {
      re += x(idx) * math.sin(twiddle * idx)
      im += x(idx) * math.cos(twiddle * idx)
    }
    2.0 * Math.sqrt(sqr(re / x.length) + sqr(im / x.length)) // E = 0.5*A**2 so A = 2*sqrt(E) and E = re**2 + im**2
  }

  /**
    * Choose a frequency free of window effects to facilitate accurate testing.
    * In order to remove window effects, we need a whole number of cycles in
    * one buffer. f = M*Fs/N where N is the buffer length, M is an integer and
    * Fs is the sample frequency. If you are confused, go study Fourier theory
    * for a bit :)
    *
    * @param M          The number of whole cycles to fit into a buffer
    * @param len        The length of the buffer to fill with a whole number of cycles
    * @param sampleFreq The sample frequency to use
    * @return A frequency in Hz
    */
  def pickFreq(M: Int, len: Int, sampleFreq: SampleFrequency): Float = {
    M * sampleFreq() / len.toFloat
  }

  /**
    * Generates a sine wave of amplitude one at the specified frequency
    *
    * @param freq       Generate a signal at this frequency
    * @param len        Number of samples to generate
    * @param sampleFreq The sampling frequency
    */
  def genSine(freq: Float, len: Int, sampleFreq: SampleFrequency): Array[Float] = {
    val buffer = new Array[Float](len)
    for (idx <- buffer.indices) {
      buffer(idx) = math.sin(2.0 * math.Pi * idx * freq / sampleFreq()).toFloat
    }
    buffer
  }

}
