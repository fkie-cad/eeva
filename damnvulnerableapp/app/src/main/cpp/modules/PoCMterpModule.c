#include <jni.h>
#include <stdint.h>


JNIEXPORT jlong JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_PoCMterpModule_leak(JNIEnv *env, jclass clazz) {
    return (jlong)Java_com_damnvulnerableapp_vulnerable_modules_PoCMterpModule_leak;
}



JNIEXPORT void JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_PoCMterpModule_writeCondition(JNIEnv *env, jclass clazz, jlong address, jlong value) {

    // Write condition
    *(uint64_t*)address = value; // <-- this is so bad I can hardly imagine someone implementing this
}

JNIEXPORT jlong JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_PoCMterpModule_readCondition(JNIEnv *env,
                                                                           jclass clazz,
                                                                           jlong address) {
    return *((int64_t*)address);
}

JNIEXPORT jbyteArray JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_PoCMterpModule_stackLeak(JNIEnv *env, jclass clazz) {

    jbyteArray array = (*env)->NewByteArray(env, 0x40);

    (*env)->SetByteArrayRegion(env, array, 0, 0x40, (jbyte*)&array);

    return array;
}

JNIEXPORT void JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_PoCMterpModule_bufferOverflow(JNIEnv *env,
                                                                            jclass clazz,
                                                                            jbyteArray input) {
    jbyte buffer[0x10];
    uint64_t i;

    jsize length = (*env)->GetArrayLength(env, input);
    jbyte *raw = (*env)->GetByteArrayElements(env, input, NULL);

    for (i = 0; i < length; i++) {
        buffer[i] = raw[i];
    }
}