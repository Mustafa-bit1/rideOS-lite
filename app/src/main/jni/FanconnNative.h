#include <jni.h>

#ifndef _Included_com_fanconn_rideOS_SerialPortManager
#define _Included_com_fanconn_rideOS_SerialPortManager
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_fanconn_rideOS_SerialPortManager_clearSendBufferNative(JNIEnv *env, jobject instance, jint  fileDescriptor);
#ifdef __cplusplus
}
#endif
#endif
