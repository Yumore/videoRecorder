package com.nathaniel.recorder;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author nathaniel
 */
public class ControllerView extends FrameLayout implements View.OnClickListener, MediaScannerConnection.OnScanCompletedListener {
    private static final String TAG = ControllerView.class.getSimpleName();
    private ImageView videoCamera;
    private ImageView videoCancel, videoFinish;
    private String parentPath, videoName;
    private ImageView recorderFlash;
    private RecorderView recorderView;
    private ImageView recorderStart;
    private TextView recorderTime;
    private int minDuration = 3;
    private int maxDuration = 15;
    private long startTime;
    private long pausedTime;
    private long pauseDuration;
    private Handler mainHandler;
    private OnRecorderListener onRecorderListener;
    private ProgressBar progressBar;
    private long realDuration;

    private Runnable durationCounter = new Runnable() {
        @Override
        public void run() {
            long currentDuration = System.currentTimeMillis() - startTime - pauseDuration;
            recorderTime.setText(formatTime(currentDuration));
            long percent = currentDuration / maxDuration / 10;
            Log.e(TAG, String.format("时间差值：%d，真实时长：%s, 录制进度：%d", currentDuration, formatTime(currentDuration), percent));
            progressBar.setProgress((int) percent);
            if (maxDuration > 0 && currentDuration >= maxDuration * 1000) {
                stopRecorder();
            } else {
                mainHandler.postDelayed(this, 100);
            }
        }
    };

    public ControllerView(@NonNull Context context) {
        this(context, null);
    }

