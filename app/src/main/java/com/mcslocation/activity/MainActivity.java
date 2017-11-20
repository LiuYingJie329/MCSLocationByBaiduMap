package com.mcslocation.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.SaveCallback;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.Poi;
import com.mcslocation.R;
import com.mcslocation.application.MapBaseApplication;
import com.mcslocation.greendao.mapdetails;
import com.mcslocation.mapdetailsDao;
import com.mcslocation.mapservice.BaiduMapLocationService;
import com.mcslocation.mapservice.LocationService;
import com.mcslocation.save.receiver.ScreenReceiverUtil;
import com.mcslocation.save.utils.Contants;
import com.mcslocation.save.utils.JobSchedulerManager;
import com.mcslocation.save.utils.ScreenManager;
import com.mcslocation.tools.CheckPermissionsActivity;
import com.mcslocation.tools.DateUtil;
import com.mcslocation.tools.Rx.RxActivityTool;
import com.mcslocation.tools.Rx.RxBroadcastTool;
import com.mcslocation.tools.Rx.RxDeviceTool;
import com.mcslocation.tools.Rx.RxNetTool;
import com.mcslocation.tools.Rx.RxToast;
import com.mcslocation.tools.TimeFormatUtils;
import com.xdandroid.hellodaemon.IntentWrapper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
 *单点定位
 * 保活逻辑
 * */
