
import java.io.{BufferedInputStream, FileInputStream, DataInputStream}
import org.scalatest._
import za.co.monadic.scopus._
import Numeric.Implicits._

class ScopusTest extends FunSpec with Matchers with GivenWhenThen {


  def swap(in: Short): Short = ((in >> 8) + ((in & 0xff) << 8)).asInstanceOf[Short]

  def readAudioFile(file: String): Array[Short] = {
    val stream = new FileInputStream(file)
    val infile = new DataInputStream(new BufferedInputStream(stream))
    val len = stream.getChannel.size().asInstanceOf[Int]
    val audio = new Array[Short](len/2)
    for (i<- 0 to len/2-1) audio(i) = swap(infile.readShort())
    infile.close()
    audio
  }

  def sqr[A : Numeric](a:A):A = a * a

  def energy(audio: Array[Short]) : Double = {
      audio.map( a => sqr(a.toDouble/(1<<15))).sum / audio.length.toDouble
  }

  def energy(audio: Array[Float]) : Double = {
    audio.map( a => sqr(a)).sum / audio.length.toDouble
  }

  def correlate(a:List[Double], b: List[Double]): Double = {
    val top = (for ((x,y) <- a zip b ) yield x*y).sum/a.length
    val bottom = math.sqrt(a.map(sqr(_)).sum/a.length) * math.sqrt(b.map(sqr(_)).sum/a.length)
    top/bottom
  }

  val audio = readAudioFile("test/audio_samples/torvalds-says-linux.int.raw")
  val audioFloat = audio.map( _.toFloat/(1<<15)) // Normalise to +-1.0
  val nChunks = (audio.length / 160) * 160
  // A list of 20ms chunks of audio rounded up to a whole number of blocks. Gotta love Scala :)
  val chunks = audio.slice(0,nChunks).grouped(160).toList
  val chunksFloat = audioFloat.slice(0,nChunks).grouped(160).toList

  val enc = Encoder(Sf8000,1)
  val dec = Decoder(Sf8000,1)

  describe("Opus codec can") {

    it("encode and decode audio segments as Short types") {
      Given("a PCM file coded as an array of short integers and a codec pair")
      enc.reset
      dec.reset
      When("the audio is encoded and then decoded")
      val coded = for (c <- chunks) yield enc.encode(c)
      val decoded  = for (c <- coded)  yield dec.decode(c)

      Then("the number of packets in the original, coded and decoded streams should be the same")
      coded.length should equal (chunks.length)
      decoded.length should equal (chunks.length)

      And("the decoded packet length should be the same as the coded packet length")
      decoded.head.length should equal (chunks.head.length)

      And("the decoded audio should sound the same as the original audio")
      // Break the input and output audio streams into 5ms chunks and compute the energy in each chunk
      val in = chunks.toArray.flatten.grouped(40).toList
      val out = decoded.toArray.flatten.grouped(40).toList
      val eIn = for (a <- in) yield energy(a)
      val eOut = for (a <- out) yield energy(a)
      correlate(eIn,eOut) should be > 0.94
    }

    it("encode and decode audio segments as Float types") {
      Given("a PCM file coded as an array of short integers and a codec pair")
      enc.reset
      dec.reset
      When("the audio is encoded and then decoded")
      val coded = for (c <- chunksFloat) yield enc.encode(c)
      val decoded  = for (c <- coded)  yield dec.decodeFloat(c)

      Then("the number of packets in the original, coded and decoded streams should be the same")
      coded.length should equal (chunksFloat.length)
      decoded.length should equal (chunksFloat.length)

      And("the decoded packet length should be the same as the coded packet length")
      decoded.head.length should equal (chunksFloat.head.length)

      And("the decoded audio should sound the same as the original audio")
      // Break the input and output audio streams into 5ms chunks and compute the energy in each chunk
      val in = chunksFloat.toArray.flatten.grouped(40).toList
      val out = decoded.toArray.flatten.grouped(40).toList
      val eIn = for (a <- in) yield energy(a)
      val eOut = for (a <- out) yield energy(a)
      correlate(eIn,eOut) should be > 0.94
    }

    it("constructs decoders and encoders for all frequencies") {
      try {
        val e = List(Encoder(Sf8000,1),Encoder(Sf16000,1),Encoder(Sf24000,1),Encoder(Sf48000,1))
        val d = List(Decoder(Sf8000,1),Decoder(Sf16000,1),Decoder(Sf24000,1),Decoder(Sf48000,1))
        e.map(_.cleanup())
        d.map(_.cleanup())
      } catch {
        case e: Exception => fail(s"Received exception ${e.getMessage}")
      }
    }

    it("get and set all the encoder parameters") (pending)
    it("get and set all the decoder parameters") (pending)
    it("deal with FEC on decode") (pending)
    it("decode erased packets for Short data")(pending)
    it("decode erased packets for Float data")(pending)
  }

}
