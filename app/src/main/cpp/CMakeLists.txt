cmake_minimum_required(VERSION 3.20)
project(DrawingApp)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ---------------------------
# Include directories
# ---------------------------
include_directories(
		${CMAKE_SOURCE_DIR}           # for native-lib.cpp, img_to_code.cpp, preprocess.cpp
		${CMAKE_SOURCE_DIR}/potrace   # for potrace headers
		${CMAKE_SOURCE_DIR}/opencv/include  # OpenCV headers (opencv2/ folder is inside)
)

# ---------------------------
# Collect source files
# ---------------------------
file(GLOB POTRACE_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/potrace/*.c")

set(SOURCES
		native-lib.cpp
		img_to_code.cpp
		preprocess.cpp
		${POTRACE_SOURCES}
)

# ---------------------------
# Create native library
# ---------------------------
add_library(
		native-lib
		SHARED
		${SOURCES}
)

# ---------------------------
# Android log library
# ---------------------------
find_library(log-lib log)

# ---------------------------
# Prebuilt OpenCV library
# ---------------------------
add_library(opencv_java4 SHARED IMPORTED)
set_target_properties(opencv_java4 PROPERTIES
		IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_java4.so
)

# ---------------------------
# Link libraries
# ---------------------------
target_link_libraries(
		native-lib
		${log-lib}
		opencv_java4
)
