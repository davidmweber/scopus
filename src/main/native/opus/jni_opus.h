#include <jni.h>

#ifndef JNI_OPUS_INC
#define JNI_OPUS_INC
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_Opus_00024_decode_1float
  (JNIEnv *, jclass, jlong, jbyteArray, jint, jfloatArray, jint, jint);

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_Opus_00024_decode
  (JNIEnv *, jclass, jlong, jbyteArray, jint, jshortArray, jint, jint);

JNIEXPORT jlong JNICALL Java_za_co_monadic_scopus_Opus_00024_decoder_1create
  (JNIEnv *, jclass, jint, jint);

JNIEXPORT void JNICALL Java_za_co_monadic_scopus_Opu_00024s_decoder_1destroy
  (JNIEnv *, jclass, jlong);

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_Opus_00024_encode
  (JNIEnv *, jclass, jlong, jbyteArray, jint, jbyteArray, jint);

JNIEXPORT jlong JNICALL Java_za_co_monadic_scopus_Opus_00024_encoder_1create
  (JNIEnv *, jclass, jint, jint);

JNIEXPORT void JNICALL Java_za_co_monadic_scopus_Opus_00024_encoder_1destroy
  (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif
