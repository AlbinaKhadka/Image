package com.example.image;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;

public class Decode extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICKER = 1;

    private ImageView encodedImageView;
    private EditText secretKeyEditText;
    private TextView decodedMessageTextView;

    private Bitmap selectedImageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode);

        encodedImageView = findViewById(R.id.encoded_image);
        secretKeyEditText = findViewById(R.id.secret_key);
        decodedMessageTextView = findViewById(R.id.decoded_message);

        Button chooseImageButton = findViewById(R.id.save_decoded_image_button);
        Button decodeButton = findViewById(R.id.decode_button);

        chooseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImagePicker();
            }
        });

        decodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                decodeImage();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
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
                encodedImageView.setImageBitmap(selectedImageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void decodeImage() {
        String secretKey = secretKeyEditText.getText().toString();

        if (selectedImageBitmap != null && !secretKey.isEmpty()) {
            String decodedMessage = decodeLSB(selectedImageBitmap, secretKey);
            if (decodedMessage != null) {
                decodedMessageTextView.setText(decodedMessage);
            } else {
                decodedMessageTextView.setText("");
                Toast.makeText(this, "Cannot decode the message with the provided secret key", Toast.LENGTH_SHORT).show();
            }
        } else {
            decodedMessageTextView.setText("");
            Toast.makeText(this, "Please choose an image and enter the secret key", Toast.LENGTH_SHORT).show();
        }
    }

    private String decodeLSB(Bitmap encodedBitmap, String secretKey) {
        StringBuilder binaryMessage = new StringBuilder();

        int keyHash = secretKey.hashCode();
        int messageIndex = 0;

        for (int y = 0; y < encodedBitmap.getHeight(); y++) {
            for (int x = 0; x < encodedBitmap.getWidth(); x++) {
                int pixel = encodedBitmap.getPixel(x, y);
                int red = Color.red(pixel);

                if (messageIndex < 16) {
                    int extractedBit = extractLSB(red, keyHash);
                    messageIndex |= (extractedBit << messageIndex);
                } else {
                    binaryMessage.append(extractLSB(red, keyHash));
                }
            }
        }

        return binaryToString(binaryMessage.toString());
    }

    private int extractLSB(int value, int keyHash) {
        return (value - keyHash) & 1;
    }

    private String binaryToString(String binaryString) {
        StringBuilder text = new StringBuilder();
        int length = binaryString.length();

        for (int i = 0; i < length; i += 8) {
            String byteStr = binaryString.substring(i, Math.min(i + 8, length));
            int decimalValue = Integer.parseInt(byteStr, 2);
            char character = (char) decimalValue;
            text.append(character);
        }

        return text.toString();
    }
}
