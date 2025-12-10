#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <math.h>

#include "potracelib.h"
#include "curve.h"
#include "main.h"
#include "backend_svg.h"
#include "lists.h"
#include "auxiliary.h"

#define MAX_BOUNDARY 22 // max val in cm
#define MAX_CHAR 254

#define INC_IDX increment_idx(&idx, &err_flag, sout_size)

int increment_idx(int* idx, int* flag, int buf_size) {
    int old_idx = *idx;
    (*idx)++;

    if (*idx >= buf_size) {
        *flag = 1;
        *idx = buf_size;
    }

    return old_idx;
}

void get_border_vals(potrace_path_t* plist, double* min_x, double* min_y, double* max_x, double* max_y) {
    for (potrace_path_t *p = plist; p; p = p->next) {
        potrace_curve_t* curve = &p->curve;

        for (int i = 0; i < curve->n; i++) {
            int points = 0;
            if (i == 0) { // starting point of the subpath
                points = 1;
            } else if (curve->tag[i] == POTRACE_CURVETO) {
                points = 3;
            } else if (curve->tag[i] == POTRACE_CORNER) {
                // points = 2;
                points = 1;
            }

            for (int j = 0; j < points; j++) {
                *min_x = fmin(*min_x, curve->c[i][j].x);
                *min_y = fmin(*min_y, curve->c[i][j].y);
                *max_x = fmax(*max_x, curve->c[i][j].x);
                *max_y = fmax(*max_y, curve->c[i][j].y);
            }
        }
    }
}

/*
unsigned char cvt_coords(double x, double min, double scale) {
    double shifted = (x - min) * scale * 10; // shift & scale
    int rounded = (int)round(shifted);

    printf("original %.3f, rounded %d\n", shifted, rounded);
    if (rounded < 0) {
        rounded = 0;
    } else {
        rounded += 1;
        if (rounded > MAX_CHAR) {
            rounded = MAX_CHAR;
        }
    }

    return (unsigned char)rounded;
}
*/

unsigned char cvt_coords(double x, double offset, double scale) {
    x -= offset;
    x *= scale;
    x *= 10; // add 1 digit after dec point
    int rounded = (int)round(x);

    printf("original %.3f, rounded %d\n", x, rounded);
    if (rounded < 0) {
        rounded = 0;
    } else {
        rounded += 1;
        if (rounded > MAX_CHAR) {
            rounded = MAX_CHAR;
        }
    }

    return (unsigned char)rounded;
}

//int draw_code(char* sout, size_t sout_size, potrace_path_t* plist, imginfo_t* imginfo)
int draw_code(unsigned char* sout, size_t sout_size, potrace_path_t* plist) {
    if (!sout || !plist) return 1;

    int idx = 0;
    int err_flag = 0;

    // First shift the coordinates so they will be all positive
    double min_x = 0;
    double min_y = 0;
    double max_x = MAX_BOUNDARY;
    double max_y = MAX_BOUNDARY;

    get_border_vals(plist, &min_x, &min_y, &max_x, &max_y);

    double scale = MAX_BOUNDARY / fmax(max_x, max_y); // preserves aspect ratio

    // --- Single path ---
    for (potrace_path_t *p = plist; p; p = p->next) {
        potrace_curve_t *curve = &p->curve;

        // Move to first point of this subpath
        double x0 = curve->c[curve->n - 1][2].x;
        double y0 = curve->c[curve->n - 1][2].y;

        sout[INC_IDX] = 'M';
        sout[INC_IDX] = cvt_coords(x0, min_x, scale);
        sout[INC_IDX] = cvt_coords(y0, min_y, scale);

        if (err_flag) {
            return 1;
        }

        // Draw segments
        for (int i = 0; i < curve->n; i++) {
            if (err_flag) {
                return 1;
            }

            if (curve->tag[i] == POTRACE_CURVETO) {
                sout[INC_IDX] = 'C';
                sout[INC_IDX] = cvt_coords(curve->c[i][0].x, min_x, scale);
                sout[INC_IDX] = cvt_coords(curve->c[i][0].y, min_y, scale);
                sout[INC_IDX] = cvt_coords(curve->c[i][1].x, min_x, scale);
                sout[INC_IDX] = cvt_coords(curve->c[i][1].y, min_y, scale);
                sout[INC_IDX] = cvt_coords(curve->c[i][2].x, min_x, scale);
                sout[INC_IDX] = cvt_coords(curve->c[i][2].y, min_y, scale);
            } else if (curve->tag[i] == POTRACE_CORNER) {
                sout[INC_IDX] = 'L';
                sout[INC_IDX] = cvt_coords(curve->c[i][1].x, min_x, scale);
                sout[INC_IDX] = cvt_coords(curve->c[i][1].y, min_y, scale);

                /*
                sout[INC_IDX] = cvt_coords(curve->c[i][2].x, min_x, scale);
                sout[INC_IDX] = cvt_coords(curve->c[i][2].y, min_y, scale);
                */

            }
        }

        sout[INC_IDX] = 'Z';

        if (err_flag) {
            return 1;
        }
    }

    return err_flag;
}