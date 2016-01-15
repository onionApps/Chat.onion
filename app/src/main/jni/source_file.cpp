#include "source_file.h"

#include <jni.h>

#include <sys/types.h>
#include <dirent.h>
#include <ctype.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <android/log.h>

extern "C"
{

bool endswith(const char *str, const char *suffix)
{
    size_t lenstr = strlen(str);
    size_t lensuffix = strlen(suffix);
    if (lensuffix >  lenstr) return 0;
    return strncmp(str + lenstr - lensuffix, suffix, lensuffix) == 0;
}

JNIEXPORT void JNICALL Java_onion_chat_Native_killTor(JNIEnv *env)
{

    DIR* d = opendir("/proc");
    dirent* de;

    while((de = readdir(d)) != 0)
    {
        int pid = atol(de->d_name);

        if(pid <= 0) continue;

        char namepath[1024];
        sprintf(namepath, "/proc/%i/cmdline", pid);

        char name[1024] = { 0 };
        if(int namefd = open(namepath, O_RDONLY)) {
            read(namefd, name, sizeof(name) - 1);
            close(namefd);
        }

        if(endswith(name, "/tor") || endswith(name, "/ftor") || endswith(name, "/ctor")) {

            __android_log_print(ANDROID_LOG_INFO, "PROCESS", "FOUND %i %s\n", pid, name);

            if (kill(pid, SIGKILL) == 0) {
                __android_log_print(ANDROID_LOG_INFO, "PROCESS", "KILLED %i %s\n", pid, name);
            }
            else {
                __android_log_print(ANDROID_LOG_INFO, "PROCESS", "FAILED TO KILL %i %s\n", pid, name);
            }
        }

    }

    closedir(d);

}

}