package za.co.monadic.scopus

/*
 * Copyright David Weber 2014
 * Released under the Creative Commons License (http://creativecommons.org/licenses/by/4.0/legalcode)
 */

import org.scalatest._
import za.co.monadic.scopus.TestUtils._
import za.co.monadic.scopus.echo.EchoCanceller
import za.co.monadic.scopus.opus._
import za.co.monadic.scopus.speex._
import za.co.monadic.scopus.pcm._

import scala.util.{Failure, Success, Try}

class ScopusTest extends FunSpec with Matchers with GivenWhenThen with BeforeAndAfterAll {

  val audio      = readAudioFile("test/audio_samples/torvalds-says-linux.int.raw")
  val audioFloat = audio.map(_.toFloat / (1 << 15))
  // Normalise to +-1.0
  val chunkSize = 160
  val nSamples  = (audio.length / chunkSize) * chunkSize
  // A list of 20ms chunks of audio rounded up to a whole number of blocks. Gotta love Scala :)
  val chunks      = audio.slice(0, nSamples).grouped(chunkSize).toList
  val chunksFloat = audioFloat.slice(0, nSamples).grouped(chunkSize).toList

  val codecs = List(
    ("Speex", SpeexEncoder(Sf8000).complexity(1), SpeexDecoderShort(Sf8000), SpeexDecoderFloat(Sf8000), 0.81),
    ("Speex with enhancement",
     SpeexEncoder(Sf8000).complexity(1),
     SpeexDecoderShort(Sf8000, true),
     SpeexDecoderFloat(Sf8000, true),
     0.81),
    ("Opus", OpusEncoder(Sf8000, 1).complexity(2), OpusDecoderShort(Sf8000, 1), OpusDecoderFloat(Sf8000, 1), 0.90),
    ("PCM", PcmEncoder(Sf8000, 1), PcmDecoderShort(Sf8000, 1), PcmDecoderFloat(Sf8000, 1), 0.95)
  )

  for ((desc, enc, dec, decFloat, corrMin) <- codecs) {

    describe(s"$desc audio api can") {

      it("encode and decode audio segments as Short types") {
        Given("a PCM file coded as an array of short integers and a codec pair")
        enc.reset
        dec.reset
        enc.getSampleRate should equal(8000)
        dec.getSampleRate should equal(8000)
        When("the audio is encoded and then decoded")
        val coded   = for (c <- chunks) yield enc(c).get
        val decoded = for (c <- coded) yield dec(c).get

        Then("the number of packets in the original, coded and decoded streams should be the same")
        coded.length should equal(chunks.length)
        decoded.length should equal(chunks.length)

        And("the decoded packet length should be the same as the coded packet length")
        decoded.head.length should equal(chunks.head.length)

        And("the decoded audio should sound the same as the original audio")
        // Break the input and output audio streams into 5ms chunks and compute the energy in each chunk
        val in  = chunks.toArray.flatten.grouped(40).toList
        val out = decoded.toArray.flatten.grouped(40).toList

        // Uncomment for audible verification.
        //writeAudioFile(s"$desc-test-short.raw",decoded.toArray.flatten)

        val eIn  = for (a <- in) yield energy(a)
        val eOut = for (a <- out) yield energy(a)
        correlate(eIn, eOut) should be > corrMin // This is a pretty decent test if all is well
      }

      it("encode and decode audio segments as Float types") {
        Given("a PCM file coded as an array of short integers and a codec pair")
        enc.reset
        decFloat.reset
        When("the audio is encoded and then decoded")
        val coded   = for (c <- chunksFloat) yield enc(c).get
        val decoded = for (c <- coded) yield decFloat(c).get

        Then("the number of packets in the original, coded and decoded streams should be the same")
        coded.length should equal(chunksFloat.length)
        decoded.length should equal(chunksFloat.length)

        And("the decoded packet length should be the same as the coded packet length")
        decoded.head.length should equal(chunksFloat.head.length)

        And("the decoded audio should sound the same as the original audio")
        // Break the input and output audio streams into 5ms chunks and compute the energy in each chunk
        val in   = chunksFloat.toArray.flatten.grouped(40).toList
        val out  = decoded.toArray.flatten.grouped(40).toList
        val eIn  = for (a <- in) yield energy(a)
        val eOut = for (a <- out) yield energy(a)
        correlate(eIn, eOut) should be > corrMin
      }

      it("decodes erased packets to the specified number of samples") {
        dec.reset
        enc.reset
        dec(enc(chunks.head).get) // Prime the decoder so it get the decoder state
        dec(chunkSize).get.length should equal(chunkSize)
      }

      it("decode erased packets for Short data") {
        enc.reset
        dec.reset
        val coded = for (c <- chunks) yield enc(c).get
        val decoded = // Decode, dropping every 10th packet
        for {
          (c, i) <- coded.zipWithIndex
          p = if (i % 15 == 1) dec(chunkSize) else dec(c)
        } yield p.get
        val in   = chunks.toArray.flatten.grouped(40).toList
        val out  = decoded.toArray.flatten.grouped(40).toList
        val eIn  = for (a <- in) yield energy(a)
        val eOut = for (a <- out) yield energy(a)
        val rho  = correlate(eIn, eOut)
        //writeAudioFile("test-short-erasure.raw",decoded.toArray.flatten)
        rho should be > 0.95 * corrMin
      }

      it("decode erased packets for Float data") {
        enc.reset
        decFloat.reset
        val coded = for (c <- chunksFloat) yield enc(c).get
        val decoded = // Decode, dropping every 10th packet
        for {
          (c, i) <- coded.zipWithIndex
          p = if (i % 15 == 1) decFloat(chunkSize) else decFloat(c)
        } yield p.get
        val in   = chunksFloat.toArray.flatten.grouped(40).toList
        val out  = decoded.toArray.flatten.grouped(40).toList
        val eIn  = for (a <- in) yield energy(a)
        val eOut = for (a <- out) yield energy(a)
        val rho  = correlate(eIn, eOut)
        rho should be > 0.95 * corrMin
      }
    }

    describe(s"The $desc performance test will") {

      val repeats = 50

      it("meet basic encoder speed requirements") {
        enc.reset
        val tStart = System.currentTimeMillis()
        for (_ <- 0 until repeats) {
          for (c <- chunks) enc(c)
        }
        val duration = (System.currentTimeMillis() - tStart) / 1000.0 // Seconds
        val speed    = repeats * nSamples / duration / 8000.0         // multiple of real time
        speed should be > 100.0
        info(f"Encoder runs at $speed%5.1f times real time")
      }

      it("meets basic decoder speed requirements") {
        enc.reset
        dec.reset
        val tStart = System.currentTimeMillis()
        val coded  = for (c <- chunks) yield enc(c).get
        for (_ <- 0 until repeats) {
          for (c <- coded) dec(c)
        }
        val duration = (System.currentTimeMillis() - tStart) / 1000.0 // Seconds
        val speed    = repeats * nSamples / duration / 8000.0         // multiple of real time
        speed should be > 400.0
        info(f"Decoder runs at $speed%5.1f times real time")
      }
    }
  }

