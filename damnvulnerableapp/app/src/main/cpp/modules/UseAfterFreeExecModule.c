//
// Created by kuehnemann on 23.08.22.
//

#include <stdint.h>
#include <jni.h>
#include <malloc.h>
#include <string.h>

struct object {
    char key[256];
    uint64_t value;
};

struct manager {
    char *values[32];
    char* (*make_printable)(const char *key, const char *debug);
};

static char *make_printable(const char *key, const char *debug) {
    return "TODO: Implement!";
}

JNIEXPORT jbyteArray JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_UseAfterFreeExecModule_lookupExamples(
        JNIEnv * env,
        jobject thiz,
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

JNIEXPORT jbyteArray JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_UseAfterFreeExecModule_storePair(
        JNIEnv *env,
        jobject thiz,
        jbyteArray key,
        jlong value) {

    struct manager *m = (struct manager*)malloc(sizeof(struct manager));
    m->make_printable = make_printable;
    free(m);

    // Allocate memory for key - value pair
    struct object *obj = (struct object*)calloc(1, sizeof(struct object));
    uint32_t length = (*env)->GetArrayLength(env, key);
    if (length > 256)
        length = 256;

    // Store key - value pair
    jboolean iscopy = JNI_FALSE;
    jbyte *key_raw = (*env)->GetByteArrayElements(env, key, &iscopy);
    memcpy(obj->key, key_raw, length);
    obj->value = value;

    // Finally show stored key - value pairs
    uint64_t result = m->make_printable(obj->key, NULL);
    uint8_t *output = &result;
    jsize output_length = strlen(output);
    jbyteArray array = (*env)->NewByteArray(env, output_length);
    (*env)->SetByteArrayRegion(env, array, 0, output_length, output);

    (*env)->ReleaseByteArrayElements(env, key, key_raw, JNI_ABORT);
    free(obj);

    return array;
}