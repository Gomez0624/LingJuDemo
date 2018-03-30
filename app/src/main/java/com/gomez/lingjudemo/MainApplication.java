package com.gomez.lingjudemo;

import android.app.Application;
import android.content.Intent;

/**
 * Created by Administrator on 2015/11/11.
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Intent intent=new Intent(this,MainService.class);
        startService(intent);
    }
}
