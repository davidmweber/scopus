#include <jni.h>
/* Header for class za_co_monadic_scopus_Speex__ */

#ifndef _Included_za_co_monadic_scopus_speex_Speex__
#define _Included_za_co_monadic_scopus_speex_Speex__
#ifdef __cplusplus
extern "C" {
#endif


JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_encoder_1ctl(JNIEnv *env, jobject clazz, jlong encoder, jint command, jint value);

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_decoder_1ctl(JNIEnv *env, jobject clazz, jlong decoder, jint command, jint value);

JNIEXPORT jlong JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_encoder_1create(JNIEnv *env, jobject clazz, jint modeID);

JNIEXPORT void JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_encoder_1destroy(JNIEnv *env, jobject clazz, jlong state);

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_encode_1short
   (JNIEnv *env, jobject clazz, jlong encoder, jshortArray input, jint len_in, jbyteArray coded, jint len_out);

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_encode_1float
   (JNIEnv *env, jobject clazz, jlong encoder, jfloatArray input, jint len_in, jbyteArray coded, jint len_out);

JNIEXPORT jstring JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_get_1version_1string(JNIEnv *env, jobject clazz);

JNIEXPORT jlong JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_decoder_1create(JNIEnv *env, jobject clazz, jint modeID, int enhance);

JNIEXPORT void JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_decoder_1destroy(JNIEnv *env, jobject clazz, jlong state_ptr);

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_decode_1float
   (JNIEnv *env, jobject clazz, jlong decoder, jbyteArray input, jint len_in, jfloatArray decoded, jint len_out);

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_decode_1short
    (JNIEnv *env, jobject clazz, jlong decoder, jbyteArray input, jint len_in, jshortArray decoded, jint len_out);

#ifdef __cplusplus
}
#endif
#endif
