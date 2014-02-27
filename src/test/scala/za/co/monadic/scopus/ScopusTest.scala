
import java.io.{BufferedInputStream, FileInputStream, DataInputStream}
import org.scalatest._
import za.co.monadic.scopus.Decoder

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

  describe("Opus codec can") {

    it("encode and decode audio segments as Short types") {
      Given("a PCM file coded as an array of short integers")
      val audio = readAudioFile("test/audio_samples/torvalds-says-linux.int.raw")
      val chunks = audio.grouped(160).toList // 20ms chunks of audio in 1 statement. Gotta love Scala :)
      val enc = new Encoder(8000,1)
      val dec = new Decoder(8000,1)

      When("the audio is encoded and then decoded")
      val coded = for (c <- chunks) yield enc.encode(c)
      val decoded  = for (c <- coded)  yield dec.decode(c)

      Then("the number of packets in the original, coded and decoded streams should be the same")
      coded.length should equal (chunks.length)
      decoded.length should equal (chunks.length)

      And("the decoded packet length should be the same as the coded packet length")
      coded.head.length should equal (chunks.head.length)

      And("the decoded audio should sound the same as the original audio")
    }

    it("encode and decode audio segments as Float types") (pending)
    it("get and set all the encoder parameters") (pending)
    it("get and set all the decoder parameters") (pending)
    it("deal with FEC on decode") (pending)
    it("decode erased packets for Short data")(pending)
    it("decode erased packets for Float data")(pending)

  }
}
