package com.nathaniel.recorder;

import android.hardware.Camera;

/**
 * @author nathaniel
 */
public final class RecorderCamera {

    private final int index;
    private final Camera camera;
    private final CameraFacing cameraFacing;
    private final int orientation;

    public RecorderCamera(int index, Camera camera, CameraFacing cameraFacing, int orientation) {
        this.index = index;
        this.camera = camera;
        this.cameraFacing = cameraFacing;
        this.orientation = orientation;
    }

    public Camera getCamera() {
        return camera;
    }

    public CameraFacing getCameraFacing() {
        return cameraFacing;
    }

    public int getOrientation() {
        return orientation;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "Camera #" + index + " : " + cameraFacing + ',' + orientation;
    }

}
