#include <jni.h>

//
// Created by kuehnemann on 06.02.23.
//

void call_bomb()
{
    call_bomb();
}

void jump()
{
    label:
    goto label;
}

JNIEXPORT void JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_CallBombModule_callBomb(JNIEnv *env, jclass clazz)
{
    //call_bomb();
    jump();
    //__cxa_finalize();
}