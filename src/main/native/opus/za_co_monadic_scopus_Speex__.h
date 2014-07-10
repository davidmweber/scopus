#include <jni.h>
/* Header for class za_co_monadic_scopus_Speex__ */

#ifndef _Included_za_co_monadic_scopus_Speex__
#define _Included_za_co_monadic_scopus_Speex__
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_za_co_monadic_scopus_Speex_00024_encoder_1create(JNIEnv *env, jobject clazz, jint modeID);

JNIEXPORT void JNICALL Java_za_co_monadic_scopus_Speex_00024_encoder_1destroy(JNIEnv *env, jobject clazz, jlong state);

JNIEXPORT jstring JNICALL Java_za_co_monadic_scopus_Speex_00024_get_1version_1string(JNIEnv *env, jobject clazz);

#ifdef __cplusplus
}
#endif
#endif
