package com.nathaniel.recorder;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;


/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CameraManager {

    private static final String TAG = CameraManager.class.getSimpleName();
    private final Context context;
    private final ConfigManager configManager;
    private RecorderCamera recorderCamera;
    private FocusedManager focusedManager;
    private Rect framingRect;
    private boolean initialized;
    private boolean previewing;
    private int requestedCameraId = CameraInterface.NO_REQUESTED_CAMERA;
    private int requestedFramingRectWidth;
    private int requestedFramingRectHeight;


    public CameraManager(Context context) {
        this.context = context;
        this.configManager = new ConfigManager(context);
    }

    private static int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
        int dim = 5 * resolution / 8;
        // Target 5/8 of each dimension
        if (dim < hardMin) {
            return hardMin;
        }
        return Math.min(dim, hardMax);
    }

    /**
     * Opens the camera driver and initializes the hardware parameters.
     *
     * @param holder The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    public synchronized Camera openDriver(SurfaceHolder holder, CameraFacing cameraFacing) throws IOException {
        RecorderCamera theCamera = recorderCamera;
        if (theCamera == null) {
            theCamera = CameraInterface.open(requestedCameraId, cameraFacing);
            if (theCamera == null) {
                throw new IOException("Camera.open() failed to return object from driver");
            }
            recorderCamera = theCamera;
        }

        if (!initialized) {
            initialized = true;
            configManager.initFromCameraParameters(theCamera);
            if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
                setManualFramingRect(requestedFramingRectWidth, requestedFramingRectHeight);
                requestedFramingRectWidth = 0;
                requestedFramingRectHeight = 0;
            }
        }

        Camera cameraObject = theCamera.getCamera();
        Camera.Parameters parameters = cameraObject.getParameters();
        String parametersFlattened = parameters == null ? null : parameters.flatten();
        try {
            configManager.setDesiredCameraParameters(theCamera, false);
        } catch (RuntimeException re) {
            // Driver failed
            Log.w(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
            Log.i(TAG, "Resetting to saved camera params: " + parametersFlattened);
            // Reset:
            if (parametersFlattened != null) {
                parameters = cameraObject.getParameters();
                parameters.unflatten(parametersFlattened);
                try {
                    cameraObject.setParameters(parameters);
                    configManager.setDesiredCameraParameters(theCamera, true);
                } catch (RuntimeException re2) {
                    // Well, darn. Give up
                    Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
                }
            }
        }
        //  cameraObject.cancelAutoFocus();
        cameraObject.setPreviewDisplay(holder);

        return recorderCamera.getCamera();
    }

    /**
     * 获取当前预览画面相机分辨率
     *
     * @return
     */
    public Point getCameraResolution() {
        return configManager.getCameraResolution();
    }

    /**
     * 获取当前相机
     *
     * @return
     */
    public synchronized Camera getRecorderCamera() {
        return recorderCamera == null ? null : recorderCamera.getCamera();
    }

    /**
     * 获取当前相机
     *
     * @return
     */
    public synchronized RecorderCamera getOpenCamera() {
        return recorderCamera;
    }

    public synchronized boolean isOpen() {
        return recorderCamera != null;
    }

    /**
     * Closes the camera driver if still in use.
     */
    public synchronized void closeDriver() {
        if (recorderCamera != null) {
            recorderCamera.getCamera().setPreviewCallback(null);
            recorderCamera.getCamera().release();
            recorderCamera = null;
            // Make sure to clear these each time we close the camera, so that any scanning rect
            // requested by intent is forgotten.
            framingRect = null;
        }
    }

    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     */
    public synchronized void startPreview() {
        RecorderCamera theCamera = recorderCamera;
        if (theCamera != null && !previewing) {
            theCamera.getCamera().startPreview();
            previewing = true;
            focusedManager = new FocusedManager(context, theCamera.getCamera());
        }
    }

    /**
     * Tells the camera to stop drawing preview frames.
     */
    public synchronized void stopPreview() {
        if (focusedManager != null) {
            focusedManager.stop();
            focusedManager = null;
        }
        if (recorderCamera != null && previewing) {
            recorderCamera.getCamera().stopPreview();
            recorderCamera.getCamera().setPreviewCallback(null);
            previewing = false;
        }
    }

    /**
     * 设置是否打开闪光灯
     *
     * @param newSetting if {@code true}, light should be turned on if currently off. And vice versa.
     */
    public synchronized void setTorch(boolean newSetting) {
        RecorderCamera theCamera = recorderCamera;
        if (theCamera != null && newSetting != configManager.getTorchState(theCamera.getCamera())) {
            boolean wasAutoFocusManager = focusedManager != null;
            if (wasAutoFocusManager) {
                focusedManager.stop();
                focusedManager = null;
            }
            configManager.setTorch(theCamera.getCamera(), newSetting);
            if (wasAutoFocusManager) {
                focusedManager = new FocusedManager(context, theCamera.getCamera());
                focusedManager.start();
            }
        }
    }

    /**
     * 获取闪光灯状态
     *
     * @return true 打开中，false 关闭
     */
    public synchronized boolean getTorchState() {
        RecorderCamera theCamera = recorderCamera;
        if (theCamera != null) {
            return configManager.getTorchState(theCamera.getCamera());
        }
        return false;
    }

    /**
     * Allows third party apps to specify the camera ID, rather than determine
     * it automatically based on available cameras and their orientation.
     *
     * @param cameraId camera ID of the camera to use. A negative value means "no preference".
     */
    public synchronized void setManualCameraId(int cameraId) {
        requestedCameraId = cameraId;
    }

    /**
     * Allows third party apps to specify the scanning rectangle dimensions, rather than determine
     * them automatically based on screen resolution.
     *
     * @param width  The width in pixels to scan.
     * @param height The height in pixels to scan.
     */
    public synchronized void setManualFramingRect(int width, int height) {
        if (initialized) {
            Point screenResolution = configManager.getScreenResolution();
            if (width > screenResolution.x) {
                width = screenResolution.x;
            }
            if (height > screenResolution.y) {
                height = screenResolution.y;
            }
            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 2;
            framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
            Log.d(TAG, "Calculated manual framing rect: " + framingRect);
        } else {
            requestedFramingRectWidth = width;
            requestedFramingRectHeight = height;
        }
    }

    /**
     * 判断屏幕与相机方向是否一致
     *
     * @return
     */
    public boolean isSameDirection() {
        Point cameraResolution = configManager.getCameraResolution();
        Point screenResolution = configManager.getScreenResolution();
        if (cameraResolution == null || screenResolution == null) {
            // Called early, before init even finished
            return true;
        }
        // 确认横竖屏
        boolean isPortraitScreen = screenResolution.x < screenResolution.y;
        boolean isPortraitCamera = cameraResolution.x < cameraResolution.y;
        return isPortraitCamera == isPortraitScreen;
    }
}
