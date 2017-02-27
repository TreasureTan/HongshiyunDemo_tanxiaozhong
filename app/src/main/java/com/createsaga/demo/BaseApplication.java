package com.createsaga.demo;

import android.app.Application;
import com.starunion.hefanlive.proxy.HefanLiveProxy;

/**
 * Created by saga_ios on 16/12/6.
 */
public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        HefanLiveProxy.getInstance().initHefanLive(this);
        super.onCreate();

    }


}
