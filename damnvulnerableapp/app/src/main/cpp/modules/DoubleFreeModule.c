#include <jni.h>
#include <stdlib.h>
#include <string.h>

//
// Created by kuehnemann on 30.08.22.
//

struct ref {
    uint64_t *location;
};


JNIEXPORT jbyteArray
JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_DoubleFreeModule_leak(
        JNIEnv *env,
        jclass clazz,
        jint index) {
    char *string_table[] = {
            "amazing_key",
            "secret_key",
            "topsecret_key",
            "a_very_very_long_key_with_fancy_features_:D"
    };

    jsize length = strlen(string_table[index]);
    jbyteArray array = (*env)->NewByteArray(env, length);

    // One '&' can ruin the day
    (*env)->SetByteArrayRegion(env, array, 0, length, &string_table[index]);

    return array;
}

JNIEXPORT void JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_DoubleFreeModule_vulnerable(
        JNIEnv *env,
        jclass clazz,
        jbyteArray input) {

    jboolean iscopy;
    char *raw_input = (*env)->GetByteArrayElements(env, input, &iscopy);

    struct ref *first = (char*)calloc(1, sizeof(struct ref));
    struct ref *second = (char*)calloc(1, sizeof(struct ref));
    struct ref *third = (char*)calloc(1, sizeof(struct ref));

    free(first);
    free(second);
    free(first);

    first = (char*)calloc(1, sizeof(struct ref));
    second = (char*)calloc(1, sizeof(struct ref));
    third = (char*)calloc(1, sizeof(struct ref));

    // write - what - where condition!
    // First 8 bytes determine the location to write to
    memcpy(&third->location, raw_input, sizeof(uint64_t*));

    // Then the next 8 bytes determine what to write
    *(first->location) = (uint64_t)*(raw_input + 8);

    free(first);
    free(second);
    // free(third); --> boom

    (*env)->ReleaseByteArrayElements(env, input, raw_input, JNI_ABORT);
}