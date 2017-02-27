package com.starunion.hefanlive.ihefan;

import android.app.Activity;
import android.app.Application;

import com.starunion.hefanlive.datasource.IHefanDataSource;

/**
 * Created by saga_ios on 16/12/6.
 */
public interface IHefanPlayer {

    /**
     * 初始化播放器对象
     *
     * @param activity
     * */
    public void initPlayer(Activity activity, IHefanDataSource dataSource);

    /**
     * 播放接口
     * */
    void onPlay();

    /**
     * 暂停接口
     * */
    void onPause();

    /**
     * 停止接口
     * */
    void onStop();

    /**
     *
     * */
    void onResume();

}
