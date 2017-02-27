package com.starunion.hefanlive.view;

import android.content.Context;
import android.util.AttributeSet;

import com.arcvideo.camerarecorder.CameraSurfaceView;

/**
 * Created by saga_ios on 16/12/6.
 * 根据SDK推流端使用的View定父类
 */
public class HefanLiveSurface extends CameraSurfaceView {
    public HefanLiveSurface(Context context) {
        super(context);
    }

    public HefanLiveSurface(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }
}
