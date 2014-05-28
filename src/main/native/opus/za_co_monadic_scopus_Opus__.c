/*
 * Copyright David Weber 2014
 * Released under the Creative Commons License (http://creativecommons.org/licenses/by/4.0/legalcode)
 */
#include "za_co_monadic_scopus_Opus__.h"
#include <opus/opus.h>

JNIEXPORT jlong JNICALL Java_za_co_monadic_scopus_Opus_00024_decoder_1create
    (JNIEnv *env, jobject clazz, jint Fs, jint channels, jintArray err){
    int error;
    OpusDecoder *decoder = opus_decoder_create(Fs, channels, &error);
    int *err_ret = (*env)->GetPrimitiveArrayCritical(env, err, 0);
    err_ret[0] = error;
    (*env)->ReleasePrimitiveArrayCritical(env, err, err_ret, 0);
    return (jlong) decoder;
}


JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_Opus_00024_decode_1short
    (JNIEnv *env, jobject clazz, jlong decoder, jbyteArray input, jint len_in, jshortArray decoded, jint len_out, jint fec) {

    jbyte *in_ptr = 0;
    jbyte *dec_ptr = 0;
    jint ret = 0;
    if ((long) input != 0 && len_in != 0) {
        in_ptr = (*env)->GetPrimitiveArrayCritical(env, input, 0);
        if (in_ptr == 0) return OPUS_ALLOC_FAIL;
    }
    dec_ptr = (*env)->GetPrimitiveArrayCritical(env, decoded, 0);
    if (dec_ptr == 0) {
	    if (in_ptr != 0) (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
    	return OPUS_ALLOC_FAIL;
    }
    ret = opus_decode( (OpusDecoder *) decoder, (unsigned char *) in_ptr, len_in, (opus_int16 *) dec_ptr, len_out, fec);
    (*env)->ReleasePrimitiveArrayCritical(env, decoded, dec_ptr, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
    return ret;
}

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_Opus_00024_decode_1float
    (JNIEnv *env, jobject clazz, jlong decoder, jbyteArray input, jint len_in, jfloatArray decoded, jint len_out, jint fec) {
    jbyte *in_ptr = 0;
    jbyte *dec_ptr = 0;
    jint ret = 0;
    if ((long) input != 0 && len_in != 0) {
        in_ptr = (*env)->GetPrimitiveArrayCritical(env, input, 0);
        if (in_ptr == 0) return OPUS_ALLOC_FAIL;
    }
    dec_ptr = (*env)->GetPrimitiveArrayCritical(env, decoded, 0);
    if (dec_ptr == 0) {
	    if (in_ptr != 0) (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
    	return OPUS_ALLOC_FAIL;
    }
    ret = opus_decode_float( (OpusDecoder *) decoder, (unsigned char *) in_ptr, len_in, (float *) dec_ptr, len_out, fec);
    (*env)->ReleasePrimitiveArrayCritical(env, decoded, dec_ptr, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
    return ret;
}

JNIEXPORT void JNICALL Java_za_co_monadic_scopus_Opus_00024_decoder_1destroy
    (JNIEnv *env, jobject clazz, jlong decoder) {
    opus_decoder_destroy((OpusDecoder *) decoder);
}

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_Opus_00024_decoder_1get_1ctl
    (JNIEnv *env, jobject clazz, jlong decoder, jint command, jintArray result) {
    int *ret = (*env)->GetPrimitiveArrayCritical(env, result, 0);
    int error;
    int retval;
    error = opus_decoder_ctl((OpusDecoder *) decoder, command, &retval);
    ret[0] = retval;
    (*env)->ReleasePrimitiveArrayCritical(env, result, ret, 0);
    return error;
}

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_Opus_00024_decoder_1set_1ctl
    (JNIEnv *env, jobject clazz, jlong decoder, jint command, jint value) {
    int error;
    error = opus_decoder_ctl((OpusDecoder *) decoder, command, value);
    return error;
}

JNIEXPORT jlong JNICALL Java_za_co_monadic_scopus_Opus_00024_encoder_1create
    (JNIEnv *env, jobject clazz, jint Fs, jint channels, jint application, jintArray err) {
    int error;
    OpusEncoder *enc = opus_encoder_create(Fs, channels, application, &error);
    int *err_ret = (*env)->GetPrimitiveArrayCritical(env, err, 0);
    err_ret[0] = error;
    (*env)->ReleasePrimitiveArrayCritical(env, err, err_ret, 0);
    return (jlong) enc;
}

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_Opus_00024_encode_1short
    (JNIEnv *env, jobject clazz, jlong encoder, jshortArray input, jint len_in, jbyteArray coded, jint len_out) {
    jshort *in_ptr;
    jbyte *cod_ptr;
    jint ret;
    in_ptr = (*env)->GetPrimitiveArrayCritical(env, input, 0);
    if (in_ptr == 0) return OPUS_ALLOC_FAIL;
    cod_ptr = (*env)->GetPrimitiveArrayCritical(env, coded, 0);
    if (cod_ptr == 0) {
	    (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
	    return OPUS_ALLOC_FAIL;
    }
    ret = opus_encode( (OpusEncoder *) encoder, (opus_int16 *) in_ptr, len_in, (unsigned char *) cod_ptr, len_out );
    (*env)->ReleasePrimitiveArrayCritical(env, coded, cod_ptr, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
    return ret;
}

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_Opus_00024_encode_1float
    (JNIEnv *env, jobject clazz, jlong encoder, jfloatArray input, jint len_in, jbyteArray coded, jint len_out) {
    jshort *in_ptr;
    jbyte *cod_ptr;
    jint ret;
    in_ptr = (*env)->GetPrimitiveArrayCritical(env, input, 0);
    if (in_ptr == 0) return OPUS_ALLOC_FAIL;
    cod_ptr = (*env)->GetPrimitiveArrayCritical(env, coded, 0);
    if (cod_ptr == 0) {
	    (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
	    return OPUS_ALLOC_FAIL;
    }
    ret = opus_encode_float( (OpusEncoder *) encoder, (float *) in_ptr, len_in, (unsigned char *) cod_ptr, len_out );
    (*env)->ReleasePrimitiveArrayCritical(env, coded, cod_ptr, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
    return ret;
}

JNIEXPORT void JNICALL Java_za_co_monadic_scopus_Opus_00024_encoder_1destroy
    (JNIEnv *env, jobject clazz, jlong enc) {
    opus_encoder_destroy((OpusEncoder *) enc);
}

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_Opus_00024_encoder_1get_1ctl
    (JNIEnv *env, jobject clazz, jlong encoder, jint command, jintArray result) {
    int *ret = (*env)->GetPrimitiveArrayCritical(env, result, 0);
    int error;
    int retval;
    error = opus_encoder_ctl((OpusEncoder *) encoder, command, &retval);
    ret[0] = retval;
    (*env)->ReleasePrimitiveArrayCritical(env, result, ret, 0);
    return error;
}

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_Opus_00024_encoder_1set_1ctl
    (JNIEnv *env, jobject clazz, jlong encoder, jint command, jint value) {
    int error;
    error = opus_encoder_ctl((OpusEncoder *) encoder, command, value);
    return error;
}

JNIEXPORT jstring JNICALL Java_za_co_monadic_scopus_Opus_00024_error_1string
    (JNIEnv *env, jobject clazz, jint errno) {
    const char *err_str = opus_strerror(errno);
    return (*env)->NewStringUTF(env, err_str);
}

JNIEXPORT jstring JNICALL Java_za_co_monadic_scopus_Opus_00024_get_1version_1string
    (JNIEnv *env, jobject clazz) {
    const char *version = opus_get_version_string();
    return (*env)->NewStringUTF(env, version);
}
