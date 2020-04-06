package com.nathaniel.recorder;

import java.io.File;

/**
 * @author nathaniel
 * @version V1.0.0
 * @package com.nathaniel.recorder
 * @datetime 2020/4/2 - 11:35
 */
public interface OnMergedListener {
    void onFinished(File file);

    void onFailure(Throwable throwable);
}
