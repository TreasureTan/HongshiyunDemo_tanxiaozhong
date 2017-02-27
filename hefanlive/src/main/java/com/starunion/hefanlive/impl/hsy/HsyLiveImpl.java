package com.starunion.hefanlive.impl.hsy;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.arcvideo.camerarecorder.CameraConfig;
import com.arcvideo.camerarecorder.CameraRecord;
import com.arcvideo.camerarecorder.CameraSurfaceView;
import com.arcvideo.camerarecorder.CameraTypes;
import com.arcvideo.camerarecorder.NotifyListener;
import com.starunion.hefanlive.datasource.IHefanDataSource;
import com.starunion.hefanlive.ihefan.IHefanLive;
import com.starunion.hefanlive.utils.CustomCfg;
import com.starunion.hefanlive.utils.NetUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by saga_ios on 16/12/6.
 */
public class HsyLiveImpl implements IHefanLive,NotifyListener {
    private static final String TAG = "HSYVideoRecord";
    private HsyLiveHandle mHandler;
    /**
     * Camera Record
     */
    private CameraRecord mCameraRecorder;				//CameraRecord引擎类
    private CameraSurfaceView mSurfaceView;				//Camera SurfaceView
    private CameraConfig mCameraConfig;					//Camera 宽高 PFS 等参数
    private boolean mFalshSate = false;							//闪光灯状态
    private int mCameraFacing = CameraTypes.CAMERA_FACING_FRONT;//摄像头前置后置状态

    /**
     * 默认Camera Record参数
     */
    private static final int VIDEO_WIDTH = 1280; // 1280; // dimensions for 720p
    private static final int VIDEO_HEIGHT = 720; // 720;
    private static final int DESIRED_PREVIEW_FPS = 25;
    //调节码率
    private static final int DESIRED_PREVIEW_RATE = 1000 * 1000;   // 600*1000

    /**
     * 状态变量
     */
    private boolean mIsOpenPreview = true;						//是否打开预览
    private boolean mIsRecording = false;						//是否正在录制推流
    private boolean mNeedResumeRecording = false;				//是否需要恢复录制推流

    private List<Camera.Size> mSupportSizes;					//支持的视频分辨率列表
    private Camera.Size mOptimalPreviewSize;					//合适的分辨率

    private int mWindowWidth = 0;								//视频宽高
    private int mWindowHeight = 0;
    private int mOrientation = 0;								//屏幕旋转角度


    HsyLiveHandle hsyLiveHandle;
    /**
     * Face process mode:
     * 0 ----- ARCVIDEO_FACEPROCESS_MODEL_NONE
     * 1 ----- ARCVIDEO_FACEPROCESS_MODEL_FACEOUTLINE
     * 2 ----- ARCVIDEO_FACEPROCESS_MODEL_FACEBEAUTY
     */
    private int mFaceProcessMode = CameraTypes.ARCVIDEO_FACEPROCESS_MODEL_FACEBEAUTY;
    private int mFaceClose = CameraTypes.ARCVIDEO_FACEPROCESS_MODEL_NONE;
    private int mFaceBrightLevel = 0;
    private int mFaceDermabrasionLevel = 0;

    private Activity activity;

    //推流的地址
    private String pushUrl = "";
    private String ALipushUrl = "";
    private String HSYpushUrl = "";
    /* 检测网络抖动 */
    private  int check_netShake = 0 ;
    private boolean mIsClosing = false;
    private boolean mIsPause = false;
    private boolean mIsShowing = false;
    private int mAirState = 0;


    private boolean mFaceBeautyEnable = false;
    private boolean mFaceDetectEnable = false;

    private boolean mForceToReconnectServer = true;
    private int     mMaxReconnectCount = 3;
    private int     mForceToReconnectCount = 0;
    /* 是否推流 */
    private boolean isPushLive  = false;
    private static final int REQUEST_CODE = 0;//请求码

    //private CheckPermission checkPermission;//检测权限器
    private boolean mIsPermissionGanted = false;					//是否授权成功

    private int errorCount = 0;
    private boolean netError = false;  //是否断网了
    private boolean isRecording = false;

    private IHefanDataSource dataSource;

    @Override
    public void initLive(Activity activity, IHefanDataSource dataSource) {
        this.activity = activity;
        this.dataSource = dataSource;
        this.mSurfaceView = dataSource.provideLiveSurface();
        ALipushUrl  = dataSource.provideMainPassageway();
        HSYpushUrl =dataSource.provideBackUpPassageway();
        pushUrl = HSYpushUrl;
        Log.e("pushUrl","pushUrl=="+pushUrl);
        mHandler = new HsyLiveHandle();
        mWindowWidth = activity.getWindowManager().getDefaultDisplay().getWidth();
        mWindowHeight = activity.getWindowManager().getDefaultDisplay().getHeight();
//        mWindowWidth = mSurfaceView.getWidth();
//        mWindowHeight = mSurfaceView.getHeight();
        copyPlginIni();
        initCamera();
    }


