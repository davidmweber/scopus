/*
 * Copyright David Weber 2014
 * Released under the Creative Commons License (http://creativecommons.org/licenses/by/4.0/legalcode)
 */
#include "za_co_monadic_scopus_Speex__.h"
#include <speex/speex.h>
#include <stdlib.h>


typedef struct {
    void *st;
    SpeexBits bits;
} encoder_state;


JNIEXPORT jlong JNICALL Java_za_co_monadic_scopus_Speex_00024_encoder_1create
    (JNIEnv *env, jobject clazz, jint modeID) {
    encoder_state* state = (encoder_state*)malloc(sizeof(encoder_state));
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
    return (jlong) state;
}

JNIEXPORT void JNICALL Java_za_co_monadic_scopus_Speex_00024_encoder_1destroy
    (JNIEnv *env, jobject clazz, jlong state_ptr) {
    encoder_state *state = (encoder_state*)(state_ptr);
    speex_encoder_destroy(state->st);
    speex_bits_destroy(&(state->bits));
    free(state);
}

JNIEXPORT jstring JNICALL Java_za_co_monadic_scopus_Speex_00024_get_1version_1string
    (JNIEnv *env, jobject clazz) {
    const char* version;
    speex_lib_ctl(SPEEX_LIB_GET_VERSION_STRING, (void*)&version);
    return (*env)->NewStringUTF(env, version);
}

