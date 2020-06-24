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

package za.co.monadic.scopus.dsp

/**
  * Structure needed to define a filter
  * @param order The order of the filter
  * @param a The coefficients of the polynomial defining the poles in the Z-domain
  * @param b The coefficients of the polynomial defining the xeros in the Z-domain
  */
case class Filter(order: Int, a: Array[Float], b: Array[Float]) {
  require(order == a.length - 1 && order == b.length - 1, "Order and coefficient array sizes must be equal")
}

/**
  * Supplies Elliptical filters that are suitable as interpolation and decimation filters
  * These were generated using scipy's filter design tools. A filter for a decimation/interpolation
  * factor of 4 has a cut off frequency of 90% of the Nyquist frequency of the original sample rate
  * divided by 4.
  */
object MultirateFilterFactory {

  /**
    * Select a filter for a cut-off frequency for the envisaged decimation/interpolation rate
    * @param factor Decimation/interpolation
    * @return A Filter
    */
  def apply(factor: Int): Filter =
    factor match {
      case 2 =>
        Filter(
          6,
          Array[Float](1.0000000000e+00f, -2.2150939834e+00f, 3.6340884990e+00f, -3.6053900178e+00f, 2.5896791922e+00f,
            -1.1678192254e+00f, 2.9534451224e-01f),
          Array[Float](2.9743123894e-02f, 5.3683139827e-02f, 9.9997243561e-02f, 1.0623698403e-01f, 9.9997243561e-02f,
            5.3683139827e-02f, 2.9743123894e-02f)
        )
      case 3 =>
        Filter(
          6,
          Array[Float](1.0000000000e+00f, -3.9597283064e+00f, 7.5464419229e+00f, -8.4777972935e+00f, 5.8803344415e+00f,
            -2.3780608271e+00f, 4.4209252816e-01f),
          Array[Float](1.1659133162e-02f, -7.6466313128e-03f, 2.3681010412e-02f, -7.8989770916e-03f, 2.3681010412e-02f,
            -7.6466313128e-03f, 1.1659133162e-02f)
        )
      case 4 =>
        Filter(
          6,
          Array[Float](1.0000000000e+00f, -4.6768497674e+00f, 9.7621423046e+00f, -1.1501934308e+01f, 8.0339245654e+00f,
            -3.1489284186e+00f, 5.4189391928e-01f),
          Array[Float](7.2935097847e-03f, -1.5409691786e-02f, 2.5558502830e-02f, -2.5750839286e-02f, 2.5558502830e-02f,
            -1.5409691786e-02f, 7.2935097847e-03f)
        )
      case 5 =>
        Filter(
          6,
          Array[Float](1.0000000000e+00f, -5.0470082922e+00f, 1.1055203383e+01f, -1.3388074677e+01f, 9.4329631691e+00f,
            -3.6626915823e+00f, 6.1243214263e-01f),
          Array[Float](5.5699828667e-03f, -1.7047032929e-02f, 3.0001912362e-02f, -3.4532704919e-02f, 3.0001912362e-02f,
            -1.7047032929e-02f, 5.5699828667e-03f)
        )
      case 6 =>
        Filter(
          6,
          Array[Float](1.0000000000e+00f, -5.2664800962e+00f, 1.1874502680e+01f, -1.4637401088e+01f, 1.0390542726e+01f,
            -4.0247175699e+00f, 6.6453255902e-01f),
          Array[Float](4.7105692599e-03f, -1.7496959962e-02f, 3.3343643097e-02f, -4.0241782497e-02f, 3.3343643097e-02f,
            -1.7496959962e-02f, 4.7105692599e-03f)
        )
      case _ => throw new RuntimeException("Unsupported decimation factor")
    }
}

/**
  * Retain every n'th sample in the sequence. The input array length must be an
  * integer multiple of the decimation factor else the decimate method will throw
  * an exception.
  */
trait Decimator {
  val factor: Int
  def decimate(x: Array[Float]): Array[Float] = {
    require(x.length % factor == 0, "Input array length must be a multiple of the decimation rate")
    var n = 0
    var m = 0
    val y = new Array[Float](x.length / factor)
    while (m < x.length) {
      y(n) = x(m)
      n += 1
      m += factor
    }
    y
  }
}

/**
  * Inserts N-1 zeros between samples provided, taking care to account for array boundaries. If the
  * interpolation factor is 3, then the sequence [1,2,3] is mapped to [1,0,0,2,0,0,3,0,0]
  */
trait Interpolator {
  val factor: Int
  def interpolate(x: Array[Float]): Array[Float] = {
    val l = x.length
    val y = new Array[Float](l * factor)
    var n = 0
    while (n < l) {
      y(factor * n) = x(n)
      n += 1
    }
    y
  }
}

/**
  * Perform an IIR filter operation using the filter configuration provided in the constructor.
  * This implementation is unoptimised.
  * @param f The input data signal to filter
  */
class FilterIIR(f: Filter) {

  val state = new Array[Float](f.order + 1)

  @inline
  def filterOne(x: Float): Float = {
    var sumA = x
    var sumB = 0.0f
    var i    = f.order
    while (i > 0) {
      sumA -= state(i) * f.a(i)
      sumB += state(i) * f.b(i)
      state(i) = state(i - 1)
      i -= 1
    }
    state(1) = sumA
    sumA * f.b(0) + sumB
  }

  /**
    * IIR filter. The multiplication factor is present to compensate for the loss in energy
    * caused by interpolation.
    * @param x Input sequence
    * @param mult A multiplication factor by which the output is multiplied.
    * @return Filtered sequence using the configured filter parameters
    */
  def filter(x: Array[Float], mult: Float = 1.0f): Array[Float] = {
    val y = new Array[Float](x.length)
    var n = 0
    while (n < x.length) {
      y(n) = filterOne(x(n)) * mult
      n += 1
    }
    y
  }
}

trait Multirate {
  def process(x: Array[Float]): Array[Float]
}

/**
  * Upsample a signal by the factor specified. If a signal originally sampled at 8kHz is upsampled by a factor
  * of 6, the returned signal will have a sample frequency of 48kHz and will retain its original bandwidth
  * @param factor The interpolation factor to use
  */
case class Upsampler(factor: Int) extends FilterIIR(MultirateFilterFactory(factor)) with Interpolator with Multirate {

  /**
    * Process a signal, increasing its effective sample rate
    * @param x Signal to be upsampled
    */
  def process(x: Array[Float]): Array[Float] = filter(interpolate(x), factor.toFloat)
}

/**
  * Reduce a signal's sample rate by first filtering it to remove all potential alias frequencies then down-sampling
  * (decimating) the signal by the specified factor. If a signal is sampled at 48kHz and a decimation factor of
  * 6 is specified, the output signal will have a sample rate of 8 kHz
  * @param factor Decimation factor.
  */
case class Downsampler(factor: Int) extends FilterIIR(MultirateFilterFactory(factor)) with Decimator with Multirate {

  /**
    * Process the signal, effectively decreasing its sample rate.
    * @param x Input audio signal
    */
  def process(x: Array[Float]): Array[Float] = decimate(filter(x))
}
