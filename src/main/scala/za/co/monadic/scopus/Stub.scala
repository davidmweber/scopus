package za.co.monadic.scopus

import scala.util.{Try, Failure, Success}

/**
 *
 */
object Stub extends App {

  val enc = Encoder(Sf8000, 1) getOrElse sys.exit(-1)
  enc.setUseDtx(1)
  // Transmit special short packets if silence is detected

  val dec = Decoder(Sf8000, 1) getOrElse sys.exit(-1)

  val coded: Try[Array[Byte]] = enc(new Array[Short](160))
  // Transmit

  // On receive end
  val decoded: Try[Array[Short]] = dec(coded.get)

  // Send decoded packet off

  println("Done....")
}
