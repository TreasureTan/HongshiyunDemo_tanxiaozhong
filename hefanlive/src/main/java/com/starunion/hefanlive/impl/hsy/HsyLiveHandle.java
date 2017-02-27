package com.starunion.hefanlive.impl.hsy;

import android.os.Handler;
import android.os.Message;
import com.starunion.hefanlive.proxy.HefanLiveProxy;

/**
 * Created by saga_ios on 16/12/7.
 */
public class HsyLiveHandle extends Handler {
    private HsyLiveImpl hsyLive  = (HsyLiveImpl) HefanLiveProxy.getInstance().getHefanLive();

    public static final int MSG_OEPN_PREVIEW = 0; 	//打开预览
    public static final int MSG_CLOSE_PREVIEW = 1;	//关闭预览
//    public static final int MSG_CAMERA_FACING = 2;	//设置摄像头方向
//    public static final int MSG_CAMERA_TORCH = 3;	//打开关闭闪光灯
    public static final int MSG_RECORD_START = 5;	//开始推流
    public static final int MSG_RECORD_STOP = 6;	//停止推流
    public static final int MSG_RECORD_PAUSE = 7;	//暂停推流
    public static final int MSG_RECORD_RESUME = 8;	//恢复推流
    public static final int MSG_BLINK_TEXT = 9;		//提示信息闪烁
    public static final int MSG_TOAST = 10;			//消息提示
    public static final int MSG_RECORD_FORCERECONNECT = 11;	//强制重连
    public static final int MSG_RECORD_CHECK_NET = 12;		//检测网络状态
    public static final int MSG_RECORD_PUSH_FAIL = 13;  //推流失败
    public static final int MSG_RECORD_WEAK_NETWORK = 14;   //弱网抖动超过两次
    public static final int MSG_RECORD_ERROR_COUNT =  15;  //统计error;


    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_OEPN_PREVIEW :
                hsyLive.openPreview();
                hsyLive.onStartRecord();
                break;
            case MSG_CLOSE_PREVIEW :
                hsyLive.closePreview();
                break;
//            case MSG_CAMERA_FACING :
//                break;
//            case MSG_CAMERA_TORCH :
//                break;
            case MSG_RECORD_START :
                this.removeMessages(MSG_RECORD_START);
                hsyLive.firstStartLive();
                break;
            case MSG_RECORD_STOP :
                this.removeMessages(MSG_RECORD_STOP);
                hsyLive.onStopRecord();
                break;
            case MSG_RECORD_PAUSE :
                this.removeMessages(MSG_RECORD_PAUSE);
                hsyLive.pauseRecord();
                break;
            case MSG_RECORD_RESUME :
                this.removeMessages(MSG_RECORD_RESUME);
                hsyLive.resumeRecord();
                break;
            case MSG_BLINK_TEXT :
                break;
            case MSG_TOAST:
                    break;
            case MSG_RECORD_FORCERECONNECT:
                this.removeMessages(MSG_RECORD_FORCERECONNECT);
                hsyLive.getFoceReconnect();
                break;
            case MSG_RECORD_CHECK_NET:
                this.removeMessages(MSG_RECORD_CHECK_NET);
                break;
            case MSG_RECORD_PUSH_FAIL:
                this.removeMessages(MSG_RECORD_CHECK_NET);
                hsyLive.getCheckNET();
                break;
            case MSG_RECORD_WEAK_NETWORK:
                this.removeMessages(MSG_RECORD_WEAK_NETWORK);
                hsyLive.getWeakNet();
                break;
            case MSG_RECORD_ERROR_COUNT:
                this.removeMessages(MSG_RECORD_ERROR_COUNT);
                hsyLive.getErrorCount();
            default :
                throw new RuntimeException("Unknown message " + msg.what);
        }
    }
}
