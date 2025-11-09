#include "preprocess.h"
#include <vector>

int open_img(const std::string& path, cv::Mat& dst)  {
    cv::Mat img = cv::imread(path, cv::IMREAD_GRAYSCALE);

    if (img.empty()) {
        return 1;
    }

    dst = img;

    return 0;
}

void prep_img(cv::Mat& src, cv::Mat& dst, int thresh) {
    cv::threshold(src, dst, thresh, 255, cv::THRESH_BINARY_INV); 
}

void cvt_mat_to_bmp(cv::Mat& src, potrace_bitmap_t& dst, MapBuffer& mb) {
    int w = src.cols;
    int h = src.rows;
    int dy = (w + BM_WORDBITS - 1) / BM_WORDBITS; // number of bytes per row (each bit = one pixel)

    
    dst.w = w;
    dst.h = h;
    dst.dy = dy;

    //Creates storage buffer for potrace
    mb.set_size(h * dy);
    dst.map = mb.get_data();


    // 3️⃣ Copy pixels
    for (int y = 0; y < h; ++y) {
        int dst_y = y; // OpenCV Mat is already top-down, adjust if needed
        for (int x = 0; x < w; ++x) {
            uint8_t pixel = src.at<uint8_t>(y, x);
            int bit = (pixel == 0) ? 1 : 0; // invert: 1 = black, 0 = white
            BM_PUT(&dst, x, dst_y, bit);
        }
    }
}

MapBuffer::MapBuffer() {
    this->buff = new std::vector<potrace_word>;
}

void MapBuffer::set_size(int size) {
    this->buff->resize(size, 0);
}

potrace_word* MapBuffer::get_data() {
    return this->buff->data();
}
MapBuffer::~MapBuffer() {
    delete this->buff;
}
