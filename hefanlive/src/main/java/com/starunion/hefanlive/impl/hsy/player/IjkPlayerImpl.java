package com.starunion.hefanlive.impl.hsy.player;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.util.Log;
import android.widget.Toast;

import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLMediaPlayer;
import com.pili.pldroid.player.widget.PLVideoTextureView;
import com.starunion.hefanlive.datasource.IHefanDataSource;
import com.starunion.hefanlive.ihefan.IHefanPlayer;
import com.starunion.hefanlive.impl.hsy.player.widget.MediaController;
import com.starunion.hefanlive.impl.utils.Utils;
import com.starunion.hefanlive.view.HefanIjkPlayer;

import java.util.ArrayList;

import rx.Subscription;
import rx.subscriptions.Subscriptions;

/**
 * Created by Administrator on 2016/12/19.
 */

public class IjkPlayerImpl implements IHefanPlayer {

    private static final int MESSAGE_ID_RECONNECTING = 0x01;
    private   String DEFAULT_TEST_URL = "http://live.hkstv.hk.lxdns.com/live/hks/playlist.m3u8";
    private boolean mIsActivityPaused = true;
    private MediaController mMediaController;
    private PLVideoTextureView mVideoView;
    private Toast mToast = null;
    private String mVideoPath = null;

    private int mDisplayAspectRatio = PLVideoTextureView.ASPECT_RATIO_PAVED_PARENT;
    private ArrayList<String> mVideoUrls = new ArrayList<>();
    private Subscription mSubscription = Subscriptions.empty();
    private Activity mActivity ;

    private int isLiveStreaming = 1;
    private AVOptions options;
    @Override
    public void initPlayer(Activity activity, IHefanDataSource dataSource) {
        Log.e("Player","Player 初始化");
        mVideoView = dataSource.provideIjkPlayer();
        DEFAULT_TEST_URL = dataSource.provideMainPassageway();
        mVideoView.setDisplayAspectRatio(mDisplayAspectRatio);
        mActivity = activity;
    }

    @Override
    public void onPlay() {
        Log.e("Player","Player 播放");

        mVideoPath = DEFAULT_TEST_URL;
        initAVOptions();
        mVideoView.setAVOptions(options);
        mMediaController = new MediaController(mActivity, false, true);
        generateUrls();
        loadVideo(mVideoPath);
    }

    @Override
    public void onPause() {
        mToast = null;
        mVideoView.pause();
        mIsActivityPaused = true;
    }

    @Override
    public void onStop() {
        mVideoView.stopPlayback();
        mSubscription.unsubscribe();
    }

    @Override
    public void onResume() {
        mIsActivityPaused = false;
        mVideoView.start();
    }


    protected Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what != MESSAGE_ID_RECONNECTING) {
                return;
            }
            if (mIsActivityPaused || !Utils.isLiveStreamingAvailable()) {
                mActivity.finish();
                return;
            }
            if (!Utils.isNetworkAvailable(mActivity)) {
                sendReconnectMessage();
                return;
            }
            mVideoView.setVideoPath(mVideoPath);
            mVideoView.start();
        }
    };


    private PLMediaPlayer.OnErrorListener mOnErrorListener = new PLMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(PLMediaPlayer mp, int errorCode) {
            boolean isNeedReconnect = false;
            switch (errorCode) {
                case PLMediaPlayer.ERROR_CODE_INVALID_URI:
                    showToastTips("Invalid URL !");
                    break;
                case PLMediaPlayer.ERROR_CODE_404_NOT_FOUND:
                    showToastTips("404 resource not found !");
                    break;
                case PLMediaPlayer.ERROR_CODE_CONNECTION_REFUSED:
                    showToastTips("Connection refused !");
                    break;
                case PLMediaPlayer.ERROR_CODE_CONNECTION_TIMEOUT:
                    showToastTips("Connection timeout !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_EMPTY_PLAYLIST:
                    showToastTips("Empty playlist !");
                    break;
                case PLMediaPlayer.ERROR_CODE_STREAM_DISCONNECTED:
                    showToastTips("Stream disconnected !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_IO_ERROR:
                    showToastTips("Network IO Error !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_UNAUTHORIZED:
                    showToastTips("Unauthorized Error !");
                    break;
                case PLMediaPlayer.ERROR_CODE_PREPARE_TIMEOUT:
                    showToastTips("Prepare timeout !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_READ_FRAME_TIMEOUT:
                    showToastTips("Read frame timeout !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.MEDIA_ERROR_UNKNOWN:
                    break;
                default:
                    showToastTips("unknown error !");
                    break;
            }
            // Todo pls handle the error status here, reconnect or call finish()
            if (isNeedReconnect) {
                sendReconnectMessage();
            } else {
                mActivity.finish();
            }
            // Return true means the error has been handled
            // If return false, then `onCompletion` will be called
            return true;
        }
    };
    private PLMediaPlayer.OnCompletionListener mOnCompletionListener = new PLMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(PLMediaPlayer plMediaPlayer) {
            showToastTips("Play Completed !");
            mActivity.finish();
        }
    };

    private void showToastTips(final String tips) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(mActivity, tips, Toast.LENGTH_SHORT);
                mToast.show();
            }
        });
    }

    private void sendReconnectMessage() {
        showToastTips("正在重连...");
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MESSAGE_ID_RECONNECTING), 500);
    }

    private void generateUrls() {
        for (int i = 0; i < 10; i++) {
            mVideoUrls.add(DEFAULT_TEST_URL);
        }
    }
    private void initAVOptions() {
        options = new AVOptions();
        // the unit of timeout is ms
        options.setInteger(AVOptions.KEY_PREPARE_TIMEOUT, 10 * 1000);
        options.setInteger(AVOptions.KEY_GET_AV_FRAME_TIMEOUT, 10 * 1000);
        // Some optimization with buffering mechanism when be set to 1
        options.setInteger(AVOptions.KEY_LIVE_STREAMING, isLiveStreaming);
        options.setInteger(AVOptions.KEY_DELAY_OPTIMIZATION, 1);
        // 1 -> hw codec enable, 0 -> disable [recommended]
        int codec = 0;
        options.setInteger(AVOptions.KEY_MEDIACODEC, codec);
        // whether start play automatically after prepared, default value is 1
        options.setInteger(AVOptions.KEY_START_ON_PREPARED, 0);
    }

    private void loadVideo(String mVideoPath) {
        mVideoView.setMediaController(mMediaController);
        mVideoView.setOnCompletionListener(mOnCompletionListener);
        mVideoView.setOnErrorListener(mOnErrorListener);
//        mVideoView.setVideoPath(mVideoUrls.get(position));
        mVideoView.setVideoPath(mVideoPath);
        mVideoView.start();
    }
}
