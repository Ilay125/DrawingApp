#ifndef __IMG2CODE__
#define __IMG2CODE__

#include <string>
#include <cstdio>
#include <iostream>

extern "C" {
    #include "potrace/potracelib.h"
    #include "potrace/backend_svg.h"  
}

// #include "potrace/main.h"


int png2svg(const std::string src, const std::string dst, const int thresh);
void write_svg_info(imginfo_t& img_info, const potrace_bitmap_t& bmp);

#endif
