package com.createsaga.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.starunion.hefanlive.datasource.IHefanDataSource;
import com.starunion.hefanlive.proxy.HefanLiveProxy;
import com.starunion.hefanlive.view.HefanIjkPlayer;
import com.starunion.hefanlive.view.HefanLiveSurface;
import com.starunion.hefanlive.view.HefanPlayerSaurface;

/**
 * Created by saga_ios on 16/12/8.
 */
public class PlayActivity extends AppCompatActivity implements IHefanDataSource{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_layout);

        HefanLiveProxy.getInstance().getHefanPlayer().initPlayer(this,this);

        HefanLiveProxy.getInstance().getHefanPlayer().onPlay();
    }

    @Override
    protected void onDestroy() {
        HefanLiveProxy.getInstance().getHefanPlayer().onStop();
        super.onDestroy();
    }

    @Override
    public String provideMainPassageway() {
        return "http://livecdn.video.taobao.com/temp/test1466295255657-65e172e6-1b96-4660-9f2f-1aba576d84e8.m3u8";
    }

    @Override
    public String provideBackUpPassageway() {
        return "http://livecdn.video.taobao.com/temp/test1466295255657-65e172e6-1b96-4660-9f2f-1aba576d84e8.m3u8";
    }

    @Override
    public HefanLiveSurface provideLiveSurface() {
        return null;
    }

    @Override
    public HefanPlayerSaurface providePlaySurface() {
//        return (HefanPlayerSaurface)findViewById(R.id.HefanPlayerSaurface);
          return null;
    }

    @Override
    public HefanIjkPlayer provideIjkPlayer() {
        return (HefanIjkPlayer) findViewById(R.id.hefan_plplayer);
    }

    public void onPlay(View v){
        HefanLiveProxy.getInstance().getHefanPlayer().onPlay();
    }

    public void onPause(View v){
        HefanLiveProxy.getInstance().getHefanPlayer().onPause();
    }

    public void onStop(View v){
        HefanLiveProxy.getInstance().getHefanPlayer().onStop();
    }
}
