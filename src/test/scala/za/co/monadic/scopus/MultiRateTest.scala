/**
  * Copyright © 2018 8eo Inc.
  */
package za.co.monadic.scopus


import org.scalatest.{FunSpec, Matchers}
import za.co.monadic.scopus.dsp.{Decimator, Filter, FilterIIR, Interpolator}

class MultiRateTest  extends FunSpec with Matchers {


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

      assertThrows[IllegalArgumentException]{
        d.decimate(Array.range(0,10).map(_.toFloat))
      }

    }

    it("filters a signal using a simple IIR filter") {
      // Very simple IIR filter that we drive with an impulse
      val p = 0.7f
      val fc = Filter(3, Array[Float](1.0f, -2.0f*p, p*p), Array[Float](1.0f, 0.0f, 0.0f))
      val x = new Array[Float](10)
      x(0) = 1.0f

      // The impulse response for the filter above will be the following
      val ty = Array.range(0, 10).map(i ⇒ (i + 1.0f)*Math.pow(p, i))

      val f = new FilterIIR(fc)
      val y = f.filter(x)
      val diff = (y, ty).zipped.map(_ - _)
      diff.foreach(d ⇒ assert(Math.abs(d) < 1e-6))
    }

    it("has stable standard interpolation and decimation filters"){
      pending
    }

    it("interpolates a sine wave, retaining the original frequency at the new sample rate") {
      pending
    }

    it("decimates a sine wave with frequency below the new Nyquist rate, retaining its frequency at the new sample rate") {
      pending
    }

    it("decimates a sine wave with frequency above the new Nyquist rate, filtering it from the new signal") {
      pending
    }

  }

}

