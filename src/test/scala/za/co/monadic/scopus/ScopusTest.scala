/*
 * Copyright David Weber 2014
 * Released under the Creative Commons License (http://creativecommons.org/licenses/by/4.0/legalcode)
 */
import java.io._
import org.scalatest._
import scala.util.{Failure, Success}
import za.co.monadic.scopus._
import Numeric.Implicits._

class ScopusTest extends FunSpec with Matchers with GivenWhenThen with BeforeAndAfterAll {

  // Byteswap. Needed for testing on Intel architectures
  def swap(in: Short): Short = ((in >> 8) + ((in & 0xff) << 8)).asInstanceOf[Short]

  def readAudioFile(file: String): Array[Short] = {
    val stream = new FileInputStream(file)
    val infile = new DataInputStream(new BufferedInputStream(stream))
    val len = stream.getChannel.size().asInstanceOf[Int]
    val audio = new Array[Short](len / 2)
    for (i <- 0 to len / 2 - 1) audio(i) = swap(infile.readShort())
    infile.close()
    audio
  }

  /**
   * Writes a raw audio file that can be played back using sox as
   * "play -r 8000 -b 16 -e signed <filename.raw> "
   * @param file Name of the file to write to
   * @param data Array containing the data formatted as Short
   */
  def writeAudioFile(file: String, data: Array[Short]): Unit = {
    val stream = new FileOutputStream(file)
    val outfile = new DataOutputStream(new BufferedOutputStream(stream))
    for (i <- 0 until data.length) outfile.writeShort(swap(data(i)))
    outfile.close()
  }

  def sqr[A: Numeric](a: A): A = a * a

  def energy(audio: Array[Short]): Double = {
    audio.map(a => sqr(a.toDouble / (1 << 15))).sum / audio.length.toDouble
  }

  def energy(audio: Array[Float]): Double = {
    audio.map(a => sqr(a)).sum / audio.length.toDouble
  }

  /**
   * Calculates the correlation coefficient using Pearson's formula.
   * @param a List for first sequence
   * @param b List for second sequence
   * @return Computed correlation coefficient.
   */
  def correlate(a: List[Double], b: List[Double]): Double = {
    val aAve = a.sum / a.length
    val bAve = b.sum / b.length
    val top = (for ((x, y) <- a zip b) yield (x - aAve) * (y - bAve)).sum
    val bottom = math.sqrt(a.map((t: Double) => sqr(t - aAve)).sum) * math.sqrt(b.map((t: Double) => sqr(t - bAve)).sum)
    top / bottom
  }

  val audio = readAudioFile("test/audio_samples/torvalds-says-linux.int.raw")
  val audioFloat = audio.map(_.toFloat / (1 << 15))
  // Normalise to +-1.0
  val chunkSize = 160
  val nSamples = (audio.length / chunkSize) * chunkSize
  // A list of 20ms chunks of audio rounded up to a whole number of blocks. Gotta love Scala :)
  val chunks = audio.slice(0, nSamples).grouped(chunkSize).toList
  val chunksFloat = audioFloat.slice(0, nSamples).grouped(chunkSize).toList

  val enc = Encoder(Sf8000, 1) match {
    case Success(ok) => ok
    case Failure(f) => fail(s"Encoder construction failed: ${f.getMessage}")
  }
  val dec = Decoder(Sf8000, 1) match {
    case Success(ok) => ok
    case Failure(f) => fail(s"Decoder construction failed: ${f.getMessage}")
  }
  val decFloat = DecoderFloat(Sf8000,1) match {
    case Success(ok) => ok
    case Failure(f) => fail(s"Float decoder construction failed: ${f.getMessage}")
  }

  override def afterAll() = {
    enc.cleanup()
    dec.cleanup()
    decFloat.cleanup()
  }

