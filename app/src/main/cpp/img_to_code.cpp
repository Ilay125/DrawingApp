
#include <opencv2/opencv.hpp>
#include "preprocess.h"
#include "img_to_code.h"


void write_svg_info(imginfo_t& imginfo, const potrace_bitmap_t& bmp) {
    /*
    // svg data
    info.opaque = 0;       // outline only, no fill
    info.color = 0x000000; // stroke color black
    info.unit = 10.0;       // coordinate unit
    info.angle = 0;
    info.grouping = 0;     // flat paths
    info.fillcolor = 0; // unused
    */

    imginfo.pixwidth = bmp.w;
    imginfo.pixheight = bmp.h;

    imginfo.width = bmp.w;    // output width
    imginfo.height = bmp.h;   // output height

    imginfo.lmar = imginfo.rmar = imginfo.tmar = imginfo.bmar = 0; // margins

    // bounding box must be nonzero
    imginfo.trans.bb[0] = bmp.w;   // width of bounding box
    imginfo.trans.bb[1] = bmp.h;   // height of bounding box
    imginfo.trans.orig[0] = 0;            // origin x
    imginfo.trans.orig[1] = bmp.h;            // origin y
    imginfo.trans.scalex = 1.0;           // x scale
    imginfo.trans.scaley = -1.0;           // y scale


}

int png2svg(const std::string src, const std::string dst, const int thresh) {
     cv::Mat img;
    if (open_img(src.c_str(), img)) {
        std::cerr << "No image was found in path: "+src << std::endl;
        return 1;
    }

    cv::Mat bin;

    prep_img(img, bin, thresh);

    potrace_bitmap_t bmp;
    MapBuffer mb;
    cvt_mat_to_bmp(bin, bmp, mb);

    potrace_param_t *param = potrace_param_default(); // default parameters
    potrace_state_t *state = potrace_trace(param, &bmp); // trace bitmap

    if (!state) {
        std::cerr << "Potrace failed!" << std::endl;
        return 1;
    }

    // Open SVG file for writing
    FILE *svg_file = std::fopen(dst.c_str(), "w");
    if (!svg_file) {
        std::fprintf(stderr, "Cannot open output.svg for writing\n");
        potrace_state_free(state);
        potrace_param_free(param);
        return 1;
    }

    // --- Configure per-image info ---
    imginfo_t imginfo = {};

    write_svg_info(imginfo, bmp);

    // Write SVG
    my_page_svg(svg_file, state->plist, &imginfo);

    // Clean up
    potrace_state_free(state);
    potrace_param_free(param);
    std::fclose(svg_file);   

    return 0;
}

