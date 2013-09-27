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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ShareActionProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
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
    private static final int MAX_SPLASH_SECONDS = 5;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StateSaver data = (StateSaver) getLastNonConfigurationInstance();
        if (data != null) { // "all this has happened before"
            if (data.showSplashScreen ) { // and we didn't already finish
                showSplashScreen();
            }
        } else {
            showSplashScreen();
            setContentView(R.layout.activity_main);
        }

        // Fill in the imagebitmap with a placeholder (a brush)
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.brush1);

        runningBGTask = false;
        GridView gridview = (GridView) findViewById(R.id.gridView);
        gridview.setAdapter(new ImageAdapter(this));
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                //Toast.makeText(MainActivity.this, "" + position, Toast.LENGTH_SHORT).show();
                // Start any heavy-duty loading here, but on its own thread
                if (runningBGTask == false) {
                    mEffectAlgorithm = position;
                    try {
                        // Load brush bitmap
                        //brushBitMap = getBitmapFromAsset(getApplicationContext(), "brush1");

                        switch( position ){
                            //brushBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.brush1);
                            case 0:
                                brushBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.brush5);
                                break;
                            case 1:
                                brushBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.brush2);
                                break;
                            case 2:
                                brushBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.brush3);
                                break;
                            case 3:
                                brushBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.brush8);
                                break;
                            case 4:
                                brushBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.brush9);
                                break;
                            default:
                                brushBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.brush4);
                        }

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

                        Log.e("OnItemClickGridView: widthSourceImage", Integer.toString(widthSourceImage));
                        Log.e("OnItemClickGridView: lengthSourceImage", Integer.toString(lengthSourceImage));
                        Log.e("OnItemClickGridView: widthBrushImage", Integer.toString(widthBrushImage));
                        Log.e("OnItemClickGridView: lengthBrushImage", Integer.toString(lengthBrushImage));

                        // TESTING CHANGE TODO
                        //widthNumBrushes = 5;
                        //lengthNumBrushes = 5;

                        // Brush sampled colors & brush locations
                        theBrushes = new brush[(widthNumBrushes + 1) * (lengthNumBrushes + 1)];
                        theHSVColor = new float[3];

                        new PaintImageTask().execute();

                    } catch (Exception e) {
                        Log.e("OnItemClickGridView:", "problem here");
                    }
                }
            }
        });


        container = (HorizontalScrollView) findViewById(R.id.horizontalScrollView);
        //container.scrollTo(220, 400);

        }

    private class PaintImageTask extends AsyncTask<URL, Integer, Long> {

        protected Long doInBackground(URL... urls) {
            if ( runningBGTask == false ) {
                runningBGTask = true;
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
                                    theBrushes[i].x = (int) (Math.random() * widthSourceImage) - 1;
                                    theBrushes[i].y = (int) (Math.random() * lengthSourceImage) - 1;
                                    // Sample the source image to get color of the brush
                                    theBrushes[i].color = imageBitmap.getPixel(theBrushes[i].x, theBrushes[i].y);
                                    // Size = brightness (value)
                                    Color.colorToHSV(theBrushes[i].color, theHSVColor);
                                    theBrushes[i].size = 1 - theHSVColor[2];
                                    // Size = constant
                                    theBrushes[i].size = (float) .8;
                                    // Angle = color
                                    theBrushes[i].angle = theHSVColor[2];
                                    break;

                                case 1:
                                    // On a Grid
                                    //theBrushes[i].x = (x*widthBrushImage)  + (widthBrushImage /2);
                                    //theBrushes[i].y = (y*lengthBrushImage) + (lengthBrushImage/2);
                                    // Randomly distributed
                                    theBrushes[i].x = (int) (Math.random() * widthSourceImage) - 1;
                                    theBrushes[i].y = (int) (Math.random() * lengthSourceImage) - 1;
                                    // Sample the source image to get color of the brush
                                    theBrushes[i].color = imageBitmap.getPixel(theBrushes[i].x, theBrushes[i].y);
                                    // Size = random
                                    theBrushes[i].size = (float) Math.random() / 2;
                                    // Size = brightness (value)
                                    Color.colorToHSV(theBrushes[i].color, theHSVColor);
                                    theBrushes[i].size = 1 - theHSVColor[2];
                                    //Angle = distance from top
                                    theBrushes[i].angle = (theBrushes[i].y / (float) widthSourceImage) * 180;
                                    break;

                                case 2:
                                    // On a Grid
                                    //theBrushes[i].x = (x*widthBrushImage)  + (widthBrushImage /2);
                                    //theBrushes[i].y = (y*lengthBrushImage) + (lengthBrushImage/2);
                                    // Randomly distributed
                                    theBrushes[i].x = (int) (Math.random() * widthSourceImage) - 1;
                                    theBrushes[i].y = (int) (Math.random() * lengthSourceImage) - 1;
                                    // Sample the source image to get color of the brush
                                    theBrushes[i].color = imageBitmap.getPixel(theBrushes[i].x, theBrushes[i].y);
                                    // Size = random
                                    //theBrushes[i].size = (float) Math.random() / 2;
                                    // Size = hue (value)
                                    Color.colorToHSV(theBrushes[i].color, theHSVColor);
                                    theBrushes[i].size = (float) (theHSVColor[0] / 360.0);

                                    // Change the brush color by a small random amount
                                    theHSVColor[0] += (Math.random() * 30);
                                    theBrushes[i].color = Color.HSVToColor(theHSVColor);

                                    //Angle = distance from top
                                    theBrushes[i].angle = (float) Math.sqrt( Math.pow(theBrushes[i].x - (widthSourceImage/2.0), 2.0)  + Math.pow((theBrushes[i].y - (lengthSourceImage/2.0)), 2.0) );
                                    break;

                                case 3:
                                    // On a Grid
                                    theBrushes[i].x = (x*widthBrushImage)  + (widthBrushImage /2);
                                    theBrushes[i].y = (y*lengthBrushImage) + (lengthBrushImage/2);
                                    // Randomly distributed
                                    //theBrushes[i].x = (int) (Math.random() * widthSourceImage) - 1;
                                    //theBrushes[i].y = (int) (Math.random() * lengthSourceImage) - 1;
                                    // Sample the source image to get color of the brush
                                    theBrushes[i].color = imageBitmap.getPixel(theBrushes[i].x, theBrushes[i].y);
                                    // Size = random
                                    //theBrushes[i].size = (float) Math.random() / 2;
                                    // Size = brightness (value)
                                    Color.colorToHSV(theBrushes[i].color, theHSVColor);
                                    theBrushes[i].size = 1 - theHSVColor[0];

                                    // Change the brush color by a small random amount
                                    theHSVColor[0] += (Math.random() * 30);
                                    theBrushes[i].color = Color.HSVToColor(theHSVColor);

                                    //Angle = distance from top
                                    theBrushes[i].angle = (float) Math.sqrt( Math.pow(theBrushes[i].x - (widthSourceImage/2.0), 2.0)  + Math.pow((theBrushes[i].y - (lengthSourceImage/2.0)), 2.0) );

                                    break;

                                case 4:
                                    // On a Grid
                                    //theBrushes[i].x = (x*widthBrushImage)  + (widthBrushImage /2);
                                    //theBrushes[i].y = (y*lengthBrushImage) + (lengthBrushImage/2);
                                    // Randomly distributed
                                    theBrushes[i].x = (int) (Math.random() * widthSourceImage) - 1;
                                    theBrushes[i].y = (int) (Math.random() * lengthSourceImage) - 1;
                                    // Sample the source image to get color of the brush
                                    theBrushes[i].color = imageBitmap.getPixel(theBrushes[i].x, theBrushes[i].y);
                                    // Size = random
                                    //theBrushes[i].size = (float) Math.random() / 2;
                                    // Size = brightness (value)
                                    Color.colorToHSV(theBrushes[i].color, theHSVColor);
                                    theBrushes[i].size = 1 - theHSVColor[0];

                                    // Change the brush value by a small random amount
                                    theHSVColor[2] += Math.min(Math.random() * 30, 1);
                                    theBrushes[i].color = Color.HSVToColor(theHSVColor);

                                    //Angle = color
                                    theBrushes[i].angle = theHSVColor[0];
                                    break;

                                default:
                                    break;
                            }

                            i++;
                        }
                    }

                    totalNumBrushes = i;


                    for ( int n = 0; n <= totalNumBrushes; n++) {

                        // Rotate the brush
                        matrix.reset();
                        matrix.postRotate(theBrushes[n].angle, brushBitMap.getWidth() / 2, brushBitMap.getHeight() / 2);
                        matrix.postScale(theBrushes[n].size, theBrushes[n].size);
                        rotatedBitmap = Bitmap.createBitmap(brushBitMap, 0, 0, brushBitMap.getWidth(), brushBitMap.getHeight(),
                                matrix, true);
                        widthBrushImage = rotatedBitmap.getWidth();
                        lengthBrushImage = rotatedBitmap.getHeight();

                        brushXStart = theBrushes[n].x - (int) ((float) widthBrushImage / 2.0) + 1;
                        brushXEnd = theBrushes[n].x + (int) ((float) widthBrushImage / 2.0) - 1;
                        brushYStart = theBrushes[n].y - (int) ((float) lengthBrushImage / 2.0) - 1;
                        brushYEnd = theBrushes[n].y + (int) ((float) lengthBrushImage / 2.0) + 1;

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
                                    sourcePixel = imageBitmap.getPixel(xSource, ySource);
                                    debug_got_pixel = 1;

                                    //brushSampleX = (int) ((float) x * ((brushSizeMult)));
                                    //brushSampleX = (int) ((float) x * ((brushSizeMult)));
                                    brushSampleX = x;
                                    brushSampleY = y;
                                    brushSample = 0;
                                    if ((brushSampleX < widthBrushImage-1) && (brushSampleX >= 0)) {
                                        if ((brushSampleY < lengthBrushImage-1) && (brushSampleY >= 0)) {


                                            debug_got_pixel = 2;
                                            //Log.e("PreSample: bx,by=", Integer.toString(brushSampleX)+","+Integer.toString(brushSampleY));
                                            brushSample = 255 - rotatedBitmap.getPixel(brushSampleX, brushSampleY) & 0x000000FF;
                                            //Log.e("PostSample:brushSample=", Integer.toString(brushSample));
                                            debug_got_pixel = 3;

                                            //outputRed = (brushRed * (255-brushSample)) >>> 8;
                                            //outputGrn = (brushGrn * (255-brushSample)) >>> 8;
                                            //outputBlu = (brushBlu * (255-brushSample)) >>> 8;

                                            if (brushSample < 255) {
                                                outputRed = ((((sourcePixel & 0x00FF0000) >>> 16) * (255 - brushSample)) +
                                                        (brushRed * (brushSample))) >>> 8;
                                                outputGrn = ((((sourcePixel & 0x0000FF00) >>> 8) * (255 - brushSample)) +
                                                        (brushGrn * (brushSample))) >>> 8;
                                                outputBlu = ((((sourcePixel & 0x000000FF)) * (255 - brushSample)) +
                                                        (brushBlu * (brushSample))) >>> 8;

                                                //outputRed = 255 - brushSample; // testing
                                                outColor = 0xFF000000 | (outputRed << 16) | (outputGrn << 8) | outputBlu;

                                                debug_got_pixel = 4;
                                                imageBitmap.setPixel(xSource, ySource, outColor);
                                                debug_got_pixel = 5;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }  // n

                } catch (Exception e) {
                    Log.e("imageBitmap:", "problem here");
                    Log.e("imageBitmap: xSource", Integer.toString(xSource));
                    Log.e("imageBitmap: ySource", Integer.toString(ySource));
                    Log.e("imageBitmap: brushXStart", Integer.toString(brushXStart));
                    Log.e("imageBitmap: brushXEnd", Integer.toString(brushXEnd));
                    Log.e("imageBitmap: brushYStart", Integer.toString(brushYStart));
                    Log.e("imageBitmap: brushYEnd", Integer.toString(brushYEnd));
                    Log.e("imageBitmap: brushSampleX", Integer.toString(brushSampleX));
                    Log.e("imageBitmap: brushSampleY", Integer.toString(brushSampleY));
                    Log.e("imageBitmap: widthSourceImage", Integer.toString(widthSourceImage));
                    Log.e("imageBitmap: lengthSourceImage", Integer.toString(lengthSourceImage));
                    Log.e("imageBitmap: widthBrushImage", Integer.toString(widthBrushImage));
                    Log.e("imageBitmap: lengthBrushImage", Integer.toString(lengthBrushImage));
                    Log.e("imageBitmap: brushSizeMult", Float.toString(brushSizeMult));
                    Log.e("imageBitmap: debug_got_pixel", Integer.toString(debug_got_pixel));
                    Log.e("imageBitmap: x", Integer.toString(x));
                    Log.e("imageBitmap: y", Integer.toString(y));
                }
            }

            return null; // TODO return something useful
        }

        protected void onProgressUpdate(Integer... progress) {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Long result) {
            //showDialog("Downloaded " + result + " bytes");
            createTempImageFile();

            // Set it to the UI
            ImageView imageView = (ImageView) findViewById(R.id.imageView);
            imageView.setImageBitmap(imageBitmap);
            runningBGTask = false;
        }
    }



    @Override
    public Object onRetainNonConfigurationInstance() {
        StateSaver data = new StateSaver();
        // save important data into this object

        if (splashDialog != null) {
            data.showSplashScreen = true;
            removeSplashScreen();
            PickGalleryImage();
        }
        return data;
    }

    private void removeSplashScreen() {
        if (splashDialog != null) {
            splashDialog.dismiss();
            splashDialog = null;
            PickGalleryImage();
        }
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
        if(requestCode==CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK && null != data)
        {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ImageView imageView = (ImageView) findViewById(R.id.imageView);
            //imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

            Bitmap tempBitmap;
            tempBitmap=BitmapFactory.decodeFile(picturePath);
            imageBitmap = convertToMutable(getApplicationContext(), tempBitmap);
            imageView.setImageBitmap(imageBitmap);
        }
    }

    public static Bitmap convertToMutable(final Context context, final Bitmap imgIn) {
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
            imgIn.recycle();
            final Bitmap result = Bitmap.createBitmap(width, height, type);
            map.position(0);
            result.copyPixelsFromBuffer(map);
            channel.close();
            randomAccessFile.close();
            outputFile.delete();
            return result;
        } catch (final Exception e) {
        } finally {
            if (outputFile != null)
                outputFile.delete();
        }
        return null;
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
        // Inflate menu resource file.
        //getMenuInflater().inflate(R.menu.share_menu, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);

        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareIntent());
        }

//        // Return true to display menu
        return true;
    }

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
            e.printStackTrace();
        }
        return null; // failure
    }

}