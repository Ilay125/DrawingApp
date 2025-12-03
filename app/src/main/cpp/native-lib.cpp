#include <jni.h>
#include <string>
#include "img_to_code.h"  // or wherever processImage() is declared

extern "C" JNIEXPORT jint JNICALL
Java_com_example_drawingapp_MainActivity_img2code(
        JNIEnv* env,
        jobject /* this */,
        jstring inputPath,
        jint thresh_val,
        jbyteArray buffer,
        jint buf_size) {

    // Convert jstring → std::string
    const char* inputCStr = env->GetStringUTFChars(inputPath, nullptr);

    const int threshCInt = static_cast<int>(thresh_val);

    std::string input(inputCStr);

    jbyte* buf_ptr = env->GetByteArrayElements(buffer, nullptr);

    unsigned char* cbuffer = reinterpret_cast<unsigned char*>(buf_ptr);
    const int buf_size_cint = static_cast<int>(buf_size);


    // Call your actual C++ function
    int result = png2code(input, threshCInt, cbuffer, buf_size_cint);

    // Release memory for jstring and buffer
    env->ReleaseStringUTFChars(inputPath, inputCStr);
    env->ReleaseByteArrayElements(buffer, buf_ptr, 0);

    // Return the result as a new jstring
    return static_cast<jint>(result);
}
