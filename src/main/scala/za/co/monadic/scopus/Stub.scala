package za.co.monadic.scopus

import scala.util.Try

/**
 *
 */
object Stub extends App {

  val enc = Encoder(Sf8000, 1, Audio)
  enc.setUseDtx(1)
  // Transmit special short packets if silence is detected

  val dec = Decoder(Sf8000, 1)

  val coded: Try[Array[Byte]] = enc(new Array[Short](160))
  // Transmit

  // On receive end
  val decoded: Try[Array[Short]] = dec(coded.get)

  println(Speex.get_version_string())

  // Send decoded packet off

  println("Done....")
}
