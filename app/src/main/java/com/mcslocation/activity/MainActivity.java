package com.mcslocation.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.Poi;
import com.mcslocation.R;
import com.mcslocation.application.MapBaseApplication;
import com.mcslocation.mapservice.BaiduMapLocationService;
import com.mcslocation.save.receiver.KeepAliveReceiver2;
import com.mcslocation.save.receiver.ScreenReceiverUtil;
import com.mcslocation.save.service.DaemonService;
import com.mcslocation.save.service.PlayerMusicService;
import com.mcslocation.save.utils.Contants;
import com.mcslocation.save.utils.JobSchedulerManager;
import com.mcslocation.save.utils.ScreenManager;
import com.mcslocation.tools.CheckPermissionsActivity;
import com.mcslocation.tools.TimeFormatUtils;
import com.vondear.rxtools.RxBroadcastTool;
import com.vondear.rxtools.RxDeviceTool;
import com.vondear.rxtools.RxNetTool;

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

    private String phonename =null;
    private String phoneNumber = null;

    private RxBroadcastTool.BroadcastReceiverNetWork broadcastReceiverNetWork;
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
            Intent intent = new Intent(MainActivity.this,MainActivity.class);
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(Contants.DEBUG)
            Log.d(TAG,"MCS-MainActivity-->onCreate");
        // 1. 注册锁屏广播监听器
        mScreenListener = new ScreenReceiverUtil(this);
        mScreenManager = ScreenManager.getScreenManagerInstance(this);
        mScreenListener.setScreenReceiverListener(mScreenListenerer);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            // 2. 启动系统任务
            mJobManager = JobSchedulerManager.getJobSchedulerInstance(this);
            mJobManager.startJobScheduler();
        }
        // 3. 华为推送保活，允许接收透传
//        mHwPushManager = HwPushManager.getInstance(this);
//        mHwPushManager.startRequestToken();
//        mHwPushManager.isEnableReceiveNormalMsg(true);
//        mHwPushManager.isEnableReceiverNotifyMsg(true);
        try{
            phonename = RxDeviceTool.getUniqueSerialNumber()+"";
            phoneNumber = RxDeviceTool.getLine1Number(this);
            Log.e(TAG,"手机唯一标识序列号"+phonename);
            Log.e(TAG,"手机号码"+phoneNumber);
        }catch (Exception e){
            phoneNumber = "";
        }
        // 3. 启动前台Service
        startDaemonService();
        // 4. 启动播放音乐Service
        startPlayMusicService();

        // 测试 leancloud SDK 是否正常工作的代码