    /**
     * 初始化推流相关部分
     */
    private void initCamera() {
        //初始化camera record
        mCameraRecorder = new CameraRecord(activity);
        mCameraConfig = new CameraConfig(DESIRED_PREVIEW_FPS, DESIRED_PREVIEW_RATE, VIDEO_WIDTH, VIDEO_HEIGHT);

        //得到可用的预览分辨率列表
        mSupportSizes = mCameraRecorder.getSupportPreviewSize();
        if(mSupportSizes != null){
            //得到可用合适的预览分辨率
            mOptimalPreviewSize = getOptimalPreviewSize(mSupportSizes, VIDEO_WIDTH, VIDEO_HEIGHT);
            //设置到CameraConfig中
            mCameraConfig.setVideoWidth(mOptimalPreviewSize.width);
            mCameraConfig.setVideoHeight(mOptimalPreviewSize.height);
            // TODO: 分辨率调整
            mCameraConfig.setVideoBitRate(DESIRED_PREVIEW_RATE);
        } else {
            mCameraConfig.setVideoWidth(VIDEO_WIDTH);
            mCameraConfig.setVideoHeight(VIDEO_HEIGHT);
            mCameraConfig.setVideoBitRate(DESIRED_PREVIEW_RATE);
        }

        //设置摄像头前置后置（CameraTypes.CAMERA_FACING_FRONT：前置，CameraTypes.CAMERA_FACING_BACK：后置）
        mCameraRecorder.SetCameraFacingType(CameraTypes.CAMERA_FACING_FRONT);

        mSurfaceView.init(activity, mCameraRecorder.getCameraManager(), mCameraConfig, mWindowWidth, mWindowHeight);
        //设置surface view 显示模式 （CameraTypes.FILL_IN ： 显示效果为保证视频所有内容都完全显示在屏幕中且不会拉伸变形,如果视频宽高比与设备屏幕宽高比不一致则显示不会完全全屏
        //							CameraTypes.FILL_OUT : 显示效果为保证视频能够全屏显示且不会拉伸变形,如果视频宽高比与设备屏幕宽高比不一致则显示全屏，同时会有部分视频内容会超出屏幕范围从而被截掉;
        //							CameraTypes.FULL_SCREEN:显示效果为保证视频所有内容都完全显示在屏幕中全屏显示, 如果视频宽高比与设备屏幕宽高比不一致则显示会被拉伸变形)
        mCameraRecorder.setCameraDisplayMode(CameraTypes.FILL_IN);

        mCameraRecorder.setCameraSurfaceView(mSurfaceView);

        //设置推流监听回调
        mCameraRecorder.setNotifyListener(this);
        //是否允许自动聚焦 ture为开启自动对焦，false为开启手动对焦。如果不设置，默认为自动对焦。
        mCameraRecorder.enableAutoFoucs(true);
        //是否允许缩放 ture为开启缩放，false为关闭缩放。 如果不设置，默认为自动开启。
        mCameraRecorder.enableZoom(true);
        //Log.e("相机翻转","manager.getCameraStatus()=="+manager.getCameraStatus());
        //manager.setCameraStatus(StatusManager.front);  //相机翻转

        boolean b = mCameraRecorder.supportFaceProcess();
        Log.i("eternal","是否支持美颜=="+b);
        Log.i("eternal","getFaceSkinSoftenLevel()=="+mCameraRecorder.getFaceSkinSoftenLevel());

    }

