package com.starunion.hefanlive.impl.hsy.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.AbsoluteLayout;

import com.arcsoft.MediaPlayer.ArcMediaPlayer;
import com.arcsoft.MediaPlayer.MV2Config;
import com.starunion.hefanlive.common.HefanContants;
import com.starunion.hefanlive.datasource.IHefanDataSource;
import com.starunion.hefanlive.ihefan.IHefanPlayer;
import com.starunion.hefanlive.utils.SystemUtils;
import com.starunion.hefanlive.view.HefanPlayerSaurface;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by saga_ios on 16/12/6.
 */
public class HsyPlayerImpl implements IHefanPlayer, SurfaceHolder.Callback,
        ArcMediaPlayer.OnCompletionListener, ArcMediaPlayer.OnPreparedListener,
        ArcMediaPlayer.OnInfoListener, ArcMediaPlayer.OnErrorListener,
        ArcMediaPlayer.OnSeekCompleteListener, ArcMediaPlayer.onMessageListener,
        ArcMediaPlayer.OnVideoSizeChangedListener {

    /* 提供数据 */
    private IHefanDataSource dataSource;

    public static final int MSG_GET_POSITION = 1;
    public static final int MSG_GET_BUFFERINGPERCENT = 2;
    public static final int MSG_DELAYED_OPEN_EXT = 3;
    public static final int MSG_DELAYED_OPEN = 4;
    public static final int MSG_GET_BITRATE = 5;
    private static final String TAG = "ArcPlayerSample";
    protected ArcMediaPlayer mMediaPlayer;
    HefanContants.AMMF_STATE m_PlayerState = HefanContants.AMMF_STATE.IDLE;
    HefanPlayerSaurface m_surfaceView = null;
    long m_lDuration = 0;
    int m_nClipWidth = 0;
    int m_nClipHeight = 0;
    int m_frameWidth = 0;
    int m_frameHeight = 0;
    int m_pixelFormat = android.graphics.PixelFormat.YCbCr_420_SP;
    int m_surfaceType = SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS;

    //private RelativeLayout m_mainLayout;
    int mConnectTimeout = 10;  //设置网络连接超时时间
    int mReceiveTimeout = 30;    //设置数据接收超时时间
    int mReconnectCount = 30;   //设置连接失败后的重试次数
    /**
     * 0----fillin显示效果为保证视频所有内容都完全显示在屏幕中且不会拉伸变形，
     * 如果视频宽高比与设备屏幕宽高比不一致则显示不会完全全屏;
     * 1---fillout显示效果为保证视频能够全屏显示且不会拉伸变形，
     * 如果视频宽高比与设备屏幕宽高比不一致则显示全屏，同时会有部分视频内容会超出屏幕范围从而被截掉;
     * 2------fullcreen显示效果为保证视频所有内容都完全显示在屏幕中全屏显示，
     * 如果视频宽高比与设备屏幕宽高比不一致则显示会被拉伸变形;
     */
    int mDisplayType = 2;
    boolean m_bVideoSizeChanged = false;
    private Activity context;
    private SurfaceHolder mSurfaceHolder = null;
    private int m_iCurOrientation = 0;

    private ArrayList<Map<String, Object>> m_aryUrl = new ArrayList<Map<String, Object>>();
    boolean isSurfaceCreated = false;
    // 虹视云拉流地址
    private String pullHsyUrl = "";
    // 阿里拉流地址
    private String pullAliUrl = "";
    // 当前拉流地址
    private String pullurl = "";
    // 是否用户主动调用onPlay()
    private boolean isOnPlay = false;
    // 是否用户主动调用onPause()
    private boolean isOnPause = false;
    // 是否用户主动调用onStop()
    private boolean isOnStop = false;

    private int currentPosition = 0;

    @Override
    public void initPlayer(Activity activity, IHefanDataSource dataSource) {
        this.context = activity;
        this.dataSource = dataSource;
        initPlayer();
    }

    private void initPlayer(){
        this.m_surfaceView = dataSource.providePlaySurface();
        m_surfaceView.setVisibility(View.VISIBLE);
        if (mMediaPlayer == null) {
            mMediaPlayer = new ArcMediaPlayer();
        }
        createPorSurface();
        //设置程序界面全屏
        Log.i("pull", "准备初始化播放器");
        //用来记录旋转方向
        if (context!= null){
            m_iCurOrientation = context.getResources().getConfiguration().orientation;
        }
        surfaceCreated(m_surfaceView.getHolder());
    }

    /**
     * 触发创建surface，并注册surface的回调
     * 调用该函数后，surface创建和使用和销毁过程中就会触发
     * surfaceCreated\surfaceDestroyed\surfaceChanged这些回调，
     * 使用者就可以根据自己的需求进行相关的处理
     */
    private void createPorSurface() {
        initSurfaceType();
        if (m_surfaceView != null) {
            mSurfaceHolder = m_surfaceView.getHolder();
        }
        if (mSurfaceHolder != null) {
            mSurfaceHolder.addCallback(this);
            mSurfaceHolder.setType(m_surfaceType);
            mSurfaceHolder.setFormat(m_pixelFormat);
        }
    }

    /**
     * 初始化surface 类型，根据android API level进行设置
     */
    private void initSurfaceType() {
        if (Integer.valueOf(android.os.Build.VERSION.SDK) < 14) {
            m_pixelFormat = android.graphics.PixelFormat.RGBA_8888;
            m_surfaceType = SurfaceHolder.SURFACE_TYPE_NORMAL;//
        } else {
            m_pixelFormat = android.graphics.PixelFormat.YCbCr_420_SP;// YCbCr_420_SP;//
            m_surfaceType = SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS;//
        }
    }

    @Override
    public void onPlay() {
        if (m_PlayerState == HefanContants.AMMF_STATE.STARTED){
            return;
        }
        if (isOnStop){
            isOnStop = false;
            initPlayer();
        }else {
            if (isOnPause){
                if (mMediaPlayer != null) {
                    mMediaPlayer.setPosition(currentPosition);
                    mMediaPlayer.start();
                    currentPosition = 0;
                }
            }else {
                if (isSurfaceCreated){
                    startToPlay(pullurl);
                }else {
                    isOnPlay = true;
                }
            }
        }
    }

    @Override
    public void onPause() {
        isOnPause = true;
        if (mMediaPlayer != null && m_PlayerState == HefanContants.AMMF_STATE.STARTED) {
            mMediaPlayer.pause();
            currentPosition = mMediaPlayer.getCurrentPosition();
            m_PlayerState = HefanContants.AMMF_STATE.PAUSED;
        }
    }

    @Override
    public void onStop() {
        if (m_PlayerState != HefanContants.AMMF_STATE.IDLE) {
            m_PlayerState = HefanContants.AMMF_STATE.STOPPED;
            if (null != mMediaPlayer)
                mMediaPlayer.stop();
            SystemUtils.controlBackLight(context, true);
            Log.e("stop", "停止");
        }
        isOnPlay = false;
        isOnPause = false;
        isSurfaceCreated = false;
        isOnStop = true;
    }

    @Override
    public void onResume() {
        m_surfaceView.setVisibility(View.VISIBLE);
        if (mMediaPlayer != null && m_PlayerState == HefanContants.AMMF_STATE.PAUSED) {
            mMediaPlayer.start();
            m_PlayerState = HefanContants.AMMF_STATE.STARTED;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.e("ArcPlayerSample","被创建");
        Surface sf = null;
        sf = m_surfaceView.getHolder().getSurface();
        //	m_iCurOrientation = this.getResources().getConfiguration().orientation;

        Log.i("pull", "mMediaPlayer");

        //设置必须的参数，必须在创建player后立马调用
        mMediaPlayer.setConfigFile(context, context.getFilesDir().getAbsolutePath() + "/MV3Plugin.ini");
        mMediaPlayer.reset();
        if (!sf.isValid()) {
            // Log.e("surfaceCheck", "surfaceCreated,Surface is invalid");
            m_surfaceView.setVisibility(View.GONE);
            m_surfaceView.setVisibility(View.VISIBLE);
            return;
        }

        m_iCurOrientation = context.getResources().getConfiguration().orientation;
        //如果mMediaPlayer已经被创建了，则使用视频宽高来初始化显示区域
        //如果mMediaPlayer还没被创建则使用屏幕宽高来初始化显示区域
        if (mMediaPlayer != null) {
            mMediaPlayer.setDisplay(mSurfaceHolder);
            Log.i("surfaceCreated", "setDisplay成功");
            onVideoSizeChanged(mMediaPlayer, mMediaPlayer.getVideoWidth(),
                    mMediaPlayer.getVideoHeight());
        } else {
            Log.i("pull", "mMediaPlayer已创建");
            //  Log.e("pull", "Set surface begin ");
            int nScreenWidth = context.getWindow().getWindowManager()
                    .getDefaultDisplay().getWidth();
            int nScreenHeight = context.getWindow().getWindowManager()
                    .getDefaultDisplay().getHeight();

            SetSurfaceRect(m_pixelFormat, 0, 0, nScreenWidth, nScreenHeight);
            Log.e("surface Created", "Set surface end ");
        }

        if (dataSource != null && dataSource.provideMainPassageway() != null) {
            pullurl = pullHsyUrl = dataSource.provideMainPassageway();
            pullAliUrl = dataSource.provideBackUpPassageway();
            changePlayer();
        }
        if (isOnPlay) {
            startToPlay(pullurl);
        }
        isSurfaceCreated = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (null != mMediaPlayer) {
            mMediaPlayer.setDisplay(null);
        }
//        if (fhPlayerInterface!=null){
//            fhPlayerInterface.destroySurfaceView();
//        }
    }


    @Override
    public void onCompletion(ArcMediaPlayer arcMediaPlayer) {
        m_bVideoSizeChanged = false;
        this.reset();
        changePlayer();
    }

    public boolean onError(ArcMediaPlayer mp, int what, int extra) {
        boolean bUpdate = false;
        String errorInfo = "";
        String codecTypeString = "";
        switch (what) {
            case ArcMediaPlayer.MEDIA_ERROR_SOURCE_NETWORK_CONNECTFAIL:
                errorInfo = "网络连接失败";
                //fhPlayerInterface.onNetworkError();
                onStop();
                this.reset();
                if (!TextUtils.isEmpty(pullurl)){
                    startToPlay(pullurl);
                }
                break;
            case ArcMediaPlayer.MEDIA_ERROR_SOURCE_STREAM_OPEN:
            case ArcMediaPlayer.MEDIA_ERROR_PLAYER_DISPLAY_INIT_FAILED: //渲染器创建失败
            case ArcMediaPlayer.MEDIA_ERROR_PLAYER_OPERATION_CANNOTEXECUTE: //当前操作无法执行，比如player初始化失败
            case ArcMediaPlayer.MEDIA_ERROR_SOURCE_STREAM_SEEK:
            case ArcMediaPlayer.MEDIA_ERROR_SOURCE_FORMAT_UNSUPPORTED:
            case android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN:
                onStop();
                break;
            case ArcMediaPlayer.MEDIA_ERROR_SOURCE_UNSUPPORTED_SCHEME:
            case ArcMediaPlayer.MEDIA_ERROR_SOURCE_DATARECEIVE_TIMEOUT:
            case ArcMediaPlayer.MEDIA_ERROR_SOURCE_DATARECEIVE_FAIL:
            case ArcMediaPlayer.MEDIA_ERROR_SOURCE_DATASEND_TIMEOUT:  //数据(网络请求)发送超时
            case ArcMediaPlayer.MEDIA_ERROR_SOURCE_DATASEND_FAIL: //数据（网络请求）发送失败
            case ArcMediaPlayer.MEDIA_ERROR_SOURCE_BUFFER_TIMEOUT:
            case ArcMediaPlayer.MEDIA_ERROR_SOURCE_DATARECEIVE_NOBODY:
                changePlayer();
                break;
            case ArcMediaPlayer.MEDIA_ERROR_SOURCE_NETWORK_CONNECTIMEOUT:
                errorInfo = "网络连接超时，重新连接";
                //fhPlayerInterface.onNetworkError();
                onStop();
                this.reset();
                startToPlay(pullurl);
                break;
            default:
                if (what >= 100400 && what <= 100599) {
                    Log.e(TAG, "onError: error type is one of the http critical status code, value = " + what);
                } else {
                    Log.e(TAG, "onError: what the Fxxxx!! error is " + what + "? code is " + extra);
                }
                changePlayer();
        }
        return true;
    }

    public boolean onInfo(ArcMediaPlayer mp, int what, int extra) {
        switch (what) {
            case android.media.MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                break;
            case ArcMediaPlayer.MEDIA_INFO_BUFFERING_START:   //开始缓冲数据，此时播放会临时暂停
                break;
            case ArcMediaPlayer.MEDIA_INFO_RENDERING_START:
                Log.e("监听直播的状态", "8888888888888888");
//                if (fhPlayerInterface != null) {
//                    fhPlayerInterface.onLoadingEnd();
//                    fhPlayerInterface.pullAgain();
//                }
                break;
            case ArcMediaPlayer.MEDIA_INFO_SPLITTER_NOAUDIO:  //媒体数据中没有audio
                break;
            case ArcMediaPlayer.MEDIA_INFO_SPLITTER_NOVIDEO:  //媒体数据中没有video
                break;
            case ArcMediaPlayer.MEDIA_INFO_VCODEC_DECODE_ERROR:  //Video解码失败，audio正常
                break;
            case ArcMediaPlayer.MEDIA_INFO_ACODEC_DECODE_ERROR: //Audio解码失败，video正常
                break;
            default:
        }
        return true;
    }

    @Override
    public void onPrepared(ArcMediaPlayer arcMediaPlayer) {
        m_PlayerState = HefanContants.AMMF_STATE.PREPARED;
        m_PlayerState = HefanContants.AMMF_STATE.STARTED;
        mMediaPlayer.start();
        Log.i("pull", "start+++++");

        //播放开始后，使用Handler来不断获取和刷新当前播放时间
        //mRefreshHandler.sendEmptyMessage(MSG_GET_POSITION); //

        if (!m_bVideoSizeChanged)
            onVideoSizeChanged(mMediaPlayer, mMediaPlayer.getVideoWidth(),
                    mMediaPlayer.getVideoHeight());
    }

    @Override
    public void onSeekComplete(ArcMediaPlayer arcMediaPlayer) {
        onStop();
    }

    @Override
    public void onVideoSizeChanged(ArcMediaPlayer arcMediaPlayer, int i, int i1) {
        if (m_frameWidth != 0 && m_frameHeight != 0 && m_PlayerState == HefanContants.AMMF_STATE.STARTED)
            m_bVideoSizeChanged = true;
        Log.v(TAG, "onVideoSizeChanged m_bVideoSizeChanged: "
                + m_bVideoSizeChanged);
        int nScreenWidth = context.getWindow().getWindowManager().getDefaultDisplay()
                .getWidth();
        int nScreenHeight = context.getWindow().getWindowManager().getDefaultDisplay()
                .getHeight();
        float aspect_ratio = mMediaPlayer.getAspectRatio();
        Log.v(TAG, "aspect_ratio=" + aspect_ratio);
        Log.v(TAG, "before adjuct aspect, w=" + m_frameWidth + ",h="
                + m_frameHeight);

        if(aspect_ratio != 0.0) {
            m_frameWidth = Float.floatToIntBits((Float.intBitsToFloat(m_frameHeight) * aspect_ratio));

            Log.v(TAG, "after adjuct aspect, w=" + m_frameWidth + ",h="
                    + m_frameHeight);
        }
		/* start */
        if (m_frameWidth != 0 && m_frameHeight != 0) {
            int estimateW, estimateH;
            switch (mDisplayType) {
                case 0:
                    if (nScreenWidth * m_frameHeight > nScreenHeight * m_frameWidth) {
                        estimateW = nScreenHeight * m_frameWidth / m_frameHeight;
                        estimateH = nScreenHeight;
                        if (estimateW % 4 != 0)
                            estimateW -= estimateW % 4;
                    } else {
                        estimateW = nScreenWidth;
                        estimateH = nScreenWidth * m_frameHeight / m_frameWidth;
                        if (estimateH % 4 != 0)
                            estimateH -= estimateH % 4;
                    }
                    break;
                case 1:
                    if (nScreenWidth * m_frameHeight > nScreenHeight * m_frameWidth) {
                        estimateW = nScreenWidth;
                        estimateH = nScreenWidth * m_frameHeight / m_frameWidth;
                        if (estimateH % 4 != 0)
                            estimateH -= estimateH % 4;
                    } else {

                        estimateW = nScreenHeight * m_frameWidth / m_frameHeight;
                        estimateH = nScreenHeight;
                        if (estimateW % 4 != 0)
                            estimateW -= estimateW % 4;
                    }
                    break;
                default:
                    estimateW = nScreenWidth;
                    estimateH = nScreenHeight;
            }
            int xOffset = (nScreenWidth - estimateW) / 2;
            int yOffset = (nScreenHeight - estimateH) / 2;
            if (xOffset % 4 != 0)
                xOffset -= xOffset % 4;
            if (yOffset % 4 != 0)
                yOffset -= yOffset % 4;
            Log.d(TAG, xOffset + ", " + yOffset + ", " + estimateW + "x"
                    + estimateH);
            if (m_surfaceType == android.view.SurfaceHolder.SURFACE_TYPE_NORMAL) {
                mMediaPlayer.setDisplayRect(xOffset, yOffset, estimateW,
                        estimateH);
                SetSurfaceRect(m_pixelFormat, xOffset, yOffset, estimateW,
                        estimateH);
            } else {
                m_nClipWidth = estimateW;
                m_nClipHeight = estimateH;
                mMediaPlayer.setDisplayRect(xOffset, yOffset, estimateW,
                        estimateH);
                SetSurfaceRect(m_pixelFormat, xOffset, yOffset, estimateW,
                        estimateH);
            }
        } else {
            if (m_surfaceType == android.view.SurfaceHolder.SURFACE_TYPE_NORMAL) {
                mMediaPlayer.setDisplayRect(0, 0, nScreenWidth, nScreenHeight);
                SetSurfaceRect(m_pixelFormat, 0, 0, nScreenWidth, nScreenHeight);
            } else {
                // some implicit rules here..
                mMediaPlayer.setDisplayRect(0, 0, nScreenWidth, nScreenHeight);
                SetSurfaceRect(m_pixelFormat, 0, 0, nScreenWidth, nScreenHeight);
            }
        }
    }

    @Override
    public boolean onMessage(ArcMediaPlayer arcMediaPlayer, int messageInfo, int level) {
        // TODO Auto-generated method stub
        switch (messageInfo) {
            default:
                Log.e(TAG, "unknown messageInfo  is " + messageInfo + ", level is " + level);
                break;
        }
        return true;
    }

    /**
     * 控制屏幕画面显示
     * @param PixelFormat   颜色空间类型
     * @param x             画面在屏幕上起始点X轴坐标位置offset x
     * @param y             画面在屏幕上起始点Y轴坐标位置offset y
     * @param w             视频宽width
     * @param h             视频高height
     */
    @SuppressWarnings("deprecation")
    public void SetSurfaceRect(int PixelFormat, int x, int y, int w, int h) {
        AbsoluteLayout.LayoutParams lp;
        {
            if (w <= 1 && h <= 1) {
                lp = (AbsoluteLayout.LayoutParams) (m_surfaceView
                        .getLayoutParams());
                lp.x = context.getWindow().getWindowManager().getDefaultDisplay()
                        .getWidth();
                lp.y = context.getWindow().getWindowManager().getDefaultDisplay()
                        .getHeight();
                lp.width = 0;
                lp.height = 0;
                m_surfaceView.setLayoutParams(lp);
            } else {
                lp = (AbsoluteLayout.LayoutParams) (m_surfaceView.getLayoutParams());
                lp.x = x;
                lp.y = y;
                lp.width = w;
                lp.height = h;
                m_surfaceView.setLayoutParams(lp);
            }
        }
    }

    //切换流播放
    private void changePlayer() {
        Log.e("拉流地址","走双路流");
        onStop();
        this.reset();
        if (!TextUtils.isEmpty(pullAliUrl)&&!TextUtils.isEmpty(pullHsyUrl)){
            Log.e("拉流地址","双通道path="+  pullurl);
            if(pullurl.equals(pullAliUrl)){
                pullurl = pullHsyUrl;
                Log.e("拉流地址","path=="+pullurl);
                startToPlay(pullurl);
            }else if (pullurl.equals(pullHsyUrl)){
                pullurl = pullAliUrl;
                Log.e("拉流地址","path=="+pullurl);
                startToPlay(pullurl);
            }
        }else {
            pullurl = pullAliUrl;
            Log.e("拉流地址","path=="+pullurl);
            startToPlay(pullurl);
        }

    }

    private void reset() {
        m_PlayerState = HefanContants.AMMF_STATE.IDLE;
    }

    private void startToPlay(String path) {
        if (m_surfaceView==null){
            return;
        }
        m_surfaceView.setVisibility(View.VISIBLE);
        try {
            openFileStr(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    private void openFileStr(String str) throws IllegalArgumentException,
            IllegalStateException, IOException {
        mMediaPlayer.reset();
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", "ArcSoft Player/3.5");
        headers.put("Referer", "ArcSoft Sample Player");
        Log.i("pull", "拉流地址==" + str);
        mMediaPlayer.setDataSource(str, headers);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        m_bVideoSizeChanged = false;
        mMediaPlayer.setOnVideoSizeChangedListener(this);
        mMediaPlayer.setOnInfoListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        mMediaPlayer.setOnMessageListener(this);
        mMediaPlayer.setHardwareMode(true);
        mMediaPlayer.setBenchmark(2);
        mMediaPlayer.setConfig(MV2Config.MEDIAFILE.INITIAL_BUFFERTIME_ID, 500);
        mMediaPlayer.setConfig(ArcMediaPlayer.CONFIG_NETWORK_CONNECT_TIMEOUT, mConnectTimeout * 1000);
        mMediaPlayer.setConfig(ArcMediaPlayer.CONFIG_NETWORK_RECEIVE_TIMEOUT, mReceiveTimeout * 1000);
        mMediaPlayer.setConfig(ArcMediaPlayer.CONFIG_NETWORK_RECONNECT_COUNT, mReconnectCount);
        //设置buffering超时设置
        mMediaPlayer.setConfig(MV2Config.MEDIAFILE.BUFFERING_TIMEOUT_ID, 5000);
        mMediaPlayer.prepareAsync();
        //使用视频宽高重新计算显示区域
        onVideoSizeChanged(mMediaPlayer, mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight());
        SystemUtils.controlBackLight(context, true);
        m_PlayerState = HefanContants.AMMF_STATE.PREPARING;
    }

}
