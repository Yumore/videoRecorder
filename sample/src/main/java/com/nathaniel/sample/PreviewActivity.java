package com.nathaniel.sample;

import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

/**
 * @author Nathaniel
 * @version V1.0.0
 * @package com.nathaniel.recorder
 * @datetime 2020/4/5 - 19:06
 */
public class PreviewActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback, MediaPlayer.OnPreparedListener {
    private static final String TAG = PreviewActivity.class.getSimpleName();
    private TextView tvBack;

    private String videoPath;
    private MediaPlayer mediaPlayer;
    private SurfaceView surfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        initData();
        initView();
    }

    private void initData() {
        videoPath = getIntent().getStringExtra("videoPath");
    }

    private void initView() {
        surfaceView = findViewById(R.id.surfaceView);
        tvBack = findViewById(R.id.preview_back_tv);
        tvBack.setOnClickListener(this);
        mediaPlayer = new MediaPlayer();
        if (TextUtils.isEmpty(videoPath)) {
            Toast.makeText(this, "暂无视频资源", Toast.LENGTH_SHORT).show();
        }
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        try {
            mediaPlayer.setDataSource(videoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.setKeepScreenOn(true);

    }

    @Override
    public void onClick(View view) {
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && surfaceHolder.getSurface().isValid()) {
//            mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            mediaPlayer.setDisplay(surfaceHolder);
            mediaPlayer.setLooping(true);
            try {
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaPlayer.setOnPreparedListener(this);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
    }
}
