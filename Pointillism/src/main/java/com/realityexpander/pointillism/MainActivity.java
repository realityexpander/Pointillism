package com.realityexpander.pointillism;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ShareActionProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class MainActivity extends Activity {


    private ShareActionProvider mShareActionProvider;
    private Intent mShareIntent;


String cool= "cool";

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;

    public Bitmap imageBitmap;

    private HorizontalScrollView container;
    private int currentX;
    private int currentY;
    private static final int MAX_SPLASH_SECONDS = 2;
    private Dialog splashDialog;

    private class StateSaver {
        private boolean showSplashScreen = true;
        // Other save state info here...
    }

    class brush {
        int x;
        int y;
        int color;
        float size;  // 0->1
        float angle; // 0->1
        int opacity;
    }


    // Painting variables
    boolean runningBGTask;
    int mEffectAlgorithm;
    Matrix matrix;
    int widthSourceImage ;
    int lengthSourceImage ;

    Bitmap brushBitMap, rotatedBitmap;
    Bitmap brushBitMap1, brushBitMap2, brushBitMap3, brushBitMap4, brushBitMap5;
    int sourceSample[][];
    int brushSample;
    int brushRed, brushGrn, brushBlu;
    int outputRed, outputGrn, outputBlu;
    int widthBrushImage, lengthBrushImage;
    int widthNumBrushes;
    int lengthNumBrushes;
    brush[] theBrushes ;
    float[] theHSVColor;
    int xSource, ySource;
    int brushXStart, brushXEnd, brushYStart, brushYEnd;
    int brushSampleX, brushSampleY;
    float brushSizeMult;
    int x, y;

    int debug_got_pixel;

    private ImageView imageView;
    private PaintImageTask task_;

    Bitmap sourceImageBitmap;
    int screenWidth ;
    int screenHeight;

    boolean alreadyShownSplash;
    boolean alreadyPickedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        StateSaver data = (StateSaver) getLastNonConfigurationInstance();
//        if (splashDialog == null && alreadyShownSplash == false) { // "all this has happened before"
//             showSplashScreen();
//             alreadyShownSplash = true;
//        } else {
//            setContentView(R.layout.activity_main);
//        }

        // Fill in the imagebitmap with a placeholder (a brush)
        imageView = (ImageView) findViewById(R.id.imageView);
        if ( imageBitmap == null)
            imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pointillism_splash);

        brushBitMap1 = BitmapFactory.decodeResource(getResources(), R.drawable.brush5);
        brushBitMap2 = BitmapFactory.decodeResource(getResources(), R.drawable.brush2);
        brushBitMap3 = BitmapFactory.decodeResource(getResources(), R.drawable.brush3);
        brushBitMap4 = BitmapFactory.decodeResource(getResources(), R.drawable.brush8);
        brushBitMap5 = BitmapFactory.decodeResource(getResources(), R.drawable.brush9);

        runningBGTask = false;

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        GridView gridview = (GridView) findViewById(R.id.gridView);
        gridview.setAdapter(new ImageAdapter(this));
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                //Toast.makeText(MainActivity.this, "" + position, Toast.LENGTH_SHORT).show();
                // Start any heavy-duty loading here, but on its own thread
                if (runningBGTask == false) {

                    ProgressBar progressView = (ProgressBar) findViewById(R.id.progressBar);
                    progressView.setVisibility(View.VISIBLE);

                    mEffectAlgorithm = position;
                    try {
                        // Load brush bitmap
                        //brushBitMap = getBitmapFromAsset(getApplicationContext(), "brush1");

                        switch( position ){
                            //brushBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.brush1);
                            case 0:
                                brushBitMap = brushBitMap1;
                                break;
                            case 1:
                                brushBitMap = brushBitMap2;
                                break;
                            case 2:
                                brushBitMap = brushBitMap3;
                                break;
                            case 3:
                                brushBitMap = brushBitMap4;
                                break;
                            case 4:
                                brushBitMap = brushBitMap5;
                                break;
                            default:
                                brushBitMap = brushBitMap2;
                        }

                        if (brushBitMap == null)
                            Log.e("0xcafebabe:problem getting brush", Integer.toString(position));

                        // Apply a rotation
                        matrix = new Matrix();
                        //imageView.setScaleType(ImageView.ScaleType.MATRIX);   //required
                        //matrix.postRotate((float) angle, pivX, pivY);
                        //matrix.postRotate( 180f, imageView.getDrawable().getBounds().width()/2, imageView.getDrawable().getBounds().height()/2);
                        //imageView.setImageMatrix(matrix);

                        widthSourceImage = imageBitmap.getWidth();
                        lengthSourceImage = imageBitmap.getHeight();

                        // Create the array of brushes
                        widthBrushImage = brushBitMap.getWidth();
                        lengthBrushImage = brushBitMap.getHeight();
                        widthNumBrushes = widthSourceImage / widthBrushImage;
                        lengthNumBrushes = lengthSourceImage / lengthBrushImage;

//                        Log.e("OnItemClickGridView: widthSourceImage", Integer.toString(widthSourceImage));
//                        Log.e("OnItemClickGridView: lengthSourceImage", Integer.toString(lengthSourceImage));
//                        Log.e("OnItemClickGridView: widthBrushImage", Integer.toString(widthBrushImage));
//                        Log.e("OnItemClickGridView: lengthBrushImage", Integer.toString(lengthBrushImage));

                        // TESTING CHANGE TODO
                        //widthNumBrushes = 5;
                        //lengthNumBrushes = 5;

                        // Brush sampled colors & brush locations
                        theBrushes = new brush[(widthNumBrushes + 1) * (lengthNumBrushes + 1)];
                        theHSVColor = new float[3];

                        task_ = new PaintImageTask(imageView);
                        task_.execute(imageBitmap);

                    } catch (Exception e) {
                        Log.e("OnItemClickGridView:", "problem here", e);
                    }
                }
            }
        });



