package com.createsaga.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.arcvideo.camerarecorder.CameraSurfaceView;
import com.starunion.hefanlive.datasource.IHefanDataSource;
import com.starunion.hefanlive.proxy.HefanLiveProxy;
import com.starunion.hefanlive.view.HefanIjkPlayer;
import com.starunion.hefanlive.view.HefanPlayerSaurface;

public class HSYLiveActivity extends AppCompatActivity {
    //推流的地址
//    private String pushUrl = "rtmp://hsypush1.hefantv.com/live1/1100175hefan20161208115146";
    private String ALipushUrl = "rtmp://hsypush1.hefantv.com/live1/1100175hefan20161208115146";
//    private String HSYpushUrl = "rtmp://hsypush1.hefantv.com/live1/1100043hefan20161208161430";
    private String HSYpushUrl = "rtmp://video-center.alivecdn.com/AppTest/1017hefan20161208161849?vhost=livetest.createsaga.com";

    private CameraSurfaceView hsylive_surfaceview;
    private Button prepareView;
    private Button closeprepareView;
    private Button start;
    private Button pause;
    private Button resume;
    private Button stop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hsylive);
        hsylive_surfaceview = (CameraSurfaceView) findViewById(R.id.hsylive_surfaceview);
        prepareView = (Button) findViewById(R.id.prepareView);
        closeprepareView = (Button) findViewById(R.id.closeprepareView);
        start = (Button) findViewById(R.id.start);
        pause = (Button) findViewById(R.id.pause);
        resume = (Button) findViewById(R.id.resume);
        stop = (Button) findViewById(R.id.stop);


    }

    @Override
    protected void onResume() {
        IHefanDataSource dataSource = new IHefanDataSource() {
            @Override
            public String provideMainPassageway() {
                return ALipushUrl;
            }

            @Override
            public String provideBackUpPassageway() {
                return HSYpushUrl;
            }

            @Override
            public CameraSurfaceView provideLiveSurface() {
                return  hsylive_surfaceview;
            }

            @Override
            public HefanPlayerSaurface providePlaySurface() {
                return null;
            }

            @Override
            public HefanIjkPlayer provideIjkPlayer() {
                return null;
            }
        };
        HefanLiveProxy.getInstance().getHefanLive().initLive(this,dataSource);
        super.onResume();
    }


    public void stop(View view) {
        HefanLiveProxy.getInstance().getHefanLive().onStopRecord();
    }

    public void resume(View view) {
        HefanLiveProxy.getInstance().getHefanLive().onResumeRecord();
    }

    public void pause(View view) {
        HefanLiveProxy.getInstance().getHefanLive().onPauseRecord();
    }

    public void start(View view) {
        HefanLiveProxy.getInstance().getHefanLive().onStartRecord();
    }

    public void closeprepareView(View view) {
        HefanLiveProxy.getInstance().getHefanLive().closePreview();
    }


    public void prepareView(View view) {
        HefanLiveProxy.getInstance().getHefanLive().openPreview();
    }
}
