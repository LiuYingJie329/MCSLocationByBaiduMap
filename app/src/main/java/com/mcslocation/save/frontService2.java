package com.mcslocation.save;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * DO NOT do anything in this Service!<br/>
 *
 * Created by liuyingjie.
 */
public class frontService2 extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }
}
