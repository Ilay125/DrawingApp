// #include <vld.h>
#include <string>
#include <iostream>

#include "img_to_code.h"

#define BUF_SIZE 867

int main() {
    std::string src = "nerd.png";

    int thresh = 128;
    unsigned char buffer[BUF_SIZE] = { 0 };

    if(png2code(src, thresh, buffer, BUF_SIZE)) {
        std::cout << "An error occured!!!" << std::endl;
    } else {
        for (int c : buffer) {
            printf("%d ", c);
        }
    }

    return 0;

}