Scotus
------
Scotus is a Scala interface to the [Opus codec](http://www.opus-codec.org). It is light and thin by design and gives programmers access to the bulk of the functionality in Opus. It uses [JNaerator](https://code.google.com/p/jnaerator/) and [Bridj](https://code.google.com/p/nativelibs4java/) to handle the native code and is presently restricted to Linux code.

It is capable of encoding and decoding an audio stream but should be considered very alpha

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


Caveats
-------
Some functions in the Opus native library do not resolve properly and are not accessible. JNaerator emits warnings for these functions. Fortunately, the crucial functions for mono and stereo coding do resolve correctly.