//        final HorizontalScrollView scrollView = (HorizontalScrollView) findViewById(R.id.horizontalScrollView);
//        ViewTreeObserver vto = scrollView.getViewTreeObserver();
//        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                scrollView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
//                Log.e("ScrollWidth",Integer.toString(scrollView.getChildAt(0)
//                        .getMeasuredWidth()-getWindowManager().getDefaultDisplay().getWidth()));
//
//            }
//        });

//        HorizontalScrollView container_view = (HorizontalScrollView) findViewById(R.id.horizontalScrollView);
//        ImageView container_image = (ImageView) findViewById(R.id.imageView);
//        Rect rectangle_image = new Rect(container_image.getLeft(), container_image.getTop(), container_image.getRight(), container_image.getBottom());
//        Rect rectangle_container = new Rect(container_view.getLeft(), container_view.getTop(), container_view.getRight(), container_view.getBottom());
//
//        int scrollH = ((rectangle_container.right - rectangle_container.left)/2) -
//                ((rectangle_image.right - rectangle_image.left)/2) ;
//        container_view.smoothScrollTo(-scrollH,0);


        }

    private class PaintImageTask extends AsyncTask<Bitmap, Integer, Bitmap> {

        private ImageView imageView_;
        private Bitmap outBitMap;

        public PaintImageTask(ImageView imageView) {
            super();
            imageView_ = imageView;
        }

        @Override
        protected Bitmap doInBackground(Bitmap... bitMap) {
            if ( runningBGTask == false ) {

                outBitMap = bitMap[0].copy(Bitmap.Config.ARGB_8888, true);

                runningBGTask = true;

                int n=0;

                try {

                    // Sample the image at the brush center points into the brushes array
                    int i=0;
                    int totalNumBrushes=0;
                    for (int x = 0; x <= widthNumBrushes; x++) {
                        for (int y = 0; y <= lengthNumBrushes; y++) {
                            // Set the location of the brush
                            theBrushes[i] = new brush();

                            switch(mEffectAlgorithm) {
                                case 0:
                                    // Randomly distributed
                                    theBrushes[i].x = (int) (Math.random() * widthSourceImage) ;
                                    theBrushes[i].y = (int) (Math.random() * lengthSourceImage) ;
                                    // Sample the source image to get color of the brush
                                    theBrushes[i].color = sourceImageBitmap.getPixel(theBrushes[i].x, theBrushes[i].y);
                                    // Size = brightness (value)
                                    Color.colorToHSV(theBrushes[i].color, theHSVColor);
                                    theBrushes[i].size = 1 - theHSVColor[2];
                                    // Size = constant
                                    //theBrushes[i].size = (float) .8;
                                    // Angle = color
                                    theBrushes[i].angle = theHSVColor[2] * 180;
                                    break;

                                case 1:
                                    // On a Grid
                                    //theBrushes[i].x = (x*widthBrushImage)  + (widthBrushImage /2);
                                    //theBrushes[i].y = (y*lengthBrushImage) + (lengthBrushImage/2);
                                    // Randomly distributed
                                    theBrushes[i].x = (int) (Math.random() * widthSourceImage);
                                    theBrushes[i].y = (int) (Math.random() * lengthSourceImage);
                                    // Sample the source image to get color of the brush
                                    theBrushes[i].color = sourceImageBitmap.getPixel(theBrushes[i].x, theBrushes[i].y);
                                    // Size = random
                                    theBrushes[i].size = (float) Math.max(Math.random() / 2, .1);
                                    // Size = brightness (value)
                                    Color.colorToHSV(theBrushes[i].color, theHSVColor);
                                    //theBrushes[i].size = 1 - theHSVColor[2];
                                    //Angle = distance from top
                                    //theBrushes[i].angle = (theBrushes[i].y / (float) widthSourceImage) * 180;
                                    //theBrushes[i].angle = (float) Math.sqrt( Math.pow(theBrushes[i].x - (widthSourceImage/2.0), 2.0)  + Math.pow((theBrushes[i].y - (lengthSourceImage/2.0)), 2.0) );
                                    theBrushes[i].angle = (float) (Math.atan( (double) (theBrushes[i].x - (widthSourceImage/2.0)) / ((theBrushes[i].y - (lengthSourceImage/2.0))) ) * 180/Math.PI);

                                    break;

                                case 2:
                                    // On a Grid
                                    //theBrushes[i].x = (x*widthBrushImage)  + (widthBrushImage /2);
                                    //theBrushes[i].y = (y*lengthBrushImage) + (lengthBrushImage/2);
                                    // Randomly distributed
                                    theBrushes[i].x = (int) (Math.random() * widthSourceImage);
                                    theBrushes[i].y = (int) (Math.random() * lengthSourceImage);
                                    // Sample the source image to get color of the brush
                                    theBrushes[i].color = sourceImageBitmap.getPixel(theBrushes[i].x, theBrushes[i].y);
                                    // Size = random
                                    //theBrushes[i].size = (float) Math.random() / 2;
                                    // Size = hue (value)
                                    Color.colorToHSV(theBrushes[i].color, theHSVColor);
                                    theBrushes[i].size = (float) Math.max(theHSVColor[2], .1);

                                    // Change the brush color by a small random amount
                                    theHSVColor[0] += (Math.random() * 30);
                                    theBrushes[i].color = Color.HSVToColor(theHSVColor);

                                    //Angle = distance from top
                                    theBrushes[i].angle = (float) Math.sqrt( Math.pow(theBrushes[i].x - (widthSourceImage/2.0), 2.0)  + Math.pow((theBrushes[i].y - (lengthSourceImage/2.0)), 2.0) );
                                    break;

                                case 3:  // Sphere with min

                                    // Sample the source image to get color of the brush
                                    // On a Grid
                                    //theBrushes[i].x = (x*widthBrushImage);
                                    //theBrushes[i].y = (y*lengthBrushImage);

                                    // Randomly
                                    theBrushes[i].x = (int) (Math.random() * widthSourceImage);
                                    theBrushes[i].y = (int) (Math.random() * lengthSourceImage);
                                    theBrushes[i].color = sourceImageBitmap.getPixel(theBrushes[i].x, theBrushes[i].y);

                                    //theBrushes[i].size = (float)  Math.random() / 2;
                                    // Size = brightness (value)
                                    Color.colorToHSV(theBrushes[i].color, theHSVColor);
                                    theBrushes[i].size = (float) Math.max(theHSVColor[2]*2, .01);

                                    // Change the brush color by a small amount based on Value
                                    theHSVColor[0] += theHSVColor[2] * 3.0;
                                    theBrushes[i].color = Color.HSVToColor(theHSVColor);

                                    // Change the brush color by a small random amount
                                    //theHSVColor[0] += (Math.random() * 50);
                                    //theBrushes[i].color = Color.HSVToColor(theHSVColor);

                                    //Angle = distance from top
                                    //theBrushes[i].angle = (float) Math.sqrt( Math.pow(theBrushes[i].x - (widthSourceImage/2.0), 2.0)  + Math.pow((theBrushes[i].y - (lengthSourceImage/2.0)), 2.0) );

                                    theBrushes[i].angle = 0; // the brush is a sphere

                                    break;

                                case 4:
                                    // On a Grid
                                    //theBrushes[i].x = (x*widthBrushImage)  + (widthBrushImage /2);
                                    //theBrushes[i].y = (y*lengthBrushImage) + (lengthBrushImage/2);
                                    // Randomly distributed
                                    theBrushes[i].x = (int) (Math.random() * widthSourceImage) ;
                                    theBrushes[i].y = (int) (Math.random() * lengthSourceImage) ;
                                    // Sample the source image to get color of the brush
                                    theBrushes[i].color = sourceImageBitmap.getPixel(theBrushes[i].x, theBrushes[i].y);
                                    // Size = random
                                    //theBrushes[i].size = (float) Math.random() / 2;
                                    // Size = brightness (value)
                                    Color.colorToHSV(theBrushes[i].color, theHSVColor);
                                    theBrushes[i].size =  theHSVColor[2] * (float) 0.7;

                                    // Change the brush value by a small random amount
                                    theHSVColor[2] += Math.max(Math.random() * 50, 2);
                                    theBrushes[i].color = Color.HSVToColor(theHSVColor);

                                    //Angle = color
                                    //theBrushes[i].angle = theHSVColor[0];
                                    theBrushes[i].angle = (float) Math.sqrt( Math.pow(theBrushes[i].x - (widthSourceImage/2.0), 2.0)  + Math.pow((theBrushes[i].y - (lengthSourceImage/2.0)), 2.0) ) / (float) 2.0;
                                    break;

                                default:
                                    break;
                            }

                            i++;
                        }
                    }

                    totalNumBrushes = i-1;

                    debug_got_pixel = -2;
                    for ( n = 0; n < totalNumBrushes; n++) {

                        if ( theBrushes[n].size > 0.02 ){
                        debug_got_pixel = -1;

                        //Log.e("theBrushes[" + Integer.toString(n) + "].size", Float.toString(theBrushes[n].size));

                        // Rotate the brush
                        matrix.reset();
                        matrix.postRotate(theBrushes[n].angle, brushBitMap.getWidth() / 2, brushBitMap.getHeight() / 2);
                        matrix.postScale(theBrushes[n].size, theBrushes[n].size);
                        rotatedBitmap = Bitmap.createBitmap(brushBitMap, 0, 0, brushBitMap.getWidth(), brushBitMap.getHeight(),
                                matrix, true);
                        widthBrushImage = rotatedBitmap.getWidth();
                        lengthBrushImage = rotatedBitmap.getHeight();

                        brushXStart = theBrushes[n].x - (widthBrushImage  / 2) ;
                        brushXEnd = theBrushes[n].x   + (widthBrushImage  / 2) ;
                        brushYStart = theBrushes[n].y - (lengthBrushImage / 2) ;
                        brushYEnd = theBrushes[n].y   + (lengthBrushImage / 2) ;

                        brushRed = (theBrushes[n].color & 0x00FF0000) >>> 16;
                        brushGrn = (theBrushes[n].color & 0x0000FF00) >>> 8;
                        brushBlu = (theBrushes[n].color & 0x000000FF);

                        // Render the brushes
                        int sourcePixel, outColor;
                        //brushSizeMult = ((float) 1 / (theBrushes[n].size));

                        x=0; y=0;
                        for (xSource = brushXStart, x = 0; xSource < brushXEnd-1; xSource++, x++) {
                            for (ySource = brushYStart, y = 0; ySource < brushYEnd-1; ySource++, y++) {
                                if ((xSource < widthSourceImage) && (ySource < lengthSourceImage) && (xSource >= 0) && (ySource >= 0)) {

                                    debug_got_pixel = 0;
                                    sourcePixel = outBitMap.getPixel(xSource, ySource);
                                    debug_got_pixel = 1;

                                    //brushSampleX = (int) ((float) x * ((brushSizeMult)));
                                    //brushSampleX = (int) ((float) x * ((brushSizeMult)));
                                    brushSampleX = x;
                                    brushSampleY = y;
                                    brushSample = 0;
                                    if ((brushSampleX < widthBrushImage-1) && (brushSampleX >= 0)) {
                                        if ((brushSampleY < lengthBrushImage-1) && (brushSampleY >= 0)) {

                                            try{
                                                brushSample = 255 - (rotatedBitmap.getPixel(brushSampleX, brushSampleY) & 0x000000FF);
                                            } catch (Exception e) {
                                                Log.e("0xcafebabe","problem getting brush sample");
                                                //Log.e("PreSample: bx,by=", Integer.toString(brushSampleX)+","+Integer.toString(brushSampleY));
                                                //Log.e("PostSample:brushSample=", Integer.toString(brushSample));
                                                //Log.e("0xcafebabe", Log.getStackTraceString(e.getCause().getCause()));
                                            }

                                            //outputRed = (brushRed * (255-brushSample)) >>> 8;
                                            //outputGrn = (brushGrn * (255-brushSample)) >>> 8;
                                            //outputBlu = (brushBlu * (255-brushSample)) >>> 8;

                                                switch(mEffectAlgorithm){

                                                    case 3:
                                                        if (brushSample > 250)
                                                            continue;
                                                        outputRed = Math.min( (((sourcePixel & 0x00FF0000) >>> 16) ),
                                                                (brushRed * (brushSample)) >>> 8);
                                                        outputGrn = Math.min( (((sourcePixel & 0x0000FF00) >>> 8) ),
                                                                (brushGrn * (brushSample)) >>> 8);
                                                        outputBlu = Math.min( (((sourcePixel & 0x000000FF)) ),
                                                                (brushBlu * (brushSample)) >>> 8);
                                                        break;

//                                                    case 3:
//                                                        if (brushSample > 250)
//                                                            continue;
//                                                        outputRed = Math.max( (((sourcePixel & 0x00FF0000) >>> 16) ),
//                                                                (brushRed * (brushSample)) >>> 8);
//                                                        outputGrn = Math.max( (((sourcePixel & 0x0000FF00) >>> 8) ),
//                                                                (brushGrn * (brushSample)) >>> 8);
//                                                        outputBlu = Math.max( (((sourcePixel & 0x000000FF)) ),
//                                                                (brushBlu * (brushSample)) >>> 8);
//                                                        break;

                                                    case 5: // square blockiness
                                                        outputRed = ((sourcePixel & 0x00FF0000) >>> 16);
                                                        outputGrn = ((sourcePixel & 0x0000FF00) >>> 8);
                                                        outputBlu = ((sourcePixel & 0x000000FF));
                                                        if (brushSample > 255)
                                                            continue;

                                                        outputRed = (outputRed * (255 - brushSample)) +
                                                                (brushRed * (brushSample)) >>> 8;
                                                        outputGrn = (outputGrn * (255 - brushSample)) +
                                                                (brushGrn * (brushSample)) >>> 8;
                                                        outputBlu = (outputBlu * (255 - brushSample)) +
                                                                (brushBlu * (brushSample)) >>> 8;
                                                        break;

                                                    default:
                                                        outputRed = ((sourcePixel & 0x00FF0000) >>> 16);
                                                        outputGrn = ((sourcePixel & 0x0000FF00) >>> 8);
                                                        outputBlu = ((sourcePixel & 0x000000FF));
                                                        if (brushSample > 253)
                                                            continue;

                                                        outputRed = (outputRed * (255 - brushSample)) +
                                                                    (brushRed * (brushSample)) >>> 8;
                                                        outputGrn = (outputGrn * (255 - brushSample)) +
                                                                    (brushGrn * (brushSample)) >>> 8;
                                                        outputBlu = (outputBlu * (255 - brushSample)) +
                                                                    (brushBlu * (brushSample)) >>> 8;
                                                }

                                                //outputRed = 255 - brushSample; // testing
                                                outColor = 0xFF000000 | (outputRed << 16) | (outputGrn << 8) | outputBlu;

                                                try{
                                                    outBitMap.prepareToDraw();
                                                    outBitMap.setPixel(xSource, ySource, outColor);
                                                } catch( Exception e){
                                                    Log.e("0xcafebabe","Problem setting pixel");
                                                    Log.e("0xcafebabe", Log.getStackTraceString(e.getCause().getCause()));
                                                }
                                        }
                                    }
                                }
                            }
                        }
                        } // theBrushes[n].size > 0
                    }  // n

                } catch (Exception e) {
//                    Log.e("imageBitmap:", "problem here");
//                    Log.e("imageBitmap: xSource", Integer.toString(xSource));
//                    Log.e("imageBitmap: ySource", Integer.toString(ySource));
//                    Log.e("imageBitmap: brushXStart", Integer.toString(brushXStart));
//                    Log.e("imageBitmap: brushXEnd", Integer.toString(brushXEnd));
//                    Log.e("imageBitmap: brushYStart", Integer.toString(brushYStart));
//                    Log.e("imageBitmap: brushYEnd", Integer.toString(brushYEnd));
//                    Log.e("imageBitmap: brushSampleX", Integer.toString(brushSampleX));
//                    Log.e("imageBitmap: brushSampleY", Integer.toString(brushSampleY));
//                    Log.e("imageBitmap: widthSourceImage", Integer.toString(widthSourceImage));
//                    Log.e("imageBitmap: lengthSourceImage", Integer.toString(lengthSourceImage));
//                    Log.e("imageBitmap: widthBrushImage", Integer.toString(widthBrushImage));
//                    Log.e("imageBitmap: lengthBrushImage", Integer.toString(lengthBrushImage));
//                    Log.e("imageBitmap: brushSizeMult", Float.toString(brushSizeMult));
//                    Log.e("imageBitmap: debug_got_pixel", Integer.toString(debug_got_pixel));
//                    Log.e("imageBitmap: x", Integer.toString(x));
//                    Log.e("imageBitmap: y", Integer.toString(y));
//                    Log.e("imagebitmap: brushBitMap", Boolean.toString((boolean) (brushBitMap == null)));
//                    Log.e("imagebitmap: n", Integer.toString(n));
//                    Log.e("imagebitmap: theBrush[n].size", Float.toString(theBrushes[n].size));
//                    //Log.e("0xcafebabe", Log.getStackTraceString(e.getCause().getCause()));
                    Log.e("0xcafebabe ", "Rendering Pixels", e);
                }

            }

            return outBitMap; // TODO return something useful
        }

        protected void onProgressUpdate(Integer... progress) {
            //setProgressPercent(progress[0]);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            //showDialog("Downloaded " + result + " bytes");

            imageBitmap.prepareToDraw();

            // Set it to the UI
            imageView_.setImageBitmap(result);
            imageBitmap = result;
            runningBGTask = false;

            //Log.e("OnPostExecute", "Pre createTempImageFile");
            //Log.e("Generation=", Integer.toString(imageBitmap.getGenerationId()));
            createTempImageFile();
            //Log.e("OnPostExecute", "Post createTempImageFile");

            ProgressBar progressView = (ProgressBar) findViewById(R.id.progressBar);
            progressView.setVisibility(View.INVISIBLE);
        }
    }



