package com.starunion.hefanlive.proxy;

import android.app.Application;

import com.starunion.hefanlive.ihefan.IHefanLive;
import com.starunion.hefanlive.ihefan.IHefanPlayer;
import com.starunion.hefanlive.ihefan.ISDKinit;
import com.starunion.hefanlive.impl.hsy.HsySDKinitImpl;

/**
 * Created by saga_ios on 16/12/6.
 */
public class HefanLiveProxy {

    private static HefanLiveProxy instance = new HefanLiveProxy();
    /**
     * sdk 初始化
     * */
    private ISDKinit isdKinit;
    /* 直播接口 */
    private IHefanLive hefanLive;

    /* 播放端接口 */
    private IHefanPlayer hefanPlayer;

    public static HefanLiveProxy getInstance(){
        return instance;
    }

    private HefanLiveProxy(){

    }

    /**
     * 初始化
     * */
    public void initHefanLive(Application application){
        if (isdKinit == null) {
            isdKinit = new HsySDKinitImpl();
        }
        isdKinit.initSDK(application);
        this.hefanLive = isdKinit.createHefanLive();
        this.hefanPlayer = isdKinit.createHefanPlayer();
    }

    /**
     * 盒饭推流断代理
     * */
    public IHefanLive getHefanLive() {
        return hefanLive;
    }

    /**
     * 盒饭拉流端代理
     * */
    public IHefanPlayer getHefanPlayer() {
        return hefanPlayer;
    }

}
