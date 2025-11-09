#ifndef __PREPROCESS__
#define __PREPROCESS__

#include "opencv2/opencv.hpp"
#include <string>


extern "C" {
    #include "potrace/potracelib.h"
}

// Taken from potracelib_demo.c
/* macros for writing individual bitmap pixels */
#define BM_WORDSIZE ((int)sizeof(potrace_word))
#define BM_WORDBITS (8*BM_WORDSIZE)
#define BM_HIBIT (((potrace_word)1)<<(BM_WORDBITS-1))
#define bm_scanline(bm, y) ((bm)->map + (y)*(bm)->dy)
#define bm_index(bm, x, y) (&bm_scanline(bm, y)[(x)/BM_WORDBITS])
#define bm_mask(x) (BM_HIBIT >> ((x) & (BM_WORDBITS-1)))
#define bm_range(x, a) ((int)(x) >= 0 && (int)(x) < (a))
#define bm_safe(bm, x, y) (bm_range(x, (bm)->w) && bm_range(y, (bm)->h))
#define BM_USET(bm, x, y) (*bm_index(bm, x, y) |= bm_mask(x))
#define BM_UCLR(bm, x, y) (*bm_index(bm, x, y) &= ~bm_mask(x))
#define BM_UPUT(bm, x, y, b) ((b) ? BM_USET(bm, x, y) : BM_UCLR(bm, x, y))
#define BM_PUT(bm, x, y, b) (bm_safe(bm, x, y) ? BM_UPUT(bm, x, y, b) : 0)

class MapBuffer {
    std::vector<potrace_word>* buff;

    public:
        MapBuffer();
        void set_size(int size);
        potrace_word* get_data();
        ~MapBuffer();
};

int open_img(const std::string& path, cv::Mat& dst);
void prep_img(cv::Mat& src, cv::Mat& dst, int thresh);
void cvt_mat_to_bmp(cv::Mat& src, potrace_bitmap_t& dst, MapBuffer& mb);

#endif