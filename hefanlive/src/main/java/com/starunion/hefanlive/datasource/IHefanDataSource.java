package com.starunion.hefanlive.datasource;

import android.view.SurfaceView;

import com.arcvideo.camerarecorder.CameraSurfaceView;
import com.starunion.hefanlive.view.HefanIjkPlayer;
import com.starunion.hefanlive.view.HefanLiveSurface;
import com.starunion.hefanlive.view.HefanPlayerSaurface;

/**
 * Created by saga_ios on 16/12/6.
 */
public interface IHefanDataSource {

    /**
     * 提供主通道
     *
     * @return string
     * */
    public String provideMainPassageway();

    /**
     * 提供备用通道
     *
     * @return String
     * */
    public String provideBackUpPassageway();

    /**
     * 提供推流surface view
     *
     * @return HefanLiveSurface
     * */
    public CameraSurfaceView provideLiveSurface();

    /**
     * 提供拉流surface view
     *
     * @return HefanPlayerSaurface
     * */
    public HefanPlayerSaurface providePlaySurface();

    public HefanIjkPlayer provideIjkPlayer();
}
