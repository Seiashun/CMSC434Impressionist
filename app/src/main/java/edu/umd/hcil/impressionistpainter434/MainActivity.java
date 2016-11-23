package edu.umd.hcil.impressionistpainter434;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements OnMenuItemClickListener {

    private static int RESULT_LOAD_IMAGE = 1;
    private static int REQUEST_TAKE_PHOTO = 2;
    private  ImpressionistView _impressionistView;

    private String[] PERMISSION_CAMERA = { Manifest.permission.CAMERA };
    // Removed String[] IMAGE_URLS after images have been downloade

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _impressionistView = (ImpressionistView)findViewById(R.id.viewImpressionist);
        ImageView imageView = (ImageView)findViewById(R.id.viewImage);
        _impressionistView.setImageView(imageView);

    }

    public void onButtonClickClear(View v) {
        new AlertDialog.Builder(this)
                .setTitle("Clear Painting?")
                .setMessage("Do you really want to clear your painting?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Toast.makeText(MainActivity.this, "Painting cleared", Toast.LENGTH_SHORT).show();
                        _impressionistView.clearPainting();
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    public void onButtonClickSetBrush(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.popup_menu);
        popupMenu.show();
    }

    public void onButtonClickMirror(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.mirror_menu);
        popupMenu.show();
    }

    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuCircle:
                Toast.makeText(this, "Circle Brush", Toast.LENGTH_SHORT).show();
                _impressionistView.setBrushType(BrushType.Circle);
                return true;
            case R.id.menuSquare:
                Toast.makeText(this, "Square Brush", Toast.LENGTH_SHORT).show();
                _impressionistView.setBrushType(BrushType.Square);
                return true;
            case R.id.menuLine:
                Toast.makeText(this, "Line Brush", Toast.LENGTH_SHORT).show();
                _impressionistView.setBrushType(BrushType.Line);
                return true;
            case R.id.menuCircleSplatter:
                Toast.makeText(this, "Circle Splatter Brush", Toast.LENGTH_SHORT).show();
                _impressionistView.setBrushType(BrushType.CircleSplatter);
                return true;
            case R.id.menuLineSplatter:
                Toast.makeText(this, "Line Splatter Brush", Toast.LENGTH_SHORT).show();
                _impressionistView.setBrushType(BrushType.LineSplatter);
                return true;
            case R.id.menuNone:
                Toast.makeText(this, "No Mirroring", Toast.LENGTH_SHORT).show();
                _impressionistView.setMirrorType(MirrorType.None);
                return true;
            case R.id.menuXAxis:
                Toast.makeText(this, "Mirror Across X Axis", Toast.LENGTH_SHORT).show();
                _impressionistView.setMirrorType(MirrorType.XAxis);
                return true;
            case R.id.menuYAxis:
                Toast.makeText(this, "Mirror Across Y Axis", Toast.LENGTH_SHORT).show();
                _impressionistView.setMirrorType(MirrorType.YAxis);
                return true;
            case R.id.menuDiagonal:
                Toast.makeText(this, "Mirror Diagonally", Toast.LENGTH_SHORT).show();
                _impressionistView.setMirrorType(MirrorType.Diagonal);
                return true;
            case R.id.menuAll:
                Toast.makeText(this, "Mirror All Quadrants", Toast.LENGTH_SHORT).show();
                _impressionistView.setMirrorType(MirrorType.All);
                return true;
        }
        return false;
    }


    // Guides used to take pictures
    // https://developer.android.com/training/camera/photobasics.html
    // http://stackoverflow.com/questions/6448856/android-camera-intent-how-to-get-full-sized-photo
    // http://stackoverflow.com/questions/7286714/android-get-orientation-of-a-camera-bitmap-and-rotate-back-90-degrees
    private Uri _imageUri;
    private File createImageFile() throws IOException {
        String externalStorageDirStr = Environment.getExternalStorageDirectory().getAbsolutePath();
        boolean checkStorage = FileUtils.checkPermissionToWriteToExternalStorage(MainActivity.this);

        // Get current time and date
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyMMdd_HHmmss");

        String filename = "Impressionist-Original_" + df.format(c.getTime());

        File image = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), filename + ".png");

        if (!image.exists()) {
            image.createNewFile();
        }

        return image;
    }

    /**
     * Take a photo to use as the primary image for the Impressionist art.
     *
     * @param v
     */
    public void onButtonClickTakePhoto(View v) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSION_CAMERA, REQUEST_TAKE_PHOTO);
        }

        FileUtils.verifyStoragePermissions(this);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;

            try {
                photoFile = createImageFile();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (photoFile != null) {
                _imageUri = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, _imageUri);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    /**
     * Take the ImageView from the Camera Intent, save the image to the gallery, and use
     * the full-sized image for the impression.
     *
     * @param imageView
     */
    public void grabImage(ImageView imageView) {
        this.getContentResolver().notifyChange(_imageUri, null);
        ContentResolver cr = this.getContentResolver();
        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(cr, _imageUri);

            ExifInterface exif = new ExifInterface(_imageUri.getPath());
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationInDegrees = exifToDegrees(rotation);

            Matrix matrix = new Matrix();
            if (rotation != 0f) {
                matrix.preRotate(rotationInDegrees);
            }

            Bitmap adjustedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            imageView.setImageBitmap(adjustedBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Code used from:
    // http://stackoverflow.com/questions/7286714/android-get-orientation-of-a-camera-bitmap-and-rotate-back-90-degrees

    /**
     * Determine how many degrees to rotate the image based on its orientation to make the image
     * oriented the correct way.
     *
     * @param exifOrientation
     * @return
     */
    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }

    // Download button and code has been changed for "Take Photo" button and code.

/**
 * Loads an image from the Gallery into the ImageView
 *
 * @param v
    /**
     * Loads an image from the Gallery into the ImageView
     *
     * @param v
     */
    public void onButtonClickLoadImage(View v){

        // Without this call, the app was crashing in the onActivityResult method when trying to read from file system
        FileUtils.verifyStoragePermissions(this);

        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }


    /**
     * Saves the drawn image to the Gallery
     *
     * @param v
     */
    public void onButtonClickSaveImage(View v) {
        FileUtils.verifyStoragePermissions(this);

        Bitmap bitmap = _impressionistView.getBitmap();
        String externalStorageDirStr = Environment.getExternalStorageDirectory().getAbsolutePath();
        boolean checkStorage = FileUtils.checkPermissionToWriteToExternalStorage(MainActivity.this);

        // Get current time and date
        // http://stackoverflow.com/questions/2271131/display-the-current-time-and-date-in-an-android-application
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyMMdd_HHmmss");

        String filename = "Impressionist-Impression_" + df.format(c.getTime()) + ".png";

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), filename);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(file));
            FileUtils.addImageToGallery(file.getAbsolutePath(), getApplication());
            Toast.makeText(getApplicationContext(), "Saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called automatically when an image has been selected in the Gallery
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri imageUri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                ImageView imageView = (ImageView) findViewById(R.id.viewImage);

                // destroy the drawing cache to ensure that when a new image is loaded, its cached
                imageView.destroyDrawingCache();
                imageView.setImageBitmap(bitmap);
                imageView.setDrawingCacheEnabled(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            ImageView imageView = (ImageView) findViewById(R.id.viewImage);

            imageView.destroyDrawingCache();
            this.grabImage(imageView);
            imageView.setDrawingCacheEnabled(true);
        }
    }
}
