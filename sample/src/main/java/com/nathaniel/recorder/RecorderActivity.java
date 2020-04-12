package com.nathaniel.recorder;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

/**
 * @author Nathaniel
 */
public class RecorderActivity extends AppCompatActivity implements OnRecorderListener {
    private RecorderView recorderView;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);
        recorderView = findViewById(R.id.recordView);
        ControllerView controllerView = findViewById(R.id.layout_controller);
        controllerView.bindRecordView(recorderView);
        controllerView.setDuration(0, 15);
        controllerView.setOnRecorderListener(this);
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        recorderView.cancelRecorder();
        finish();
    }

    @Override
    public void onRecorderStart() {
        Toast.makeText(RecorderActivity.this, "开始录制", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onRecorderCancel() {
        Toast.makeText(RecorderActivity.this, "取消录制", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecorderComplete(long duration, String filePath) {
        Looper.prepare();
        Toast.makeText(RecorderActivity.this, "结束录制，时长" + duration, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, PreviewActivity.class);
        intent.putExtra("videoPath", filePath);
        startActivity(intent);
        finish();
    }
}