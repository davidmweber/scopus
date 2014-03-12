/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 gcc -I/usr/lib/jvm/java-7-openjdk-amd64/include/ -I/usr/local/include -shared -Wl,-soname,libjni_opus.so -fPIC -o libjni_opus.so jni_opus.c -lc
  */

#include "jni_opus.h"

#include <stdint.h>
#include <opus/opus.h>

JNIEXPORT jint JNICALL
Java_za_co_monadic_scopus_scopus_Opus_00024_decode
    (JNIEnv *env, jclass clazz, jlong decoder, jbyteArray input,
        jint inputLength, jbyteArray output,
        jint outputFrameSize, jint decodeFEC)
{
    int ret;

    if (output) {
        jbyte *input_;

        if (input && inputLength) {
            input_ = (*env)->GetPrimitiveArrayCritical(env, input, 0);
            ret = input_ ? OPUS_OK : OPUS_ALLOC_FAIL;
        }
        else {
            input_ = 0;
            ret = OPUS_OK;
        }
        if (OPUS_OK == ret) {
            jbyte *output_ = (*env)->GetPrimitiveArrayCritical(env, output, 0);

            if (output_) {
                ret = opus_decode(
                            (OpusDecoder *) decoder,
                            (unsigned char *) input_ ,
                            inputLength,
                            (opus_int16 *) output_,
                            outputFrameSize,
                            decodeFEC);
                (*env)->ReleasePrimitiveArrayCritical(env, output, output_, 0);
            }
            else {
                ret = OPUS_ALLOC_FAIL;
            }
            if (input_)
            {
                (*env)->ReleasePrimitiveArrayCritical(
                        env,
                        input, input_, JNI_ABORT);
            }
        }
    }
    else {
        ret = OPUS_BAD_ARG;
    }
    return ret;
}

JNIEXPORT jlong JNICALL
Java_za_co_monadic_scopus_Opus_00024_decoder_1create
    (JNIEnv *env, jclass clazz, jint Fs, jint channels, jintArray err)
{
    int error;
    OpusDecoder *decoder = opus_decoder_create(Fs, channels, &error);
    int *err_ret = (*env)->GetPrimitiveArrayCritical(env, err, 0);
    err_ret[0] = error;
    (*env)->ReleasePrimitiveArrayCritical(env, err, err_ret, 0);
    return (jlong) decoder;
}

JNIEXPORT void JNICALL
Java_za_co_monadic_scopus_scopus_Opus_decoder_1destroy
    (JNIEnv *env, jclass clazz, jlong decoder)
{
    opus_decoder_destroy((OpusDecoder *) (intptr_t) decoder);
}



JNIEXPORT jint JNICALL
Java_za_co_monadic_scopus_Opus_00024_encode
    (JNIEnv *env, jclass clazz, jlong encoder, jbyteArray input,
        jint inputFrameSize, jbyteArray output, jint outputLength)
{
    int ret;

    if (input && output) {
        jbyte *input_ = (*env)->GetPrimitiveArrayCritical(env, input, 0);

        if (input_)
        {
            jbyte *output_ = (*env)->GetPrimitiveArrayCritical(env, output, 0);

            if (output_) {
                ret = opus_encode(
                            (OpusEncoder *) encoder,
                            (opus_int16 *) input_, inputFrameSize,
                            (unsigned char *) output_,
                            outputLength);
                (*env)->ReleasePrimitiveArrayCritical(env, output, output_, 0);
            }
            else {
                ret = OPUS_ALLOC_FAIL;
            }
            (*env)->ReleasePrimitiveArrayCritical(env, input, input_, JNI_ABORT);
        }
        else
            ret = OPUS_ALLOC_FAIL;
    }
    else
        ret = OPUS_BAD_ARG;
    return ret;
}

JNIEXPORT jlong JNICALL
Java_za_co_monadic_scopus_Opus_00024_encoder_1create
    (JNIEnv *env, jclass clazz, jint Fs, jint channels)
{
    int error;
    OpusEncoder *encoder = opus_encoder_create(Fs, channels, OPUS_APPLICATION_VOIP, &error);
    if (OPUS_OK != error) encoder = 0;
    return (jlong) encoder;
}

JNIEXPORT void JNICALL
Java_za_co_monadic_scopus_Opus_00024_encoder_1destroy
    (JNIEnv *env, jclass clazz, jlong encoder)
{
    opus_encoder_destroy((OpusEncoder *) (intptr_t) encoder);
}

