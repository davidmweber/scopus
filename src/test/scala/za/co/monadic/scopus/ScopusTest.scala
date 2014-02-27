
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

  // Imperative all the way, but we are working with Arrays. Purists, look away now.
  def chunk[A: Manifest](buf: Array[A],chunkSize: Int): List[Array[A]] = {
    var cnt = 0
    var chunkCnt = 0
    val chunkList = List[Array[A]]()
    var chunk = new Array[A](chunkSize)
    do {
      chunk(chunkCnt) = buf(cnt)
      chunkCnt += 1
      cnt += 1
      if (chunkCnt == chunkSize) {
        chunkCnt = 0
        chunkList ++ chunk
        chunk = new Array[A](chunkSize)
      }
    } while (cnt < buf.length)
    chunkList
  }

  describe("Opus codec can") {

    it("encode and decode audio segments as Short types") {
      Given("a PCM file coded as an array of short integers")
      val uAudio = readAudioFile("torvalds-says-linux.int.raw")
      val chunks = chunk(uAudio,160) // 20ms chunks of audio spewed at the encoder
      val coded = List[Array[Byte]]()
      val enc = new Encoder(8000,1)
      val dec = new Decoder(8000,1)
      val decoded = List[Array[Short]]()

      When("the audio is encoded and decoded")
      for (c <- chunks) {
        coded ++ enc.encode(c)
      }
      for (c <- coded) {
        decoded ++ dec.decode(c)
      }

      Then("the number of packets in the original, doded and decoded streams should be the same")
      coded.length should equal (chunks.length)
      decoded.length should equal (chunks.length)

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
