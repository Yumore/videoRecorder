package com.nathaniel.recorder;

/**
 * @author Nathaniel
 */
public interface OnRecorderListener {
    /**
     * 开始录制
     */
    void onRecorderStart();

    /**
     * 录制结束
     *
     * @param duration 录制时长
     * @param filePath 文件路径
     */
    void onRecorderComplete(long duration, String filePath);

    /**
     * 取消录制
     */
    void onRecorderCancel();
}
