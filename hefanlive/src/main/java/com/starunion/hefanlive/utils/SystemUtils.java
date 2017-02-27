package com.starunion.hefanlive.utils;

import android.app.Activity;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by saga_ios on 16/12/7.
 */
public class SystemUtils {

    /**
     * 控制屏幕常亮
     */
    public static void controlBackLight(Activity activity, boolean flag) {
        if (null == activity)
            return;
        Window win = activity.getWindow();
        if (flag) {
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
