/**
  * Copyright © 2018 8eo Inc.
  */
package za.co.monadic.scopus

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import za.co.monadic.scopus.dsp._

class MultiRateTest extends AnyFunSpec with Matchers {

  import FreqUtils._

  describe("The multirate filter tools should") {

    it("interpolate a signal with zeros") {
      val x0 = Array[Float](1.0f, 2.0f, 3.0f)
      val y0 = Array[Float](1.0f, 0.0f, 0.0f, 2.0f, 0.0f, 0.0f, 3.0f, 0.0f, 0.0f)
      val ip3 = new {
        val factor = 3
      } with Interpolator
      ip3.interpolate(x0) shouldBe y0
    }

    it("decimates a signal and manages array boundaries") {
      val x = Array.range(0, 9).map(_.toFloat)
      val y = Array.range(0, 9, 3).map(_.toFloat)
      val d = new {
        val factor = 3
      } with Decimator
      d.decimate(x) shouldBe y

      assertThrows[IllegalArgumentException] {
        d.decimate(Array.range(0, 10).map(_.toFloat))
      }
    }

    it("filters a signal using a simple IIR filter, proving the IIR filter works") {
      // Very simple IIR filter that we drive with an impulse
      val p  = 0.7f
      val fc = Filter(2, Array[Float](1.0f, -2.0f * p, p * p), Array[Float](1.0f, 0.0f, 0.0f))
      val x  = new Array[Float](10)
      x(0) = 1.0f

      // The impulse response for the filter above will be the following
      val ty = Array.range(0, 10).map(i => (i + 1.0f) * Math.pow(p, i))

      val f    = new FilterIIR(fc)
      val y    = f.filter(x)
      val diff = (y, ty).zipped.map(_ - _)
      diff.foreach(d => assert(Math.abs(d) < 1e-6))
    }

    it("has stable standard interpolation and decimation filters") {
      val l = 1000
      for (o <- 2 until 7) {
        val x = new Array[Float](l)
        x(0) = 1.0f
        val f = new FilterIIR(MultirateFilterFactory(o))
        val y = f.filter(x)
        y(l - 1) should be < 1e-6f // Filters must converge when activated with an impulse
      }
    }

    it("interpolates a sine wave, retaining the original frequency at the new sample rate") {
      val freqTable = Map(2 -> Sf16000, 3 -> Sf24000, 4 -> Sf32000, 6 -> Sf48000)
      val l         = 1000
      freqTable.foreach {
        case (factor, sf) =>
          val f   = pickFreq(150, l, Sf8000) // 1200Hz
          val x   = genSine(f, l, Sf8000)
          val ups = Upsampler(factor)
          // We run the signal through the interpolator a few times and then check for the presence of
          // the signal in the upsampled signal
          ups.process(x).length shouldBe l * factor
          val y    = ups.process(x)
          val amp2 = getAmplitude(y, pickFreq(100, l, sf), sf) // Check another frequency
          amp2 should be < 1e-6 //
          val amp = getAmplitude(y, f, sf) // Check our target frequency
          amp should be(1.0 +- 0.1) // Must be 1 give or take the passband ripple of the filter.
      }
    }

    it("decimates a sine wave, retaining its frequency at the new sample rate") {
      // We start at 48kHz and decimate down by various factors
      val freqTable = Map(2 -> Sf24000, 3 -> Sf16000, 4 -> Sf12000, 6 -> Sf8000)
      freqTable.foreach {
        case (factor, sf) =>
          val l  = factor * (1000 / factor) // Ensure that the length is a multiple of our decimation factor
          val f  = pickFreq(45, l, Sf48000) // Approx 2100Hz
          val x  = genSine(f, l, Sf48000)
          val ds = Downsampler(factor)
          // We run the signal through the interpolator a few times and then check for the presence of
          // the signal in the upsampled signal
          ds.process(x).length shouldBe l / factor
          val y    = ds.process(x)
          val amp2 = getAmplitude(y, pickFreq(30, l, sf), sf) // Check another frequency
          amp2 should be < 0.1
          val amp = getAmplitude(y, f, sf) // Check our target frequency
          amp should be(1.0 +- 0.11) // Must be 1 give or take the passband ripple of the filter.
      }
    }

    it("decimates a sine wave with frequency above the new Nyquist rate, filtering it from the new signal") {
      // We start at 48kHz and decimate down by various factors and input a frequency outside the
      // band of the decimated sample rate. The energy in the signal should be very small after decimation.
      List(2, 3, 4, 6).foreach { factor =>
        val l  = factor * (1000 / factor) // Ensure that the length is a multiple of our decimation factor
        val f  = pickFreq(400, l, Sf48000) // Must be above Nyquist frequency for 24kHz and lower
        val x  = genSine(f, l, Sf48000)
        val ds = Downsampler(factor)
        ds.process(x).length shouldBe l / factor
        val y = ds.process(x)
        val e = y.foldLeft(0.0f)((s, a) => s + a * a) / l // Computes the energy in the signal
        e should be < 1e-6f
      }
    }
  }
}