    public Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }


    @Override
    public void openPreview() {
        Log.e("mSurfaceView","mSurfaceView="+mSurfaceView);
        mSurfaceView.setVisibility(View.VISIBLE);
        if (CameraTypes.CAMERA_INIT_SUCCESS == mCameraRecorder.openCamera(mCameraConfig)) {
            Log.e(TAG, "openCamera  success");
            mIsOpenPreview = true;
            mCameraRecorder.setCameraDisplayMode(CameraTypes.FILL_IN);

        } else {
            Log.d(TAG, "openCamera  failed");
        }
//        initCamera();
    }

    @Override
    public void closePreview() {
        Log.d(TAG, "clickClosePreview");
        mSurfaceView.setVisibility(View.GONE);
        mCameraRecorder.closePreview();
        mIsOpenPreview = false;
    }

    public void firstStartLive(){
         mHandler.removeMessages(HsyLiveHandle.MSG_RECORD_START);
        if (NetUtil.isWiFiActive(activity)) {
            Log.i("切流","mRtmpAdress=="+pushUrl);
            startRecord(pushUrl);
        } else if (NetUtil.isMobileActive(activity)) {
            new AlertDialog.Builder(activity).setTitle("提示").setMessage("您正在使用的是运营商网络，继续将产生流量费用")
                    .setPositiveButton("取消", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int arg1) {
                            dialog.dismiss();
                        }
                    }).setNegativeButton("确认", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int arg1) {
                    dialog.dismiss();
                    startRecord(pushUrl);
                }
            }).show();
        } else {
            new AlertDialog.Builder(activity).setTitle("提示").setMessage("推流时检测到网络未连接，请检查网络设置。")
                    .setPositiveButton("确认", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int arg1) {
                            dialog.dismiss();
                        }
                    }).show();
        }
    }

    @Override
    public void onStartRecord() {
        if (mCameraRecorder.isCameraSurfaceCreated()) {
            HsyLiveImpl.this.firstStartLive();
        } else {
            mHandler.sendEmptyMessageDelayed(HsyLiveHandle.MSG_RECORD_START, 300);
        }
    }

    @Override
    public void onStopRecord() {
        stopRecord();
    }

    @Override
    public void flashLight(boolean isOpen) {
        if (null == activity)
            return;

        if (isOpen) {
            mCameraRecorder.setTorchState(true);
        } else {
            mCameraRecorder.setTorchState(false);
        }
    }

    /**
     * Handles onClick for "CameraFacing" button.
     */
    public void onCameraFacing() {
        if (mSurfaceView.getVisibility() == View.INVISIBLE) {
            Toast.makeText(activity, "请先打开摄像头！", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mCameraFacing == CameraTypes.CAMERA_FACING_BACK) {
            mCameraFacing = CameraTypes.CAMERA_FACING_FRONT;
        } else {
            mCameraFacing = CameraTypes.CAMERA_FACING_BACK;
        }
        Log.d(TAG, "clickCameraFacing " + mCameraFacing);
        mCameraRecorder.switchCameraFacing(mCameraFacing);
    }

    /**
     * Handles onClick for "CameraTorch" button.
     */
    public void onCameraTorch() {
        mFalshSate = !mFalshSate;
        Log.d(TAG, "clickCameraTorch " + mFalshSate);

        if (!mCameraRecorder.setTorchState(mFalshSate)) {
            Toast.makeText(activity, "前置摄像头禁止开关闪关灯！", Toast.LENGTH_SHORT).show();
        }
    }

   public void getWeakNet(){
        if (check_netShake>3){
            changePushUrl();
        }
        check_netShake = 0 ;
    }
   //开启直播
   private void startLivePlayer() {
        mSurfaceView.setCameraConfig(mCameraConfig);
        Log.d("eternal", "openCamera  mCameraConfig.width" + mCameraConfig.getVideoWidth() + "; mCameraConfig.height" + mCameraConfig.getVideoHeight());
        if (CameraTypes.CAMERA_INIT_SUCCESS == mCameraRecorder.openCamera(mCameraConfig)) {
            Log.i("eternal", "openCamera  success");
            mHandler.sendEmptyMessageDelayed(HsyLiveHandle.MSG_OEPN_PREVIEW,1000);
            mNeedResumeRecording  = false;
        } else {
            Log.i("eternal", "openCamera  failed");
        }

        if (mNeedResumeRecording) {
            mHandler.sendEmptyMessageDelayed(HsyLiveHandle.MSG_RECORD_START, 2000);
        }
        if (mSurfaceView.isCameraSurfaceCreated()){
            Log.i("eternal","isRecording= true;");
            isRecording= true;
        }
    }

    @Override
    public boolean onInfo(int i, int i1) {
        Log.i("startRecord", "onInfo mainInfoId = " + i + ", subInfoId = " + i1);
        String infoString = "";
        switch (i) {
            case CameraTypes.MEDIA_CONNECTED:  //表示推流服务器连接成功
                Log.i(TAG, "onInfo: ifno type is MEDIA_CONNECTED, value = " + i1);
                //  infoString = "推流服务器连接成功";
                Log.e("推流","推流服务器连接成功");
                mForceToReconnectCount = 0;
                errorCount = 0;
                if (isPushLive){
                    notifyLiveStart();
                }
                break;
            case CameraTypes.MEDIA_WARN_DELAY: //表示推流服务器当前buffer池数据的过多，发送跟存留的差距已经超出3.5秒导致抛异常给客户端，由客户端决定如何处理。
                Log.i(TAG, "onInfo: ifno type is MEDIA_WARN_DELAY, value = " + i1);
                infoString = "网络抖动";
                check_netShake++;
                Log.e("网络抖动","check_netShake=="+check_netShake);
                if (check_netShake == 1){
                    mHandler.sendEmptyMessageDelayed(HsyLiveHandle.MSG_RECORD_WEAK_NETWORK,10*1000);
                }
                break;
            case CameraTypes.MEDIA_ERR_AUTOCONNECTING: //表示推流服务器正在自动重连
                Log.i(TAG, "onInfo: ifno type is MEDIA_WARN_DELAY, value = " + i1);
                //  infoString = "推流服务器正在自动重连";
                break;
            default:
                break;
        }
        return false;
    }

    private void notifyLiveStart() {

    }

    @Override
    public boolean onError(int mainErrorId, int subErrorId) {
        Log.v("eternal", "onError mainErrorId = " + mainErrorId + ", subErrorId = " + subErrorId);
        String errorString = "";
        switch (mainErrorId) {
            case CameraTypes.MEDIA_ERR_SENDDATA :				//发送数据错误
                Log.e("eternal", "onError: error type is MEDIA_ERR_SENDDATA, value = " + subErrorId);
                // errorString = "发送数据错误";
                break;
            case CameraTypes.MEDIA_ERR_CONNECTERROR:			//连接错误
                Log.e("eternal", "onError: error type is MEDIA_ERR_CONNECTERROR, value = " + subErrorId);
                //  errorString = "连接错误";
                break;
            case CameraTypes.MEDIA_ERR_ENCODEVIDEO:				//视频编码错误
                Log.e("eternal", "onError: error type is MEDIA_ERR_ENCODEVIDEO, value = " + subErrorId);
                //  errorString = "视频编码错误";
                break;
            case CameraTypes.MEDIA_ERR_ENCODEAUDIO:				//音频编码错误
                Log.e("eternal", "onError: error type is MEDIA_ERR_ENCODEAUDIO, value = " + subErrorId);
                // errorString = "音频编码错误";
                break;
            case CameraTypes.MEDIA_ERR_STREAMING_N_TIMEOUT:		//连接超时
                Log.e("eternal", "onError: error type is MEDIA_ERR_STREAMING_N_TIMEOUT, value = " + subErrorId);
                // errorString = "连接超时";
                break;
            case CameraTypes.MEDIA_ERR_STREAMING_N_CONNFAIL:	//连接失败
                Log.e("eternal", "onError: error type is MEDIA_ERR_STREAMING_N_CONNFAIL, value = " + subErrorId);
                //   errorString = "连接失败";
                break;
            case CameraTypes.MEDIA_ERR_STREAMING_N_RECVTIMEOUT:	//接收数据超时
                Log.e("eternal", "onError: error type is MEDIA_ERR_STREAMING_N_RECVTIMEOUT, value = " + subErrorId);
                //  errorString = "接收数据超时";
                break;
            case CameraTypes.MEDIA_ERR_STREAMING_N_RECVFAIL:	//接收数据失败
                Log.e("eternal", "onError: error type is MEDIA_ERR_STREAMING_N_RECVFAIL, value = " + subErrorId);
                //    errorString = "接收数据失败";
                break;
            case CameraTypes.MEDIA_ERR_STREAMING_N_SENDTIMEOUT:	//发送数据超时
                Log.e("eternal", "onError: error type is MEDIA_ERR_STREAMING_N_SENDTIMEOUT, value = " + subErrorId);
                //   errorString = "发送数据超时";
                break;
            case CameraTypes.MEDIA_ERR_STREAMING_N_SENDFAIL:	//发送数据失败
                Log.e("eternal", "onError: error type is MEDIA_ERR_STREAMING_N_SENDFAIL, value = " + subErrorId);
                //   errorString = "发送数据失败";
                break;
            default://其他未知的错误信息
                Log.e("eternal", "onError: what the Fxxxx!! error is " + mainErrorId + "? code is " + subErrorId);
                //    errorString = "其他未知错误";
                break;
        }

        //根据错误类型类使用者可以根据自己的需求进行重连或者关闭录制推流
        switch (mainErrorId){
            case CameraTypes.MEDIA_ERR_SENDDATA :				//发送数据错误
            case CameraTypes.MEDIA_ERR_STREAMING_N_TIMEOUT:		//连接超时
            case CameraTypes.MEDIA_ERR_STREAMING_N_CONNFAIL:	//连接失败
            case CameraTypes.MEDIA_ERR_STREAMING_N_RECVTIMEOUT:	//接收数据超时
            case CameraTypes.MEDIA_ERR_STREAMING_N_RECVFAIL:	//接收数据失败
            case CameraTypes.MEDIA_ERR_STREAMING_N_SENDTIMEOUT:	//发送数据超时
            case CameraTypes.MEDIA_ERR_STREAMING_N_SENDFAIL:	//发送数据失败
            {
                if (mForceToReconnectServer) {
                    //网络相关错误可以根据需要进行强制重连服务器，
                    //该操作其它资源不需要停止和释放，只进行服务器重连
                    errorString += ": " + mainErrorId + ", 正在强制重连！";
                    mHandler.sendEmptyMessageDelayed(HsyLiveHandle.MSG_RECORD_FORCERECONNECT, 2000);

                }else {
                    //停止录制会释放录制相关的所有资源，如果要重启录制需重新初始化
                    if (mIsRecording) {
                        // errorString += ": " + mainErrorId + ",推流将停止!";
                        Log.e("eternal","停止推流 ");
                        mHandler.sendEmptyMessage(HsyLiveHandle.MSG_RECORD_STOP);
                        mHandler.sendEmptyMessage(HsyLiveHandle.MSG_RECORD_PUSH_FAIL);
                        mIsRecording = false;
                    }

                }
                mHandler.sendEmptyMessage(HsyLiveHandle.MSG_RECORD_ERROR_COUNT);
                break;
            }
            case CameraTypes.MEDIA_ERR_CONNECTERROR:			//连接错误
                Log.e("重连","重连重连重连重连重连mIsRecording== "+mIsRecording);
                Log.e("重连","netError==="+netError);
                Log.e("重连","errorCount=="+errorCount);
                errorCount++;
//                if (!liveController.isFinishLive()&&errorCount<=30){
                    if (netError){
                        if (mIsRecording){
                            mHandler.sendEmptyMessage(HsyLiveHandle.MSG_RECORD_FORCERECONNECT);
                        }else {
                            startRecord(null);
                        }
                    } else {
                        mHandler.sendEmptyMessage(HsyLiveHandle.MSG_RECORD_STOP);
                        Log.e("eternal","停止推流");
                        mHandler.sendEmptyMessage(HsyLiveHandle.MSG_RECORD_PUSH_FAIL);
                    }
//                }else {
////                    ((LiveActivity)getMyActivity()).cancleLoading();
//                    mHandler.sendEmptyMessage(HsyLiveHandle.MSG_RECORD_STOP);
////                    EventBus.getDefault().post(new ErrorCountJump(true));
//
//                }

                break;
            case CameraTypes.MEDIA_ERR_ENCODEVIDEO:				//视频编码错误
            case CameraTypes.MEDIA_ERR_ENCODEAUDIO:				//音频编码错误
            default://其他未知的错误信息
                //停止录制会释放录制相关的所有资源，如果要重启录制需重新初始化
                Log.i("eternal","mIsRecording=="+mIsRecording);
                Log.e("eternal","重新连接 ");
                // errorString += ": " + mainErrorId + ",重新连接!";
                mHandler.sendEmptyMessage(HsyLiveHandle.MSG_RECORD_STOP);
                Log.e("eternal","停止推流");
                mHandler.sendEmptyMessageDelayed(HsyLiveHandle.MSG_RECORD_PUSH_FAIL, 2000);
                mHandler.sendEmptyMessage(HsyLiveHandle.MSG_RECORD_ERROR_COUNT);
                break;
        }
        //toast
        Message msg = new Message();
        msg.what = HsyLiveHandle.MSG_TOAST;
        msg.obj = errorString;
        mHandler.sendMessage(msg);
        return false;
    }

    private BroadcastReceiver m_wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mIsClosing)
                return;
            if (mIsShowing)
                return;
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo wifiInfo = (NetworkInfo) connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                NetworkInfo mobileInfo = (NetworkInfo) connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                if (mobileInfo == null) {
                    if (wifiInfo != null && NetworkInfo.State.DISCONNECTED == wifiInfo.getState()) {
                        // WIFI disconnected
                        if (!mIsPause) {
                            mIsPause = false;
                            mIsShowing = true;
                            new AlertDialog.Builder(activity)
                                    .setTitle("提示")
                                    .setMessage("网络未连接，请检查网络设置。")
                                    .setPositiveButton("确认", new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int arg1) {
                                            dialog.dismiss();
                                            mIsShowing = false;
                                            if (mIsRecording ) {
                                                stopRecord();
                                            }
                                        }
                                    }).show();
                        }
                    }
                } else {
                    if (wifiInfo != null) {
                        if (NetworkInfo.State.CONNECTED == wifiInfo.getState() && NetworkInfo.State.CONNECTED != mobileInfo.getState()) {
                            // WIFI connected
                            netError = false;
                        } else if (NetworkInfo.State.CONNECTED != wifiInfo.getState() && NetworkInfo.State.CONNECTED == mobileInfo.getState()) {
                            // 2G/3G/4G connected
                            netError = false;
                            if (!mIsPause) {
                                mIsPause = false;
                                if (mIsRecording ) {
                                    new AlertDialog.Builder(activity)
                                            .setTitle("提示")
                                            .setMessage("您正在使用的是运营商网络，继续将产生流量费用")
                                            .setPositiveButton("取消", new DialogInterface.OnClickListener() {

                                                @Override
                                                public void onClick(DialogInterface dialog, int arg1) {
                                                    dialog.dismiss();
                                                    mIsShowing = false;
                                                    stopRecord();
                                                }
                                            }).setNegativeButton("继续", new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int arg1) {
                                            dialog.dismiss();
                                            mIsShowing = false;
                                        }
                                    }).show();
                                }
                            }
                        } else if (NetworkInfo.State.CONNECTED != wifiInfo.getState() && NetworkInfo.State.CONNECTED != mobileInfo.getState()) {
                            Log.e("重连","断网");
                            netError = true;
                            if (!mIsPause) {
                                mIsPause = false;
                                mIsShowing = true;

                                Toast.makeText(activity,"网络未连接，请检查网络设置。",Toast.LENGTH_SHORT).show();

                            }
                        }
                    }
                }
            } else if (action.equals("android.intent.action.SERVICE_STATE")) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    mAirState = bundle.getInt("state");

                    Log.e(TAG, "飞行模式状态 1为开启状态，0为关闭状态 airState==" + mAirState);
                    switch (mAirState) {
                        case 0 : // air mode close success
                            Log.i(TAG, "close state  airState=" + mAirState);

                            break;
                        case 1 : // air mode opening
                            Log.i(TAG, "opening airState=" + mAirState);
                            break;
                        case 3 : // air mode open success
                            Log.i(TAG, "open state  airState=" + mAirState);
                            if (!mIsPause) {
                                mIsPause = false;
                                mIsShowing = true;
                                new AlertDialog.Builder(activity)
                                        .setTitle("提示")
                                        .setMessage("网络未连接，请检查网络设置。")
                                        .setPositiveButton("确认", new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int arg1) {
                                                dialog.dismiss();
                                                mIsShowing = false;
                                            }
                                        }).show();
                            }
                            netError = true;
                            break;
                    }
                }
            }
        }
    };

    private void registerNetReceiver() {
        IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        wifiFilter.addAction("android.intent.action.SERVICE_STATE");
        activity.registerReceiver(m_wifiReceiver, wifiFilter);
    }

    private void unRegisterNetReceiver() {
        activity.unregisterReceiver(m_wifiReceiver);
    }

    public void getErrorCount(){
        errorCount++;
        Log.e("统计错误","errorCount="+errorCount);
        if (errorCount>30){
//                        ((LiveActivity)getMyActivity()).cancleLoading();
            mHandler.sendEmptyMessage(HsyLiveHandle.MSG_RECORD_STOP);
//                        EventBus.getDefault().post(new ErrorCountJump(true));
        }
    }


    /**
     * resume record
     */
    public void resumeRecord() {
        mNeedResumeRecording = false;
        mCameraRecorder.resumeRecord();
    }
    /**
     * pause record
     */
    public void pauseRecord() {
        mNeedResumeRecording = true;
        mCameraRecorder.pauseRecord();
    }

    /**
     * Force reConnect
     */
    private void forceReconnect(){
        if (mCameraRecorder != null) {
            Log.d(TAG, "forceReconnect()");
            mCameraRecorder.forceReconnect();
        }
    }
    //强制重连
    public void getFoceReconnect(){

        //如果退出直播间。关闭强制重连机制
//        if (mLiveController!=null){
//            if (mLiveController.isFinishLive()){
//                mHandler.sendEmptyMessage(HsyLiveHandle.MSG_RECORD_STOP);
//                return;
//            }
//        }

        if (this.mForceToReconnectCount < this.mMaxReconnectCount) {
            this.forceReconnect();
            this.mForceToReconnectCount++;
        }
    }

    public void getCheckNET(){
        if (!NetUtil.isWiFiActive(activity) && !NetUtil.isMobileActive(activity)) {
            // Toast.makeText(getMyActivity(), "网络异常，请检查网络设置！", Toast.LENGTH_SHORT).show();
            Log.e("网络错了","网络挂了" );
            HsyLiveImpl.this.stopRecord();
        }
    }


    //更换推流线路
    private void changePushUrl() {
        stopRecord();
        if (pushUrl.equals(ALipushUrl)){
            pushUrl = HSYpushUrl;
        }else if (pushUrl.equals(HSYpushUrl)){
            pushUrl = ALipushUrl ;
        }
        Log.e("切流","push="+pushUrl);
        startRecord(null);
    }

    /**
     * 开始推流
     *
     * @param mRtmpAdress
     * */
    private void startRecord(String mRtmpAdress) {
        int res = 0;
        LockScreenOrientation(); //锁定屏幕方向
        if (mSurfaceView.getVisibility() == View.INVISIBLE) {
            Toast.makeText(activity, "请先打开摄像头！", Toast.LENGTH_SHORT).show();
            return;
        }
        mCameraRecorder.initEncoder();
        if (isScreenLandScape()) {
            res = mCameraRecorder.setClipInfo(CameraTypes.RECORD_FILE_TYPE_TCP, 0, mCameraConfig.getVideoWidth(),
                    mCameraConfig.getVideoHeight(), 0, 0, true, true);
            if (CameraTypes.MEDIA_ERR_NONE != res) {
                 HsyLiveShowError.showError(res);
                mCameraRecorder.uninitEncoder();
                //UnlockScreenOrientation();
                return;
            }
            res = mCameraRecorder.setAudioInfo(CameraTypes.RECORD_CODEC_TYPE_AAC, 0, 1, 16, 0, 44100, 64000);
            if (CameraTypes.MEDIA_ERR_NONE != res) {
                 HsyLiveShowError.showError(res);
                mCameraRecorder.uninitEncoder();
                //  UnlockScreenOrientation();
                return;
            }
            res = mCameraRecorder.setVideoInfo(CameraTypes.RECORD_CODEC_TYPE_H264, 0, mCameraConfig.getVideoWidth(),
                    mCameraConfig.getVideoHeight(), mCameraConfig.getVideoFrameRate(), mCameraConfig.getVideoBitRate(),
                    0);
            if (CameraTypes.MEDIA_ERR_NONE != res) {
                 HsyLiveShowError.showError(res);
                mCameraRecorder.uninitEncoder();
                // UnlockScreenOrientation();
                return;
            }

        } else {
            res = mCameraRecorder.setClipInfo(CameraTypes.RECORD_FILE_TYPE_TCP, 0, mCameraConfig.getVideoHeight(),
                    mCameraConfig.getVideoWidth(), 0, 0, true, true);
            if (CameraTypes.MEDIA_ERR_NONE != res) {
                 HsyLiveShowError.showError(res);
                mCameraRecorder.uninitEncoder();
                // UnlockScreenOrientation();
                return;
            }
            res = mCameraRecorder.setAudioInfo(CameraTypes.RECORD_CODEC_TYPE_AAC, 0, 1, 16, 0, 44100, 64000);
            if (CameraTypes.MEDIA_ERR_NONE != res) {
                 HsyLiveShowError.showError(res);
                mCameraRecorder.uninitEncoder();
                //   UnlockScreenOrientation();
                return;
            }
            res = mCameraRecorder.setVideoInfo(CameraTypes.RECORD_CODEC_TYPE_H264, 0, mCameraConfig.getVideoHeight(),
                    mCameraConfig.getVideoWidth(), mCameraConfig.getVideoFrameRate(), mCameraConfig.getVideoBitRate(),
                    0);
            if (CameraTypes.MEDIA_ERR_NONE != res) {
                 HsyLiveShowError.showError(res);

                mCameraRecorder.uninitEncoder();
                //  UnlockScreenOrientation();
                return;
            }
        }
        res = mCameraRecorder.initRecorder(mRtmpAdress);
        if (CameraTypes.MEDIA_ERR_NONE != res) {
             HsyLiveShowError.showError(res);
            mCameraRecorder.uninitEncoder();
            // UnlockScreenOrientation();
            return;
        }
        mCameraRecorder.enableAutoConnect();//允许自动重连
        Log.e("push","允许自动重连3次");
        mCameraRecorder.lockOrientation();//锁方向
        Log.e("hsytest","准备录制");
        res = mCameraRecorder.startRecord();//开始录制
        Log.e("hsytest","开始录制录制res=="+res);

        if (CameraTypes.MEDIA_ERR_NONE != res) {
             HsyLiveShowError.showError(res);
            Log.e("hsytest"," mCameraRecorder.uninitEncoder()");
            mCameraRecorder.uninitEncoder();
            // UnlockScreenOrientation();
            return;
        }
        mIsRecording = true;
        mForceToReconnectServer = true;
        mForceToReconnectCount = 0;
    }

    /**
     * 锁定横竖屏
     * */
    private void LockScreenOrientation(){
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    }

    /**
     * 结束推流
     *
     * @param @null
     * */
    private void stopRecord() {
        if (!mIsRecording) {
            return;
        }
        Log.e("统计错误", "RecordStop");
        mCameraRecorder.stopRecord();
        mCameraRecorder.uninitEncoder();
        mCameraRecorder.unlockOrientation();
        mIsRecording = false;
        mForceToReconnectServer = false;
        mHandler.removeMessages(HsyLiveHandle.MSG_RECORD_FORCERECONNECT);
    }

    /**
     * 判断是否室横竖屏
     *
     * @return boolean
     * */
    private boolean isScreenLandScape() {
        Configuration mConfiguration = activity.getResources().getConfiguration(); // 获取设置的配置信息
        int ori = mConfiguration.orientation; // 获取屏幕方向

        if (ori == mConfiguration.ORIENTATION_LANDSCAPE) {
            // 横屏
            return true;
        } else if (ori == mConfiguration.ORIENTATION_PORTRAIT) {
            // 竖屏
            return false;
        }
        return false;
    }

    @Override
    public void brightnessSwitch(boolean flag, int length) {
        Log.e("eternal","美颜亮度=="+length);
        if (flag){
            mCameraRecorder.setFaceProcessMode(CameraTypes.ARCVIDEO_FACEPROCESS_MODEL_FACEBEAUTY,null);
            mCameraRecorder.setFaceBrightLevel(length);
        }else {
            mCameraRecorder.setFaceProcessMode(CameraTypes.ARCVIDEO_FACEPROCESS_MODEL_NONE,null);
            mCameraRecorder.setFaceBrightLevel(0);
        }
    }

    @Override
    public void buffingSwitch(boolean flag, int length) {
        Log.e("eternal","美颜磨皮=="+length);
        if (flag){
            mCameraRecorder.setFaceProcessMode(CameraTypes.ARCVIDEO_FACEPROCESS_MODEL_FACEBEAUTY,null);
            mCameraRecorder.setFaceSkinSoftenLevel(length);
        }else {
            mCameraRecorder.setFaceProcessMode(CameraTypes.ARCVIDEO_FACEPROCESS_MODEL_NONE,null);
            mCameraRecorder.setFaceSkinSoftenLevel(0);
        }
    }

    @Override
    public void destroyLive() {
        unRegisterNetReceiver();
        mIsClosing = true;
        mCameraRecorder.stopRecord();
        mCameraRecorder.releaseRecord();
        mNeedResumeRecording =false;
        //liveController.setCaremastatus(1000);
        isPushLive= false;
        Log.e("onDestroy","onDestroy已经销毁");
        //EventBus.getDefault().unregister(this);
        //liveController.onLiveEnd();
    }

    @Override
    public void onPauseRecord() {
        if (mIsRecording) {
            stopRecord();
            mNeedResumeRecording = true;
            mIsRecording =false;
        }

        if (CustomCfg.mFaceProcessLibType == 1
                && mCameraRecorder.supportFaceProcess()) {
            mFaceDetectEnable = false;
            if (mCameraRecorder != null) {
                mFaceProcessMode = CameraTypes.ARCVIDEO_FACEPROCESS_MODEL_NONE;
                mCameraRecorder.setFaceProcessMode(mFaceProcessMode, null);
            }
        }else if (CustomCfg.mFaceProcessLibType == 2
                && mCameraRecorder.supportFaceProcess()) {
            mFaceBeautyEnable = false;
            if (mCameraRecorder != null) {
                mFaceProcessMode = CameraTypes.ARCVIDEO_FACEPROCESS_MODEL_NONE;
                mCameraRecorder.setFaceProcessMode(mFaceProcessMode, null);
            }
        }
        //liveController.onLivePause();
    }

    @Override
    public void onResumeRecord() {
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mIsPermissionGanted)
                || (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)) {
            int res = 0;
            Log.e(TAG, "openCamera  mCameraConfig.width" + mCameraConfig.getVideoWidth() + "; mCameraConfig.height" + mCameraConfig.getVideoHeight());
            res = mCameraRecorder.openCamera(mCameraConfig);
            Log.e("resume res","res=="+res);
            if (CameraTypes.CAMERA_INIT_SUCCESS != res) {
                Toast.makeText(activity, "打开摄像头失败", Toast.LENGTH_SHORT).show();
                //  mCameraRecorder.openCamera(mCameraConfig);
            }else {
                mSurfaceView.setVisibility(View.VISIBLE);
            }
        }
        mSurfaceView.setVisibility(View.VISIBLE);
        mIsPause = false;

        try {
            startLivePlayer();
//            if (liveController.isLivingPause()) {
//                liveController.onLiveStart();
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * copy plgin ini
     */
    private void copyPlginIni() {
        try {
            String strINIFile = "/sdcard/arcvideo/ini/";

            File dirFile = new File(strINIFile);
            if (!dirFile.exists()) {
                if (!dirFile.mkdirs()) {
                    return;
                }
            }
            File fileExist = new File(strINIFile + "ArcPlugin.ini");
            if(fileExist.exists()) {
                fileExist.delete();
            }

            InputStream is = activity.getAssets().open("ArcPlugin.ini");
            File file = new File(strINIFile + "ArcPlugin.ini");
            if (!file.exists()) {
                byte[] buf = new byte[1024];
                file.createNewFile();
                FileOutputStream os = new FileOutputStream(file);
                while (true) {
                    int ins = is.read(buf);
                    if (ins > 0) {
                        os.write(buf, 0, ins);
                    } else {
                        is.close();
                        os.flush();
                        os.close();
                        break;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
