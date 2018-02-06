/**
  * Copyright © 2018 8eo Inc.
  */
package za.co.monadic.scopus

import org.scalatest.{FunSpec, Matchers}

class MultiRateTest  extends FunSpec with Matchers {


  describe("The multirate filter tools should") {

    it("interpolate a signal with zeros and manages array boundaries") {
      pending
    }

    it("decimates a signal and manages array boundaries") {
      pending
    }

    it("filters a signal using a simple IIR filter") {
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

