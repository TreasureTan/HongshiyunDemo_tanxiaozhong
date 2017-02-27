package com.starunion.hefanlive.ihefan;

import android.app.Application;

/**
 * Created by saga_ios on 16/12/6.
 */
public interface ISDKinit {
    /**
     * 初始化SDK
     *
     * @param application
     * */
    void initSDK(Application application);

    /**
     * 创建直播对象
     *
     * @return IHefanLive
     * */
    public IHefanLive createHefanLive();

    /**
     * 创建拉流对象
     *
     * @return IHefanPlayer
     * */
    public IHefanPlayer createHefanPlayer();
}
