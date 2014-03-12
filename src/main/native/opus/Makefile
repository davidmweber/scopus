
CC=gcc
CFLAGS = -Ofast -Wall -I/usr/lib/jvm/java-7-openjdk-amd64/include/ -I/usr/local/include -fPIC
LDFLAGS = -Wl,-soname,libjni_opus.so -shared 
LIBS = -lopus -lc
SOURCES = jni_opus.c
TARGETLIB = libjni_opus.so

all: libjni_opus.so

clean:
	rm -f *.o *~ $(TARGET_LIB)

$(TARGETLIB) : $(SOURCES:.c=.o)
	$(CC) $(LDFLAGS) -o $@ $< $(LIBS)
	execstack -c $@

install: $(TARGETLIB)
	cp $(TARGETLIB) ../../../../lib/native/linux-64/

.c.o:
	$(CC) -c  $(CFLAGS) $< -o $@

