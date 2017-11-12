package com.mcslocation.application;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.os.Vibrator;

import com.avos.avoscloud.AVOSCloud;
import com.baidu.mapapi.SDKInitializer;
import com.marswin89.marsdaemon.DaemonApplication;
import com.marswin89.marsdaemon.DaemonClient;
import com.marswin89.marsdaemon.DaemonConfigurations;
import com.mcslocation.mapservice.BaiduMapLocationService;


/**
 * Created by ly on 2017/10/12.
 */

public class MapBaseApplication extends Application {
    private static MapBaseApplication mapBaseApplicationInstance;
    public BaiduMapLocationService locationService;
    public Vibrator mVibrator;
    private DaemonClient daemonClient;
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化参数依次为 this, AppId, AppKey
        AVOSCloud.initialize(this,"og9iakzRMjvfAvgr3JDAaKJu-gzGzoHsz","31HdOiQbWLwevJ3VbYmQ4ByQ");
        // 放在 SDK 初始化语句 AVOSCloud.initialize() 后面，只需要调用一次即可
        AVOSCloud.setDebugLogEnabled(true);
        /*初始化定位sdk*/
        locationService = new BaiduMapLocationService(getApplicationContext());
        mVibrator =(Vibrator)getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);
        SDKInitializer.initialize(this);
    }

    public MapBaseApplication(){
        super();
        mapBaseApplicationInstance = this;
    }
    public static synchronized MapBaseApplication getMapBaseApplicationInstance(){
        if(mapBaseApplicationInstance == null){
            mapBaseApplicationInstance = new MapBaseApplication();
        }
        return mapBaseApplicationInstance;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        daemonClient = new DaemonClient(createDaemonConfigurations());
        daemonClient.onAttachBaseContext(base);
    }

    private DaemonConfigurations createDaemonConfigurations() {
        DaemonConfigurations.DaemonConfiguration configuration1 = new DaemonConfigurations.DaemonConfiguration(
                "com.mcslocation.save:process1",
                com.mcslocation.save.frontService.class.getCanonicalName(),
                com.mcslocation.save.frontReceiver1.class.getCanonicalName()
        );
        DaemonConfigurations.DaemonConfiguration configuration2 = new DaemonConfigurations.DaemonConfiguration(
                "com.mcslocation.save:process2",
                com.mcslocation.save.frontService2.class.getCanonicalName(),
                com.mcslocation.save.frontReceiver2.class.getCanonicalName()
        );
        DaemonConfigurations.DaemonListener listener = new MyDaemonListener();
        return new DaemonConfigurations(configuration1,configuration2,listener);
    }

    class MyDaemonListener implements DaemonConfigurations.DaemonListener{

        @Override
        public void onPersistentStart(Context context) {

        }

        @Override
        public void onDaemonAssistantStart(Context context) {

        }

        @Override
        public void onWatchDaemonDaed() {

        }
    }
}
