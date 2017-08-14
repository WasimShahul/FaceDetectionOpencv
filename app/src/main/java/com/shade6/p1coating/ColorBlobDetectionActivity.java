package com.shade6.p1coating;

import java.io.IOException;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.pes.androidmaterialcolorpickerdialog.ColorPicker;
import com.pes.androidmaterialcolorpickerdialog.ColorPickerCallback;

public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private boolean mIsColorSelected = false;
    private Mat mRgba;
    private Mat matImage;
    private Mat mFinalRgba;
    private Mat mOriginal;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    private static Scalar selectedColor;
    private ColorBlobDetector mDetector;
    private Mat mSpectrum;
    private Mat hierarchy;
    private Size SPECTRUM_SIZE;
    private Scalar CONTOUR_COLOR;
    private static float alpha;
    private Button colourPicker;
    private CameraBridgeViewBase mOpenCvCameraView;
    ColorPicker cp;
    SeekBar seekBar1;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public ColorBlobDetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        seekBar1 = (SeekBar) findViewById(R.id.seekBar1);
        seekBar1.setMax(10);
        seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                alpha = (float) i;
                alpha = alpha / 10;
                Toast.makeText(ColorBlobDetectionActivity.this, i+" :: "+alpha, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });



        selectedColor = new Scalar(0, 255, 0, alpha);
        cp = new ColorPicker(ColorBlobDetectionActivity.this, 255, 0, 0);
        colourPicker = (Button) findViewById(R.id.pickColour);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.main_surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        colourPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* Show color picker dialog */
                cp.show();

    /* Set a new Listener called when user click "select" */
                cp.setCallback(new ColorPickerCallback() {
                    @Override
                    public void onColorChosen(@ColorInt int color) {
                        // Do whatever you want
                        selectedColor = new Scalar(Color.red(color), Color.green(color), Color.blue(color), alpha);
                        // Examples
                        Log.d("Alpha", Integer.toString(Color.alpha(color)));
                        Log.d("Red", Integer.toString(Color.red(color)));
                        Log.d("Green", Integer.toString(Color.green(color)));
                        Log.d("Blue", Integer.toString(Color.blue(color)));

                        Log.d("Pure Hex", Integer.toHexString(color));
                        Log.d("#Hex no alpha", String.format("#%06X", (0xFFFFFF & color)));
                        Log.d("#Hex with alpha", String.format("#%08X", (0xFFFFFFFF & color)));
                        cp.dismiss();
                    }
                });
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        matImage = new Mat(height, width, CvType.CV_8UC4);
        try {
            matImage = Utils.loadResource(ColorBlobDetectionActivity.this, R.drawable.image);
        } catch (IOException e) {
            Toast.makeText(this, " Hello ", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        mFinalRgba = new Mat(height, width, CvType.CV_8UC4);
        mOriginal = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        hierarchy = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255, 0, 0, 255);
    }

    public void onCameraViewStopped() {
        mRgba.release();
        hierarchy.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x > 4) ? x - 4 : 0;
        touchedRect.y = (y > 4) ? y - 4 : 0;

        touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mRgba.copyTo(mOriginal);
        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();

            for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
//                Imgproc.drawContours(mRgba, contours, contourIdx, new Scalar(Math.random() * 255, Math.random() * 255, Math.random() * 255), -1, 8, hierarchy, 0, new Point());
//                Imgproc.drawContours(mRgba, contours, contourIdx, selectedColor, -1);
                // Get bounding rect of contour
                Rect rect = Imgproc.boundingRect(contours.get(contourIdx));
                Imgproc.rectangle(mRgba, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height), selectedColor , -1);
            }
            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);
            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }
//        mOriginal.setTo(new Scalar(255,255,255));
        Core.addWeighted(mRgba, alpha , mOriginal, 1 - alpha, 0.0, mOriginal);
        return mOriginal;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}
