This plans to become an interface into the Opus codec from Scala.
It uses [JNaerator](https://code.google.com/p/jnaerator/) and [Bridj](https://code.google.com/p/nativelibs4java/)
to handle the native code and is presently restricted to Linux code.

The relevant headers for [Opus](http://www.opus-codec.org) are included in the source code as is the Linux 64bit library.
The sources for Opus can be downloaded [here](http://www.opus-codec.org/downloads/).

Licensing
---------
Scopus is released under the [Creative Commons](https://creativecommons.org/licenses/by/4.0/) with the exception that
files in the `opus-1.1/include` directory which are subject to the [Opus License](http://www.opus-codec.org/license/).
Basically, play nice.

<a rel="license" href="http://creativecommons.org/licenses/by/4.0/">
<img alt="Creative Commons Licence" style="border-width:0" src="http://i.creativecommons.org/l/by/4.0/80x15.png" />
</a><br />This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by/4.0/">Creative Commons Attribution 4.0 International License</a>.

Building
--------
Clone the sources and do a `sbt test`. Once it works on multiple architectures, I'll put it on Maven Central.
Using JNaerator is pretty simple (thanks Oliver!). The following steps should see you through to obtaining a
JAR file with the OPUS JNI:

First, [download the JNaerator jar](https://code.google.com/p/jnaerator/downloads/list). I used the `shaded-0.11`
version.
```bash
cd opus-1.1
java -jar <path to jnaerator>/jnaerator-0.11-shaded.jar
```

The parameters used for the build are in `opus-1.1/config.jnaerator`.
You should end up with an new version of `Opus.jar` in the `lib` directory.
(TODO: Release the Opus jar separately to Maven....)