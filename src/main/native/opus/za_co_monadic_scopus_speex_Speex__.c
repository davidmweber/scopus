/*
 * Copyright David Weber 2014
 * Released under the Creative Commons License (http://creativecommons.org/licenses/by/4.0/legalcode)
 */
#include "za_co_monadic_scopus_speex_Speex__.h"
#include <speex/speex.h>
#include <speex/speex_echo.h>
#include <stdlib.h>
#include <math.h>

typedef struct {
    void *st;
    SpeexBits bits;
} codec_state;


JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_encoder_1ctl
    (JNIEnv *env, jobject clazz, jlong encoder, jint command, jint value) {
    jint ret = value;
    codec_state *state = (codec_state *) encoder;
    speex_encoder_ctl(state->st, command, &ret);
    return ret;
}

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_decoder_1ctl
    (JNIEnv *env, jobject clazz, jlong decoder, jint command, jint value) {
    jint ret = value;
    codec_state *state = (codec_state *) decoder;
    speex_decoder_ctl(state->st, command, &ret);
    return ret;
}

JNIEXPORT jlong JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_encoder_1create
    (JNIEnv *env, jobject clazz, jint modeID) {
    codec_state* state = (codec_state*)malloc(sizeof(codec_state));
    switch (modeID) {
        case SPEEX_MODEID_NB:
            state->st = speex_encoder_init(&speex_nb_mode);
            break;
        case SPEEX_MODEID_WB:
            state->st = speex_encoder_init(&speex_wb_mode);
            break;
        case SPEEX_MODEID_UWB:
            state->st = speex_encoder_init(&speex_uwb_mode);
            break;
        default:
            free(state);
            return (jlong) 0;
    }

    speex_bits_init(&(state->bits));
    return  (unsigned long)state;
}

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_encode_1short
    (JNIEnv *env, jobject clazz, jlong encoder, jshortArray input, jint len_in, jbyteArray coded, jint len_out) {

    codec_state *state = (codec_state*)encoder;
    jshort *in_ptr;
    jbyte *cod_ptr;
    jint ret;

    in_ptr = (*env)->GetPrimitiveArrayCritical(env, input, 0);
    if (in_ptr == 0) return -1;
    cod_ptr = (*env)->GetPrimitiveArrayCritical(env, coded, 0);
    if (cod_ptr == 0) {
	    (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
	    return -1;
    }
    speex_bits_reset(&(state->bits));
    speex_encode_int(state->st,in_ptr, &(state->bits));
    ret = speex_bits_write(&(state->bits),(char *)cod_ptr,len_out);
    (*env)->ReleasePrimitiveArrayCritical(env, coded, cod_ptr, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
    return ret;
}

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_encode_1float
    (JNIEnv *env, jobject clazz, jlong encoder, jfloatArray input, jint len_in, jbyteArray coded, jint len_out) {
    codec_state *state = (codec_state*)encoder;
    jfloat *in_ptr;
    jbyte *cod_ptr;
    jint ret;
    spx_int32_t N;
    int i;
    spx_int16_t short_in[640];
    speex_encoder_ctl(state->st, SPEEX_GET_FRAME_SIZE, &N);

    if (N != len_in) return -2;

    in_ptr = (*env)->GetPrimitiveArrayCritical(env, input, 0);
    if (in_ptr == 0) return -1;
    cod_ptr = (*env)->GetPrimitiveArrayCritical(env, coded, 0);
    if (cod_ptr == 0) {
	    (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
	    return -1;
    }

    for (i=0;i<N;i++) {
       if (in_ptr[i]>1.f)
          short_in[i] = 1.f;
       else if (in_ptr[i]<-1.f)
           short_in[i] = -1.f;
       else
          short_in[i] = (spx_int16_t)floorf(.5+ 32767.f*in_ptr[i]);
    }
    speex_bits_reset(&(state->bits));
    speex_encode_int(state->st, short_in, &(state->bits));
    ret = speex_bits_write(&(state->bits),(char *)cod_ptr,len_out);
    (*env)->ReleasePrimitiveArrayCritical(env, coded, cod_ptr, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
    return ret;
}

JNIEXPORT void JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_encoder_1destroy
    (JNIEnv *env, jobject clazz, jlong state_ptr) {
    codec_state *state = (codec_state*)(state_ptr);
    speex_encoder_destroy(state->st);
    speex_bits_destroy(&(state->bits));
    free(state);
}

JNIEXPORT jstring JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_get_1version_1string
    (JNIEnv *env, jobject clazz) {
    const char* version;
    speex_lib_ctl(SPEEX_LIB_GET_VERSION_STRING, (void*)&version);
    return (*env)->NewStringUTF(env, version);
}


/* Decoders */

JNIEXPORT jlong JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_decoder_1create
    (JNIEnv *env, jobject clazz, jint modeID, int enhance) {
    codec_state* state = (codec_state*)malloc(sizeof(codec_state));
    switch (modeID) {
        case SPEEX_MODEID_NB:
            state->st = speex_decoder_init(&speex_nb_mode);
            break;
        case SPEEX_MODEID_WB:
            state->st = speex_decoder_init(&speex_wb_mode);
            break;
        case SPEEX_MODEID_UWB:
            state->st = speex_decoder_init(&speex_uwb_mode);
            break;
        default:
            free(state);
            return (jlong) -1;
    }
    speex_bits_init(&(state->bits));
    speex_decoder_ctl(state->st, SPEEX_SET_ENH, &enhance);
    return (unsigned long) state;
}


JNIEXPORT void JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_decoder_1destroy
    (JNIEnv *env, jobject clazz, jlong state_ptr) {
    codec_state *state = (codec_state*)(state_ptr);
    speex_decoder_destroy(state->st);
    speex_bits_destroy(&(state->bits));
    free(state);
}


JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_decode_1float
    (JNIEnv *env, jobject clazz, jlong decoder, jbyteArray input, jint len_in, jfloatArray decoded, jint len_out) {
    codec_state *state = (codec_state*)decoder;
    jbyte *in_ptr = 0;
    jfloat *dec_ptr = 0;
    jint ret = 0;
    if ((long) input != 0 && len_in != 0) {
        in_ptr = (*env)->GetPrimitiveArrayCritical(env, input, 0);
        if (in_ptr == 0) return -1;
    }
    dec_ptr = (*env)->GetPrimitiveArrayCritical(env, decoded, 0);
    if (dec_ptr == 0) {
	    if (in_ptr != 0) (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
    	return -1;
    }
    speex_bits_read_from(&(state->bits), (char *)in_ptr, len_in);
    speex_decode(state->st, &(state->bits), dec_ptr);
    speex_decoder_ctl(state->st, SPEEX_GET_FRAME_SIZE, &ret);
    (*env)->ReleasePrimitiveArrayCritical(env, decoded, dec_ptr, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
    return ret;
}

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_decode_1short
    (JNIEnv *env, jobject clazz, jlong decoder, jbyteArray input, jint len_in, jshortArray decoded, jint len_out) {
    codec_state *state = (codec_state*)decoder;
    jbyte *in_ptr = 0;
    jshort *dec_ptr = 0;
    jint ret = 0;
    if ((long) input != 0 && len_in != 0) {
        in_ptr = (*env)->GetPrimitiveArrayCritical(env, input, 0);
        if (in_ptr == 0) return -1;
    }
    dec_ptr = (*env)->GetPrimitiveArrayCritical(env, decoded, 0);
    if (dec_ptr == 0) {
	    if (in_ptr != 0) (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
    	return -1;
    }
    speex_bits_read_from(&(state->bits), (char *)in_ptr, len_in);
    speex_decode_int(state->st, &(state->bits), dec_ptr);
    speex_decoder_ctl(state->st, SPEEX_GET_FRAME_SIZE, &ret);
    (*env)->ReleasePrimitiveArrayCritical(env, decoded, dec_ptr, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, input, in_ptr, 0);
    return ret;
}


/* Echo canceller API */

JNIEXPORT jlong JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_echo_1state_1init
    (JNIEnv *env, jobject clazz, jint frame_size, jint filter_length ) {
    SpeexEchoState *state = speex_echo_state_init(frame_size,filter_length);
    return (unsigned long) state;
}

JNIEXPORT void JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_echo_1state_1destroy
    (JNIEnv *env, jobject clazz, jlong state) {
    speex_echo_state_destroy((SpeexEchoState *)state);
}

JNIEXPORT void JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_echo_1cancellation
    (JNIEnv *env, jobject clazz, jlong state, jshortArray rec, jshortArray play, jshortArray out){
    jshort *rec_ptr = 0;
    jshort *play_ptr = 0;
    jshort *out_ptr = 0;
    rec_ptr = (*env)->GetPrimitiveArrayCritical(env, rec, 0);
    play_ptr = (*env)->GetPrimitiveArrayCritical(env, play, 0);
    out_ptr = (*env)->GetPrimitiveArrayCritical(env, out, 0);
    speex_echo_cancellation((SpeexEchoState *)state, rec_ptr, play_ptr, out_ptr);
    (*env)->ReleasePrimitiveArrayCritical(env, rec, rec_ptr, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, play, play_ptr, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, out, out_ptr, 0);
}

JNIEXPORT void JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_echo_1capture
    (JNIEnv *env, jobject clazz, jlong state, jshortArray rec, jshortArray out) {
    jshort *rec_ptr = 0;
    jshort *out_ptr = 0;
    rec_ptr = (*env)->GetPrimitiveArrayCritical(env, rec, 0);
    out_ptr = (*env)->GetPrimitiveArrayCritical(env, out, 0);
    speex_echo_capture((SpeexEchoState *)state, rec_ptr, out_ptr);
    (*env)->ReleasePrimitiveArrayCritical(env, rec, rec_ptr, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, out, out_ptr, 0);
}

JNIEXPORT void JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_echo_1playback
    (JNIEnv *env, jobject clazz, jlong state, jshortArray play) {
    jshort *play_ptr = 0;
    play_ptr = (*env)->GetPrimitiveArrayCritical(env, play, 0);
    speex_echo_playback((SpeexEchoState *)state, play_ptr);
    (*env)->ReleasePrimitiveArrayCritical(env, play, play_ptr, 0);
}

JNIEXPORT void JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_echo_1state_1reset
    (JNIEnv *env, jobject clazz, jlong state){
    speex_echo_state_reset((SpeexEchoState *)state);
}

JNIEXPORT jint JNICALL Java_za_co_monadic_scopus_speex_Speex_00024_echo_1ctl
    (JNIEnv *env, jobject clazz, jlong state, jint request, jlong ptr) {
    return speex_echo_ctl((SpeexEchoState *)state, request, (void *) ptr);
}

