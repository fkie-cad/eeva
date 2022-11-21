//
// Created by kuehnemann on 17.08.22.
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
    uint64_t *values[32];
    uint64_t id;
};

JNIEXPORT jbyteArray JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_UseAfterFreeWriteModule_lookupExamples(
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

JNIEXPORT void JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_UseAfterFreeWriteModule_storePair(
        JNIEnv *env,
        jobject thiz,
        jbyteArray key,
        jlong value) {

    struct manager *m = (struct manager*)malloc(sizeof(struct manager));
    free(m);

    struct object *obj = (struct object*)malloc(sizeof(struct object));
    uint32_t length = (*env)->GetArrayLength(env, key);
    if (length > 256)
        length = 256;
    jboolean iscopy = JNI_FALSE;
    jbyte *key_raw = (*env)->GetByteArrayElements(env, key, &iscopy);
    memcpy(obj->key, key_raw, length);

    // Write condition
    *(m->values[0]) = value; // <-- this is so bad I can hardly imagine someone implementing this

    (*env)->ReleaseByteArrayElements(env, key, key_raw, JNI_ABORT);
    free(obj);
}