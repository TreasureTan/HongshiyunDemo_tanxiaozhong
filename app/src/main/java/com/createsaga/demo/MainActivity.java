package com.createsaga.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

    }

    public void toLive(View v){
        Intent intent = new Intent(this,HSYLiveActivity.class);
        startActivity(intent);
    }

    public void toPlay(View v){
        Intent intent = new Intent();
        intent.setClass(this, PlayActivity.class);
        startActivity(intent);
    }
}
