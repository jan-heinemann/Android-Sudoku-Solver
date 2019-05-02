package com.example.sudoku;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.googlecode.tesseract.android.TessBaseAPI;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static String DATA_PATH;
    private static final String TESS_DATA = "/tessdata";
    String TAG = "myApp";

    ImageView imageView;

    Bitmap myBitmap = null;

    SudokuManager sudokuManager;

    Uri imageUri = null;

    private TessBaseAPI tessBaseAPI;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        DATA_PATH = getFilesDir().toString() + "/Tess";

        imageView = findViewById(R.id.imageView);


        int res = 0;
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                        res);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }


        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, res);
        //Utils.bitmapToMat(bmp, ImageMat);

        Button btn_Open = findViewById(R.id.btn_Open);
        btn_Open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage("Camera or Photos?")
                        .setCancelable(true)
                        .setPositiveButton("Camera", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                ContentValues values = new ContentValues();
                                values.put(MediaStore.Images.Media.TITLE, "New Picture");
                                values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
                                imageUri = getContentResolver().insert(
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                                startActivityForResult(intent, 0);
                            }
                        })
                        .setNegativeButton("Photos", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(pickPhoto , 1);//one can be replaced with any action code
                            }
                        })
                        .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                });

                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        Button btn_Solve = findViewById(R.id.btn_Solve);
        btn_Solve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initSolving();
                Intent intent = new Intent(MainActivity.this, FieldActivity.class);
                Bundle b = new Bundle();

                b.putSerializable("myBoard", sudokuManager.sudokuField);

                intent.putExtras(b);

                startActivity(intent);
            }
        });
    }

    private void saveImage(Bitmap finalBitmap, String image_name) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = "Image-" + image_name+ ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        Uri selected = null;
        switch(requestCode) {
            case 0:
                if(resultCode == RESULT_OK){
                    selected = imageUri;

                }

                break;
            case 1:
                if(resultCode == RESULT_OK){
                    selected = imageReturnedIntent.getData();
                }
                break;
        }
        if(selected != null) {
            try{
                myBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selected);
                imageView.setImageURI(selected);

                imageView.setImageBitmap(myBitmap);

                saveImage(myBitmap, "myFile");
            }
            catch(Exception e) {}

        }
    }

    private void initSolving() {
        initTesseract();
        Mat preprocessed = preprocessGrid(myBitmap);
        Bitmap detectedSudokuGrid = detectSudokuGrid(preprocessed, myBitmap);

        detectedSudokuGrid = Bitmap.createScaledBitmap(detectedSudokuGrid, 1200, 1200, false);

        int[][] extractedSudokuGrid = detectDigits(detectedSudokuGrid);
        sudokuManager = new SudokuManager(extractedSudokuGrid);
        sudokuManager.printField(sudokuManager.sudokuField);

    }

    private void prepareTessData() {

        try{
            File dir = new File(DATA_PATH+TESS_DATA);
            if(!dir.exists()) {

                boolean myres =  dir.mkdirs();
                Log.d(TAG, "akldhlsd");
            }
            if(!dir.exists()) {
                dir.mkdirs();
            }
            String fileList[] = getAssets().list("");
            //for(String fileName : fileListm) {
            String fileName = "eng.traineddata";
                String pathToDataFile = DATA_PATH + TESS_DATA + "/" + fileName;
                if(!(new File(pathToDataFile)).exists()) {
                    InputStream is = getAssets().open(fileName);
                    OutputStream os = new FileOutputStream(pathToDataFile);
                    byte[]  buff = new byte[1024];
                    int len;
                    while((len = is.read(buff)) > 0) {
                        os.write(buff, 0, len);
                    }
                    is.close();
                    os.close();
                }
           // }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void initTesseract() {
        if(tessBaseAPI == null) {
            try{
                tessBaseAPI = new TessBaseAPI();
            }catch (Exception e){
                Log.e(TAG, e.getMessage());
            }
        }

        tessBaseAPI.init(DATA_PATH,"eng", TessBaseAPI.OEM_TESSERACT_ONLY);
    }

    private String getText(Bitmap bitmap){

        tessBaseAPI.setImage(bitmap);

        //tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_CHAR);
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, ".-!?@#$%&*()<>_-+=/:;'\"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789");
        //tessBaseAPI.setVariable("classify_bln_numeric_mode", "1");

        String retStr = "No result";
        try{
            retStr = tessBaseAPI.getUTF8Text();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        return retStr;
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
    }

    Mat preprocessGrid(Bitmap input) {
        Bitmap bmp = input;
        Mat sudoku = new Mat();
        Utils.bitmapToMat(bmp, sudoku);

        Imgproc.cvtColor(sudoku, sudoku, Imgproc.COLOR_BGRA2GRAY);

        Imgproc.GaussianBlur(sudoku, sudoku, new Size(7,7), 0);
        Imgproc.adaptiveThreshold(sudoku, sudoku, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);

        Core.bitwise_not(sudoku, sudoku);

        Mat kernel = new Mat( 3, 3, CvType.CV_8U );
        int row = 0, col = 0;
        kernel.put(row ,col, 0, 1, 0, 1, 1, 1, 0, 1, 0 );
        Imgproc.dilate(sudoku, sudoku, kernel);

        return sudoku;
    }


    Bitmap detectSudokuGrid(Mat input, Bitmap originalSudoku) {
        Mat outerBox = input.clone();
        Mat sudoku = new Mat();

        Utils.bitmapToMat(originalSudoku, sudoku);

        List<MatOfPoint> conts = new ArrayList<MatOfPoint>();

        Imgproc.findContours(outerBox, conts, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        Imgproc.cvtColor(outerBox, outerBox, Imgproc.COLOR_GRAY2BGR);

        //Sort contours by area
        Collections.sort(conts, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                double i = Math.abs( Imgproc.contourArea(o1));
                double j = Math.abs( Imgproc.contourArea(o2));
                return j > i ? -1 : j < i ? 1 : 0;
            }
        });

        Collections.reverse(conts);

        //Get the corner Points of the biggest contour
        //Biggest contour is assumed to be the outer grid of the sudoku
        Point topRight = new Point(0,0);
        Point bottomRight = new Point(0,0);
        Point topLeft = new Point(Integer.MAX_VALUE,Integer.MAX_VALUE);
        Point bottomLeft = new Point(Integer.MAX_VALUE,Integer.MAX_VALUE);
        for(Point point : conts.get(0).toList()) {
            if(point.x - point.y > topRight.x - topRight.y) {
                topRight = point;
            }
            if(point.x + point.y > bottomRight.x + bottomRight.y) {
                bottomRight = point;
            }

            if((point.x + point.y) < (topLeft.x + topLeft.y)) {
                topLeft = point;
            }
            if((point.x - point.y) < (bottomLeft.x - bottomLeft.y)) {
                bottomLeft = point;
            }

        }



        Imgproc.circle(outerBox, topLeft, 20, new Scalar(255,0,0), Imgproc.FILLED);
        Imgproc.circle(outerBox, bottomLeft, 20, new Scalar(255,0,0), Imgproc.FILLED);
        Imgproc.circle(outerBox, topRight, 20, new Scalar(255,0,0), Imgproc.FILLED);
        Imgproc.circle(outerBox, bottomRight, 20, new Scalar(255,0,0), Imgproc.FILLED);

        //Imgproc.drawContours(myMat, conts, -1, new Scalar(255, 0, 0), 20);

        double maxLength = distance_between(topLeft, topRight);

        double tmp = distance_between(topLeft, bottomLeft);
        maxLength = maxLength < tmp ? tmp : maxLength;

        tmp = distance_between(topRight, bottomRight);
        maxLength = maxLength < tmp ? tmp : maxLength;

        tmp = distance_between(bottomRight, bottomLeft);
        maxLength = maxLength < tmp ? tmp : maxLength;


        Point[] src = new Point[4];
        Point[] dst = new Point[4];

        src[0] = topLeft;
        src[1] = topRight;
        src[2] = bottomRight;
        src[3] = bottomLeft;

        dst[0] = new Point(0,0);
        dst[1] = new Point(maxLength - 1, 0);
        dst[2] = new Point(maxLength - 1, maxLength-1);
        dst[3] = new Point(0, maxLength - 1);

        Mat undistorted = new Mat(new Size(maxLength, maxLength), CvType.CV_8UC1);

        Mat pers = new Mat();

        MatOfPoint2f src1 = new MatOfPoint2f();
        src1.fromArray(src);
        MatOfPoint2f dst1 = new MatOfPoint2f();
        dst1.fromArray(dst);

        Imgproc.warpPerspective(sudoku, undistorted, Imgproc.getPerspectiveTransform(src1, dst1), new Size(maxLength, maxLength));

        Bitmap bmp = Bitmap.createBitmap(undistorted.width(), undistorted.height(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(undistorted, bmp);

        return bmp;
    }


    int[][] detectDigits(Bitmap inputGrid) {

        Bitmap[][] matrix = splitImage(inputGrid);
        int[][] field = new int[9][9];

        if(true) {
            for(int i = 0; i <9 ;i++) {
                for(int j = 0; j <9 ;j++) {
                    try{
                         String s = getText(filterDigit(matrix[i][j]));
                        if(s.length() != 0 && Integer.parseInt(s) <= 9) {
                            field[i][j] = Integer.parseInt(s);
                        }
                        else {
                            field[i][j] = 0;

                        }
                    }catch(NumberFormatException e ) {
                        field[i][j] = 0;
                    }
                }
            }
        }
        return field;
    }

    Bitmap filterDigit(Bitmap bmpIn) {
        Bitmap bmp = bmpIn;


        Mat test = new Mat();
        Utils.bitmapToMat(bmp, test);

        Imgproc.cvtColor(test, test, Imgproc.COLOR_BGRA2GRAY);

        Mat orig = test.clone();
        Imgproc.cvtColor(orig, orig, Imgproc.COLOR_GRAY2BGR);


        Mat kernel = new Mat( 3, 3, CvType.CV_8U );
        int row = 0, col = 0;
        kernel.put(row ,col, 0, 1, 0, 1, 1, 1, 0, 1, 0 );

        List<MatOfPoint> conts = new ArrayList<MatOfPoint>();

        Mat hier = new Mat();



        Imgproc.GaussianBlur(test, test, new Size(7,7), 0);
        Imgproc.adaptiveThreshold(test, test, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11,2);

        Core.bitwise_not(test, test);
        kernel.put(row ,col, 0, 1, 0, 1, 1, 1, 0, 1, 0 );

        Imgproc.line(test, new Point(0, 0), new Point(8, 8), new Scalar(0,0,0), 2);

        Imgproc.line(test, new Point(0, test.height()), new Point(8, test.height()-8), new Scalar(0,0,0), 2);

        Imgproc.line(test, new Point(test.width(), 0), new Point(test.width()-8, 8), new Scalar(0,0,0), 2);
        Imgproc.line(test, new Point(test.width(), test.height()), new Point(test.width()-8, test.height()-8), new Scalar(0,0,0), 2);


        Imgproc.findContours(test, conts, hier, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        Imgproc.cvtColor(test, test, Imgproc.COLOR_GRAY2BGR);

        Collections.sort(conts, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                double i = Math.abs( Imgproc.contourArea(o1));
                double j = Math.abs( Imgproc.contourArea(o2));
                return j > i ? -1 : j < i ? 1 : 0;
            }
        });


        Collections.reverse(conts);


        MatOfPoint closest = conts.get(0);
        MatOfPoint closest2 = conts.size() > 1 ? conts.get(1) : conts.get(0);
        Point center = new Point(test.width() * 0.5, test.height() * 0.5);

        int index = 0;
        for(MatOfPoint p : conts) {
            double area = Imgproc.contourArea(p);
            if(area < 100)
                continue;
            double x = Imgproc.moments(p).m10 / Imgproc.moments(p).m00;
            double y = Imgproc.moments(p).m01 / Imgproc.moments(p).m00;

            double xC = Imgproc.moments(closest).m10 / Imgproc.moments(closest).m00;
            double yC = Imgproc.moments(closest).m01 / Imgproc.moments(closest).m00;

            double xC2 = Imgproc.moments(closest2).m10 / Imgproc.moments(closest2).m00;
            double yC2 = Imgproc.moments(closest2).m01 / Imgproc.moments(closest2).m00;

            if(distance_between(new Point(x,y), center) < distance_between(new Point(xC, yC), center)) {
                closest2 = closest;
                closest = p;


            }
            else if(distance_between(new Point(x,y), center) < distance_between(new Point(xC2, yC2), center)) {
                closest2 = p;
            }

            index++;
        }

        double xC = Imgproc.moments(closest).m10 / Imgproc.moments(closest).m00;
        double yC = Imgproc.moments(closest).m01 / Imgproc.moments(closest).m00;

        double xC2 = Imgproc.moments(closest2).m10 / Imgproc.moments(closest2).m00;
        double yC2 = Imgproc.moments(closest2).m01 / Imgproc.moments(closest2).m00;

        if(Imgproc.contourArea(closest2) > Imgproc.contourArea(closest) && distance_between(new Point(xC, yC), new Point(xC2, yC2)) < 1) {
            closest = closest2;
        }

        bmp = Bitmap.createBitmap(test.width(), test.height(), Bitmap.Config.ARGB_8888);

        ArrayList<MatOfPoint> mp = new ArrayList<MatOfPoint>();

        mp.add(closest);

         xC = Imgproc.moments(closest).m10 / Imgproc.moments(closest).m00;
         yC = Imgproc.moments(closest).m01 / Imgproc.moments(closest).m00;

        Mat mask = new Mat(test.width(), test.height(), CvType.CV_8UC3, new Scalar(0,0,0));

        Point pp = new Point(xC, yC);
        double dis = distance_between(center, pp);

        Mat mask2 = new Mat(test.width(), test.height(), CvType.CV_8UC3, new Scalar(0,0,0));
        Imgproc.circle(mask2, center, 10, new Scalar(255,255,255), Imgproc.FILLED);

        Mat test2 = new Mat();
        Core.bitwise_and(test, mask2, test2);

        Imgproc.cvtColor(test2,test2, Imgproc.COLOR_RGB2GRAY);

        int num = Core.countNonZero(test2);


        if(dis < 50.0d && num >= 10) {
            Imgproc.drawContours(mask, mp, -1, new Scalar(255,255,255), 0);

            //Core.bitwise_and(test, mask, test);*/

            Imgproc.fillPoly(mask, mp, new Scalar(255,255,255));
        }

        Core.bitwise_and(mask, test, test);

        Utils.matToBitmap(test, bmp);

        return bmp;
    }

    double distance_between(Point p1, Point p2) {
        return Math.sqrt(Math.pow((p2.x - p1.x), 2) + Math.pow((p2.y - p1.y), 2));
    }


    private Bitmap[][] splitImage(Bitmap inputImage) {
        Bitmap[][] imageMatrix = new Bitmap[9][9];
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        int widthSteps = width / 9;
        int heightSteps = height / 9;

        for(int i = 0; i < 9; i++) {
            for(int j = 0; j < 9; j++) {
                if(i == 8 || j == 8) {
                    imageMatrix[i][j] = Bitmap.createBitmap(inputImage, widthSteps*j, heightSteps*i, widthSteps, heightSteps);

                }
                else
                  imageMatrix[i][j] = Bitmap.createBitmap(inputImage, widthSteps*j, heightSteps*i, widthSteps+8, heightSteps+8);
            }
        }
        return imageMatrix;
    }



    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");

                    prepareTessData();

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

}
