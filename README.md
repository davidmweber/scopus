[![Build Status](https://travis-ci.org/davidmweber/scopus.png?branch=master)](https://travis-ci.org/davidmweber/scopus)
Scopus
------
Scopus is a Scala interface to the [Opus codec](http://www.opus-codec.org). It is light and thin by design and gives
programmers access to the bulk of the functionality in Opus.
It uses JNI to handle the native code and works for Linux and OSX.
Benchmarks performed on the encoder show that it is 9% slower than a native C implementation. On a 3.5Ghz i5, it runs
at 380 times real time (complexity factor set to 2). The decoder runs at around 1600 times real time. A native
benchmark shows the encoder to run at 412 times real time.

The sources for Opus can be downloaded [here](http://www.opus-codec.org/downloads/).

Licensing
---------
Scopus by David Weber is is released under the [Creative Commons Attribution 4.0 International License](https://creativecommons.org/licenses/by/4.0/).

Building
--------
Clone the sources and do a `sbt test`. Once it works on multiple architectures, I'll put it on Maven Central.

The native libraries can be built using the makefiles in the src/native/opus directory. You do need to have the
Opus libraries and headers installed and copied to the appropriate native directories in the `resource` directory.
You may have to customise the paths to the include file (jni.h) a little. Both libjni-opus.{so,dylib} and
libopus.{so.0,0.dylib} should be placed in the appropriate `resources/native/` directory.
Some resources to understanding this process are:

* [Benson Margulies' JNI test bed](https://github.com/bimargulies/jni-origin-testbed) shows how to avoid `LD_LIBRARY_PATH` hell.
* Read the `ld` man page, especially the bit about -rpath=$ORIGIN (Linux) and @loader_path (OSX). It makes the magic work.
* For OSX, `otool` and `install_name_tool` are your friends. See [here](http://www.tribler.org/trac/wiki/MacBinaries) for more details.

This library explicitly copies the dynamic libraries out of the resources directory in the jar file to
a unique location (`/tmp/scopus_418af7c0b63` on Linux systems) from where it loads them. Note that if you have
existing installations of Opus, the linker will likely find that library rather than the bundled one (demonstrated for
Linux). This may cause the test cases to fail.

If you feel the need to generate header prototypes, build the Scala code then run `javah` from the root directory
of the project as follows:

```bash
javah  -classpath target/scala-2.10/classes:/usr/local/lib/scala-2.10.3/lib/*\
    -d src/main/native/opus/ -stubs  za.co.monadic.scopus.Opus$
```

You may have to adjust your classpath for your Scala installation.

Usage
-----

Encoding a stream is pretty simple.

```scala
  val enc = Encoder(Sf8000,1)
  enc.setSetDtx(1)

  //...

  val audio : Array[Soort] = getAudioFromSomewhere()
  val encoded = enc.encode(audio)

  // Send compressed audio to wherever
```

Decoding is just as simple:
```scala
  val dec = Decoder(Sf8000,1)

  //...
  val packet: Array[Byte] = getCompressedPacket()
  val audio = enc.encode(codedPacket)

  // Play audio....
```

There are restrictions on the size of the input buffer. Audio frames may be one of the following durations: 2.5, 5, 10, 20, 40 or 60 ms.
Smaller values obviously give less delay but at the expense of slightly less efficient compression.
Note that Java is big endian while most raw audio data are little endian (at least on Intel Architectures). This
means you may have to do some byte swapping when reading audio streams from external sources.
