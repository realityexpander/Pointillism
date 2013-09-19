package com.realityexpander.pointillism;

import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends Activity {
    private ShareActionProvider mShareActionProvider;


String cool= "cool";

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;

    public Bitmap imageBitmap;

    private LinearLayout container;
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
                Toast.makeText(MainActivity.this, "" + position, Toast.LENGTH_SHORT).show();
            }
        });

        TextView textView2 = (TextView) findViewById(R.id.textView2);
        textView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    // Load brush bitmap
                    Bitmap brushBitMap;
                    int sourceSample;
                    int brushSample;
                    int sourceSampleRed, sourceSampleGrn, sourceSampleBlu;
                    int outputRed, outputGrn, outputBlu;
                    //brushBitMap = getBitmapFromAsset(getApplicationContext(), "brush1");
                    brushBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.brush1);

                    for (int x = 0; x < 100; x+= 50) {
                        for (int y = 0; y < 100; y+= 50) {

                            // Sample the source image
                            sourceSample = imageBitmap.getPixel(x+25, y+25);
                            sourceSampleRed = (sourceSample & 0x00FF0000 ) >>> 16;
                            sourceSampleGrn = (sourceSample & 0x0000FF00 ) >>> 8;
                            sourceSampleBlu = (sourceSample & 0x000000FF );

                            // Render the brush
                            for(int xb=0; xb<50; xb++) {
                                for(int yb=0; yb<50; yb++){
                                    int sourcePixel;
                                    sourcePixel = imageBitmap.getPixel(x+xb, y+yb);
                                    brushSample = brushBitMap.getPixel(xb, yb) & 0x000000FF;

                                    outputRed = (sourceSampleRed * (255-brushSample)) >>> 8;
                                    outputGrn = (sourceSampleGrn * (255-brushSample)) >>> 8;
                                    outputBlu = (sourceSampleBlu * (255-brushSample)) >>> 8;

                                    outputRed = ( (((sourcePixel & 0x00FF0000 ) >>> 16) * (brushSample) ) +
                                                     (outputRed * (255-brushSample)) ) >>> 8;
                                    outputGrn = ( (((sourcePixel & 0x0000FF00 ) >>> 8) * (brushSample) ) +
                                                    (outputGrn * (255-brushSample)) ) >>> 8;
                                    outputBlu = ( (((sourcePixel & 0x000000FF ) ) * (brushSample) ) +
                                                    (outputBlu * (255-brushSample)) ) >>> 8;

                                    imageBitmap.setPixel(x+xb, y+yb, Color.argb(255, outputRed, outputGrn, outputBlu));
                                }
                            }
                        }
                    }

                    // Set it to the UI
                    ImageView imageView = (ImageView) findViewById(R.id.imageViewEffect);
                    imageView.setImageBitmap(imageBitmap);
                } catch (Exception e) {
                    Log.e("imageBitmap:", "problem here");
                }
            }

        });



        container = (LinearLayout) findViewById(R.id.Container);
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
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

            Bitmap tempBitmap;
            tempBitmap=BitmapFactory.decodeFile(picturePath);
            imageBitmap = convertToMutable(getApplicationContext(), tempBitmap);
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
        getMenuInflater().inflate(R.menu.share_menu, menu);


        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);


        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();

        // Return true to display menu

        return true;
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.");
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
        shareIntent.getStringExtra(cool);
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }
}