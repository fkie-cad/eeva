#include <jni.h>

#include <stdint.h>
#include <string.h>
#include <malloc.h>

#define BUFFER_SIZE 0x20

static uint8_t called = 0;
static uint8_t *buffer = NULL;

JNIEXPORT jbyteArray JNICALL Java_com_damnvulnerableapp_vulnerable_modules_PoCPrimaryPoisoning_free(
        JNIEnv *env,
        jobject class,
        jbyteArray chunk) {

    if (!called) {
        called++;
        buffer = malloc(BUFFER_SIZE);
        jbyteArray ar = (*env)->NewByteArray(env, 8);
        jbyte *leak = (jbyte*)&buffer;
        (*env)->SetByteArrayRegion(env, ar, 0, 8, leak);
        return ar;
    }

    uint8_t *raw = (uint8_t*)(*env)->GetByteArrayElements(env, chunk, NULL);
    uint32_t length = (*env)->GetArrayLength(env, chunk);
    if (raw) {
        memcpy(buffer, raw, (length <= BUFFER_SIZE) ? length : BUFFER_SIZE);

        // Brings attacker - controlled chunk into primary
        free(buffer + 0x10); // combined header

        uint8_t *new = malloc(0x10);
        jbyteArray output = (*env)->NewByteArray(env, 0x10);
        (*env)->SetByteArrayRegion(env, output, 0, 0x10, new);
        return output;
    }
    return NULL;
}