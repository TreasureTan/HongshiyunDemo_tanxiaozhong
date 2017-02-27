package com.starunion.hefanlive.impl;

import android.app.Application;
import com.starunion.hefanlive.ihefan.IHefanLive;
import com.starunion.hefanlive.ihefan.IHefanPlayer;
import com.starunion.hefanlive.ihefan.ISDKinit;

/**
 * Created by saga_ios on 16/12/6.
 */
public class HefanSDKinit implements ISDKinit {



    @Override
    public void initSDK(Application application) {

    }

    @Override
    public IHefanLive createHefanLive() {
        return null;
    }

    @Override
    public IHefanPlayer createHefanPlayer() {
        return null;
    }
}
