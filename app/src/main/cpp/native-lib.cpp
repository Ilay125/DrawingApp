#include <jni.h>
#include <string>
#include "img_to_code.h"  // or wherever processImage() is declared

extern "C" JNIEXPORT jint JNICALL
Java_com_example_drawingapp_MainActivity_img2code(
        JNIEnv* env,
        jobject /* this */,
        jstring inputPath,
        jstring outputPath,
        jint thresh_val) {

    // Convert jstring → std::string
    const char* inputCStr = env->GetStringUTFChars(inputPath, nullptr);
    const char* outputCStr = env->GetStringUTFChars(outputPath, nullptr);

    const int threshCInt = static_cast<int>(thresh_val);

    std::string input(inputCStr);
    std::string output(outputCStr);

    // Call your actual C++ function
    int result = png2svg(input, output, threshCInt);

    // Release memory for jstring
    env->ReleaseStringUTFChars(inputPath, inputCStr);
    env->ReleaseStringUTFChars(outputPath, outputCStr);

    // Return the result as a new jstring
    return static_cast<jint>(result);
}
