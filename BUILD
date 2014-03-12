# Build JNI thunk layer
gcc -I/usr/lib/jvm/java-7-openjdk-amd64/include/ -I/usr/local/include -shared -Wl,-soname,libjni_opus.so -fPIC -o libjni_opus.so jni_opus.c -lopus
execstack -c libjni_opus.so  # There is a linker switch to make this obsolete.
cp libjni_opus.so ../../../../lib/native/linux-64/

 # Build the headers from the project root
 javah  -classpath target/scala-2.10/classes:/usr/local/lib/scala-2.10.3/lib/* -d src/main/native/opus/ -stubs  za.co.monadic.scopus.Opus$