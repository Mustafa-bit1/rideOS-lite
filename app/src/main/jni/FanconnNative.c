#include <jni.h>
#include <termios.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <stdio.h>
#include <string.h>
#include "FanconnNative.h"
JNIEXPORT void JNICALL Java_com_fanconn_rideOS_SerialPortManager_clearSendBufferNative(JNIEnv *env, jobject thiz, jint fdValue) {
    printf("clearSendBufferNative：%d",fdValue);
    fflush(stdout);
       if (fdValue >= 0) {
        int result = tcflush(fdValue, TCOFLUSH); // Flush the output buffer
        if (result == 0) {
            printf("Successfully flushed the output buffer for fd: %d\n", fdValue);
        } else {
            printf("Failed to flush the output buffer for fd: %d\n", fdValue);
            printf("Error: %s\n", strerror(errno)); // 打印错误原因
        }
        fflush(stdout); // 确保打印立即输出到控制台
        printf("clearSendBufferNative：%d",fdValue);
        fflush(stdout);
    }
}
