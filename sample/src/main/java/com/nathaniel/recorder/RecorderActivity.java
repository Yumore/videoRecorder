package com.nathaniel.recorder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class RecorderActivity extends Activity {

    RecorderView mRecordView;
    ControllerView mController;
    String videoPath;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);
        mRecordView = findViewById(R.id.recordView);
        mController = findViewById(R.id.layout_controller);
        mController.bindRecordView(mRecordView);
        mController.setDuration(0, 15);
        mController.setOnRecorderListener(new OnRecorderListener() {
            @Override
            public void onStartRecord() {
                Toast.makeText(RecorderActivity.this, "开始录制", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopRecord(long duration, String filePath) {
                videoPath = filePath;
                Toast.makeText(RecorderActivity.this, "结束录制，时长" + duration, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelRecord() {
                Toast.makeText(RecorderActivity.this, "取消录制", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {
                Intent intent = new Intent(RecorderActivity.this, PreviewActivity.class);
                intent.putExtra("videoPath", videoPath);
                startActivity(intent);
            }
        });
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
    protected void onPause() {
        super.onPause();
        mController.cancelRecord();
        mRecordView.closeCamera();
    }
}