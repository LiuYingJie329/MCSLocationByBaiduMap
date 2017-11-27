package com.mcslocation.activity;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewManager;
import android.widget.Button;
import android.widget.TextView;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.SaveCallback;
import com.mcslocation.R;
import com.mcslocation.application.MapBaseApplication;
import com.mcslocation.greendao.mapdetails;
import com.mcslocation.mapdetailsDao;
import com.mcslocation.mapservice.BaiduMapLocationService;
import com.mcslocation.save.receiver.ScreenReceiverUtil;
import com.mcslocation.save.utils.Contants;
import com.mcslocation.save.utils.JobSchedulerManager;
import com.mcslocation.save.utils.ScreenManager;
import com.mcslocation.tools.CheckPermissionsActivity;
import com.mcslocation.tools.DateUtil;
import com.mcslocation.tools.Rx.RxActivityTool;
import com.mcslocation.tools.Rx.RxAppTool;
import com.mcslocation.tools.Rx.RxBroadcastTool;
import com.mcslocation.tools.Rx.RxDeviceTool;
import com.mcslocation.tools.Rx.RxLocationTool;
import com.mcslocation.tools.Rx.RxNetTool;
import com.mcslocation.tools.Rx.RxSPTool;
import com.mcslocation.tools.Rx.RxToast;
import com.mcslocation.tools.TimeFormatUtils;
import com.xdandroid.hellodaemon.IntentWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

/*
 *单点定位
 * 保活逻辑
 * */
