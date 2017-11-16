/*
 * Copyright David Weber 2014
 * Released under the Creative Commons License (http://creativecommons.org/licenses/by/4.0/legalcode)
 */
package za.co.monadic.scopus

import java.util.UUID
import java.io.{FileOutputStream, File}
import java.nio.channels.Channels

/**
  * General loader for dynamic libraries from the resources directory in the jar file.
  * It copies the required library from the jar to the target temporary directory and then
  * optionally loads the file using System.load(). It will clean the directory on JVM
  * shutdown using shutdown hooks. It is clever enough to find the correct
  * directory for the native version of the library (although this is untested). Note that
  * you need to build the JNI native stubs with some care so that dependent libraries
  * can be loaded. In Linux for example, Java must load libjni_opus.so (JNI native code).
  * The actual Opus library is loaded by the dynamic linker from a dependency in the stub.
  * Libjni_opus.so is built to look in the same directory for libopus, so we must copy
  * it to a temporary directory as well, but there is no need to ask the JVM to load it.
  * In Windows, you will need to load it as well.
  *
  * Libraries are stored in the resources/native/os.name/os.arch directory and copied
  * to the system temporary directory as indicated by the java.io.tmpdir property plus
  * a random one time directory name with the prefix "scopus_". In Linux, the temporary
  * directory would have a name like "/tmp/scopus_418af7c0b63b/".
  */
object LibLoader {

  private val tempPath = "scopus_" + UUID.randomUUID.toString.split("-").last
  private val path     = "native/" + getOsArch
  private val destDir  = System.getProperty("java.io.tmpdir") + "/" + tempPath + "/"

  // Create the temporary directory
  new File(destDir).mkdirs()

  def getOsArch: String = System.getProperty("os.name") + "/" + System.getProperty("os.arch")

  /**
    * Copy the OS dependent library from the resources dir in a JAR to a temporary location
    * and optionally ask the JVM to load the library. The library is deleted on exit from the
    * JVM. If load is false, it just copies the library to the temporary location without loading it.
    * @param libName The name of the library to copy and optionally load
    * @param load If true, ask the JVM to dynamically load the library using System.load()
    */
  def apply(libName: String, load: Boolean = true): Unit = {
    try {
      val source  = Channels.newChannel(LibLoader.getClass.getClassLoader.getResourceAsStream(path + "/" + libName))
      val fileOut = new File(destDir, libName)
      val dest    = new FileOutputStream(fileOut)
      dest.getChannel.transferFrom(source, 0, Long.MaxValue)
      source.close()
      dest.close()

      // Tee up deletion of the temporary library file when we quit
      sys.addShutdownHook {
        new File(destDir, libName).delete
        // Attempt to delete the directory also. It goes if the directory is empty
        new File(destDir).delete
      }
      // Finally, load the dynamic library if required
      if (load) System.load(fileOut.getAbsolutePath)
    } catch {
      // This is pretty catastrophic so bail.
      case e: Exception =>
        println(s"Fatal error in LibLoader while loading $path/$libName: ${e.getMessage}")
        sys.exit(-1)
    }
  }
}