//    @Override
//    public Object onRetainNonConfigurationInstance() {
//        StateSaver data = new StateSaver();
//        // save important data into this object
//
//        if (splashDialog != null) {
//            data.showSplashScreen = true;
//            removeSplashScreen();
//        }
//        return data;
//    }

    private void removeSplashScreen() {
        if (splashDialog != null) {
            splashDialog.dismiss();
            splashDialog = null;
        }

        PickGalleryImage();
    }

    private void showSplashScreen() {

        splashDialog = new Dialog(this,android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        splashDialog.setContentView(R.layout.splashscreen);
        splashDialog.setCancelable(false);
        splashDialog.show();

        // Start background Handler to cancel it, to be save
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                removeSplashScreen();
            }
        }, MAX_SPLASH_SECONDS * 1000);
    }

    public static Bitmap getBitmapFromAsset(Context context, String strName) {
        AssetManager assetManager = context.getAssets();

        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(strName);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            return null;
        }

        return bitmap;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK &&  data != null)
        {
            ProgressBar progressView = (ProgressBar) findViewById(R.id.progressBar);
            progressView.setVisibility(View.VISIBLE);

            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            //ImageView imageView = (ImageView) findViewById(R.id.imageView);
            //imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

            Bitmap tempBitmap;
            tempBitmap=BitmapFactory.decodeFile(picturePath);
            imageBitmap = convertToMutable(getApplicationContext(), tempBitmap, screenHeight);
            imageView.setImageBitmap(imageBitmap);
//            imageView.setImageBitmap(tempBitmap); // testing

            sourceImageBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true);
            progressView.setVisibility(View.INVISIBLE);

        }
    }

    public static Bitmap convertToMutable(final Context context, final Bitmap imgIn, float screenHeight) {
        final int width = imgIn.getWidth(), height = imgIn.getHeight();
        final Bitmap.Config type = imgIn.getConfig();
        File outputFile = null;
        final File outputDir = context.getCacheDir();
        try {
            outputFile = File.createTempFile(Long.toString(System.currentTimeMillis()), null, outputDir);
            outputFile.deleteOnExit();
            final RandomAccessFile randomAccessFile = new RandomAccessFile(outputFile, "rw");
            final FileChannel channel = randomAccessFile.getChannel();
            final MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.getRowBytes() * height);
            imgIn.copyPixelsToBuffer(map);
            //imgIn.recycle();
            final Bitmap imgToScale = Bitmap.createBitmap(width, height, type);
            map.position(0);
            imgToScale.copyPixelsFromBuffer(map);
            channel.close();
            randomAccessFile.close();
            outputFile.delete();

//            int width2 = imgIn.getWidth();
//            int height2 = imgIn.getHeight();
//            int width3 = imgToScale.getWidth();
//            int height3 = imgToScale.getHeight();


            Bitmap result = scaleDown(imgToScale, screenHeight, false);
            //Bitmap result = imgToScale;
            imgToScale.recycle();
            return result;

        } catch (final Exception e) {
        } finally {
            if (outputFile != null)
                outputFile.delete();
        }
        return null;
    }

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize,
                                   boolean filter) {

        float realImageWidth = realImage.getWidth();
        float realImageHeight = realImage.getHeight();

        if ( (realImage.getWidth() >= maxImageSize) || (realImage.getHeight() >= maxImageSize) ) {
            float ratio = Math.min(
                    (float) maxImageSize / (float) realImageWidth,
                    (float) maxImageSize / (float) realImageHeight);
            int width = Math.round((float) ratio * (float) realImage.getWidth());
            int height = Math.round((float) ratio * (float) realImage.getHeight());
            Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                    height, filter);
            return newBitmap;
        } else {
            // no need to resize this image
            Bitmap newBitmap = Bitmap.createBitmap(realImage);
            return newBitmap;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                currentX = (int) event.getRawX();
                currentY = (int) event.getRawY();
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                int x2 = (int) event.getRawX();
                int y2 = (int) event.getRawY();
                container.scrollBy(currentX - x2 , currentY - y2);
                currentX = x2;
                currentY = y2;
                break;
            }
            case MotionEvent.ACTION_UP: {
                break;
            }
        }
        return true;
    }

    public void PickGalleryImage(){
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        i.setType("image/*");
        startActivityForResult(i, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.gallery_button:
                PickGalleryImage();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);

        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareIntent());
        }

        // Return true to display menu
        return true;
    }

    // Fire off the share intent
    private Intent createShareIntent() {
        Intent shareIntent = null;
        // save to  cache out dir
        // put uri into extra_stream (uri = path +b filename)
        Uri fileURI;

        fileURI = createTempImageFile();
        if ( fileURI != null  ) {

            shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileURI);
            //shareIntent.setType("image/png");
            shareIntent.setType("image/*");
            //startActivity(Intent.createChooser(shareIntent, "send picture using"));
        }
        return shareIntent;
    }

    // Save the current effect to a file in case user selects share
    private Uri createTempImageFile() {
        File outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) ;
        //File outputDir = Environment.getDataDirectory(); //getPackageName();

        String myPath;
        myPath = outputDir.getAbsolutePath() + File.separator + getPackageName() + File.separator;
        File newDirectory = new File(myPath);
        newDirectory.mkdirs();

        File imageBitmapFile = new File(myPath, "temp.jpg");
        FileOutputStream fileOutPutStream = null;
        Uri fileURI = Uri.parse("file://" + imageBitmapFile.getAbsolutePath());
        try {
            fileOutPutStream = new FileOutputStream(imageBitmapFile);
            //imageBitmap.compress(Bitmap.CompressFormat.PNG, 80, fileOutPutStream);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutPutStream);
            fileOutPutStream.flush();
            fileOutPutStream.close();
            return fileURI; // result is OK
        } catch (Exception e) {
            //e.printStackTrace();
            Log.e("0xcafebabe", "help", e);
        }
        return null; // failure
    }

}