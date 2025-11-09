// #include <vld.h>
#include <string>
#include <iostream>


#include "img_to_code.h"


int main() {
    std::string src = "nerd.png";
    std::string dst = "nerd_out.svg";

    int thresh = 100;

    if(png2svg(src, dst, thresh)) {
        std::cout << "An error occured." << std::endl;
    } else {
        std::cout << "Managed to write svg file." << std::endl;
    }

    return 0;

}