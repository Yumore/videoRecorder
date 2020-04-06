package com.nathaniel.recorder;

import android.hardware.Camera;
import android.util.Log;

/**
 * Abstraction over the {@link Camera} API that helps open them and return their metadata.
 *
 * @author nathaniel
 */
public final class CameraInterface {

    /**
     * means no preference for which camera to open.
     */
    public static final int NO_REQUESTED_CAMERA = -1;
    private static final String TAG = CameraInterface.class.getSimpleName();

    private CameraInterface() {
    }

    /**
     * Opens the requested camera with {@link Camera#open(int)}, if one exists.
     *
     * @param cameraId camera ID of the camera to use. A negative value
     *                 or {@link #NO_REQUESTED_CAMERA} means "no preference", in which case a rear-facing
     *                 camera is returned if possible or else any camera
     * @return handle to {@link RecorderCamera} that was opened
     */
    public static RecorderCamera open(int cameraId, CameraFacing cameraFacing) {

        int numCameras = Camera.getNumberOfCameras();
        if (numCameras == 0) {
            Log.w(TAG, "No cameras!");
            return null;
        }
        if (cameraId >= numCameras) {
            Log.w(TAG, "Requested camera does not exist: " + cameraId);
            return null;
        }

        if (cameraId <= NO_REQUESTED_CAMERA) {
            cameraId = 0;
            while (cameraId < numCameras) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(cameraId, cameraInfo);
                if (CameraFacing.values()[cameraInfo.facing] == cameraFacing) {
                    break;
                }
                cameraId++;
            }
            if (cameraId == numCameras) {
                Log.i(TAG, "No camera facing " + cameraFacing + "; returning camera #0");
                cameraId = 0;
            }
        }

        Log.i(TAG, "Opening camera #" + cameraId);
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        Camera camera = Camera.open(cameraId);
        camera.setDisplayOrientation(0);
        return new RecorderCamera(cameraId, camera, CameraFacing.values()[cameraInfo.facing], cameraInfo.orientation);
    }

}
