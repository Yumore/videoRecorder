package com.nathaniel.recorder;

import android.content.Context;
import android.os.Environment;
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

import com.nathaniel.recorder.lib.R;

import java.io.File;

/**
 * @author nathaniel
 */
public class ControllerView extends FrameLayout implements View.OnClickListener {
    private static final String TAG = ControllerView.class.getSimpleName();
    private String parentPath, videoName;
    private ImageView recorderFlash;
    private RecorderView recorderView;
    private ImageView recorderStart;
    private TextView recorderTime;
    private int minDuration = 3;
    private int maxDuration = 15;
    private long startTime, pausedTime, pauseDuration;
    private Handler mainHandler;
    private OnRecorderListener onRecorderListener;
    private ProgressBar progressBar;

    private Runnable durationCounter = new Runnable() {
        @Override
        public void run() {
            long currentDuration = System.currentTimeMillis() - startTime - pauseDuration;
            recorderTime.setText(formatTime(currentDuration));
            long percent = currentDuration / maxDuration / 10;
            Log.e(TAG, String.format("时间差值：%d，真实时长：%s, 录制进度：%d", currentDuration, formatTime(currentDuration), percent));
            progressBar.setProgress((int) percent);
            if (maxDuration > 0 && currentDuration >= maxDuration * 1000) {
                stopRecord(false);
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
        parentPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "vipon" + File.separator;
        videoName = "sample";
        View layoutController = LayoutInflater.from(context).inflate(R.layout.layout_record_controller, this);
        recorderStart = layoutController.findViewById(R.id.btn_start);
        recorderTime = layoutController.findViewById(R.id.tv_time);
        ImageView videoCancel = layoutController.findViewById(R.id.btn_cancel);
        ImageView videoFinish = layoutController.findViewById(R.id.btn_finish);
        TextView videoBack = layoutController.findViewById(R.id.tv_back);
        ImageView videoCamera = layoutController.findViewById(R.id.iv_camera);
        recorderFlash = layoutController.findViewById(R.id.iv_flash);
        progressBar = layoutController.findViewById(R.id.progress);
        progressBar.setProgress(0);
        recorderStart.setOnClickListener(this);
        videoCancel.setOnClickListener(this);
        videoFinish.setOnClickListener(this);
        recorderFlash.setOnClickListener(this);
        videoCamera.setOnClickListener(this);
        videoBack.setOnClickListener(this);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void bindRecordView(@NonNull RecorderView recorderView) {
        if (this.recorderView != null) {
            Log.w("RecordControllerLayout", "RecordView 已经绑定，不能再次绑定");
            return;
        }
        this.recorderView = recorderView;
        recorderView.setVideoPath(parentPath, videoName);
    }

    public int getMinDuration() {
        return minDuration;
    }

    public int getMaxDuration() {
        return maxDuration;
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
        if (view.getId() == R.id.btn_start) {
            if (recorderView == null) {
                Log.w("RecordControllerLayout", "请先绑定 RecordView");
                return;
            }
            startRecord();
        } else if (view.getId() == R.id.btn_cancel) {
            cancelRecord();
            if (onRecorderListener != null) {
                onRecorderListener.onCancelRecord();
            }
        } else if (view.getId() == R.id.btn_finish) {
            stopRecord(false);
            if (onRecorderListener != null) {
                onRecorderListener.onComplete();
            }
        } else if (view.getId() == R.id.iv_camera) {
            recorderView.reverseCamera();
        } else if (view.getId() == R.id.iv_flash) {
            recorderView.setTorch(!recorderView.getTorchState());
            recorderFlash.setImageResource(recorderView.getTorchState() ? R.drawable.icon_flash_active : R.drawable.icon_flash_normal);
        }
    }

    private void startRecord() {
        Log.e(TAG, "recorder status is " + recorderView.getRecorderStatus().name());
        if (onRecorderListener == null) {
            throw new RuntimeException(" onRecorderListener is null");
        }
        if (recorderView.getRecorderStatus() == RecorderStatus.RECORDING) {
            recorderView.setRecorderStatus(RecorderStatus.PAUSING);
            recorderView.pausedRecord();
            pausedTime = System.currentTimeMillis();
            recorderStart.setImageResource(R.drawable.button_start_record);
            mainHandler.removeCallbacks(durationCounter);
        } else if (recorderView.getRecorderStatus() == RecorderStatus.PAUSING) {
            recorderView.setRecorderStatus(RecorderStatus.RECORDING);
            pauseDuration += System.currentTimeMillis() - pausedTime;
            recorderView.resumeRecord();
            recorderStart.setImageResource(R.drawable.button_stop_record);
            mainHandler.post(durationCounter);
        } else {
            recorderView.setRecorderStatus(RecorderStatus.RECORDING);
            recorderView.startRecord();
            startTime = System.currentTimeMillis();
            recorderStart.setImageResource(R.drawable.button_stop_record);
            onRecorderListener.onStartRecord();
            mainHandler.removeCallbacks(durationCounter);
            mainHandler.post(durationCounter);
        }
    }

    /**
     * 强制结束录制
     */
    public void cancelRecord() {
        stopRecord(true);
    }

    /**
     * 结束录制
     *
     * @param manual true:手动取消
     *               false:手动结束
     */
    private void stopRecord(boolean manual) {
        if (recorderView.getRecorderStatus() == RecorderStatus.PREPARED) {
            return;
        }
        long duration = System.currentTimeMillis() - startTime - pauseDuration;
        if (!manual && minDuration > 0 && (duration < minDuration * 1000)) {
            Toast.makeText(getContext(), getContext().getString(R.string.record_min_duration_hint, minDuration), Toast.LENGTH_SHORT).show();
            return;
        }
        recorderView.stopRecord();
        recorderView.setRecorderStatus(RecorderStatus.PREPARED);
        recorderStart.setImageResource(R.drawable.button_start_record);
        recorderTime.setText("00:00");
        if (onRecorderListener != null) {
            if (manual) {
                onRecorderListener.onCancelRecord();
            } else {
                onRecorderListener.onStopRecord(duration, recorderView.getVideoPath());
            }
        }
        mainHandler.removeCallbacks(durationCounter);
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
}
