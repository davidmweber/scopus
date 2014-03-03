
import java.io._
import org.scalatest._
import za.co.monadic.scopus._
import Numeric.Implicits._

class ScopusTest extends FunSpec with Matchers with GivenWhenThen with BeforeAndAfterAll {

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
  def writeAudioFile(file:String, data: Array[Short]): Unit = {
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

  def correlate(a: List[Double], b: List[Double]): Double = {
    val top = (for ((x, y) <- a zip b) yield x * y).sum / a.length
    val bottom = math.sqrt(a.map(sqr(_)).sum / a.length) * math.sqrt(b.map(sqr(_)).sum / a.length)
    top / bottom
  }

  val audio = readAudioFile("test/audio_samples/torvalds-says-linux.int.raw")
  val audioFloat = audio.map(_.toFloat / (1 << 15))
  // Normalise to +-1.0
  val nChunks = (audio.length / 160) * 160
  // A list of 20ms chunks of audio rounded up to a whole number of blocks. Gotta love Scala :)
  val chunks = audio.slice(0, nChunks).grouped(160).toList
  val chunksFloat = audioFloat.slice(0, nChunks).grouped(160).toList

  val enc = Encoder(Sf8000, 1)
  val dec = Decoder(Sf8000, 1)

  override def afterAll() = {
    enc.cleanup()
    dec.cleanup()
  }

  describe("Opus codec can") {

    it("encode and decode audio segments as Short types") {
      Given("a PCM file coded as an array of short integers and a codec pair")
      enc.reset
      dec.reset
      enc.getSampleRate should equal(8000)
      dec.getSampleRate should equal(8000)
      When("the audio is encoded and then decoded")
      val coded = for (c <- chunks) yield enc.encode(c)
      val decoded = for (c <- coded) yield dec.decode(c)

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
      correlate(eIn, eOut) should be > 0.94 // This is a pretty decent test if all is well
      // Uncomment for audible verification.
      //writeAudioFile("test-short.raw",decoded.toArray.flatten)
    }

    it("encode and decode audio segments as Float types") {
      Given("a PCM file coded as an array of short integers and a codec pair")
      enc.reset
      dec.reset
      When("the audio is encoded and then decoded")
      val coded = for (c <- chunksFloat) yield enc.encode(c)
      val decoded = for (c <- coded) yield dec.decodeFloat(c)

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
      correlate(eIn, eOut) should be > 0.94
    }

    it("constructs decoders and encoders for all frequencies") {
      try {
        val freqs = List(8000, 12000, 16000, 24000, 48000)
        val e = List(Encoder(Sf8000, 1), Encoder(Sf12000, 1), Encoder(Sf16000, 1), Encoder(Sf24000, 1), Encoder(Sf48000, 1))
        val d = List(Decoder(Sf8000, 1), Decoder(Sf12000, 1), Decoder(Sf16000, 1), Decoder(Sf24000, 1), Decoder(Sf48000, 1))
        for ((f, t) <- freqs zip e) {
          t.getSampleRate should equal(f)
        }
        e.map(_.cleanup())
        for ((f, t) <- freqs zip d) {
          t.getSampleRate should equal(f)
        }
        d.map(_.cleanup())
      } catch {
        case e: Exception => fail(s"Received exception ${e.getMessage}")
      }
    }

    it("get and set the encoder parameters (tests opus_encoder_ctl call only)") {
      enc.setUseDtx(1)
      enc.getUseDtx should equal(1)
      enc.setUseDtx(0)
      enc.getUseDtx should equal(0)
    }

    it("get and set decoder parameters (tests opus_decoder_ctl call only)") {
      dec.setGain(10)
      dec.getGain should equal(10)
    }

    it("decode erased packets for Short data") {
      enc.reset
      dec.reset
      val coded = for (c <- chunks) yield enc.encode(c)
      val decoded = // Decode, dropping every 100th packet
        for {
          (c, i) <- coded zip (0 until coded.length)
          p = if (i % 10 == 1) dec.decode() else dec.decode(c)
        } yield p
      val in = chunks.toArray.flatten.grouped(40).toList
      val out = decoded.toArray.flatten.grouped(40).toList
      val eIn = for (a <- in) yield energy(a)
      val eOut = for (a <- out) yield energy(a)
      val rho = correlate(eIn, eOut)
      //writeAudioFile("test-short-erasure.raw",decoded.toArray.flatten)
      rho should be > 0.92
    }

    it("decode erased packets for Float data") {
      enc.reset
      dec.reset
      val coded = for (c <- chunksFloat) yield enc.encode(c)
      val decoded = // Decode, dropping every 100th packet
        for {
          (c, i) <- coded zip (0 until coded.length)
          p = if ( i % 10 == 1) dec.decodeFloat() else dec.decodeFloat(c)
        } yield p
      val in = chunksFloat.toArray.flatten.grouped(40).toList
      val out = decoded.toArray.flatten.grouped(40).toList
      val eIn = for (a <- in) yield energy(a)
      val eOut = for (a <- out) yield energy(a)
      val rho = correlate(eIn, eOut)
      rho should be > 0.92
    }
  }

}
