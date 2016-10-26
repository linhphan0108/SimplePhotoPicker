package com.example.linh.photopicker;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final int REQUEST_PIC_FROM_LIBRARY = 8989;
    private final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 9090;

    private Button btn;
    private ImageView img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btn = (Button) findViewById(R.id.btn);
        img = (ImageView) findViewById(R.id.img);
        btn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn:
                onButtonClicked();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhotoFromGallery();

                }else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED){
                    // user also CHECKED "never ask again"
                    // you can either enable some fall back,
                    // disable features of your app
                    // or open another dialog explaining
                    // again the permission and directing to
                    // the app setting
                    new AlertDialog.Builder(this)
                            .setMessage("need your Acceptation")
                            .setNegativeButton("Nope", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setPositiveButton("App setting", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    gotoSystemAppPermissionManager();
                                    dialog.dismiss();
                                }
                            }).create().show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_PIC_FROM_LIBRARY:
                if (resultCode == RESULT_OK) {
                    img.setImageBitmap(null);
                    final Uri imageUri = data.getData();
                    final Bitmap selectedImage= getBitmap(imageUri);
                    img.setImageBitmap(selectedImage);
                }
                break;
        }
    }

    private void onButtonClicked(){
        if (Build.VERSION.SDK_INT >= 23){
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "shouldShowRequestPermissionRationale", Toast.LENGTH_SHORT).show();
                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    new AlertDialog.Builder(this)
                            .setMessage("need your Acceptation")
                            .setNegativeButton("Nope", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestStorageAccess();
                                    dialog.dismiss();
                                }
                            }).create().show();

                } else {
                    // No explanation needed, we can request the permission.
                    requestStorageAccess();
                }
            }else{
                takePhotoFromGallery();
            }
        }else{
            takePhotoFromGallery();
        }
    }

    @TargetApi(23)
    private void requestStorageAccess(){
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
    }

    private void takePhotoFromGallery(){
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
//        photoPickerIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_PIC_FROM_LIBRARY);
    }

    private void takePictureFromGalleryWithoutGooglePhotos(){
        List<Intent> intentShareList = new ArrayList<>();
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_PICK);
        shareIntent.setType("image/*");
        List<ResolveInfo> resolveInfoList = getPackageManager().queryIntentActivities(shareIntent, 0);

        for (ResolveInfo resInfo : resolveInfoList) {
            String packageName = resInfo.activityInfo.packageName;
            String name = resInfo.activityInfo.name;

            if (!packageName.contains("com.google.android.apps.photos")){
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(packageName, name));
                intent.setAction(Intent.ACTION_PICK);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                intentShareList.add(intent);
            }
        }

        if (intentShareList.isEmpty()) {
            Toast.makeText(this, "No apps available!", Toast.LENGTH_SHORT).show();
        } else {
            Intent chooserIntent = Intent.createChooser(intentShareList.remove(0), "");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentShareList.toArray(new Parcelable[]{}));
            startActivityForResult(chooserIntent, REQUEST_PIC_FROM_LIBRARY);
        }
    }

    private void gotoSystemAppPermissionManager(){
        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(i);
    }

    public Bitmap fixOrientation(Uri imagePath) {
        Bitmap bitmap = getBitmap(imagePath);
        try {
            ExifInterface exif = new ExifInterface(imagePath.toString());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();

            int rotate = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
            matrix.preRotate(rotate);
            Bitmap rotatedBMP = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            return rotatedBMP;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private Bitmap getBitmap(Uri uri) {
        InputStream in;
        try {
            final int IMAGE_MAX_SIZE = 800;

            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            in = getContentResolver().openInputStream(uri);

            BitmapFactory.decodeStream(in, null, o);
            if (in != null) {
                in.close();
            }

            int scale = 1;
            if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
                scale = (int) Math.pow(
                        2,
                        (int) Math.round(Math.log(IMAGE_MAX_SIZE
                                / (double) Math.max(o.outHeight, o.outWidth))
                                / Math.log(0.5)));
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            in = getContentResolver().openInputStream(uri);
            Bitmap b = BitmapFactory.decodeStream(in, null, o2);

            if (in != null) {
                in.close();
            }
            return b;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}