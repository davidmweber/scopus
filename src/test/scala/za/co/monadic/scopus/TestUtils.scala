package za.co.monadic.scopus

import java.io._
import Numeric.Implicits._
import scala.util.Random

/**
  *
  */
object TestUtils {

  def shortGauss(): Short = {
    Math.round(Random.nextGaussian() * 10000.0).toShort
  }

  // Byteswap. Needed for testing on Intel architectures
  def swap(in: Short): Short = ((in >> 8) + ((in & 0xff) << 8)).asInstanceOf[Short]

  def readAudioFile(file: String): Array[Short] = {
    val stream = new FileInputStream(file)
    val infile = new DataInputStream(new BufferedInputStream(stream))
    val len    = stream.getChannel.size().asInstanceOf[Int]
    val audio  = new Array[Short](len / 2)
    for (i <- 0 until len / 2) audio(i) = swap(infile.readShort())
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
    val stream  = new FileOutputStream(file)
    val outfile = new DataOutputStream(new BufferedOutputStream(stream))
    for (i <- data.indices) outfile.writeShort(swap(data(i)))
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
    val top  = (for ((x, y) <- a zip b) yield (x - aAve) * (y - bAve)).sum
    val bottom = math.sqrt(a.map((t: Double) => sqr(t - aAve)).sum) * math.sqrt(
        b.map((t: Double) => sqr(t - bAve)).sum)
    top / bottom
  }

}
