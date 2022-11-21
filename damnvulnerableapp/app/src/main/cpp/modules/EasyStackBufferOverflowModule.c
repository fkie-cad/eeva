//
// Created by kuehnemann on 10.08.22.
//

#include <jni.h>

#include <stdio.h>
#include <string.h>
#include <ctype.h>
#include <malloc.h>

uint32_t perfect_strlen(const char *str) {
    uint32_t i = 0;
    while (str[i] != 0) i++;
    return i;
}

void perfect_memcpy(char *buffer, const char *source, uint32_t length) {
    uint32_t i;
    for (i = 0; i < length; i++)
        buffer[i] = source[i];
}

JNIEXPORT jbyteArray
JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_EasyStackBufferOverflowModule_vulnerableToUpper(
        JNIEnv *env,
        jobject thiz,
        jbyteArray string,
        jint unknown) {

    char buffer[0x20] = { 0 };
    jbyte *bytes = (*env)->GetByteArrayElements(env, string, NULL);
    jint length = (*env)->GetArrayLength(env, string);

    // Cannot use memcpy et al, because libc FORTIFY would get in our way ...
    // Its actually hard to write insecure code...
    perfect_memcpy(buffer, bytes, length);

    // TO UPPER
    uint32_t i;
    for (i = 0; i < 0x20; i++)
        buffer[i] = toupper(buffer[i]);

    // Ofc. we need the string length of our buffer
    if (unknown <= 0x100)
        length = perfect_strlen(buffer) + unknown;
    else
        length = perfect_strlen(buffer);

    jbyteArray upper = (*env)->NewByteArray(env, length);

    (*env)->SetByteArrayRegion(env, upper, 0, length, buffer);
    return upper;
}