public class MainActivity extends CheckPermissionsActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private BaiduMapLocationService locationService;
    // 动态注册锁屏等广播
    private ScreenReceiverUtil mScreenListener;
    // 1像素Activity管理类
    private ScreenManager mScreenManager;
    // JobService，执行系统任务
    private JobSchedulerManager mJobManager;
    private String phonename = null;
    private String phoneNumber = null;
    private RxBroadcastTool.BroadcastReceiverNetWork broadcastReceiverNetWork;
    private mapdetailsDao mapDao = MapBaseApplication.getMapBaseApplicationInstance().getDaoInstance().getMapdetailsDao();
    private List<String> PoiList = new ArrayList<String>();
    public static Handler mHandler = null;
    private Context mContext;
    private String todayTime;
    public static final int upload = 1;
    public static final int quit = 2;
    private long firstTime = 0;
    private boolean UpDateFail =false ; //数据是否上传失败，true ,数据上传失败
    private ScreenReceiverUtil.SreenStateListener mScreenListenerer = new ScreenReceiverUtil.SreenStateListener() {
        @Override
        public void onSreenOn() {
            // 亮屏，移除"1像素"
            mScreenManager.finishActivity();
        }

        @Override
        public void onSreenOff() {
            // 接到锁屏广播，将MainActivity切换到可见模式
            // "咕咚"、"乐动力"、"悦动圈"就是这么做滴
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            startActivity(intent);
            // 如果你觉得，直接跳出MainActivity很不爽
            // 那么，我们就制造个"1像素"惨案
            mScreenManager.startActivity();
        }

        @Override
        public void onUserPresent() {
            // 解锁，暂不用，保留
        }
    };

    /**
     * 判断是否是当日第一次登陆
     */
    private boolean isTodayFirstLogin() {
        //取
        SharedPreferences preferences = getSharedPreferences("LastLoginTime", MODE_PRIVATE);
        String lastTime = preferences.getString("LoginTime", "2017-11-19");
        todayTime = DateUtil.getCurrentTime_Y_M_d();
        if (lastTime.equals(todayTime)) { //如果两个时间段相等
            Log.e(TAG, "不是当日首次登陆");
            return false;
        } else {
            Log.e(TAG, "当日首次登陆送积分");
            return true;
        }
    }

    /**
     * 保存每次退出的时间
     * @param extiLoginTime
     */
    private void saveExitTime(String extiLoginTime) {
        SharedPreferences.Editor editor = getSharedPreferences("LastLoginTime", MODE_PRIVATE).edit();
        editor.putString("LoginTime", extiLoginTime);
        editor.apply();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Contants.DEBUG)
            Log.d(TAG, "MCS-MainActivity-->onCreate");
        mContext = this;
        // 1. 注册锁屏广播监听器
        mScreenListener = new ScreenReceiverUtil(this);
        mScreenManager = ScreenManager.getScreenManagerInstance(this);
        mScreenListener.setScreenReceiverListener(mScreenListenerer);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 2. 启动系统任务
            mJobManager = JobSchedulerManager.getJobSchedulerInstance(this);
            mJobManager.startJobScheduler();
        }

        View btn_quit = findViewById(R.id.btn_quit);
        btn_quit.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                long secondTime = System.currentTimeMillis();
                if( secondTime - firstTime < 2000){
                    RxActivityTool.finishAllActivity();
                    mHandler.sendMessage(Message.obtain(mHandler,MainActivity.quit));
                }else{
                    RxToast.info("再按一次退出程序");
                    firstTime = System.currentTimeMillis();
                }
            }
        });
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case MainActivity.upload:
                        RxToast.success("定位成功");
                        break;
                    case quit:
                        AVObject Object = new AVObject("MCSLocation");
                        Object.put("Success","Quit");
                        Object.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(AVException e) {
                                if (e == null) {

                                }
                            }
                        });
                        break;
                    default:
                        break;
                }

            }
        };
    }

    private void UpdataByNet(mapdetails detail){
        AVObject mapObject = new AVObject("MCSLocation");
        mapObject.put("Success",detail.getSuccess());
        mapObject.put("PhoneName",detail.getPhoneName()+"");
        mapObject.put("PhoneNumber",detail.getPhoneNumber()+"");
        mapObject.put("DataTime",detail.getDataTime()+"");
        mapObject.put("ClientTime",detail.getClientTime()+"");
        mapObject.put("ServerTime",detail.getServerTime()+"");
        mapObject.put("LocType",detail.getLocType()+"");
        mapObject.put("Latitude",detail.getLatitude()+"");
        mapObject.put("Longitude",detail.getLongitude()+"");
        mapObject.put("Radius",detail.getRadius()+"");
        mapObject.put("City",detail.getCity());
        mapObject.put("District",detail.getDistrict());
        mapObject.put("Street",detail.getStreet());
        mapObject.put("AddrStr",detail.getAddrStr());
        mapObject.put("UserIndoorState",detail.getUserIndoorState()+"");
        mapObject.put("LocationDescribe",detail.getLocationDescribe());
        mapObject.put("PoiList",detail.getPoiList());
        mapObject.put("Operators",detail.getOperators()+"");
        mapObject.put("describe",detail.getDescribe());
        mapObject.put("isNetAble",detail.getIsNetAble());
        mapObject.put("isWifiAble",detail.getIsWifiAble());
        mapObject.put("GPSStatus",detail.getGPSStatus());
        mapObject.saveInBackground(new SaveCallback() {
            @Override
            public void done(AVException e) {
                if (e == null) {
                    RxToast.success("数据上传成功");
                }
            }
        });

    }

    //今天第一次登陆，上传昨天的数据
    private void UpDateYesterdayData(){
        boolean isshow = isTodayFirstLogin();
        List<mapdetails> list = mapDao.queryBuilder().where(mapdetailsDao.Properties.DataTime.like(DateUtil.getYesterdayTime_Y_M_d())).build().list();
        Log.e(TAG,"------------>昨天的数据库的数量为："+list.size());
        if(isshow && list.size() != 0){

            //获取今天的数据
            //List<mapdetails> list = mapDao.queryBuilder().where(mapdetailsDao.Properties.DataTime.like(DateUtil.getCurrentTime_Y_M_d())).build().list();
            for(mapdetails data : list){
                UpdataByNet(data);
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (Contants.DEBUG)
            Log.d(TAG, "MCS-MainActivity-->onStart");
        //RxTools网络监测广播
        broadcastReceiverNetWork = RxBroadcastTool.initRegisterReceiverNetWork(mContext);

        if(isTodayFirstLogin() && RxNetTool.isConnected(mContext)){
            IntentWrapper.whiteListMatters(this, "MCS实验系统持续运行");
        }

        //判断首次登录上传数据和数据首次登录是否上传成功


        //今天首次登录
        if(isTodayFirstLogin()){
            Log.e(TAG,"--------->首次登录成功，上传昨天定位数据");
            if(RxNetTool.isConnected(mContext)){
                UpDateYesterdayData();
            }else{
                UpDateFail = true;
                RxToast.error("数据上传失败,请打开网络,重启App,重新上传数据");
            }
        }

        //首次登录没有成功上传数据
        if((UpDateFail+"").toString().contains("true")){
            Log.e(TAG,"--------->数据上传失败,重新上传昨天定位数据");
            UpDateYesterdayData();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        saveExitTime(todayTime);
    }

    @Override
    protected void onStop() {
        //RxTools销毁广播
        unregisterReceiver(broadcastReceiverNetWork);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (Contants.DEBUG)
            Log.d(TAG, "MCS-MainActivity-->onDestroy");
        super.onDestroy();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //Log.e(TAG, "can not back to background");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
