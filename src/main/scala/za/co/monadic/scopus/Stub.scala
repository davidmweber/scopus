package za.co.monadic.scopus

/**
 *
 */
object Stub extends App {
  val err = Array[Int](1)
  val tem = Opus.decoder_create(8000, 1, err)

  println(s"foo: ${err(0)} -> $tem")
  Opus.decoder_destroy(tem)
}
