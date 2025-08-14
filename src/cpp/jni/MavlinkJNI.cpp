#include <jni.h>
#include <string>
#include "core/MavlinkCore.h"

#ifdef __ANDROID__
#include <android/log.h>
#define LOG_TAG "MavlinkJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#else
#define LOGD(...) printf(__VA_ARGS__); printf("\n")
#endif

// Helper function to convert C++ std::string to Java String
jstring cppStringToJString(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

// Helper function to convert Java String to C++ std::string  
std::string jStringToCppString(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return result;
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_pixhawk_gcs_jni_MavlinkJNI_nativeCreate(JNIEnv* env, jobject thiz) {
    LOGD("Creating MavlinkCore instance");
    MavlinkCore* core = new MavlinkCore();
    return reinterpret_cast<jlong>(core);
}

JNIEXPORT void JNICALL
Java_com_pixhawk_gcs_jni_MavlinkJNI_nativeDestroy(JNIEnv* env, jobject thiz, jlong ptr) {
    LOGD("Destroying MavlinkCore instance");
    if (ptr != 0) {
        MavlinkCore* core = reinterpret_cast<MavlinkCore*>(ptr);
        delete core;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_pixhawk_gcs_jni_MavlinkJNI_nativeStartConnection(JNIEnv* env, jobject thiz, 
                                                         jlong ptr, jstring host, jint port) {
    if (ptr == 0) return JNI_FALSE;
    
    MavlinkCore* core = reinterpret_cast<MavlinkCore*>(ptr);
    std::string hostStr = jStringToCppString(env, host);
    
    LOGD("Starting connection to %s:%d", hostStr.c_str(), port);
    return core->startConnection(hostStr, port) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_pixhawk_gcs_jni_MavlinkJNI_nativeStopConnection(JNIEnv* env, jobject thiz, jlong ptr) {
    if (ptr == 0) return;
    
    MavlinkCore* core = reinterpret_cast<MavlinkCore*>(ptr);
    core->stopConnection();
}

JNIEXPORT jboolean JNICALL
Java_com_pixhawk_gcs_jni_MavlinkJNI_nativeIsConnected(JNIEnv* env, jobject thiz, jlong ptr) {
    if (ptr == 0) return JNI_FALSE;
    
    MavlinkCore* core = reinterpret_cast<MavlinkCore*>(ptr);
    return core->isConnected() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_pixhawk_gcs_jni_MavlinkJNI_nativeArmDisarm(JNIEnv* env, jobject thiz, 
                                                    jlong ptr, jboolean arm) {
    if (ptr == 0) return;
    
    MavlinkCore* core = reinterpret_cast<MavlinkCore*>(ptr);
    core->armDisarm(arm == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_pixhawk_gcs_jni_MavlinkJNI_nativeReturnToLaunch(JNIEnv* env, jobject thiz, jlong ptr) {
    if (ptr == 0) return;
    
    MavlinkCore* core = reinterpret_cast<MavlinkCore*>(ptr);
    core->returnToLaunch();
}

JNIEXPORT void JNICALL
Java_com_pixhawk_gcs_jni_MavlinkJNI_nativeTakeoff(JNIEnv* env, jobject thiz, 
                                                  jlong ptr, jfloat altitude) {
    if (ptr == 0) return;
    
    MavlinkCore* core = reinterpret_cast<MavlinkCore*>(ptr);
    core->takeoff(altitude);
}

JNIEXPORT jobject JNICALL
Java_com_pixhawk_gcs_jni_MavlinkJNI_nativeGetState(JNIEnv* env, jobject thiz, jlong ptr) {
    if (ptr == 0) return nullptr;
    
    MavlinkCore* core = reinterpret_cast<MavlinkCore*>(ptr);
    const MavlinkCore::VehicleState& state = core->getVehicleState();
    
    // Find the VehicleState class
    jclass stateClass = env->FindClass("com/pixhawk/gcs/jni/MavlinkJNI$VehicleState");
    if (!stateClass) {
        LOGD("Failed to find VehicleState class");
        return nullptr;
    }
    
    // Find the constructor - simplified version
    jmethodID constructor = env->GetMethodID(stateClass, "<init>", "()V");
    if (!constructor) {
        LOGD("Failed to find default VehicleState constructor");
        return nullptr;
    }
    
    // Create the VehicleState object with default constructor
    jobject stateObject = env->NewObject(stateClass, constructor);
    
    return stateObject;
}

} // extern "C"