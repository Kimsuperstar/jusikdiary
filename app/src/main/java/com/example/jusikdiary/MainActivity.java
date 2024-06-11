package com.example.jusikdiary;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    CalendarView calendarView;
    Spinner spinner;
    String selectedDate;
    HashMap<String, String> dateComments;
    Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("오늘의 주목");

        calendarView = findViewById(R.id.calendarView);
        spinner = findViewById(R.id.spinner1);
        dateComments = new HashMap<>();
        saveButton = findViewById(R.id.saveButton);

        // 권한 요청
        checkPermissions();

        // Spinner에 들어갈 기분 설정
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_items, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                selectedDate = year + "/" + (month + 1) + "/" + dayOfMonth;
                Toast.makeText(MainActivity.this, "선택한 날짜: " + selectedDate, Toast.LENGTH_SHORT).show();
                loadSpinnerSelection();
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                saveSpinnerSelection();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing here
            }
        });

        saveButton.setOnClickListener(v -> {
            if (selectedDate == null) {
                Toast.makeText(this, "날짜를 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MainActivity.this, DiaryActivity.class);
            intent.putExtra("selectedDate", selectedDate);
            startActivityForResult(intent, 1);
        });
    }

    private void saveSpinnerSelection() {
        if (selectedDate != null) {
            String selectedComment = spinner.getSelectedItem().toString();
            dateComments.put(selectedDate, selectedComment);
        }
    }

    private void loadSpinnerSelection() {
        if (selectedDate != null) {
            String comment = dateComments.get(selectedDate);
            if (comment != null) {
                ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinner.getAdapter();
                int position = adapter.getPosition(comment);
                spinner.setSelection(position);
            } else {
                spinner.setSelection(0);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String diaryEntry = data.getStringExtra("diaryEntry");
            Toast.makeText(this, "일기 저장됨: " + diaryEntry, Toast.LENGTH_SHORT).show();
            // 여기서 일기 내용을 저장하거나 처리할 수 있습니다.
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                Toast.makeText(this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
