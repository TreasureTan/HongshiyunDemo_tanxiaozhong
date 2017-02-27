package com.starunion.hefanlive.ihefan;

import android.app.Activity;
import android.app.Application;

import com.starunion.hefanlive.datasource.IHefanDataSource;

/**
 * Created by saga_ios on 16/11/23.
 */
public interface IHefanLive {

    /**
     * 初始化推流
     *
     * @param activity
     * */
    public void initLive(Activity activity, IHefanDataSource dataSource);

    /**
     * 打开预览
     * */
    public void openPreview();

    /**
     * 关闭预览
     * */
    public void closePreview();

    /**
     * 开始推流
     * */
    public void onStartRecord();

    /**
     * 结束推流
     * */
    public void onStopRecord();

    /**
     * 闪光灯开关
     *
     * @param isOpen
     * */
    public void flashLight(boolean isOpen);


    /**
     * 切换前摄像头
     * */
    public void onCameraFacing();

    /**
     * 切换后摄像头
     * */
    public void onCameraTorch();

    /**
     * 亮度开关
     *
     * @param length
     * */
    public void brightnessSwitch(boolean flag,int length);

    /**
     * 磨皮开关
     *
     * @param length
     * */
    public void buffingSwitch(boolean flag,int length);

    /**
     * 销毁推流播放器
     * */
    public void destroyLive();

    /**
     * 暂停推流
     */
    public void onPauseRecord();

    /**
     * 恢复推流
     */
    public void onResumeRecord();

}
