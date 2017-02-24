package za.co.monadic.scopus

object ArrayConversion {

  /**
    * Re-interpret a byte array as an array of short. This means that two bytes are combined
    * to give a short.
    * @param in Input byte array
    * @return Converted short array
    */
  def byteArrayToShortArray(in: Array[Byte]): Array[Short] = {
    require(in.length % 2 == 0)
    val dest = new Array[Short](in.length / 2)
    var i    = 0
    while (i < dest.length) {
      dest(i) = (((in(2 * i + 1) & 0xff) << 8) | (in(2 * i) & 0xFF)).toShort
      i += 1
    }
    dest
  }

  /**
    * Re-interpret a short array as an array of bytes.
    * @param in Input Short array
    * @return Converted array of bytes
    */
  def shortArrayToByteArray(in: Array[Short]): Array[Byte] = {
    val dest = new Array[Byte](in.length * 2)
    var i    = 0
    while (i < in.length) {
      dest(2 * i) = (in(i) & 0xFF).toByte
      dest(2 * i + 1) = ((in(i) & 0xFF00) >> 8).toByte
      i += 1
    }
    dest
  }

  def floatArrayToByteArray(in: Array[Float]): Array[Byte] = {
    val dest = new Array[Byte](in.length * 4)
    var i    = 0
    while (i < in.length) {
      val temp = java.lang.Float.floatToIntBits(in(i))
      dest(4 * i) = (temp & 0xFF).toByte
      dest(4 * i + 1) = ((temp & 0xFF00) >> 8).toByte
      dest(4 * i + 2) = ((temp & 0xFF0000) >> 16).toByte
      dest(4 * i + 3) = ((temp & 0xFF000000) >> 24).toByte
      i += 1
    }
    dest
  }

  def byteArrayToFloatArray(in: Array[Byte]): Array[Float] = {
    require(in.length % 4 == 0)
    val dest = new Array[Float](in.length / 4)
    var i    = 0
    while (i < dest.length) {
      val temp = ((in(4 * i + 3) & 0xFF) << 24) |
          ((in(4 * i + 2) & 0xFF) << 16) |
          ((in(4 * i + 1) & 0xFF) << 8) |
          (in(4 * i) & 0xFF)
      dest(i) = java.lang.Float.intBitsToFloat(temp)
      i += 1
    }
    dest
  }
}
