package com.nathaniel.recorder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;

/**
 * @author nathaniel
 */
public class RecorderView extends SurfaceView implements SurfaceHolder.Callback {
    /**
     * 后置相机
     */
    public static final int FACING_BACK = 0;
    /**
     * 前置相机
     */
    public static final int FACING_FRONT = 1;
    /**
     * 默认录制宽度
     */
    public static final int DEFAULT_WIDTH = 1920;
    /**
     * 默认录制高度
     */
    public static final int DEFAULT_HEIGHT = 1080;
    /**
     * 默认帧数
     */
    public static final int DEFAULT_FRAME_RATE = 30;
    /**
     * 默认码率，512 KB
     */
    public static final int DEFAULT_BIT_RATE = 512;
    private static final String TAG = RecorderView.class.getSimpleName();
    /**
     * 1KB大小
     */
    private static final int KB = 1024 * 8;
    private static final String FILE_EXTENSION = ".mp4";
    /**
     * 相机管理类
     */
    private CameraManager cameraManager;
    /**
     * 媒体录制类
     */
    private MediaRecorder mediaRecorder;
    /**
     * Surface是否已打开
     */
    private boolean surfaceEnable;
    /**
     * 是否正在录制
     */
    private RecorderStatus recorderStatus = RecorderStatus.PREPARED;
    /**
     * 输出路径
     */
    private String parentPath;
    /**
     * 是否自动打开相机，如果是true，需要确保有 CAMERA 与 RECORD_AUDIO 权限，否则会报错
     */
    private boolean autoOpened;
    /**
     * 视频目标分辨率宽度
     */
    private int videoWidth = DEFAULT_WIDTH;
    /**
     * 视频目标分辨率高度
     */
    private int videoHeight = DEFAULT_HEIGHT;
    /**
     * 帧数
     */
    private int frameRate = DEFAULT_FRAME_RATE;
    /**
     * 码率，单位是 KB，此单位控制视频1秒钟的体积大小，在分辨率固定的情况下，码率越大画质越好，码率越小画质越差
     */
    private int bitRate = DEFAULT_BIT_RATE;
    /**
     * 指定相机位置
     */
    private CameraFacing cameraFacing = CameraFacing.FRONT;
    private String videoName;
    private String fileFullName;

    public RecorderView(Context context) {
        this(context, null);
    }

