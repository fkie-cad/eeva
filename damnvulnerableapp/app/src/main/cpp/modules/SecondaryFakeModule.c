#include <jni.h>

#include <stdint.h>
#include <stdlib.h>
#include <string.h>


#define BUFFER_SIZE 0x100

static uint8_t called = 0;

JNIEXPORT jbyteArray JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_SecondaryFakeModule_free(JNIEnv *env,
                                                                       jclass clazz,
                                                                       jbyteArray chunk) {
    uint8_t buffer[BUFFER_SIZE] = { 0 };
    if (!called) {
        called++;
        jbyteArray ar = (*env)->NewByteArray(env, 8);
        jbyte *leak = (jbyte*)&buffer;
        (*env)->SetByteArrayRegion(env, ar, 0, 8, &leak);
        return ar;
    }

    uint8_t *raw = (uint8_t*)(*env)->GetByteArrayElements(env, chunk, NULL);
    uint32_t length = (*env)->GetArrayLength(env, chunk);
    if (raw) {
        memcpy(buffer, raw, (length <= BUFFER_SIZE) ? length : BUFFER_SIZE);

        // Brings attacker - controlled chunk into secondary cache
        free(buffer + 0x30 + 0x10); // large header + combined header

        // Triggers potential write - what - where condition. This could also be triggered by another
        // thread, although it might be problematic what that thread will write and how much...
        uint8_t *write_trigger = malloc(length - 0x40);
        memcpy(write_trigger, raw + 0x40, length - 0x40);
        free(write_trigger);
    }
    return NULL;
}