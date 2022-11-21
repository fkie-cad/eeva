#include <jni.h>

#include <stdint.h>
#include <malloc.h>
#include <time.h>

#define NS_PER_SECOND 1000000000


JNIEXPORT jbyteArray JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_HeapSCAModule_handleMessage(JNIEnv *env,
                                                                          jclass class,
                                                                          jbyteArray message) {

    uint32_t length = (*env)->GetArrayLength(env, message);
    if (length == 0)
        return NULL;

    jbyte *raw = (*env)->GetByteArrayElements(env, message, NULL);
    if (raw) {

        struct timespec before = { 0 };
        struct timespec after = { 0 };
        uint64_t elapsed;

        jbyteArray result = (*env)->NewByteArray(env, 16);
        switch (raw[0]) {
            case 0: {
                // Malloc
                uint64_t size = *((uint64_t*)&raw[1]);
                uint8_t *ptr;
                clock_gettime(CLOCK_THREAD_CPUTIME_ID, &before);
                ptr = malloc(size);
                clock_gettime(CLOCK_THREAD_CPUTIME_ID, &after);
                elapsed = (after.tv_sec * NS_PER_SECOND + after.tv_nsec) - (before.tv_sec * NS_PER_SECOND + before.tv_nsec);
                (*env)->SetByteArrayRegion(env, result, 0, 8, (jbyte*)&ptr);
                (*env)->SetByteArrayRegion(env, result, 8, 8, (jbyte*)&elapsed);
                break;
            }
            case 1: {
                // Free
                uint8_t *ptr = (uint8_t*)(*(uint64_t*)&raw[1]);
                clock_gettime(CLOCK_THREAD_CPUTIME_ID, &before);
                free(ptr);
                clock_gettime(CLOCK_THREAD_CPUTIME_ID, &after);
                elapsed = (after.tv_sec * NS_PER_SECOND + after.tv_nsec) - (before.tv_sec * NS_PER_SECOND + before.tv_nsec);
                (*env)->SetByteArrayRegion(env, result, 0, 8, (jbyte*)&ptr);
                (*env)->SetByteArrayRegion(env, result, 8, 8, (jbyte*)&elapsed);
                break;
            }
        }
        return result;
    }

    return NULL;
}