  describe("The Speex codec") {

    it("constructs decoders and encoders for supported sample frequencies") {
      try {
        Given("a set of sampling frequencies for the encoders and decoders")
        val freqs = List(8000, 16000, 32000)
        When("they are constructed for different sample frequencies")
        val e  = List(SpeexEncoder(Sf8000), SpeexEncoder(Sf16000), SpeexEncoder(Sf32000))
        val d  = List(SpeexDecoderShort(Sf8000), SpeexDecoderShort(Sf16000), SpeexDecoderShort(Sf32000))
        val df = List(SpeexDecoderFloat(Sf8000), SpeexDecoderFloat(Sf16000), SpeexDecoderFloat(Sf32000))
        Then("the encoder structures return the correct sample frequency it was configured for")
        for ((f, t) <- freqs zip e) {
          t.getSampleRate should equal(f)
        }
        e.foreach(_.cleanup())
        And("the short decoder structures return the correct frequencies")
        for ((f, t) <- freqs zip d) {
          t.getSampleRate should equal(f)
        }
        d.foreach(_.cleanup())
        And("the float decoder structures return the correct frequencies")
        for ((f, t) <- freqs zip df) {
          t.getSampleRate should equal(f)
        }
        df.foreach(_.cleanup())
      } catch {
        case e: Exception => fail(s"Received exception ${e.getMessage}")
      }
    }

    it("won't allow unvalid sampling frequencies to be used") {
      a[RuntimeException] should be thrownBy SpeexEncoder(Sf12000)
      a[RuntimeException] should be thrownBy SpeexEncoder(Sf24000)
      a[RuntimeException] should be thrownBy SpeexEncoder(Sf48000)
      a[RuntimeException] should be thrownBy SpeexDecoderShort(Sf12000)
      a[RuntimeException] should be thrownBy SpeexDecoderShort(Sf24000)
      a[RuntimeException] should be thrownBy SpeexDecoderShort(Sf48000)
      a[RuntimeException] should be thrownBy SpeexDecoderFloat(Sf12000)
      a[RuntimeException] should be thrownBy SpeexDecoderFloat(Sf24000)
      a[RuntimeException] should be thrownBy SpeexDecoderFloat(Sf48000)
    }

    it("cleans up after itself") {
      val e = SpeexEncoder(Sf8000)
      e.cleanup()
      e.cleanup() // Must not segfault
      val d = SpeexDecoderShort(Sf8000)
      d.cleanup()
      d.cleanup()
      val df = SpeexDecoderFloat(Sf8000)
      df.cleanup()
      df.cleanup()
    }
  }

