#include <jni.h>
#include <string>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>


using namespace std;
using namespace cv;

extern "C"
{

Mat JNICALL Java_com_shade6_p1coating_MainActivity_salt(JNIEnv *env, jobject instance,
                                                                           jlong matAddrGray,
                                                                           jint nbrElem) {
    Mat &mGr = *(Mat *) matAddrGray;
//    Mat mat = *(Mat *) matAddrGray;
//    cvtColor( image, gray_image, CV_BGR2GRAY );
//    addWeighted( &mGr, 2, &mGr, 2, 0.0, &mGr);
    for (int k = 0; k < nbrElem; k++) {
        int i = rand() % mGr.cols;
        int j = rand() % mGr.rows;
        mGr.at<uchar>(j, i) = 255;
    }

}
}
