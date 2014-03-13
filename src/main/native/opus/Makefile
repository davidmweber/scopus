
CC=gcc
INCLUDES = -I/usr/lib/jvm/java-7-openjdk-amd64/include/ -I/usr/local/include
CFLAGS = -Ofast -Wall -fPIC $(INCLUDES)
LDFLAGS = -Wl,-soname,libjni_opus.so,-rpath='$$ORIGIN' -shared 
LIBS = -lopus
SOURCES = za_co_monadic_scopus_Opus__.c
TARGETLIB = libjni_opus.so

all: libjni_opus.so

clean:
	rm -f *.o *~ $(TARGET_LIB)

$(TARGETLIB) : $(SOURCES:.c=.o)
	$(CC) $(LDFLAGS) -o $@ $< $(LIBS)

install: $(TARGETLIB)
	cp $(TARGETLIB) ../../../../lib/native/linux-64/

.c.o:
	$(CC) -c  $(CFLAGS) $< -o $@

