package com.nathaniel.recorder;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;


public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();
    private static final String ROOT_PATH = "recorder";
    private static final String CACHE_PATH = ".cache";
    private static final String IMAGE_PATH = "image";
    private static final String AUDIO_PATH = "audio";
    private static final String VIDEO_PATH = "video";
    private static final String LOGGER_PATH = ".logger";

    private static boolean isExists(String path) {
        return new File(path).exists();
    }

    public static String getRootPath() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File file;
            String vendor = Build.MANUFACTURER;
            if (vendor != null && vendor.toLowerCase().contains("samsung")) {
                file = new File("/mnt/sdcard", ROOT_PATH);
            } else {
                file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), ROOT_PATH);
            }
            makeDirs(file);
            return file.getAbsolutePath();
        } else {
            File file = new File(Environment.getRootDirectory().getPath(), ROOT_PATH);
            makeDirs(file);
            return file.getAbsolutePath();
        }
    }

    public static String getImagePath() {
        File file = new File(getRootPath(), IMAGE_PATH);
        makeDirs(file);
        return file.getAbsolutePath();
    }

    public static String getAudioPath() {
        File file = new File(getRootPath(), AUDIO_PATH);
        makeDirs(file);
        return file.getAbsolutePath();
    }

    public static String getVideoPath() {
        File file = new File(getRootPath(), VIDEO_PATH);
        makeDirs(file);
        return file.getAbsolutePath();
    }

    public static String getLoggerPath() {
        File file = new File(getRootPath(), LOGGER_PATH);
        makeDirs(file);
        return file.getAbsolutePath();
    }

    public static String getCachePath() {
        File file = new File(getRootPath(), CACHE_PATH);
        makeDirs(file);
        return file.getAbsolutePath();
    }

    public static String getCacheName() {
        return CACHE_PATH;
    }

    public static void makeDirs(String path) {
        File file = new File(path);
        if (!file.exists()) {
            boolean flag = file.mkdirs();
        }
    }

    private static void makeDirs(File file) {
        File parentFile = file.getParentFile();
        if (parentFile == null) {
            return;
        }
        if (!parentFile.exists()) {
            boolean flag = parentFile.mkdirs();
            Log.e(TAG, "create dir " + file.getName() + " success or not " + flag);
        }
        if (!file.exists()) {
            boolean flag = file.mkdirs();
            Log.e(TAG, "create dir " + file.getName() + " success or not " + flag);
        }
    }
}