    public RecorderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecorderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (TextUtils.isEmpty(parentPath) || TextUtils.isEmpty(videoName)) {
            parentPath = FileUtils.getVideoPath();
            videoName = "sample";
        }
        initAttrs(attrs);
        getHolder().addCallback(this);
        Log.e(TAG, " initialize recorder view ");
        cameraManager = new CameraManager(getContext().getApplicationContext());
    }

    /**
     * 初始化View属性
     *
     * @param attrs 属性
     */
    private void initAttrs(@Nullable AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.RecorderView);
        cameraFacing = typedArray.getInt(R.styleable.RecorderView_facing, FACING_BACK) == FACING_FRONT
                ? CameraFacing.FRONT
                : CameraFacing.BACK;
        videoWidth = typedArray.getInteger(R.styleable.RecorderView_videoWidth, DEFAULT_WIDTH);
        videoHeight = typedArray.getInteger(R.styleable.RecorderView_videoHeight, DEFAULT_HEIGHT);
        frameRate = typedArray.getInteger(R.styleable.RecorderView_frameRate, DEFAULT_FRAME_RATE);
        autoOpened = typedArray.getBoolean(R.styleable.RecorderView_autoOpen, false);
        bitRate = typedArray.getInteger(R.styleable.RecorderView_bitRate, DEFAULT_BIT_RATE);
        typedArray.recycle();
    }

    /**
     * 根据相机尺重设控件宽高
     *
     * @param camera
     */
    private void resizeWithCamera(Camera camera) {
        if (camera != null && camera.getParameters() != null) {
            Camera.Size size = camera.getParameters().getPreviewSize();
            int sizeWith = size.width > size.height ? size.height : size.width;
            int sizeHeight = size.width + size.height - sizeWith;
            // 算出宽高比
            double ratio = sizeHeight / (double) sizeWith;
            int targetHeight = (int) (getWidth() * ratio);
            // 如果目标高度与当前高度差值大于100，就重设高度
            if (Math.abs(targetHeight - getHeight()) > 100) {
                ViewGroup.LayoutParams params = getLayoutParams();
                params.height = targetHeight;
                setLayoutParams(params);
            }
        }
    }

    /**
     * 打开相机
     */
    public void openCamera() {
        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(getContext(), R.string.open_camera_error, Toast.LENGTH_SHORT).show();
            return;
        }
        if (surfaceEnable) {
            closeCamera();
            try {
                cameraManager.openDriver(getHolder(), cameraFacing);
                cameraManager.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), R.string.open_camera_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void closeCamera() {
        if (cameraManager != null) {
            cameraManager.stopPreview();
            cameraManager.closeDriver();
        }
    }

    public void startRecorder() {
        Camera camera = cameraManager.getRecorderCamera();
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            printCameraParams(parameters);
            camera.unlock();
            mediaRecorder = RecorderFactory.newCustomConfigInstance(camera, parameters, videoWidth, videoHeight,
                    frameRate, bitRate * KB);
            File file = new File(parentPath, fileFullName);
            if (file.exists()) {
                boolean flag = file.delete();
                Log.e(TAG, " delete file " + file.getName() + " success is " + flag);
            }
            Log.e(TAG, "begin to recorder, file " + file.getAbsolutePath());
            mediaRecorder.setOutputFile(file.getAbsolutePath());
            int cameraOrientation = cameraManager.getOpenCamera().getOrientation();
            mediaRecorder.setOrientationHint(cameraOrientation);
            mediaRecorder.setPreviewDisplay(getHolder().getSurface());
            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
            } catch (IllegalStateException | IOException e) {
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                camera.lock();
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "open camera first ");
        }
    }

    private void printCameraParams(Camera.Parameters parameters) {
        Log.e(TAG, "video support size : " + parameters.getPreferredPreviewSizeForVideo().width + "X"
                + parameters.getPreferredPreviewSizeForVideo().height);
        for (int[] sizes : parameters.getSupportedPreviewFpsRange()) {
            for (int size : sizes) {
                Log.e(TAG, "video support fps range : " + size);
            }
        }
        for (Integer sizes : parameters.getSupportedPreviewFrameRates()) {
            Log.e(TAG, "video support frame range : " + sizes);
        }
    }

    public void stopRecorder() {
        Log.e(TAG, " stop recorder  " + recorderStatus.name() + " mediaRecorder is empty or not "
                + (mediaRecorder == null));
        if (mediaRecorder == null) {
            return;
        }
        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            Camera camera = cameraManager.getRecorderCamera();
            if (camera != null) {
                camera.lock();
                camera.release();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void cancelRecorder() {
        resetRecorder();
        deleteFile();
    }

    private void deleteFile() {
        File file = new File(parentPath, fileFullName);
        if (file.exists()) {
            boolean flag = file.delete();
            Log.e(TAG, " delete file " + file.getName() + " success is " + flag);
        }
    }

    public void resetRecorder() {
        if (mediaRecorder == null) {
            return;
        }
        Log.e(TAG, "reset recorder and status is " + recorderStatus.name());
        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
            openCamera();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public RecorderStatus getRecorderStatus() {
        return recorderStatus;
    }

    public void setRecorderStatus(RecorderStatus recorderStatus) {
        this.recorderStatus = recorderStatus;
    }

    public String getVideoPath() {
        return parentPath + videoName + FILE_EXTENSION;
    }

    /**
     * 设置video的路径
     * 
     * @param parentPath 文件的路路径
     * @param videoName  文件名称不带后缀名
     */
    public void setVideoPath(String parentPath, String videoName) {
        this.parentPath = parentPath;
        this.videoName = videoName;
        fileFullName = videoName + FILE_EXTENSION;
    }

    public boolean isAutoOpen() {
        return autoOpened;
    }

    public void setAutoOpen(boolean autoOpen) {
        this.autoOpened = autoOpen;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public void setTorch(boolean isOpen) {
        if (cameraManager != null) {
            cameraManager.setTorch(isOpen);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceEnable = true;
        if (autoOpened) {
            openCamera();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e(TAG, "surfaceChanged()");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceEnable = false;
        Log.e(TAG, "surfaceDestroyed()");
    }

    public void reverseCamera() {
        cameraFacing = cameraFacing == CameraFacing.FRONT ? CameraFacing.BACK : CameraFacing.FRONT;
        openCamera();
    }

    public boolean getTorchState() {
        return cameraManager != null && cameraManager.getTorchState();
    }

    public boolean getTorchEnable() {
        return cameraFacing == CameraFacing.BACK;
    }

    public void pauseRecord() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder.pause();
        } else {
            Log.e(TAG, "system version name is " + Build.VERSION.CODENAME);
        }
    }

    public void resumeRecord() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder.resume();
        } else {
            Log.e(TAG, "system version name is " + Build.VERSION.CODENAME);
        }
    }
}
