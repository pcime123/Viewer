/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class android_serialport_api_SerialPort */

#ifndef _Included_com_sscctv_seeeyes_Rs485Port_
#define _Included_com_sscctv_seeeyes_Rs485Port_
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     android_serialport_api_SerialPort
 * Method:    open
 * Signature: (Ljava/lang/String;II)Ljava/io/FileDescriptor;
 */
JNIEXPORT jobject JNICALL Java_com_sscctv_seeeyes_Rs485Port_open
        (JNIEnv *, jclass, jstring, jint, jint);

/*
 * Class:     android_serialport_api_SerialPort
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sscctv_seeeyes_Rs485Port_close
        (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