//        AVObject testObject = new AVObject("TestObject");
//        testObject.put("words","Hello World!");
//        testObject.saveInBackground(new SaveCallback() {
//            @Override
//            public void done(AVException e) {
//                if(e == null){
//                    Log.d("saved","success!");
//                }
//            }
//        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(Contants.DEBUG)
            Log.d(TAG,"MCS-MainActivity-->onStart");
        // -----------location config ------------
        locationService = ((MapBaseApplication) getApplication()).locationService;
        //获取locationservice实例，建议应用中只初始化1个location实例，然后使用，可以参考其他示例的activity，都是通过此种方式获取locationservice实例的
        locationService.registerListener(mListener);
        //注册监听
        int type = getIntent().getIntExtra("from", 0);
        if (type == 0) {
            locationService.setLocationOption(locationService.getDefaultLocationClientOption());
        } else if (type == 1) {
            locationService.setLocationOption(locationService.getOption());
        }
        locationService.start();// 定位SDK. start之后会默认发起一次定位请求，开发者无须判断isstart并主动调用request
        //locationService.stop(); //停止定位


    }

    @Override
    protected void onResume() {
        super.onResume();
        //RxTools网络监测广播
        broadcastReceiverNetWork = KeepAliveReceiver2.initRegisterReceiverNetWork(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //RxTools销毁广播
        unregisterReceiver(broadcastReceiverNetWork);
    }

    private void stopPlayMusicService() {
        Intent intent = new Intent(MainActivity.this, PlayerMusicService.class);
        stopService(intent);
    }

    private void startPlayMusicService() {
        Intent intent = new Intent(MainActivity.this,PlayerMusicService.class);
        startService(intent);
    }

    private void startDaemonService() {
        Intent intent = new Intent(MainActivity.this, DaemonService.class);
        startService(intent);
    }

    private void stopDaemonService() {
        Intent intent = new Intent(MainActivity.this, DaemonService.class);
        stopService(intent);
    }
    @Override
    protected void onStop() {
        locationService.unregisterListener(mListener); //注销掉监听
        locationService.stop(); //停止定位服务
        if(Contants.DEBUG)
            Log.d(TAG,"MCS-MainActivity-->onDestroy");
        super.onStop();
    }

    private BDAbstractLocationListener mListener = new BDAbstractLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (null != location && location.getLocType() != BDLocation.TypeServerError) {
                StringBuffer sb = new StringBuffer(256);
                sb.append("请求时间:"+ TimeFormatUtils.formatUTC(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss")+"\n");
                sb.append("服务端出本次结果的时间: ");
                /**
                 * 时间也可以使用systemClock.elapsedRealtime()方法 获取的是自从开机以来，每次回调的时间；
                 * location.getTime() 是指服务端出本次结果的时间，如果位置不发生变化，则时间不变
                 */

                sb.append(location.getTime());
                sb.append("\n定位类型 : ");// 定位类型
                sb.append(location.getLocType());
                sb.append("\n对应的定位类型说明 : ");// *****对应的定位类型说明*****
                sb.append(location.getLocTypeDescription());
                sb.append("\n纬度 : ");// 纬度
                sb.append(location.getLatitude());
                sb.append("\n经度 : ");// 经度
                sb.append(location.getLongitude());
                sb.append("\n半径 : ");// 半径
                sb.append(location.getRadius());
                sb.append("\n国家码 : ");// 国家码
                sb.append(location.getCountryCode());
                sb.append("\n国家名称 : ");// 国家名称
                sb.append(location.getCountry());
                sb.append("\n城市编码 : ");// 城市编码
                sb.append(location.getCityCode());
                sb.append("\n城市 : ");// 城市
                sb.append(location.getCity());
                sb.append("\n区 : ");// 区
                sb.append(location.getDistrict());
                sb.append("\n街道 : ");// 街道
                sb.append(location.getStreet());
                sb.append("\n地址信息 : ");// 地址信息
                sb.append(location.getAddrStr());
                sb.append("\n返回用户室内外判断结果: ");// *****返回用户室内外判断结果*****
                sb.append(location.getUserIndoorState());
                sb.append("\nDirection(not all devices have value): ");
                sb.append(location.getDirection());// 方向
                sb.append("\n位置语义化信息: ");
                sb.append(location.getLocationDescribe());// 位置语义化信息
                sb.append("\nPOI信息: ");// POI信息
                if (location.getPoiList() != null && !location.getPoiList().isEmpty()) {
                    for (int i = 0; i < location.getPoiList().size(); i++) {
                        Poi poi = (Poi) location.getPoiList().get(i);
                        sb.append(poi.getName() + ";");
                    }
                }
                if (location.getLocType() == BDLocation.TypeGpsLocation) {
                    // GPS定位结果
                    sb.append("\nGPS定位速度 : ");
                    sb.append(location.getSpeed());// 速度 单位：km/h
                    sb.append("\n卫星数目 : ");
                    sb.append(location.getSatelliteNumber());// 卫星数目
                    sb.append("\n海拔高度 : ");
                    sb.append(location.getAltitude());// 海拔高度 单位：米
                    sb.append("\ngps质量判断 : ");
                    sb.append(location.getGpsAccuracyStatus());// *****gps质量判断*****
                    sb.append("\ndescribe : ");
                    sb.append("gps定位成功");
                } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {
                    // 网络定位结果
                    // 运营商信息
                    if (location.hasAltitude()) {// *****如果有海拔高度*****
                        sb.append("\nheight : ");
                        sb.append(location.getAltitude());// 单位：米
                    }
                    sb.append("\n运营商信息 : ");// 运营商信息
                    sb.append(location.getOperators());
                    sb.append("\ndescribe : ");
                    sb.append("网络定位成功");
                } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {
                    // 离线定位结果
                    sb.append("\ndescribe : ");
                    sb.append("离线定位成功，离线定位结果也是有效的");
                } else if (location.getLocType() == BDLocation.TypeServerError) {
                    sb.append("\ndescribe : ");
                    sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
                } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                    sb.append("\ndescribe : ");
                    sb.append("网络不同导致定位失败，请检查网络是否通畅");
                } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                    sb.append("\ndescribe : ");
                    sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
                }
                Toast.makeText(getApplicationContext(),"定位成功",Toast.LENGTH_SHORT).show();
                Log.i(TAG,"MCS-->MainActivity-->定位结果："+sb.toString());

            }
        }
    };
}
