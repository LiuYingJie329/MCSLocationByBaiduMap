package com.mcslocation.mapservice;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.Poi;
import com.mcslocation.activity.MainActivity;
import com.mcslocation.application.MapBaseApplication;
import com.mcslocation.greendao.mapdetails;
import com.mcslocation.mapdetailsDao;
import com.mcslocation.tools.DateUtil;
import com.mcslocation.tools.Rx.RxDeviceTool;
import com.mcslocation.tools.Rx.RxNetTool;
import com.mcslocation.tools.TimeFormatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ly on 2017/11/8.
 */

public class LocationService extends Service {
    private static final String TAG = LocationService.class.getSimpleName();
    private BaiduMapLocationService locationService;
    private mapdetailsDao mapDao = MapBaseApplication.getMapBaseApplicationInstance().getDaoInstance().getMapdetailsDao();
    private List<String> PoiList = new ArrayList<String>();
    private String phonename = null;
    private String phoneNumber = null;
    private long firstTime = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            phonename = RxDeviceTool.getUniqueSerialNumber() + "";
            phoneNumber = RxDeviceTool.getLine1Number(getApplicationContext());
        } catch (Exception e) {
            phoneNumber = "";
        }

        locationService = ((MapBaseApplication) getApplication()).locationService;
        //获取locationservice实例，建议应用中只初始化1个location实例，然后使用，可以参考其他示例的activity，都是通过此种方式获取locationservice实例的
        locationService.registerListener(mListener);
        //注册监听
        int type = intent.getIntExtra("from", 0);
        if (type == 0) {
            locationService.setLocationOption(locationService.getDefaultLocationClientOption());
        } else if (type == 1) {
            locationService.setLocationOption(locationService.getOption());
        }
        locationService.start();// 定位SDK. start之后会默认发起一次定位请求，开发者无须判断isstart并主动调用request
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        locationService.unregisterListener(mListener); //注销掉监听
        locationService.stop(); //停止定位服务
        // 重启自己
        Intent intent = new Intent(getApplicationContext(), LocationService.class);
        startService(intent);
        super.onDestroy();
    }

    private BDAbstractLocationListener mListener = new BDAbstractLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation location) {
            long secondTime = System.currentTimeMillis();
            //10分钟定位一次，10min = 10*60*000ms
            //正负一分钟时间差
            if (Math.abs(secondTime - firstTime) >= 1 * 60 * 1000 ) {
                if (null != location && location.getLocType() != BDLocation.TypeServerError) {
                    mapdetails mapdetail = new mapdetails();
                    mapdetail.setSuccess("Success");   //请求成功
                    mapdetail.setPhoneName(phoneNumber); //设备的手机号码
                    mapdetail.setPhoneName(phonename);  //设备名称
                    mapdetail.setDataTime(DateUtil.getCurrentTime_Y_M_d());//客户端设备时间
                    firstTime = System.currentTimeMillis();
                    mapdetail.setClientTime(TimeFormatUtils.formatUTC(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss")); //手机端请求时间
                    mapdetail.setServerTime(location.getTime()); //服务端返回的时间,如果位置不发生变化，则时间不变
                    mapdetail.setLocType(location.getLocType()); //定位类型
                    mapdetail.setLongitude(location.getLongitude());//经度
                    mapdetail.setLatitude(location.getLatitude()); //纬度
                    mapdetail.setRadius(location.getRadius()); //定位精度
                    mapdetail.setCity(location.getCity());// 城市
                    mapdetail.setDistrict(location.getDistrict());// 区
                    mapdetail.setStreet(location.getStreet());// 街道
                    mapdetail.setAddrStr(location.getAddrStr());// 地址信息
                    mapdetail.setUserIndoorState(location.getUserIndoorState());// 返回用户室内外判断结果
                    mapdetail.setLocationDescribe(location.getLocationDescribe());// 位置语义化信息
                    if (location.getPoiList() != null && !location.getPoiList().isEmpty()) {
                        for (int i = 0; i < location.getPoiList().size(); i++) {
                            Poi poi = (Poi) location.getPoiList().get(i);
                            PoiList.add(poi.getName());
                        }
                    }
                    mapdetail.setPoiList(PoiList.toString());
                    if (location.getLocType() == BDLocation.TypeGpsLocation) {
                        // GPS定位结果
                        mapdetail.setDescribe("gps定位成功");
                    } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {
                        // 网络定位结果
                        // 运营商信息
                        mapdetail.setOperators(location.getOperators());// 运营商信息
                        mapdetail.setDescribe("网络定位成功");
                    } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {
                        // 离线定位结果
                        mapdetail.setDescribe("离线定位成功，离线定位结果也是有效的");
                    } else if (location.getLocType() == BDLocation.TypeServerError) {
                        mapdetail.setDescribe("百度服务器网络定位失败");
                    } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                        mapdetail.setDescribe("网络不同导致定位失败，请检查网络是否通畅");
                    } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                        mapdetail.setDescribe("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
                    }
                    mapdetail.setIsNetAble((RxNetTool.isConnected(getApplicationContext()) + "").contains("true") ? "Net true" : "Net false");//判断网络是否连接
                    mapdetail.setIsWifiAble((RxNetTool.isWifi(getApplicationContext()) + "").contains("true") ? "Wifi true" : "Wifi false");//判断网络连接方式是否为WIFI
                    mapdetail.setGPSStatus((RxNetTool.isGpsEnabled(getApplicationContext()) + "").contains("true") ? "GPS true" : "GPS false");//GPS是否打开
                    mapDao.insert(mapdetail);
                    Message msg = new Message();
                    msg.obj = mapdetail;
                    msg.what = MainActivity.upload;
                    MainActivity.mHandler.sendMessage(msg);
                }
            }
        }

    };

}