public class MainActivity extends CheckPermissionsActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private BaiduMapLocationService locationService;
    // 动态注册锁屏等广播
    private ScreenReceiverUtil mScreenListener;
    // 1像素Activity管理类
    private ScreenManager mScreenManager;
    // JobService，执行系统任务
    private JobSchedulerManager mJobManager;
    private RxBroadcastTool.BroadcastReceiverNetWork broadcastReceiverNetWork;
    private mapdetailsDao mapDao = MapBaseApplication.getMapBaseApplicationInstance().getDaoInstance().getMapdetailsDao();
    public static Handler mHandler = null;
    private Context mContext;
    private String todayTime;
    public static final int upload = 1;
    public static final int quit = 2;
    public static final int upMapDateActive = 3;  //主动上传昨日数据
    public static final int upMapDateUnActive = 4;//被动上传昨日数据
    private long firstTime = 0;
    private Button btn_quit, btn_update;
    private TextView appVersion;
    private String phonename = null;
    private String phoneNumber = null;
    private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";
    public static boolean isForeground = false;

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
        String lastTime = "";
        lastTime = RxSPTool.getString(MapBaseApplication.getMapBaseApplicationInstance(), "LastLoginTime");
        if (RxDeviceTool.isNullString(lastTime)) {
            lastTime = "2017-11-19";
        }
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
     *
     * @param extiLoginTime
     */
    private void saveExitTime(String extiLoginTime) {
        RxSPTool.putString(MapBaseApplication.getMapBaseApplicationInstance(), "LastLoginTime", extiLoginTime);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Contants.DEBUG)
            Log.e(TAG, "MCS-MainActivity-->onCreate");
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

        initViews();
        initData();

        isNotificationEnabled(MapBaseApplication.getMapBaseApplicationInstance());
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MainActivity.upload:
                        RxToast.success("定位成功");
                        break;
                    case quit:
                        mapdetails mapdetail = new mapdetails();
                        mapdetail.setSuccess("Quit");   //请求成功
                        mapdetail.setPhoneName(phoneNumber); //设备的手机号码
                        mapdetail.setPhoneName(phonename);  //设备名称
                        mapdetail.setDataTime(DateUtil.getCurrentTime_Y_M_d());//客户端设备时间
                        mapdetail.setClientTime(TimeFormatUtils.formatUTC(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss")); //手机端请求时间
                        mapdetail.setIsNetAble((RxNetTool.isConnected(getApplicationContext()) + "").contains("true") ? "Net true" : "Net false");//判断网络是否连接
                        mapdetail.setIsWifiAble((RxNetTool.isWifi(getApplicationContext()) + "").contains("true") ? "Wifi true" : "Wifi false");//判断网络连接方式是否为WIFI
                        mapdetail.setGPSStatus((RxLocationTool.isGpsEnabled(getApplicationContext()) + "").contains("true") ? "GPS true" : "GPS false");//GPS是否打开
                        mapDao.insert(mapdetail);
                        break;
                    case upMapDateActive:
                        UpDateYesterdayData(MainActivity.upMapDateActive);
                        Log.e(TAG, "mHandler-----UpDateYesterdayData-------主动上传数据");
                        break;
                    case upMapDateUnActive:
                        UpDateYesterdayData(MainActivity.upMapDateUnActive);
                        Log.e(TAG, "mHandler-----UpDateYesterdayData-------被动上传数据");
                        break;
                    default:
                        break;
                }

            }
        };
    }

    private void UpdataByNet(mapdetails detail) {
        AVObject mapObject = new AVObject("MCSLocation");
        mapObject.put("Success", detail.getSuccess());
        mapObject.put("PhoneName", detail.getPhoneName() + "");
        mapObject.put("PhoneNumber", detail.getPhoneNumber() + "");
        mapObject.put("DataTime", detail.getDataTime() + "");
        mapObject.put("ClientTime", detail.getClientTime() + "");
        mapObject.put("ServerTime", detail.getServerTime() + "");
        mapObject.put("LocType", detail.getLocType() + "");
        mapObject.put("Latitude", detail.getLatitude() + "");
        mapObject.put("Longitude", detail.getLongitude() + "");
        mapObject.put("Radius", detail.getRadius() + "");
        mapObject.put("City", detail.getCity());
        mapObject.put("District", detail.getDistrict());
        mapObject.put("Street", detail.getStreet());
        mapObject.put("AddrStr", detail.getAddrStr());
        mapObject.put("UserIndoorState", detail.getUserIndoorState() + "");
        mapObject.put("LocationDescribe", detail.getLocationDescribe());
        mapObject.put("PoiList", detail.getPoiList());
        mapObject.put("Operators", detail.getOperators() + "");
        mapObject.put("describe", detail.getDescribe());
        mapObject.put("isNetAble", detail.getIsNetAble());
        mapObject.put("isWifiAble", detail.getIsWifiAble());
        mapObject.put("GPSStatus", detail.getGPSStatus());
        mapObject.put("IndoorLocationSurpport", detail.getIndoorLocationSurpport());
        mapObject.put("IndoorLocationSource", detail.getIndoorLocationSource());
        mapObject.put("IndoorNetworkState", detail.getIndoorNetworkState());
        mapObject.put("IndoorLocationSurpportBuidlingName", detail.getIndoorLocationSurpportBuidlingName());
        mapObject.put("IndoorLocMode", detail.getIndoorLocMode());
        mapObject.put("BuildingName", detail.getBuildingName());
        mapObject.put("Floor", detail.getFloor());
        mapObject.put("GpsAccuracyStatus", detail.getGpsAccuracyStatus());
        mapObject.put("networktype", detail.getNetworktype());

        mapObject.saveInBackground(new SaveCallback() {
            @Override
            public void done(AVException e) {
                if (e == null) {
                    RxToast.success("昨日数据上传成功");
                    RxSPTool.putString(MapBaseApplication.getMapBaseApplicationInstance(), DateUtil.getYesterdayTime_Y_M_d(), "UpSuccess");
                }
            }
        });

    }

    private void initData() {
        String upYesterday = RxSPTool.getString(MapBaseApplication.getMapBaseApplicationInstance(), DateUtil.getYesterdayTime_Y_M_d());
        if (RxDeviceTool.isNullString(upYesterday) ) {
            Log.e(TAG, "用户第一次安装,昨天和今天上传数据的状态都为空");
            RxSPTool.putString(MapBaseApplication.getMapBaseApplicationInstance(), DateUtil.getYesterdayTime_Y_M_d(), "NoUpDate");
        }
        try {
            phonename = RxDeviceTool.getUniqueSerialNumber() + "";
            phoneNumber = RxDeviceTool.getLine1Number(getApplicationContext());
        } catch (Exception e) {
            phoneNumber = "";
        }
        String appversion = RxAppTool.getAppVersionName(MapBaseApplication.getMapBaseApplicationInstance());
        appVersion.setText("MCS Version:"+appversion);
    }

    private void UpDateYesterdayData(int type) {
        if (!RxNetTool.isConnected(mContext)) {
            RxToast.error("上传数据请保持网络畅通");
            return;
        }
        Log.e(TAG,"------昨天的日期为"+DateUtil.getYesterdayTime_Y_M_d());
        Log.e(TAG,"------今天的日期为"+DateUtil.getCurrentTime_Y_M_d());
        List<mapdetails> list2 = mapDao.queryBuilder().where(mapdetailsDao.Properties.DataTime.isNotNull()).build().list();
        List<mapdetails> list = mapDao.queryBuilder().where(mapdetailsDao.Properties.DataTime.like(DateUtil.getYesterdayTime_Y_M_d())).build().list();
        Log.e(TAG, "------------>昨天的数据库的数量为：" + list.size());
        Log.e(TAG, "------------>今天的数据库的数量为：" + mapDao.queryBuilder().where(mapdetailsDao.Properties.DataTime.like(DateUtil.getCurrentTime_Y_M_d())).build().list().size());
        Log.e(TAG,"--------->数据库的所有数据的数量为"+list2.size());
        if (list.size() != 0) {
            if (type == 3) {
                Log.e(TAG, "UpDateYesterdayData-------程序每次打开------主动上传数据");
                if (RxSPTool.getString(MapBaseApplication.getMapBaseApplicationInstance(), DateUtil.getYesterdayTime_Y_M_d()).contains("UpSuccess")) {
                    Log.e(TAG, "type=3 ------ 昨日数据上传成功,无须再次上传");
                } else {
                    for (mapdetails data : list) {
                        UpdataByNet(data);
                    }
                }
            }
            if (type == 4) {
                Log.e(TAG, "UpDateYesterdayData-------用户点击上传按钮------被动上传数据");
                if (RxSPTool.getString(MapBaseApplication.getMapBaseApplicationInstance(), DateUtil.getYesterdayTime_Y_M_d()).contains("UpSuccess")) {
                    RxToast.info("昨日数据上传成功,无须再次上传");
                } else {
                    for (mapdetails data : list) {
                        UpdataByNet(data);
                    }
                }
            }
        } else {
            Log.e(TAG, "------昨天的数据库的数量为0");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        isForeground = true;
        if (Contants.DEBUG)
            Log.e(TAG, "MCS---MainActivity-->onStart");
        //RxTools网络监测广播
        broadcastReceiverNetWork = RxBroadcastTool.initRegisterReceiverNetWork(mContext);

        //周一或周五提醒一次
        String date = DateUtil.getWeek(new Date());
        Log.d(TAG,"--------"+date);
        if(isTodayFirstLogin()){
            if(date.contains("Friday")){
                IntentWrapper.whiteListMatters(this, "MCS实验系统持续运行");
            }
            if(date.contains("Monday")){
                IntentWrapper.whiteListMatters(this, "MCS实验系统持续运行");
            }
        }

        mHandler.sendMessage(Message.obtain(mHandler, MainActivity.upMapDateActive));

    }

    @Override
    protected void onResume() {
        super.onResume();
        isTodayFirstLogin();
        isForeground = true;

    }

    @Override
    protected void onPause() {
        isForeground = false;
        super.onPause();
        saveExitTime(todayTime);
    }

    @Override
    protected void onStop() {
        isForeground = false;
        //RxTools销毁广播
        unregisterReceiver(broadcastReceiverNetWork);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        isForeground = false;
        if (Contants.DEBUG)
            Log.e(TAG, "MCS-MainActivity-->onDestroy");
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

    private void initViews() {
        btn_quit = (Button) findViewById(R.id.btn_quit);
        btn_update = (Button) findViewById(R.id.btn_updata);
        btn_quit.setOnClickListener(this);
        btn_update.setOnClickListener(this);
        appVersion = (TextView)findViewById(R.id.tx_app);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_updata:
                mHandler.sendMessage(Message.obtain(mHandler, MainActivity.upMapDateUnActive));
                break;
            case R.id.btn_quit:
                long secondTime = System.currentTimeMillis();
                if (secondTime - firstTime < 2000) {
                    RxActivityTool.finishActivity(MainActivity.class);
                    mHandler.sendMessage(Message.obtain(mHandler, MainActivity.quit));
                } else {
                    RxToast.info("再按一次退出程序");
                    firstTime = System.currentTimeMillis();
                }
                break;
        }
    }

    /**
     * 跳转到权限设置界面
     */
    private void getAppDetailSettingIntent(Context context){
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if(Build.VERSION.SDK_INT >= 9){
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.fromParts("package", getPackageName(), null));
        } else if(Build.VERSION.SDK_INT <= 8){
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName("com.android.settings","com.android.settings.InstalledAppDetails");
            intent.putExtra("com.android.settings.ApplicationPkgName", getPackageName());

        }
        context.startActivity(intent);
    }

    public boolean isNotificationEnabled(final Context context){
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ){
            Log.i("11111","通知栏功能");
        }else{
            AppOpsManager mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            ApplicationInfo appInfo = context.getApplicationInfo();
            String pkg = context.getApplicationContext().getPackageName();
            int uid = appInfo.uid;
            Class appOpsClass = null;
      		/* Context.APP_OPS_MANAGER */
            try {
                appOpsClass = Class.forName(AppOpsManager.class.getName());
                Method checkOpNoThrowMethod = appOpsClass.getMethod(CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE,
                        String.class);
                Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);
                int value = (Integer) opPostNotificationValue.get(Integer.class);
                if((Integer) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) == AppOpsManager.MODE_ALLOWED){
                }else{
                    showDialogs(MainActivity.this);
                }
                return ((Integer) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) == AppOpsManager.MODE_ALLOWED);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }
        return false;
    }

    private void showDialogs( final Context mcontext) {
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("请开启通知栏")
                .setContentText("点击确定 --> 自定义通知(或通知管理) --> 允许通知")
                .setCancelText("取消")
                .setConfirmText("确定")
                .showCancelButton(true)
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        // reuse previous dialog instance, keep widget user state, reset them if you need
                        sDialog.setTitleText("取消了操作")
                                .setContentText("为了更好的运行,请您保持开启")
                                .setConfirmText("OK")
                                .showCancelButton(false)
                                .setCancelClickListener(null)
                                .setConfirmClickListener(null)
                                .changeAlertType(SweetAlertDialog.ERROR_TYPE);
                    }
                })
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismiss();
                        getAppDetailSettingIntent(MapBaseApplication.getMapBaseApplicationInstance());
                        if(isNotificationEnabled(MapBaseApplication.getMapBaseApplicationInstance()) && isForeground) {
                            sDialog.setTitleText("完成")
                                    .setContentText("操作完成")
                                    .setConfirmText("OK")
                                    .showCancelButton(false)
                                    .setCancelClickListener(null)
                                    .setConfirmClickListener(null)
                                    .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                        }
                        else{
                            sDialog.setTitleText("取消了操作")
                                    .setContentText("为了更好的运行,请您保持开启")
                                    .setConfirmText("OK")
                                    .showCancelButton(false)
                                    .setCancelClickListener(null)
                                    .setConfirmClickListener(null)
                                    .changeAlertType(SweetAlertDialog.ERROR_TYPE);
                        }

                    }
                })
                .show();
    }
}
