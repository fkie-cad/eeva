#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

#include <time.h>

//
// Created by kuehnemann on 30.08.22.
//

#define BUFFER_SIZE 0x100

struct logged_data {
    char message[BUFFER_SIZE];
    char* (*filter)(char *message, uint64_t length);
};
static struct logged_data *g_logs;
static uint64_t g_logs_size;

static char *new_filter(char *message, uint64_t length);
static char *default_filter(char *message, uint64_t length);

static void *filters[] = {
        default_filter,
        new_filter
};
#define DEFAULT_FILTER 0
#define NEW_FILTER 1

JNIEXPORT jbyteArray JNICALL
Java_com_damnvulnerableapp_vulnerable_modules_OffByOneModule_logMessage(
        JNIEnv *env,
        jclass clazz,
        jbyteArray message) {

    jboolean iscopy;
    jbyte *raw_message = (*env)->GetByteArrayElements(env, message, &iscopy);

    g_logs = (struct logged_data*) realloc(g_logs, ++g_logs_size * sizeof(struct logged_data));
    if (!g_logs)
        return NULL;
    struct logged_data *new = &g_logs[g_logs_size - 1];

    memset(new, 0, sizeof(struct logged_data));
    memcpy(&new->filter, &filters[DEFAULT_FILTER], sizeof(filters[DEFAULT_FILTER]));

    jsize length = (*env)->GetArrayLength(env, message);
    if (length - 1 > BUFFER_SIZE)   // off - by - one bug
        length = BUFFER_SIZE - 1;
    memcpy(new->message, raw_message, length);

    // Totally not control flow obfuscation to make filters align on page :)
    struct timespec time;
    if (clock_gettime(CLOCK_REALTIME, &time) != -1) {
        time.tv_nsec += 10;
    }

    jbyte *filtered_message = (jbyte*)new->filter(new->message, length);
    length = (jint)strlen((char*)filtered_message);

    jbyteArray logged_message = (*env)->NewByteArray(env, length);
    (*env)->SetByteArrayRegion(env, logged_message, 0, length, filtered_message);
    (*env)->ReleaseByteArrayElements(env, message, raw_message, JNI_ABORT);
    return logged_message;
}

char *new_filter(char *message, uint64_t length) {
    // do weird stuff here
    const char *strings[] = {
            "test",
            "test12321"
    };
    uint8_t index = *((uint8_t*)message);
    return (char*)(&strings[index]);
}

char *default_filter(char *message, uint64_t length) {
    // do secure stuff here

    // maybe one buffer overflow, but NOTHING else --> basically useless, unless ....
    char buffer[BUFFER_SIZE >> 1];

    uint64_t i;
    for (i = 0; i < length; i++)
        buffer[i] = message[i];
    return message;
}