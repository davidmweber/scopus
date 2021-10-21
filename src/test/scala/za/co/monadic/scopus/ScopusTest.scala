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

/*
 * Copyright David Weber 2014
 * Released under the Creative Commons License (http://creativecommons.org/licenses/by/4.0/legalcode)
 */

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen}
import za.co.monadic.scopus.TestUtils._
import za.co.monadic.scopus.g711u.{G711uDecoderFloat, G711uDecoderShort, G711uEncoder}
import za.co.monadic.scopus.opus._
import za.co.monadic.scopus.pcm._

import scala.util.{Failure, Success, Try}

case class AudioTest(
    name: String,
    chunkSize: Int,
    nSamples: Int,
    chunks: List[Array[Short]],
    chunksFloat: List[Array[Float]]
)

class ScopusTest extends AnyFunSpec with Matchers with GivenWhenThen with BeforeAndAfterAll {

  private val audio: (String, Int, String) => AudioTest = { (name, chunkSize, fileName) =>
    val audio: Array[Short]      = readAudioFile(s"test/audio_samples/$fileName")
    val audioFloat: Array[Float] = audio.map(_.toFloat / (1 << 15))
    // Normalise to +-1.0
    val nSamples: Int = (audio.length / chunkSize) * chunkSize
    // A list of 20ms chunks of audio rounded up to a whole number of blocks. Gotta love Scala :)
    def chunks[T](audio: Array[T]): List[Array[T]] = audio.slice(0, nSamples).grouped(chunkSize).toList

    AudioTest(name, chunkSize, nSamples, chunks(audio), chunks(audioFloat))
  }

  val mono   = audio("mono", 160, "torvalds-says-linux.int.raw")
  val stereo = audio("stereo", 1920, "torvalds-says-linux-stereo-48k.int.raw")

  val codecs = List(
    (
      "Opus",
      mono,
      OpusEncoder(Sf8000, 1).complexity(2),
      OpusDecoderShort(Sf8000, 1),
      OpusDecoderFloat(Sf8000, 1),
      0.90
    ),
    (
      "Opus mono to stereo",
      mono,
      OpusEncoder(Sf8000, 2).complexity(2),
      OpusDecoderShort(Sf8000, 2),
      OpusDecoderFloat(Sf8000, 2),
      0.80
    ),
    (
      "Opus stereo",
      stereo,
      OpusEncoder(Sf48000, 2).complexity(10),
      OpusDecoderShort(Sf48000, 2),
      OpusDecoderFloat(Sf48000, 2),
      0.90
    ),
    ("PCM", mono, PcmEncoder(Sf8000, 1), PcmDecoderShort(Sf8000, 1), PcmDecoderFloat(Sf8000, 1), 0.95),
    ("g.711u", mono, G711uEncoder(Sf8000, 1), G711uDecoderShort(Sf8000, 1), G711uDecoderFloat(Sf8000, 1), 0.90),
    ("g.711u mono to stereo", mono, G711uEncoder(Sf8000, 2), G711uDecoderShort(Sf8000, 2), G711uDecoderFloat(Sf8000, 2), 0.90)
  )

