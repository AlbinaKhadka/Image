package com.example.image;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Encode extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICKER = 1;
    private static final int REQUEST_PERMISSIONS = 2;

    private ImageView imageView;
    private TextView whetherEncoded;
    private EditText messageEditText;
    private EditText secretKeyEditText;

    private Bitmap selectedImageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encode);

        imageView = findViewById(R.id.plus_sign_image);
        whetherEncoded = findViewById(R.id.whether_encoded);
        messageEditText = findViewById(R.id.message);
        secretKeyEditText = findViewById(R.id.secret_key);

        Button chooseImageButton = findViewById(R.id.choose_image_button);
        Button encodeButton = findViewById(R.id.submit);
        Button saveImageButton = findViewById(R.id.save_image_button);

        chooseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkGalleryPermissionAndOpenImagePicker();
            }
        });

        encodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                encodeImage();
            }
        });

        saveImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveEncodedImageToGallery();
            }
        });

        // Check permissions
        checkPermissions();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQUEST_PERMISSIONS);
        }
    }

    private void checkGalleryPermissionAndOpenImagePicker() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_IMAGE_PICKER);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, you can proceed
            } else {
                // Permissions denied, handle accordingly
                Toast.makeText(this, "Permission denied. Cannot access gallery.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_IMAGE_PICKER) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied to access gallery.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICKER && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                selectedImageBitmap = BitmapFactory.decodeStream(inputStream);
                imageView.setImageBitmap(selectedImageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void encodeImage() {
        String message = messageEditText.getText().toString();
        String secretKey = secretKeyEditText.getText().toString();

        if (!message.isEmpty() && !secretKey.isEmpty()) {
            if (selectedImageBitmap != null) {
                Bitmap encodedImage = encodeLSB(selectedImageBitmap, message, secretKey);
                imageView.setImageBitmap(encodedImage);
                whetherEncoded.setText("Message encoded");
            } else {
                Toast.makeText(this, "Please choose an image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please enter message and secret key", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap encodeLSB(Bitmap originalImage, String message, String secretKey) {
        Bitmap encodedImage = originalImage.copy(Bitmap.Config.ARGB_8888, true);

        int keyHash = secretKey.hashCode();
        int messageIndex = 0;

        for (int y = 0; y < encodedImage.getHeight(); y++) {
            for (int x = 0; x < encodedImage.getWidth(); x++) {
                int pixel = encodedImage.getPixel(x, y);

                int alpha = Color.alpha(pixel);
                int red = Color.red(pixel);

                if (messageIndex < message.length()) {
                    char charToEncode = message.charAt(messageIndex);
                    int bitToEncode = (charToEncode >> keyHash) & 1;

                    red = modifyLSB(red, bitToEncode);
                    messageIndex++;
                }

                encodedImage.setPixel(x, y, Color.argb(alpha, red, Color.green(pixel), Color.blue(pixel)));
            }
        }

        return encodedImage;
    }

    private int modifyLSB(int value, int bitToEncode) {
        if (bitToEncode == 1) {
            if (value % 2 == 0) {
                value++;
            }
        } else {
            if (value % 2 == 1) {
                value--;
            }
        }
        return value;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void saveEncodedImageToGallery() {
        if (selectedImageBitmap != null) {
            String fileName = "encoded_image.png";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            try {
                OutputStream outputStream = getContentResolver().openOutputStream(imageUri);
                if (outputStream != null) {
                    selectedImageBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.close();
                    Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please choose an image to save", Toast.LENGTH_SHORT).show();
        }
    }
}
