<!-- [![Build Status](https://travis-ci.org/davidmweber/scopus.png?branch=master)](https://travis-ci.org/davidmweber/scopus) -->
Scopus
------
Scopus is a Scala interface to the [Opus codec](http://www.opus-codec.org). It is light and thin by design and gives
programmers access to the bulk of the functionality in Opus.
It uses JNI to handle the native code and is presently restricted to Linux code. JNA and Bridj both proved to
be slower than JNI and have some awkward limitations to boot.
Benchmarks performed on the encoder show that it is 9% slower than a native C implementation. On a 3.5Ghz i5, it runs
at 380 times real time (complexity factor set to 2). The decoder runs at around 1600 times real time. A native
benchmark shows the encoder to run at 412 times real time.

The relevant headers for [Opus](http://www.opus-codec.org) are included in the source code as is the Linux 64bit library.
The sources for Opus can be downloaded [here](http://www.opus-codec.org/downloads/).

Licensing
---------
Scopus is released under the [Creative Commons Attribution 4.0 International License](https://creativecommons.org/licenses/by/4.0/) with the exception of
files in the `opus-1.1/include` directory which are subject to the [Opus License](http://www.opus-codec.org/license/).

Building
--------
Clone the sources and do a `sbt test`. Once it works on multiple architectures, I'll put it on Maven Central.

The native libraries can be built using the makefile in the src/native/opus directory. You may have
to customise the paths to the include file (jni.h) a little. It also assumes you have opus-1.1 built
and installed. Both libjni-opus.so and libopus.so should be placed in the appropriate directory in the lib
directory. Getting the libs located on an architecture independent way is work in progress.

If you feel the need to generate header prototypes, build the Scala code then run `javah` from the root directory
of the project as follows:

```bash
javah  -classpath target/scala-2.10/classes:/usr/local/lib/scala-2.10.3/lib/*\
    -d src/main/native/opus/ -stubs  za.co.monadic.scopus.Opus$
```

You may have to adjust your classpath for your Scala installation.

Usage
-----

Encoding a stream is pretty simple. Java is big endian while most raw audio data are little endian (at least on Intel Architectures). Bridj takes
care of most endian issues for you so trade in arrays of Short and Float. Encoding is simple:

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

Caveats
-------
The erased packet functionality does not pass the test case I have set. It will be looked into. Also, my library searches from
the JVM need some work.