    public ControllerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ControllerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parentPath = FileUtils.getVideoPath();
        videoName = "sample";
        View layoutController = LayoutInflater.from(context).inflate(R.layout.layout_record_controller, this);
        recorderStart = layoutController.findViewById(R.id.recorder_start_iv);
        recorderTime = layoutController.findViewById(R.id.recorder_time_tv);
        videoCancel = layoutController.findViewById(R.id.recorder_cancel_iv);
        videoFinish = layoutController.findViewById(R.id.recorder_finish_iv);
        TextView videoBack = layoutController.findViewById(R.id.recorder_back_iv);
        videoCamera = layoutController.findViewById(R.id.recorder_camera_iv);
        recorderFlash = layoutController.findViewById(R.id.recorder_flash_iv);
        progressBar = layoutController.findViewById(R.id.recorder_progress_pb);
        progressBar.setProgress(0);
        recorderStart.setOnClickListener(this);
        videoCancel.setOnClickListener(this);
        videoFinish.setOnClickListener(this);
        recorderFlash.setOnClickListener(this);
        videoCamera.setOnClickListener(this);
        videoBack.setOnClickListener(this);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void cancelRecorder() {
        Log.e(TAG, "cancel: last recorder status " + recorderView.getRecorderStatus().name());
        if (recorderView.getRecorderStatus() == RecorderStatus.PREPARED) {
            return;
        }
        recorderView.setRecorderStatus(RecorderStatus.PREPARED);
        recorderStart.setImageResource(R.drawable.icon_start_record);
        startTime = System.currentTimeMillis();
        recorderView.cancelRecorder();
        mainHandler.removeCallbacks(durationCounter);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);
        videoCancel.setVisibility(View.GONE);
        videoFinish.setVisibility(View.GONE);
    }

    public void bindRecordView(@NonNull RecorderView recorderView) {
        if (this.recorderView != null) {
            Log.w("RecordControllerLayout", "RecordView 已经绑定，不能再次绑定");
            return;
        }
        this.recorderView = recorderView;
        recorderView.setVideoPath(parentPath, videoName);
    }

    public void setDuration(int minDuration, int maxDuration) {
        if (minDuration > maxDuration) {
            throw new IllegalArgumentException("minDuration 不能大于 maxDuration");
        }
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
    }

    public void setOnRecorderListener(OnRecorderListener onRecorderListener) {
        this.onRecorderListener = onRecorderListener;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.recorder_start_iv) {
            if (recorderView == null) {
                Log.e("RecordControllerLayout", "请先绑定 RecordView");
                return;
            }
            startRecorder();
        } else if (view.getId() == R.id.recorder_cancel_iv) {
            cancelRecorder();
            if (onRecorderListener != null) {
                onRecorderListener.onRecorderCancel();
            }
        } else if (view.getId() == R.id.recorder_finish_iv) {
            stopRecorder();
        } else if (view.getId() == R.id.recorder_camera_iv) {
            recorderView.reverseCamera();
        } else if (view.getId() == R.id.recorder_flash_iv) {
            recorderView.setTorch(!recorderView.getTorchState());
            recorderFlash.setImageResource(recorderView.getTorchState() ? R.drawable.icon_flash_closed : R.drawable.icon_flash_normal);
        }
    }

    private void startRecorder() {
        Log.e(TAG, "recorder status is " + recorderView.getRecorderStatus().name());
        if (onRecorderListener == null) {
            throw new RuntimeException(" onRecorderListener is null");
        }
        if (recorderView.getRecorderStatus() == RecorderStatus.RECORDING) {
            // 从录制状态转换到暂停状态
            pausedTime = System.currentTimeMillis();
            recorderStart.setImageResource(R.drawable.icon_start_record);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                recorderView.setRecorderStatus(RecorderStatus.PAUSING);
                videoCancel.setVisibility(View.VISIBLE);
                long duration = System.currentTimeMillis() - startTime;
                videoFinish.setVisibility(duration > 3000 ? View.VISIBLE : View.GONE);
                recorderView.pauseRecord();
            } else {
                recorderView.stopRecorder();
                recorderView.setRecorderStatus(RecorderStatus.PREPARED);
            }
            mainHandler.removeCallbacks(durationCounter);
        } else if (recorderView.getRecorderStatus() == RecorderStatus.PAUSING) {
            // 暂停状态到录制状态
            pauseDuration += System.currentTimeMillis() - pausedTime;
            Log.e(TAG, String.format("暂停时长:%s", formatTime(pauseDuration)));
            recorderStart.setImageResource(R.drawable.icon_stop_record);
            recorderView.setRecorderStatus(RecorderStatus.RECORDING);
            mainHandler.post(durationCounter);
            recorderView.resumeRecord();
            videoFinish.setVisibility(View.GONE);
            videoCancel.setVisibility(View.GONE);
        } else {
            // 从未录制到录制状态
            recorderStart.setImageResource(R.drawable.icon_stop_record);
            recorderView.setRecorderStatus(RecorderStatus.RECORDING);
            startTime = System.currentTimeMillis();
            mainHandler.removeCallbacks(durationCounter);
            recorderView.startRecorder();
            mainHandler.post(durationCounter);
        }
    }

    private void stopRecorder() {
        if (recorderView.getRecorderStatus() == RecorderStatus.PREPARED) {
            return;
        }
        realDuration = System.currentTimeMillis() - startTime - pauseDuration;
        if (minDuration > 0 && (realDuration < minDuration * 3000)) {
            recorderView.cancelRecorder();
            Toast.makeText(getContext(), getContext().getString(R.string.record_min_duration_hint, minDuration), Toast.LENGTH_SHORT).show();
            recorderStart.setVisibility(View.VISIBLE);
            return;
        }
        recorderStart.setImageResource(R.drawable.icon_start_record);
        recorderView.setRecorderStatus(RecorderStatus.PREPARED);
        Log.e(TAG, String.format("结束录制，时长: %s", formatTime(realDuration)));
        recorderTime.setText(formatTime(realDuration));
        mainHandler.removeCallbacks(durationCounter);
        recorderView.resetRecorder();
        videoCamera.setClickable(recorderView.getRecorderStatus() != RecorderStatus.RECORDING);
        recorderFlash.setVisibility(recorderView.getTorchEnable() ? View.VISIBLE : View.GONE);
        recorderStart.setVisibility(View.VISIBLE);
        MediaScannerConnection.scanFile(getContext(), new String[]{recorderView.getVideoPath()}, null, this);
    }


    private String formatTime(long duration) {
        long seconds = duration / 1000;
        if (seconds >= 60) {
            long minutes = seconds / 60;
            seconds = seconds - minutes * 60;
            return formatUnit(minutes) + ":" + formatUnit(seconds);
        } else {
            return "00:" + formatUnit(seconds);
        }
    }

    private String formatUnit(long number) {
        return number >= 10 ? String.valueOf(number) : "0" + number;
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        Log.e(TAG, "onScanCompleted() path = " + path + ", uri = " + uri);
        if (onRecorderListener != null) {
            onRecorderListener.onRecorderComplete(realDuration, path);
        }
    }
}
