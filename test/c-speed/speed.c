#include <stdio.h>
#include <stdlib.h>
#include <opus/opus.h>
#include <sys/time.h>

/* build using "gcc -O2 speed.c -lopus -o speed"*/
int main() {

  FILE *fp;
  fp = fopen("../audio_samples/torvalds-says-linux.int.raw","rb");
  fseek(fp, 0L, SEEK_END);
  int sz = ftell(fp)/2;
  fseek(fp, 0L, SEEK_SET);
  printf("%d\n",sz);
  short *audio = (short *)calloc(sz,sizeof(short));
  unsigned char *decoded = malloc(1000);
  short *coded = (short *)calloc(1000,sizeof(short));
  int n = fread(audio, sizeof(short), sz, fp);
  printf("%d\n",n);

  int error;
  OpusEncoder *enc;
  OpusDecoder *dec;
  enc = opus_encoder_create(8000, 1, OPUS_APPLICATION_VOIP, &error);
  dec = opus_decoder_create(8000, 1, &error);
  if (error == 0) {
    int ret = 0;
    error = opus_encoder_ctl(enc,OPUS_SET_COMPLEXITY(2));
    error = opus_encoder_ctl(enc,OPUS_SET_SIGNAL(OPUS_SIGNAL_VOICE));
    printf("Complexity = %d\n", ret);
    int count = 0;
    int i = 0;
    struct timeval t1,t2;
    float duration;
    gettimeofday(&t1,NULL);
    int j = 0;
    for (i = 0; i < 100; i++) {
      while ((count + 160) < sz) {
	int len = opus_encode(enc,audio+count,160,decoded,1000);
/*
	if (j % 20 == 3) {
	   //opus_decoder_ctl(dec,OPUS_RESET_STATE);
	   len = opus_decode(dec,0,0,coded,1000,0);
	} else {
	   len = opus_decode(dec,decoded,len,coded,1000,0);
	}
	j++;
	printf("%d\n",len);
*/
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
