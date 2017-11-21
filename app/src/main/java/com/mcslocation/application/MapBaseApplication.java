package com.mcslocation.application;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Vibrator;

import com.avos.avoscloud.AVOSCloud;
import com.baidu.mapapi.SDKInitializer;
import com.marswin89.marsdaemon.DaemonApplication;
import com.marswin89.marsdaemon.DaemonClient;
import com.marswin89.marsdaemon.DaemonConfigurations;
import com.mcslocation.DaoMaster;
import com.mcslocation.DaoSession;
import com.mcslocation.mapservice.BaiduMapLocationService;
import com.mcslocation.mapservice.LocationService;
import com.mcslocation.save.daemon.TraceServiceImpl;
import com.mcslocation.save.frontService;
import com.mcslocation.save.service.DaemonService;
import com.mcslocation.save.service.PlayerMusicService;
import com.mcslocation.tools.Rx.RxTool;
import com.xdandroid.hellodaemon.DaemonEnv;

import org.greenrobot.greendao.database.Database;


/**
 * Created by ly on 2017/10/12.
 */

public class MapBaseApplication extends Application {
    private static MapBaseApplication mapBaseApplicationInstance;
    public BaiduMapLocationService locationService;
    public Vibrator mVibrator;
    private DaemonClient daemonClient;
    private DaoSession daoSession;
    public static final boolean ENCRYPTED = false;//是否创建加密数据库
    @Override
    public void onCreate() {
        super.onCreate();
        mapBaseApplicationInstance = this;

        // 初始化参数依次为 this, AppId, AppKey
        AVOSCloud.initialize(mapBaseApplicationInstance,"og9iakzRMjvfAvgr3JDAaKJu-gzGzoHsz","31HdOiQbWLwevJ3VbYmQ4ByQ");
        // 放在 SDK 初始化语句 AVOSCloud.initialize() 后面，只需要调用一次即可
        AVOSCloud.setDebugLogEnabled(true);
        /*初始化定位sdk*/
        locationService = new BaiduMapLocationService(mapBaseApplicationInstance);
        mVibrator =(Vibrator)mapBaseApplicationInstance.getSystemService(Service.VIBRATOR_SERVICE);
        SDKInitializer.initialize(mapBaseApplicationInstance);
        //RxTools
        RxTool.init(mapBaseApplicationInstance);
        //GreenDao配置数据库
        setupDatabase("mapdetails.db");
        //需要在 Application 的 onCreate() 中调用一次 DaemonEnv.initialize()
        DaemonEnv.initialize(this, TraceServiceImpl.class, DaemonEnv.DEFAULT_WAKE_UP_INTERVAL);
        TraceServiceImpl.sShouldStopService = false;
        DaemonEnv.startServiceMayBind(TraceServiceImpl.class);
        DaemonEnv.startServiceMayBind(DaemonService.class);
        DaemonEnv.startServiceMayBind(PlayerMusicService.class);
        DaemonEnv.startServiceMayBind(frontService.class);
        DaemonEnv.startServiceMayBind(LocationService.class);
    }

    /*配置数据库
    * */
    private void setupDatabase(String str) {
        //初始化GreenDao
        //创建数据库，参数1：上下文，参数2：库名，参数3：游标工厂
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, str, null);
        SQLiteDatabase db = helper.getWritableDatabase();
        //实例化DaoMaster对象
        DaoMaster daoMaster = new DaoMaster(db);
        //实例化DaoSession对象
        daoSession = daoMaster.newSession();
    }

    //加密方式创建数据库
    private void setupdatabase(){
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, ENCRYPTED ? "notes-db-encrypted" : "user-db");
        Database db = ENCRYPTED ? helper.getEncryptedWritableDb("super-secret") : helper.getWritableDb();
        daoSession = new DaoMaster(db).newSession();
    }

    public DaoSession getDaoInstance() {
        return daoSession;
    }

    public MapBaseApplication(){
        super();
    }
    public static  MapBaseApplication getMapBaseApplicationInstance(){

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
