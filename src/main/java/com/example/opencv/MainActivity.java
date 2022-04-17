package com.example.opencv;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private Mat mRgba, mGrey;
    JavaCameraView javaCameraView;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        javaCameraView = findViewById(R.id.my_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.enableView();
        javaCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(javaCameraView!=null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(javaCameraView!=null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()){
            Log.i(TAG, "OpenCV loaded Successfully");
            Toast.makeText(getApplicationContext(), "There is a problem in opencv", Toast.LENGTH_SHORT).show();
        } else {
            Log.i(TAG,"OpenCV loaded failed");
        }
    }
    private double average (ArrayList<Double> list) {
        double sum = 0;
        for (int i = 0; i < list.size(); i++) {
            sum += list.get(i);
        }
        return sum / list.size();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGrey = inputFrame.gray();
        // to detect lane
        Mat edges = new Mat();
        // edge detection
        // reduce noise
        Imgproc.GaussianBlur(mGrey, mGrey, new Size(7, 7), 0);
        // region of interest
        Rect roi = new Rect(mGrey.width() / 4, mGrey.height() / 2,  mGrey.width() / 2, mGrey.height() / 2);
        Mat masked_image = new Mat(mGrey, roi);
        Imgproc.Canny(masked_image, edges, 10, 200);
        // eliminate the horizontal info
        Mat kernel = Mat.zeros(1, 3, CvType.CV_32F);
        kernel.put(0,0,-1);
        kernel.put(0, 2, 1);
        Imgproc.filter2D(edges, edges, -1, kernel);
        // then detect lines in frame
        // define variables first
        // store lines in mat format
        Mat lines = new Mat();
        // starting and ending point of lines
        Point p1 = new Point();
        Point p2 = new Point();
        double a, b;
        double x0, y0;
        Imgproc.HoughLinesP(edges, lines, 2.0, Math.PI/180.0, 40, 100, 50);
        // then loop through each line
        for (int i = 0; i < lines.rows(); i++) {
            // for each line
            double[] vec = lines.get(i, 0);
            double rho = vec[0];
            double theta = vec[1];
            a = Math.cos(theta);
            b = Math.sin(theta);
            x0 = a * rho;
            y0 = b * rho;
            // starting point and end point
            p1.x = vec[0] + mGrey.width() / 4;
            p1.y = vec[1] + mGrey.height() / 2;
            p2.x = vec[2] + mGrey.width() / 4;
            p2.y = vec[3] + mGrey.height() / 2;
            Imgproc.line(mRgba, p1, p2, new Scalar(0, 255.0, 0), 20, Imgproc.LINE_AA, 0);
        }
        return mRgba;
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGrey.release();
    }

    @Override
    public void onCameraViewStarted(int width, int height)
    {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGrey = new Mat(height, width, CvType.CV_8UC4);
    }
}