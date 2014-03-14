
CC=gcc
NATIVE = ../../resources/native/Linux/amd64/
INCLUDES = -I/usr/lib/jvm/java-7-openjdk-amd64/include/ -I/usr/local/include
CFLAGS = -Ofast -Wall -fPIC $(INCLUDES)
LDFLAGS = -Wl,-soname,libjni_opus.so,-rpath='$$ORIGIN' -shared
LDPATH = -L$(NATIVE)
LIBS = -lopus
SOURCES = za_co_monadic_scopus_Opus__.c
TARGETLIB = libjni_opus.so

all: libjni_opus.so

clean:
	rm -f *.o *~ $(TARGETLIB)

$(TARGETLIB) : $(SOURCES:.c=.o)
	$(CC) $(LDFLAGS) $(LDPATH) -o $@ $< $(LIBS)

install: $(TARGETLIB)
	cp $(TARGETLIB) $(NATIVE)

.c.o:
	$(CC) -c  $(CFLAGS) $< -o $@

