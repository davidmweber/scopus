<!-- [![Build Status](https://travis-ci.org/davidmweber/scopus.png?branch=master)](https://travis-ci.org/davidmweber/scopus) --!>
Scopus
------
Scopus is a Scala interface to the [Opus codec](http://www.opus-codec.org). It is light and thin by design and gives
programmers access to the bulk of the functionality in Opus.
It uses [JNaerator](https://code.google.com/p/jnaerator/) to handle the native code and is presently restricted to Linux code.
Benchmarks performed on the encoder show that it is 9% slower than a native C implementation. On a 3.5Ghz i5, it runs
at 120 times real time. The decoder runs at around 1300 times real time.

The relevant headers for [Opus](http://www.opus-codec.org) are included in the source code as is the Linux 64bit library.
The sources for Opus can be downloaded [here](http://www.opus-codec.org/downloads/).

Licensing
---------
Scopus is released under the [Creative Commons Attribution 4.0 International License](https://creativecommons.org/licenses/by/4.0/) with the exception of
files in the `opus-1.1/include` directory which are subject to the [Opus License](http://www.opus-codec.org/license/).

Building
--------
Clone the sources and do a `sbt test`. Once it works on multiple architectures, I'll put it on Maven Central.
Using JNaerator is pretty simple (thanks Oliver!). The following steps should see you through to obtaining a
Java interface to the Opus native library:

First, [download the JNaerator jar](https://code.google.com/p/jnaerator/downloads/list). I used the `shaded-0.11`
version but the latest snapshot (July 2013) works.
```bash
cd opus-1.1
java -jar <path to jnaerator>/jnaerator-0.11-shaded.jar
```

You should end up with an new version of `opus.jar` in the `lib` directory.The parameters used for the build are in `opus-1.1/config.jnaerator`.

Note that it uses [Bridj](https://code.google.com/p/nativelibs4java/) instead of [JNA](https://github.com/twall/jna) because
straight JNA is significantly slower than Bridj and "direct" JNA does not support the varargs needed for certain Opus functions.


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
Some functions in the Opus native library do not resolve properly and are not accessible. JNaerator emits warnings for these functions. Fortunately, the crucial functions for mono and stereo coding do resolve correctly.
