package com.nathaniel.recorder;

public interface OnRecorderListener {
    void onStartRecord();

    void onStopRecord(long duration, String videdoPath);

    void onCancelRecord();

    void onComplete();
}