  describe("The Opus codec") {

    it("Fails if the encoder and decoder constructors throw") {
      // cannot build 4 channel decoders like this
      Try(OpusEncoder(Sf8000, 4)) match {
        case Success(_) => fail("Encoder constructor did not fail on bad construction")
        case Failure(f) => f.getMessage should equal("Failed to create the Opus encoder: invalid argument")
      }
      Try(OpusEncoder(Sf8000, 1, Voip, -1)) match {
        case Success(_) => fail("Encoder constructor did not fail on bad construction")
        case Failure(f) => f.getMessage should equal("requirement failed: Buffer size must be positive")
      }
      Try(OpusDecoderShort(Sf8000, 4)) match {
        case Success(_) => fail("Short decoder constructor did not fail on bad construction")
        case Failure(f) => f.getMessage should equal("Failed to create the Opus encoder: invalid argument")
      }
      Try(OpusDecoderFloat(Sf8000, 4)) match {
        case Success(_) => fail("Float decoder constructor did not fail on bad construction")
        case Failure(f) => f.getMessage should equal("Failed to create the Opus encoder: invalid argument")
      }
    }

    it("detects silence packets in DTX mode") {
      val enc = OpusEncoder(Sf8000, 1)
      enc.setUseDtx(1)
      val coded = for (c <- chunksFloat) yield enc(c).get
      coded.count(enc.isDTX) shouldBe 7
      val blank = Array.fill[Float](chunkSize)(0.0f)
      // With DTS, packets are either 1 or 6 bytes long. The 6 byte one gets transmitted, the
      // 1 byte long packets should not be transmitted.
      val b = (0 to 100).map(_ ⇒ enc(blank).get)
      b.count(_.length <= 6) should be > 95
    }

    it("detects silence packets in DTX mode at 48kHz") {
      val enc = OpusEncoder(Sf48000, 1)
      enc.setUseDtx(1)
      val blank = Array.fill[Float](chunkSize*6*2)(0.0f)
      // With DTS, packets are either 1 or 6 bytes long. The 6 byte one gets transmitted, the
      // 1 byte long packets should not be transmitted.
      val b = (0 to 100).map(_ ⇒ enc(blank).get)
      b.count(_.length <= 6) should be > 85
    }

    it("returns the correct number of samples in the packet") {
      val enc = OpusEncoder(Sf8000, 1)
      enc.setVbr(1)
      chunks.foreach{ c⇒
        val d = enc(c)
        val l = Opus.decoder_get_nb_samples(d.get, d.get.length, Sf8000())
        l shouldBe c.length
      }
    }

    it("encodes and decodes DTX packets") {
      // This is not really a test. I just used it to experiment with VBR and DTX packet streams
      val enc = OpusEncoder(Sf8000, 1)
      val dec = OpusDecoderFloat(Sf8000, 1)
      enc.setVbr(1)
      val blank = Array.fill[Float](chunkSize)(0.0f)
      // With DTS, packets are either 1 or 6 bytes long. The 6 byte one gets transmitted, the
      // 1 byte long packets should not be transmitted.
      val b = (0 to 100).map(c ⇒ (c, enc(blank).get))
      // We decode all the packets and ensure the ones we transmit reconstruct to a silent frame
      b.foreach {
        case (_, p) ⇒
        if (!enc.isDTX(p)) {
          val r = dec(p).get.toList
          r.count(Math.abs(_) > 1e-4) shouldBe 0 // Apart from the first packet, all packets should be zero
        }
      }
    }

    it("get and set the encoder parameters (tests opus_encoder_set/get_ctl call only)") {
      val enc = OpusEncoder(Sf8000, 1)
      enc.setUseDtx(1)
      enc.getUseDtx should equal(1)
      enc.setUseDtx(0)
      enc.getUseDtx should equal(0)
    }

    it("get and set decoder parameters (tests opus_decoder_set/get_ctl call only)") {
      val dec = OpusDecoderShort(Sf8000, 1)
      dec.setGain(10)
      dec.getGain should equal(10)
    }

    it("correctly returns error messages for a given error condition") {
      val msg = opus.Opus.error_string(-3)
      msg should equal("internal error")
    }

    it("constructs decoders and encoders for all frequencies") {
      try {
        Given("a set of sampling frequencies for the encoders and decoders")
        val freqs = List(8000, 12000, 16000, 24000, 48000)
        When("they are constructed for different sample frequencies")
        val e = List(OpusEncoder(Sf8000, 1),
                     OpusEncoder(Sf12000, 1),
                     OpusEncoder(Sf16000, 1),
                     OpusEncoder(Sf24000, 1),
                     OpusEncoder(Sf48000, 1))
        val d = List(OpusDecoderShort(Sf8000, 1),
                     OpusDecoderShort(Sf12000, 1),
                     OpusDecoderShort(Sf16000, 1),
                     OpusDecoderShort(Sf24000, 1),
                     OpusDecoderShort(Sf48000, 1))
        Then("the encoder structures return the correct sample frequency it was configured for")
        for ((f, t) <- freqs zip e) {
          t.getSampleRate should equal(f)
        }
        e.foreach(_.cleanup())
        And("the decoder structures return the correct frequencies")
        for ((f, t) <- freqs zip d) {
          t.getSampleRate should equal(f)
        }
        d.foreach(_.cleanup())
      } catch {
        case e: Exception => fail(s"Received exception ${e.getMessage}")
      }
    }

    it("cleans up after itself") {
      val e = OpusEncoder(Sf8000, 1)
      e.cleanup()
      e.cleanup() // Must not segfault
      val d = OpusDecoderShort(Sf8000, 1)
      d.cleanup()
      d.cleanup()
      val df = OpusDecoderFloat(Sf8000, 1)
      df.cleanup()
      df.cleanup()
    }
  }

