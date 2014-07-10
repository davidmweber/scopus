package za.co.monadic.scopus

import za.co.monadic.scopus.opus.Opus
import za.co.monadic.scopus.speex.Speex

/**
 * Load all the native libraries
 */
object Libraries {

  // System dependent load of native libraries
  LibLoader.getOsArch match {
    case "Linux/amd64" =>
      LibLoader("libopus.so.0", load = false) // Don't load this as it is dynamically found by the linker in Linux
      LibLoader("libspeex.so.1", load = false)
      LibLoader("libjni_opus.so")
    case "Linux/i386" =>
      LibLoader("libopus.so.0", load = false)
      LibLoader("libjni_opus.so")
    case "Mac OS X/x86_64" =>
      LibLoader("libopus.0.dylib", load = false)
      LibLoader("libspeex.1.dylib", load = false)
      LibLoader("libjni_opus.dylib")
    case s: String =>
      println(s"Unknown OS/platform combination: $s")
      sys.exit(-1)
  }
  // Verify we have the correct library loaded. Linux sometimes messes this up.
  if (Opus.get_version_string() != "libopus 1.1")
    throw new RuntimeException("libopus version must be 1.1")

  if (Speex.get_version_string() != "1.2rc1")
    throw new RuntimeException("libspeex version must be 1.2rc1")

  def apply() = Unit
}
