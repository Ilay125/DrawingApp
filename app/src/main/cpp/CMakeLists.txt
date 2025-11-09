cmake_minimum_required(VERSION 3.20)
project(PotraceCppProject)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Include directories
include_directories(
    ${CMAKE_SOURCE_DIR}           # for bitmap.h, main.cpp
    ${CMAKE_SOURCE_DIR}/potrace   # for potracelib.h, config.h, etc
)

# Get all .c files in potrace directory
file(GLOB POTRACE_SOURCES "${CMAKE_SOURCE_DIR}/potrace/*.c")

# All source files for project
set(SOURCES
        main.cpp
        preprocess.cpp
        img_to_code.cpp
    ${POTRACE_SOURCES}
)

# --- OpenCV ---
set(OpenCV_DIR "C:/Users/natan/Documents/OpenCV-android-sdk/sdk/native/jni")
find_package(OpenCV REQUIRED)
include_directories(${OpenCV_INCLUDE_DIRS})

# --- Creates Executable ---
# Create executable
add_library(
	native-lib
	SHARED
	${SOURCES}
)

find_library(
	log-lib
	log
)

target_link_libraries(
	native-lib
    ${OpenCV_LIBS}
    ${log-lib}
)