[![Build Status](https://img.shields.io/travis/davidmweber/scopus.svg)](https://travis-ci.org/davidmweber/scopus)
[![Maven Central](https://img.shields.io/maven-central/v/za.co.monadic/scopus_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Cscopus)

Scopus
------

Scopus is a Scala interface to the [Opus 1.3.1](http://www.opus-codec.org) and 
g.711u codecs. There is also a PCM "codec" which is effectively a NULL codec and is useful 
in testing.

It is light and thin by design and gives programmers access to the bulk
of the functionality to the codecs. It
uses JNI to handle the native code and works for Linux (amd64 and i386) and
OSX. Benchmarks performed on the encoder show that it is 10% slower than a
native C implementation. For example, on a 3.5Ghz i5, the Opus coder runs at
360 times real time (complexity factor set to 2). The Opus decoder runs at around
1600 times real time. A native benchmark shows the encoder to run at 400 times
real time. The LLVM C compiler (clang v3.4) pips GCC by about 5%.

The sources for Opus can be downloaded [here](http://www.opus-codec.org/downloads/).

Licensing
---------
Scopus by David Weber is released under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)) license.

Building
--------
Clone the sources and do a `sbt test`. Package and deploy as you see fit.

The native libraries can be built using the makefiles in the `src/native/opus
directory`. You do need to have the Opus libraries and headers installed and
copied to the appropriate native directories in the `resource` directory. You
may have to customise the paths to the include file (jni.h) a little. The
libraries libjni-opus.{so,dylib} and libopus.{so.0,0.dylib} should be placed 
in the appropriate `resources/native/` directory. Some resources to understanding 
this process are:

* [Benson Margulies' JNI test bed](https://github.com/bimargulies/jni-origin-testbed) shows how to avoid `LD_LIBRARY_PATH` hell.
* Read the `ld` man page, especially the bit about -rpath=$ORIGIN (Linux) and @loader_path (OSX). It makes the magic work.
* For OSX, `otool` and `install_name_tool` are your friends. 

This library explicitly copies the dynamic libraries out of the resources
directory in the jar file to a unique location (something like
`/tmp/scopus_418af7c0b63` on Linux systems) from where it loads them. The
temporary files are deleted when the JVM exits. Note that if you have existing
installations of Opus, the linker will likely find that library rather than
the bundled one (demonstrated for Linux). This may cause the test cases to
fail.

The Opus codec performance improves if compiled with `clang`. Configure, build
and test it as follows:

```bash
CC=clang CFLAGS=-O3 ./configure
make
make check
```

Usage
-----
Scopus is available from the Sonatype Maven repo with builds for Scala 2.12 and 2.13. Add the
following dependency to your sbt build:

```scala
  libaryDependencies +=  "za.co.monadic" %% "scopus" % "0.4.0"
```

Encoding a stream is pretty simple. Return types are Scala are wrapped in a `Try[_]`
so it is up to you to manage errors reported by the decoder or the encoder.

```scala
   // Variable bit rate Opus encoder with discontinuous transmission
   val enc = OpusEncoder(Sf8000, 1, Audio).setUseDtx(1).setVbr(1)  

   // Corresponding decoder
   val dec = OpusDecoder(Sf8000, 1)

   val coded: Try[Array[Byte]] = enc(new Array[Short](160))
   // Transmit

   // On receive end
   val decoded: Try[Array[Short]] = dec(coded.get)

   // Send decoded packet off
```

There is now support for a G711 u-law encoder. The standard applies to 8kHz signals
but this codec will upsample and downsample the signal to achieve particular sampling
rate goals. If the encoder is set to decode to a 48kHz sampling rate, it expects an
8kHz u-Law codec signal which it first decodes to a linear signal then upsamples it
to reach 48kHz. Similarly, a decoder configured for 48kHz will first downsample the
signal to 8kHz then u-Law encode it. Stereo is not supported for this mode.

There are restrictions on the size of the input buffer. For Speex, all audio
frames must be 20ms long. For 8kHz sampling rate, this is 20ms. For Opus,
audio frames may be one of the following durations: 2.5, 5, 10, 20, 40 or 60
ms. Smaller values obviously give less delay but at the expense of slightly
less efficient compression. Note that Java is big endian while most raw audio
data are little endian (at least on Intel Architectures). This means you may
have to do some byte swapping when reading audio streams from external
sources.

Opus can handle two channels while the Speex codec is restricted to 1 channel.

Scala does not seem to have a [convention for error
handling](http://grokbase.com/t/gg/scala-user/1293fwp1je/trying-to-work-with-try). 
I went with [Try](http://www.scala-lang.org/api/2.10.3/index.html#scala.util.Try). 
If this is not how you think it should be done, read the link and make a case. 
Try can be flatmapped which is important in my application.

Future plans include adding codecs and abstracting the codec layer so they are
more pluggable. Pull requests welcome.