  describe("Opus codec can") {

    it("encode and decode audio segments as Short types") {
      Given("a PCM file coded as an array of short integers and a codec pair")
      enc.reset
      dec.reset
      enc.getSampleRate should equal(8000)
      dec.getSampleRate should equal(8000)
      When("the audio is encoded and then decoded")
      val coded = for (c <- chunks) yield enc(c).get
      val decoded = for (c <- coded) yield dec(c).get

      Then("the number of packets in the original, coded and decoded streams should be the same")
      coded.length should equal(chunks.length)
      decoded.length should equal(chunks.length)

      And("the decoded packet length should be the same as the coded packet length")
      decoded.head.length should equal(chunks.head.length)

      And("the decoded audio should sound the same as the original audio")
      // Break the input and output audio streams into 5ms chunks and compute the energy in each chunk
      val in = chunks.toArray.flatten.grouped(40).toList
      val out = decoded.toArray.flatten.grouped(40).toList
      val eIn = for (a <- in) yield energy(a)
      val eOut = for (a <- out) yield energy(a)
      correlate(eIn, eOut) should be > 0.93 // This is a pretty decent test if all is well
      // Uncomment for audible verification.
      //writeAudioFile("test-short.raw",decoded.toArray.flatten)
    }

    it("Fails if the encoder and decoder constructors throw") {
      // cannot build 4 channel decoders like this
      Encoder(Sf8000, 4) match {
        case Success(ok) => fail("Encoder constructor did not fail on bad construction")
        case Failure(f) => f.getMessage should equal("Failed to create the Opus encoder: invalid argument")
      }
      Encoder(Sf8000, 1, Voip, -1 ) match {
        case Success(ok) => fail("Encoder constructor did not fail on bad construction")
        case Failure(f) => f.getMessage should equal("requirement failed: Buffer size must be positive")
      }
      Decoder(Sf8000, 4) match {
        case Success(ok) => fail("Short decoder constructor did not fail on bad construction")
        case Failure(f) => f.getMessage should equal("Failed to create the Opus encoder: invalid argument")
      }
      DecoderFloat(Sf8000,4) match {
        case Success(ok) => fail("Float decoder constructor did not fail on bad construction")
        case Failure(f) => f.getMessage should equal("Failed to create the Opus encoder: invalid argument")
      }
    }

    it("encode and decode audio segments as Float types") {
      Given("a PCM file coded as an array of short integers and a codec pair")
      enc.reset
      dec.reset
      When("the audio is encoded and then decoded")
      val coded = for (c <- chunksFloat) yield enc(c).get
      val decoded = for (c <- coded) yield decFloat(c).get

      Then("the number of packets in the original, coded and decoded streams should be the same")
      coded.length should equal(chunksFloat.length)
      decoded.length should equal(chunksFloat.length)

      And("the decoded packet length should be the same as the coded packet length")
      decoded.head.length should equal(chunksFloat.head.length)

      And("the decoded audio should sound the same as the original audio")
      // Break the input and output audio streams into 5ms chunks and compute the energy in each chunk
      val in = chunksFloat.toArray.flatten.grouped(40).toList
      val out = decoded.toArray.flatten.grouped(40).toList
      val eIn = for (a <- in) yield energy(a)
      val eOut = for (a <- out) yield energy(a)
      correlate(eIn, eOut) should be > 0.93
    }

    it("constructs decoders and encoders for all frequencies") {
      try {
        Given("a set of sampling frequencies for the encoders and decoders")
        val freqs = List(8000, 12000, 16000, 24000, 48000)
        When("they are constructed for different sample frequencies")
        val e = List(Encoder(Sf8000, 1), Encoder(Sf12000, 1), Encoder(Sf16000, 1), Encoder(Sf24000, 1), Encoder(Sf48000, 1))
        val d = List(Decoder(Sf8000, 1), Decoder(Sf12000, 1), Decoder(Sf16000, 1), Decoder(Sf24000, 1), Decoder(Sf48000, 1))
        Then("the encoder structures return the correct sample frequency it was configured for")
        for ((f, t) <- freqs zip e) {
          t.get.getSampleRate should equal(f)
        }
        e.map(_.get.cleanup())
        And("the decoder structures return the correct frequencies")
        for ((f, t) <- freqs zip d) {
          t.get.getSampleRate should equal(f)
        }
        d.map(_.get.cleanup())
      } catch {
        case e: Exception => fail(s"Received exception ${e.getMessage}")
      }
    }

    it("get and set the encoder parameters (tests opus_encoder_set/get_ctl call only)") {
      enc.setUseDtx(1)
      enc.getUseDtx should equal(1)
      enc.setUseDtx(0)
      enc.getUseDtx should equal(0)
    }

    it("get and set decoder parameters (tests opus_decoder_set/get_ctl call only)") {
      dec.setGain(10)
      dec.getGain should equal(10)
    }

    it("correctly returns error messages for a given error condition"){
      val msg = Opus.error_string(-3)
      msg should equal("internal error")
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
          (c, i) <- coded zip (0 until coded.length)
          p = if (i % 15 == 1) dec(chunkSize) else dec(c)
        } yield p.get
      val in = chunks.toArray.flatten.grouped(40).toList
      val out = decoded.toArray.flatten.grouped(40).toList
      val eIn = for (a <- in) yield energy(a)
      val eOut = for (a <- out) yield energy(a)
      val rho = correlate(eIn, eOut)
      //writeAudioFile("test-short-erasure.raw",decoded.toArray.flatten)
      rho should be > 0.91
    }

    it("decode erased packets for Float data") {
      enc.reset
      dec.reset
      val coded = for (c <- chunksFloat) yield enc(c).get
      val decoded = // Decode, dropping every 10th packet
        for {
          (c, i) <- coded zip (0 until coded.length)
          p = if (i % 15 == 1) decFloat(chunkSize) else decFloat(c)
        } yield p.get
      val in = chunksFloat.toArray.flatten.grouped(40).toList
      val out = decoded.toArray.flatten.grouped(40).toList
      val eIn = for (a <- in) yield energy(a)
      val eOut = for (a <- out) yield energy(a)
      val rho = correlate(eIn, eOut)
      rho should be > 0.91
    }
  }

  describe("Performance test will") {

    val repeats = 50

    it("meet basic encoder speed requirements") {
      enc.reset
      enc.setComplexity(2)
      val tStart = System.currentTimeMillis()
      for (i <- 0 until repeats) {
        for (c <- chunks) enc(c)
      }
      val duration = (System.currentTimeMillis() - tStart) / 1000.0 // Seconds
      val speed = repeats * nSamples / duration / 8000.0 // multiple of real time
      speed should be > 100.0
      info(f"Encoder runs at $speed%5.1f times real time")
    }

    it("meets basic decoder speed requirements") {
      enc.reset
      dec.reset
      val tStart = System.currentTimeMillis()
      val coded = for (c <- chunks) yield enc(c).get
      for (i <- 0 until repeats) {
        for (c <- coded) dec(c)
      }
      val duration = (System.currentTimeMillis() - tStart) / 1000.0 // Seconds
      val speed = repeats * nSamples / duration / 8000.0 // multiple of real time
      speed should be > 500.0
      info(f"Decoder runs at $speed%5.1f times real time")
    }

  }

}
