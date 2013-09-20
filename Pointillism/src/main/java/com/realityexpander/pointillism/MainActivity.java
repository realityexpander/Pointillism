package com.realityexpander.pointillism;

import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.provider.MediaStore;
import android.support.v7.internal.view.menu.MenuView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PickGalleryImage();

        GridView gridview = (GridView) findViewById(R.id.gridView);
        gridview.setAdapter(new ImageAdapter(this));
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                //Toast.makeText(MainActivity.this, "" + position, Toast.LENGTH_SHORT).show();

                int xSource = 0, ySource = 0;
                try {
                    // Load brush bitmap
                    Bitmap brushBitMap, rotatedBitmap;
                    int sourceSample[][];
                    int brushSample;
                    int brushRed, brushGrn, brushBlu;
                    int outputRed, outputGrn, outputBlu;
                    int widthBrushImage, lengthBrushImage;

                    //brushBitMap = getBitmapFromAsset(getApplicationContext(), "brush1");
                    //brushBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.brush1);
                    brushBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.brush2);

                    // Apply a rotation
                    Matrix matrix = new Matrix();
                    //imageView.setScaleType(ImageView.ScaleType.MATRIX);   //required
                    //matrix.postRotate((float) angle, pivX, pivY);
                    //matrix.postRotate( 180f, imageView.getDrawable().getBounds().width()/2, imageView.getDrawable().getBounds().height()/2);
                    //imageView.setImageMatrix(matrix);


                    int widthSourceImage = imageBitmap.getWidth();
                    int lengthSourceImage = imageBitmap.getHeight();

                    // Create the array of brushes
                    widthBrushImage = brushBitMap.getWidth();
                    lengthBrushImage = brushBitMap.getHeight();
                    int widthNumBrushes = widthSourceImage / widthBrushImage;
                    int lengthNumBrushes = lengthSourceImage / lengthBrushImage;

                    Log.e("imageBitmap: widthSourceImage", Integer.toString(widthSourceImage));
                    Log.e("imageBitmap: lengthSourceImage", Integer.toString(lengthSourceImage));
                    Log.e("imageBitmap: widthBrushImage", Integer.toString(widthBrushImage));
                    Log.e("imageBitmap: lengthBrushImage", Integer.toString(lengthBrushImage));

                    // TESTING CHANGE TODO
                    //widthNumBrushes = 5;
                    //lengthNumBrushes = 5;

                    class brush {
                        int x;
                        int y;
                        int color;
                        float size;  // 0->1
                        float angle; // 0->1
                        int opacity;
                    }

                    // Brush sampled colors & brush locations
                    brush[][] theBrushes = new brush[widthNumBrushes + 1][lengthNumBrushes + 1];
                    float[] theHSVColor = new float[3];

                    // Sample the image at the brush center points into the brushes array
                    for (int x = 0; x <= widthNumBrushes; x++) {
                        for (int y = 0; y <= lengthNumBrushes; y++) {

                            // Set the location of the brush
                            theBrushes[x][y] = new brush();

                            // On a Grid
                            //theBrushes[x][y].x = (x*widthBrushImage)  + (widthBrushImage /2);
                            //theBrushes[x][y].y = (y*lengthBrushImage) + (lengthBrushImage/2);

                            // Randomly distributed
                            theBrushes[x][y].x = (int) (Math.random() * widthSourceImage) - 1;
                            theBrushes[x][y].y = (int) (Math.random() * lengthSourceImage) - 1;

                            // Sample the source image to get color of the brush
                            theBrushes[x][y].color = imageBitmap.getPixel(theBrushes[x][y].x, theBrushes[x][y].y);

                            // Size = random
                            //theBrushes[x][y].size = (float) Math.random();

                            // Size = brightness (value)
                            Color.colorToHSV(theBrushes[x][y].color, theHSVColor);
                            theBrushes[x][y].size = 1 - theHSVColor[2];

                            // Size = constant
                            theBrushes[x][y].size = (float) .2;

                            // Angle = color
                            theBrushes[x][y].angle = theHSVColor[2];

                            //Angle = distance from top
                            //theBrushes[x][y].angle = (theBrushes[x][y].y / (float) widthSourceImage) * 180;

                        }
                    }


                    int brushXStart, brushXEnd, brushYStart, brushYEnd;
                    float brushSizeMult;
                    float brushAngle;
                    for (int i = 0; i <= widthNumBrushes; i++) {
                        for (int j = 0; j <= lengthNumBrushes; j++) {

                            // Rotate the brush
                            matrix.reset();
                            matrix.postRotate(theBrushes[i][j].angle, brushBitMap.getWidth() / 2, brushBitMap.getHeight() / 2);
                            rotatedBitmap = Bitmap.createBitmap(brushBitMap, 0, 0, brushBitMap.getWidth(), brushBitMap.getHeight(),
                                    matrix, true);
                            widthBrushImage = rotatedBitmap.getWidth();
                            lengthBrushImage = rotatedBitmap.getHeight();

                            // for non-rotated
                            brushXStart = theBrushes[i][j].x - (int) ((float) widthBrushImage / 2.0);
                            brushXEnd = theBrushes[i][j].x + (int) ((float) widthBrushImage / 2.0);
                            brushYStart = theBrushes[i][j].y - (int) ((float) lengthBrushImage / 2.0);
                            brushYEnd = theBrushes[i][j].y + (int) ((float) lengthBrushImage / 2.0);
//                            brushXStart = theBrushes[i][j].x - (int)((float)widthBrushImage);
//                            brushXEnd   = theBrushes[i][j].x + (int)((float)widthBrushImage);
//                            brushYStart = theBrushes[i][j].y - (int)((float)lengthBrushImage);
//                            brushYEnd   = theBrushes[i][j].y + (int)((float)lengthBrushImage);
                            brushRed = (theBrushes[i][j].color & 0x00FF0000) >>> 16;
                            brushGrn = (theBrushes[i][j].color & 0x0000FF00) >>> 8;
                            brushBlu = (theBrushes[i][j].color & 0x000000FF);

                            // Render the brushes
                            int x, y;

                            int brushSampleX, brushSampleY;
                            int sourcePixel, outColor;
                            brushSizeMult = ((float) 1 / theBrushes[i][j].size);

                            for (xSource = brushXStart, x = 0; xSource <= brushXEnd; xSource++, x++) {
                                for (ySource = brushYStart, y = 0; ySource <= brushYEnd; ySource++, y++) {
                                    if (xSource < widthSourceImage) {
                                        if (ySource < lengthSourceImage && xSource >= 0 && ySource >= 0) {

                                            sourcePixel = imageBitmap.getPixel(xSource, ySource);

                                            brushSampleX = (int) ((float) x * ((brushSizeMult) / 2.0));
                                            brushSampleY = (int) ((float) y * ((brushSizeMult) / 2.0));
                                            brushSample = 255;
                                            if (brushSampleX < widthBrushImage && brushSampleX >= 0) {
                                                if (brushSampleY < lengthBrushImage && brushSampleY >= 0) {

                                                    brushSample = 255 - rotatedBitmap.getPixel(brushSampleX, brushSampleY) & 0x000000FF;

                                                    //outputRed = (brushRed * (255-brushSample)) >>> 8;
                                                    //outputGrn = (brushGrn * (255-brushSample)) >>> 8;
                                                    //outputBlu = (brushBlu * (255-brushSample)) >>> 8;

                                                    if (brushSample < 254) {
                                                        outputRed = ((((sourcePixel & 0x00FF0000) >>> 16) * (brushSample)) +
                                                                (brushRed * (255 - brushSample))) >>> 8;
                                                        outputGrn = ((((sourcePixel & 0x0000FF00) >>> 8) * (brushSample)) +
                                                                (brushGrn * (255 - brushSample))) >>> 8;
                                                        outputBlu = ((((sourcePixel & 0x000000FF)) * (brushSample)) +
                                                                (brushBlu * (255 - brushSample))) >>> 8;


                                                        //outputRed = 255 - brushSample; // testing
                                                        outColor = 0xFF000000 | (outputRed << 16) | (outputGrn << 8) | outputBlu;

                                                        imageBitmap.setPixel(xSource, ySource, outColor);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Set it to the UI
                    ImageView imageView = (ImageView) findViewById(R.id.imageViewEffect);
                    imageView.setImageBitmap(imageBitmap);
                } catch (Exception e) {
                    Log.e("imageBitmap:", "problem here");
                    Log.e("imageBitmap: xSource", Integer.toString(xSource));
                    Log.e("imageBitmap: ySource", Integer.toString(ySource));
                }
            }

        });



        container = (HorizontalScrollView) findViewById(R.id.Container);
        //container.scrollTo(220, 400);

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

            ImageView imageView = (ImageView) findViewById(R.id.imageViewEffect);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        // Inflate menu resource file.
        //getMenuInflater().inflate(R.menu.share_menu, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);
        // Fetch and store ShareActionProvider
        mShareIntent = new Intent();
        mShareIntent.setAction(Intent.ACTION_SEND);
        mShareIntent.setType("text/plain");
        mShareIntent.putExtra(Intent.EXTRA_TEXT, "From me to you, this text is new.");

        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(mShareIntent);
        }

        // Return true to display menu
        return true;
    }

}