  for ((desc, audio, enc, dec, decFloat, corrMin) <- codecs) {

    describe(s"$desc audio api can") {

      it("encode and decode audio segments as Short types") {
        Given("a PCM file coded as an array of short integers and a codec pair")
        enc.reset
        dec.reset
        enc.getSampleRate should equal(dec.getSampleRate)
        enc.getSampleRate should equal(decFloat.getSampleRate)

        When("the audio is encoded and then decoded")
        val coded   = for (c <- audio.chunks) yield enc(c).get
        val decoded = for (c <- coded) yield dec(c).get

        Then("the number of packets in the original, coded and decoded streams should be the same")
        coded.length should equal(audio.chunks.length)
        decoded.length should equal(audio.chunks.length)

        And("the decoded packet length should be the same as the coded packet length")
        decoded.head.length should equal(audio.chunks.head.length)

        And("the decoded audio should sound the same as the original audio")
        // Break the input and output audio streams into 5ms chunks and compute the energy in each chunk
        val fiveMS = audio.chunkSize / 4
        val in     = audio.chunks.toArray.flatten.grouped(fiveMS).toList
        val out    = decoded.toArray.flatten.grouped(fiveMS).toList

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
        val coded   = for (c <- audio.chunksFloat) yield enc(c).get
        val decoded = for (c <- coded) yield decFloat(c).get

        Then("the number of packets in the original, coded and decoded streams should be the same")
        coded.length should equal(audio.chunksFloat.length)
        decoded.length should equal(audio.chunksFloat.length)

        And("the decoded packet length should be the same as the coded packet length")
        decoded.head.length should equal(audio.chunksFloat.head.length)

        And("the decoded audio should sound the same as the original audio")
        // Break the input and output audio streams into 5ms chunks and compute the energy in each chunk
        val fiveMS = audio.chunkSize / 4
        val in     = audio.chunksFloat.toArray.flatten.grouped(fiveMS).toList
        val out    = decoded.toArray.flatten.grouped(fiveMS).toList
        val eIn    = for (a <- in) yield energy(a)
        val eOut   = for (a <- out) yield energy(a)
        correlate(eIn, eOut) should be > corrMin
      }

      it("decodes erased packets to the specified number of samples") {
        dec.reset
        enc.reset
        dec(enc(audio.chunks.head).get) // Prime the decoder so it get the decoder state
        dec(audio.chunkSize).get.length should equal(audio.chunkSize)
      }

      it("decode erased packets for Short data") {
        enc.reset
        dec.reset
        val coded = for (c <- audio.chunks) yield enc(c).get
        val decoded = // Decode, dropping every 10th packet
          for {
            (c, i) <- coded.zipWithIndex
            p = if (i % 15 == 1) dec(audio.chunkSize) else dec(c)
          } yield p.get
        val fiveMS = audio.chunkSize / 4
        val in     = audio.chunks.toArray.flatten.grouped(fiveMS).toList
        val out    = decoded.toArray.flatten.grouped(fiveMS).toList
        val eIn    = for (a <- in) yield energy(a)
        val eOut   = for (a <- out) yield energy(a)
        val rho    = correlate(eIn, eOut)
        //writeAudioFile("test-short-erasure.raw",decoded.toArray.flatten)
        rho should be > 0.95 * corrMin
      }

      it("decode erased packets for Float data") {
        enc.reset
        decFloat.reset
        val coded = for (c <- audio.chunksFloat) yield enc(c).get
        val decoded = // Decode, dropping every 10th packet
          for {
            (c, i) <- coded.zipWithIndex
            p = if (false) decFloat(audio.chunkSize) else decFloat(c)
          } yield p.get

        val fiveMS = audio.chunkSize / 4
        val in     = audio.chunksFloat.toArray.flatten.grouped(fiveMS).toList
        val out    = decoded.toArray.flatten.grouped(fiveMS).toList
        val eIn    = for (a <- in) yield energy(a)
        val eOut   = for (a <- out) yield energy(a)
        val rho    = correlate(eIn, eOut)
        rho should be > 0.95 * corrMin
      }
    }

    describe(s"The $desc performance test will") {

      val repeats = 50

      it("meet basic encoder speed requirements") {
        enc.reset
        val tStart = System.currentTimeMillis()
        for (_ <- 0 until repeats) {
          for (c <- audio.chunks) enc(c)
        }
        val duration = (System.currentTimeMillis() - tStart) / 1000.0          // Seconds
        val speed    = repeats * audio.nSamples / duration / enc.getSampleRate // multiple of real time
        speed should be > 100.0
        info(f"Encoder runs at $speed%5.1f times real time")
      }

      it("meets basic decoder speed requirements") {
        enc.reset
        dec.reset
        val tStart = System.currentTimeMillis()
        val coded  = for (c <- audio.chunks) yield enc(c).get
        for (_ <- 0 until repeats) {
          for (c <- coded) dec(c)
        }
        val duration = (System.currentTimeMillis() - tStart) / 1000.0          // Seconds
        val speed    = repeats * audio.nSamples / duration / enc.getSampleRate // multiple of real time
        speed should be > 400.0
        info(f"Decoder runs at $speed%5.1f times real time")
      }
    }
  }

