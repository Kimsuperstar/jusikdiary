package com.example.jusikdiary;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

public class DiaryActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    EditText diaryEntry;
    Button saveButton;
    Button imageButton;
    TextView dateTextView;
    ImageView selectedImageView;
    String selectedDate, fileName;
    Uri selectedImageUri;

    // 이미지 선택기를 시작하기 위한 ActivityResultLauncher
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        saveImageToInternalStorage(imageUri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary);
        setTitle("주목 일기 작성");

        diaryEntry = findViewById(R.id.diaryEntry);
        saveButton = findViewById(R.id.saveButton);
        imageButton = findViewById(R.id.imageButton);
        dateTextView = findViewById(R.id.dateTextView);
        selectedImageView = findViewById(R.id.selectedImageView);

        // MainActivity에서 전달된 날짜를 가져옴
        Intent intent = getIntent();
        selectedDate = intent.getStringExtra("selectedDate");

        if (selectedDate == null) {
            // 현재 날짜를 가져와서 선택된 날짜로 설정
            Calendar calendar = Calendar.getInstance();
            int cYear = calendar.get(Calendar.YEAR);
            int cMonth = calendar.get(Calendar.MONTH);
            int cDay = calendar.get(Calendar.DAY_OF_MONTH);
            selectedDate = cYear + "/" + (cMonth + 1) + "/" + cDay;
        }

        // 선택한 날짜를 TextView에 표시
        dateTextView.setText("일기 작성 날짜: " + selectedDate);

        // 파일 이름 설정 (예: 2023_06_10.txt)
        fileName = selectedDate.replace("/", "_") + ".txt";

        // 파일에서 일기 내용 읽기
        String diaryContent = readDiary(fileName);
        diaryEntry.setText(diaryContent);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    FileOutputStream outFs = openFileOutput(fileName, Context.MODE_PRIVATE);
                    String str = diaryEntry.getText().toString();
                    String imageUriString = selectedImageUri != null ? selectedImageUri.toString() : "";
                    String combinedContent = str + "\n" + imageUriString;
                    outFs.write(combinedContent.getBytes());
                    outFs.close();
                    Toast.makeText(getApplicationContext(), fileName + " 이 저장됨", Toast.LENGTH_SHORT).show();

                    // 일기 내용을 MainActivity로 전달
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("diaryEntry", str);
                    resultIntent.putExtra("imageUri", imageUriString);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } catch (IOException e) {
                    Log.e("DiaryActivity", "File write failed", e);
                    Toast.makeText(getApplicationContext(), "저장 중 오류 발생", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 이미지 선택 버튼 클릭 이벤트 처리
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageSelector();
            }
        });
    }

    // 파일에서 일기 내용 읽기 메서드
    private String readDiary(String fName) {
        StringBuilder diaryStr = new StringBuilder();
        FileInputStream inFs;
        try {
            File file = new File(getFilesDir(), fName);
            if (file.exists()) {
                inFs = openFileInput(fName);
                byte[] txt = new byte[(int) file.length()];
                inFs.read(txt);
                inFs.close();
                String[] contentParts = (new String(txt)).trim().split("\n");
                if (contentParts.length > 0) {
                    diaryStr.append(contentParts[0]);
                    if (contentParts.length > 1 && !contentParts[1].isEmpty()) {
                        selectedImageUri = Uri.parse(contentParts[1]);
                        loadImageFromInternalStorage(selectedImageUri);
                    }
                }
                saveButton.setText("수정 하기");
            } else {
                diaryEntry.setHint("일기 없음");
                saveButton.setText("새로 저장");
            }
        } catch (IOException e) {
            Log.e("DiaryActivity", "File read failed", e);
            diaryEntry.setHint("일기 없음");
            saveButton.setText("새로 저장");
        }
        return diaryStr.toString();
    }

    // 이미지 선택기 열기 메서드
    private void openImageSelector() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // URI 읽기 권한 추가
        pickImageLauncher.launch(intent); // 이미지 선택기 인텐트 실행
    }

    // 내부 저장소에 이미지 저장
    private void saveImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                File file = new File(getFilesDir(), "image_" + System.currentTimeMillis() + ".jpg");
                OutputStream outputStream = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.close();
                inputStream.close();
                selectedImageUri = Uri.fromFile(file);
                selectedImageView.setImageURI(selectedImageUri);
                selectedImageView.setVisibility(View.VISIBLE);
            }
        } catch (IOException e) {
            Log.e("DiaryActivity", "Image save failed", e);
        }
    }

    // 내부 저장소에서 이미지 로드
    private void loadImageFromInternalStorage(Uri uri) {
        try {
            File file = new File(uri.getPath());
            if (file.exists()) {
                Glide.with(this)
                        .load(file)
                        .into(selectedImageView);
                selectedImageView.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e("DiaryActivity", "Image load failed", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용되면 이미지를 다시 설정합니다.
                if (selectedImageUri != null) {
                    saveImageToInternalStorage(selectedImageUri);
                }
            } else {
                Toast.makeText(this, "이미지 읽기 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
