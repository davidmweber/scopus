#include <stdio.h>
#include <stdlib.h>
#include <opus/opus.h>
#include <sys/time.h>

/* build using "gcc -O2 speed.c -lopus -o speed"*/
int main() {

  FILE *fp;
  fp = fopen("torvalds-says-linux.int.raw","rb");
  fseek(fp, 0L, SEEK_END);
  int sz = ftell(fp)/2;
  fseek(fp, 0L, SEEK_SET);
  printf("%d\n",sz);
  short *audio = (short *)calloc(sz,sizeof(short));
  unsigned char *decoded = malloc(1000);
  int n = fread(audio, sizeof(short), sz, fp);
  printf("%d\n",n);

  int error;
  OpusEncoder *enc;
  enc = opus_encoder_create(8000, 1, OPUS_APPLICATION_VOIP, &error);
  if (error == 0) {
    int count = 0;
    int i = 0;
    struct timeval t1,t2;
    float duration;
    gettimeofday(&t1,NULL);
    for (i = 0; i < 100; i++) {
      while ((count + 160) < sz) {
	int len = opus_encode(enc,audio+count,160,decoded,1000);
	count += 160;
      }
      count = 0;
    }
    gettimeofday(&t2,NULL);
    duration = (t2.tv_sec - t1.tv_sec);
    duration += (t2.tv_usec - t1.tv_usec) / 1e6;
    printf("%10.5f\n",100*sz/duration/8000.0);
  } else {
    printf("Failed to create the encoder\n");
  }

}