  describe("The PCM codec") {
    it("handles Shorts and Floats interchangeably ") {

      def check(a: Array[Float], b: Array[Float]): Unit = {
        a.length should be(b.length)
        for (i ← a.indices) {
          Math.abs(a(i) - b(i)) should be <= 0.5f
        }
      }
      val freq = 1000f
      val e    = PcmEncoder(Sf8000, 1)
      val ds   = PcmDecoderShort(Sf8000, 1)
      val df   = PcmDecoderFloat(Sf8000, 1)
      val a0   = (0 until 10).map((idx: Int) ⇒ (30000.0 * math.sin(2.0 * math.Pi * idx * freq / 8000.0)).toShort).toArray
      val a1 =
        (0 until 10).map((idx: Int) ⇒ 0.9155273f * math.sin(2.0f * math.Pi * idx * freq / 8000.0f).toFloat).toArray

      ds(e(a0).get).get should equal(a0)
      ds(e(a1).get).get should equal(a0)
      check(df(e(a0).get).get, a1)
      check(df(e(a1).get).get, a1)
    }
  }

  describe("Echo canceller") {

    it("should remove all output sound from the input (synchronous call") {
      val N  = 320
      val ec = new EchoCanceller(N, 256)
      val error = for {
        _ ← 0 to 100
        play = Seq.fill(N)(shortGauss()).toArray[Short]
        rec  = play.map((s: Short) ⇒ (s / 2).toShort)
      } yield energy(ec.cancel(play, rec))
      error.head should be > 0.01
      error.last should be < 1e-8
      ec.cleanup()
      ec.cleanup()
    }

    it("should remove all output sound from the input (separate calls)") {
      val N  = 320
      val ec = new EchoCanceller(N, 256)
      // There is a packet delay of 2 * frame_size in the playback queue
      val sound = for (_ ← 0 to 101) yield Seq.fill(N)(shortGauss()).toArray[Short]
      ec.capture(sound.head)
      val error = for {
        i ← 1 to 100
        _   = ec.playback(sound(i + 1))
        rec = sound(i).map((s: Short) ⇒ (s / 2).toShort)
        out = ec.capture(rec)
        e   = energy(out)
      } yield e
      //error.head should be > 0.01
      error.last should be < 1e-8
      ec.cleanup()
      ec.cleanup() // Test for no bomb on second cleanup */
    }
  }

  describe("Array conversion utilities") {

    import za.co.monadic.scopus.ArrayConversion._

    it("convert between Short and Byte arrays") {
      val bytes0   = Array[Byte](0, 1, 2, 3, 200.toByte, 129.toByte)
      val out      = byteArrayToShortArray(bytes0)
      val swapped0 = shortArrayToByteArray(out)
      bytes0 should equal(swapped0)

      val bytes1   = Array[Short](0, -1, -2, -3, -4, -5, -6, -7, -8, -9, 12000, -15000)
      val swapped1 = byteArrayToShortArray(shortArrayToByteArray(bytes1))

      bytes1 should equal(swapped1)
    }

    it("convert between Float and Byte arrays") {
      val f    = Array[Float](0.0f, 1.0f, -1.0f, 3.1415e11f, -3.1415e15f)
      val b    = floatArrayToByteArray(f)
      val fOut = byteArrayToFloatArray(b)
      fOut should be(f)
    }
  }
}