  describe("The G711u codec") {

    it("fails if an invalid codec construction is requested") {
      a[RuntimeException] should be thrownBy G711uEncoder(Sf12000, 1)
      a[RuntimeException] should be thrownBy G711uDecoderShort(Sf12000, 1)
      a[RuntimeException] should be thrownBy G711uDecoderFloat(Sf12000, 1)
    }

    it("encodes and decodes a Float signal, generating the correct output") {
      import FreqUtils._
      val freqTable = Map(1 -> Sf8000, 2 -> Sf16000, 3 -> Sf24000, 4 -> Sf32000, 6 -> Sf48000)
      freqTable.foreach {
        case (factor, sf) =>
          val l: Int = (0.04 * sf()).toInt
          val f      = pickFreq(100, l, sf)
          val x      = genSine(f, l, sf)
          val enc    = G711uEncoder(sf, 1)
          val dec    = G711uDecoderFloat(sf, 1)
          val e      = enc(x).get
          e.length shouldBe l / factor
          dec(enc(x).get) // Work past transient from filter startup
          val y   = dec(e).get
          val amp = getAmplitude(y, f, sf) // Check our target frequency
          amp should be(1.0 +- 0.18) // Must be 1 give or take the passband ripple of the filter.
      }
    }

    it("encodes and decodes a Short signal, generating the correct output") {
      import ArrayConversion._
      import FreqUtils._
      val freqTable = Map(1 -> Sf8000, 2 -> Sf16000, 3 -> Sf24000, 4 -> Sf32000, 6 -> Sf48000)
      freqTable.foreach {
        case (factor, sf) =>
          val l: Int = (0.04 * sf()).toInt
          val f      = pickFreq(100, l, sf)
          val x      = floatToShort(genSine(f, l, sf))
          val enc    = G711uEncoder(sf, 1)
          val dec    = G711uDecoderShort(sf, 1)
          val e      = enc(x).get
          e.length shouldBe l / factor
          dec(enc(x).get) // Work past transient from filter startup
          val y   = shortToFloat(dec(e).get)
          val amp = getAmplitude(y, f, sf) // Check our target frequency
          amp should be(1.0 +- 0.18) // Must be 1 give or take the passband ripple of the filter.
      }
    }
  }

