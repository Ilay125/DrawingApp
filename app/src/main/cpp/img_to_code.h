#ifndef __IMG2CODE__
#define __IMG2CODE__

#include <string>
#include <cstdio>
#include <iostream>

extern "C" {
#include "potrace/potracelib.h"
#include "potrace/backend_code.h"
}

// #include "potrace/main.h"


int png2code(const std::string src, const int thresh, unsigned char* buffer, int buf_size);
//void write_svg_info(imginfo_t& img_info, const potrace_bitmap_t& bmp);

#endif
