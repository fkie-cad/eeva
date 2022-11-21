#include <jni.h>

#include <stdlib.h>
#include <stdint.h>
#include <string.h>


static uint8_t *alloc = NULL;

JNIEXPORT jbyteArray JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_HeapOverflowModule_leak(JNIEnv *env, jobject unused) {


    alloc = malloc(0x10);
    if (alloc) {
        jbyteArray output = (*env)->NewByteArray(env, 16);

        jbyte *user_pointer = (jbyte*)&alloc;
        jbyte *header = (jbyte*)(alloc - 0x10);

        (*env)->SetByteArrayRegion(env, output, 0, 8, user_pointer);
        (*env)->SetByteArrayRegion(env, output, 8, 8, header);
        return output;
    }

    return NULL;
}

JNIEXPORT jbyteArray JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_HeapOverflowModule_processMessage(JNIEnv *env, jobject unused, jbyteArray message) {


    /*jbyte buffer[0x20] = { 0 };
    jbyte *buf = buffer;

    jbyte *array = (*env)->GetByteArrayElements(env, message, NULL);
    jsize length = (*env)->GetArrayLength(env, message);
    jsize i;
    for (i = 0; i < length; i++)
        buf[i] = array[i];

    jbyteArray output;
    if (alloc) {
        free(buf + 0x10);
        alloc = malloc(0x10);

        length = strlen(alloc);
        output = (*env)->NewByteArray(env, length);
        (*env)->SetByteArrayRegion(env, output, 0, length, alloc);
    } else {
        alloc = malloc(0x10);

        output = (*env)->NewByteArray(env, 8);
        (*env)->SetByteArrayRegion(env, output, 0, 8, &buf);
    }

    return output;*/

    jbyteArray ar = (*env)->NewByteArray(env, 0x10);
    uint8_t *tmp = (uint8_t*)malloc(0x10);
    if (tmp) {

        memset(tmp, 0x42, 0x10);
        free(tmp);
        tmp = (uint8_t*)malloc(0x10);

        (*env)->SetByteArrayRegion(env, ar, 0, 0x10, (jbyte*)tmp);
        free(tmp);
    }

    return ar;
}