  Seq((1, mono, Sf8000), (2, stereo, Sf48000)).foreach {
    case (channels, audio, sf) =>
      describe(s"The Opus codec for ${audio.name} audio") {

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
          val result = Map(1 -> 6, 2 -> 9)(channels)
          val enc    = OpusEncoder(sf, channels)
          enc.setUseDtx(1)
          val coded = for (c <- audio.chunksFloat) yield {
            val ret = enc(c).get
            (ret, enc.wasSilentPacket, enc.wasDtx)
          }
          coded.count(t => enc.isDTX(t._1)) shouldBe result // Tests old API
          coded.count(t => t._2) shouldBe result
          coded.count(t => t._3) shouldBe result + 1
          val blank = Array.fill[Float](audio.chunkSize)(0.0f)
          // With DTS, packets are either 1 or 6 bytes long. The 6 byte one gets transmitted, the
          // 1 byte long packets should not be transmitted.
          val b = (0 to 100).map(_ => enc(blank).get)
          b.count(_.length <= 6) should be > 95
        }

        it("detects silence packets in DTX mode at 48kHz") {
          val enc = OpusEncoder(Sf48000, channels)
          enc.setUseDtx(1)
          val blank = Array.fill[Float](1920)(0.0f)
          // With DTS, packets are either 1 or 6 bytes long. The 6 byte one gets transmitted, the
          // 1 byte long packets should not be transmitted.
          val b = (0 to 100).map(_ => enc(blank).get)
          b.count(_.length <= 6) should be > 85
        }

        it("returns the correct number of samples in the packet") {
          val enc = OpusEncoder(Sf8000, channels)
          enc.setVbr(1)
          audio.chunks.foreach { c =>
            val d = enc(c)
            val l = Opus.decoder_get_nb_samples(d.get, d.get.length, Sf8000())
            l shouldBe (c.length / channels)
          }
        }

        it("encodes and decodes DTX packets") {
          // This is not really a test. I just used it to experiment with VBR and DTX packet streams
          val enc = OpusEncoder(Sf8000, channels)
          val dec = OpusDecoderFloat(Sf8000, channels)
          enc.setVbr(1)
          val blank = Array.fill[Float](audio.chunkSize)(0.0f)
          // With DTS, packets are either 1 or 6 bytes long. The 6 byte one gets transmitted, the
          // 1 byte long packets should not be transmitted.
          val b = (0 to 100).map(c => (c, enc(blank).get))
          // We decode all the packets and ensure the ones we transmit reconstruct to a silent frame
          b.foreach {
            case (_, p) =>
              if (!enc.isDTX(p)) {
                val r = dec(p).get.toList
                r.count(Math.abs(_) > 1e-4) shouldBe 0 // Apart from the first packet, all packets should be zero
              }
          }
        }

        it("get and set the encoder parameters (tests opus_encoder_set/get_ctl call only)") {
          val enc = OpusEncoder(Sf8000, channels)
          enc.setUseDtx(1)
          enc.getUseDtx should equal(1)
          enc.setUseDtx(0)
          enc.getUseDtx should equal(0)
        }

        it("get and set decoder parameters (tests opus_decoder_set/get_ctl call only)") {
          val dec = OpusDecoderShort(Sf8000, channels)
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
            val e = List(
              OpusEncoder(Sf8000, channels),
              OpusEncoder(Sf12000, channels),
              OpusEncoder(Sf16000, channels),
              OpusEncoder(Sf24000, channels),
              OpusEncoder(Sf48000, channels)
            )
            val d = List(
              OpusDecoderShort(Sf8000, channels),
              OpusDecoderShort(Sf12000, channels),
              OpusDecoderShort(Sf16000, channels),
              OpusDecoderShort(Sf24000, channels),
              OpusDecoderShort(Sf48000, channels)
            )
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
          val e = OpusEncoder(Sf8000, channels)
          e.cleanup()
          e.cleanup() // Must not segfault
          val d = OpusDecoderShort(Sf8000, channels)
          d.cleanup()
          d.cleanup()
          val df = OpusDecoderFloat(Sf8000, channels)
          df.cleanup()
          df.cleanup()
        }
      }
  }
  describe("The PCM codec") {
    it("handles Shorts and Floats interchangeably ") {

      def check(a: Array[Float], b: Array[Float]): Unit = {
        a.length should be(b.length)
        for (i <- a.indices) {
          Math.abs(a(i) - b(i)) should be <= 0.5f
        }
      }
      val freq = 1000f
      val e    = PcmEncoder(Sf8000, 1)
      val ds   = PcmDecoderShort(Sf8000, 1)
      val df   = PcmDecoderFloat(Sf8000, 1)
      val a0   = (0 until 10).map((idx: Int) => (30000.0 * math.sin(2.0 * math.Pi * idx * freq / 8000.0)).toShort).toArray
      val a1 =
        (0 until 10).map((idx: Int) => 0.9155273f * math.sin(2.0f * math.Pi * idx * freq / 8000.0f).toFloat).toArray

      ds(e(a0).get).get should equal(a0)
      ds(e(a1).get).get should equal(a0)
      check(df(e(a0).get).get, a1)
      check(df(e(a1).get).get, a1)
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
