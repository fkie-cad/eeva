#include <jni.h>
#include <stdint.h>
#include <malloc.h>
#include <string.h>


#define BUFFER_SIZE 0x20

static uint8_t called = 0;
static uint8_t *buffer = NULL;

static inline jsize min(jsize x, jsize y)
{
    return (x < y) ? x : y;
}

JNIEXPORT jbyteArray JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_PoCMemoryProbing_storeInChunk(JNIEnv *env,
                                                                            jclass clazz,
                                                                            jbyteArray data) {
    if (!called) {
        called++;
        buffer = malloc(BUFFER_SIZE);
        jbyteArray ar = (*env)->NewByteArray(env, 8);
        jbyte *leak = (jbyte*)&buffer;
        (*env)->SetByteArrayRegion(env, ar, 0, 8, leak);
        return ar;
    }

    uint8_t *raw = (uint8_t*)(*env)->GetByteArrayElements(env, data, NULL);
    uint32_t length = (*env)->GetArrayLength(env, data);
    if (raw) {
        memcpy(buffer, raw, (length <= BUFFER_SIZE) ? length : BUFFER_SIZE);

        // Brings attacker - controlled chunk into primary
        free(buffer + 0x10); // combined header

        uint8_t *new = malloc(0x10);
        jbyteArray output = (*env)->NewByteArray(env, 0x10);
        (*env)->SetByteArrayRegion(env, output, 0, 0x10, new);

        called = 0;
        free(buffer);
        return output;
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_PoCMemoryProbing_leakHeader(JNIEnv *env,
                                                                          jclass clazz) {
    jbyteArray output = (*env)->NewByteArray(env, 16);
    uint8_t *ptr = malloc(0x10);
    (*env)->SetByteArrayRegion(env, output, 0, 8, (jbyte*)(ptr - 0x10));
    (*env)->SetByteArrayRegion(env, output, 8, 8, (jbyte*)(&ptr));
    free(ptr);
    return output;
}