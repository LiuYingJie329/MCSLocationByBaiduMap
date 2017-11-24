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
import com.mcslocation.tools.Rx.RxLocationTool;
import com.mcslocation.tools.Rx.RxNetTool;
import com.mcslocation.tools.Rx.RxToast;
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
        super.onDestroy();
        locationService.unregisterListener(mListener); //注销掉监听
        locationService.stop(); //停止定位服务
        // 重启自己
        Intent intent = new Intent(getApplicationContext(), LocationService.class);
        startService(intent);
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
                    // 返回用户室内外判断结果
                    //1----#USER_INDDOR_TRUE,
                    //0--- #USER_INDOOR_FALSE,
                    //-1--- #USER_INDOOR_UNKNOW
                    mapdetail.setUserIndoorState(location.getUserIndoorState());
                    mapdetail.setLocationDescribe(location.getLocationDescribe());// 位置语义化信息
                    if (location.getPoiList() != null && !location.getPoiList().isEmpty()) {
                        for (int i = 0; i < location.getPoiList().size(); i++) {
                            Poi poi = (Poi) location.getPoiList().get(i);
                            PoiList.add(poi.getName());
                        }
                    }
                    mapdetail.setPoiList(PoiList.toString());

                    //返回是否支持室内定位
                    //2--#INDOOR_LOCATION_NEARBY_SURPPORT_TRUE,
                    //0--#INDOOR_LOCATION_SURPPORT_FALSE,#INDOOR_LOCATION_SURPPORT_UNKNOWN
                    mapdetail.setIndoorLocationSurpport(location.getIndoorLocationSurpport());
                    //返回支持的室内定位类型
                    //1---#INDOOR_LOCATION_SOURCE_WIFI,
                    //4---#INDOOR_LOCATION_SOURCE_BLUETOOTH,
                    //2--#INDOOR_LOCATION_SOURCE_MAGNETIC,
                    //8--#INDOOR_LOCATION_SOURCE_SMALLCELLSTATION,
                    //0--#INDOOR_LOCATION_SOURCE_UNKNOWN
                    mapdetail.setIndoorLocationSource(location.getIndoorLocationSource());
                    //返回室内定位网络状态
                    //2--#INDOOR_NETWORK_STATE_HIGH,
                    //0--#INDOOR_NETWORK_STATE_LOW,
                    //1--#INDOOR_NETWORK_STATE_MIDDLE
                    mapdetail.setIndoorNetworkState(location.getIndoorNetworkState());
                    //返回支持室内定位的building名称
                    mapdetail.setIndoorLocationSurpportBuidlingName(location.getIndoorLocationSurpportBuidlingName());
                    //是否处于室内定位模式
                    mapdetail.setIndoorLocMode(location.isIndoorLocMode()+"");
                    //获取buildingname信息，目前只在百度支持室内定位的地方有返回，默认null
                    mapdetail.setBuildingName(location.getBuildingName());
                    //获取楼层信息，目前只在百度支持室内定位的地方有返回，默认null
                    mapdetail.setFloor(location.getFloor());
                    if (location.getLocType() == BDLocation.TypeGpsLocation) {

                        //如果是GPS位置，获得当前由百度自有算法判断的GPS质量,
                        //1---#GPS_ACCURACY_GOOD ,
                        //2---#GPS_ACCURACY_MID,
                        //3-- #GPS_ACCURACY_BAD
                        //0--#GPS_ACCURACY_UNKNOWN
                        mapdetail.setGpsAccuracyStatus(location.getGpsAccuracyStatus());
                        // GPS定位结果
                        mapdetail.setDescribe("gps定位成功");
                    } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {
                        // 网络定位结果
                        // 运营商信息
                        //0--OPERATORS_TYPE_UNKONW : 未知运营商;
                        //1--OPERATORS_TYPE_MOBILE : 中国移动；
                        //2--OPERATORS_TYPE_UNICOM : 中国联通；
                        //3--OPERATORS_TYPE_TELECOMU : 中国电信
                        mapdetail.setOperators(location.getOperators());// 运营商信息

                        //在网络定位结果的情况下，获取网络定位结果是通过基站定位得到的还是通过wifi定位得到的还是GPS得结果
                        String networktype = location.getNetworkLocationType();
                        mapdetail.setNetworktype(location.getNetworkLocationType());
                        //获取定位类型相关描述信息
                        location.getLocTypeDescription();

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
                    mapdetail.setGPSStatus((RxLocationTool.isGpsEnabled(getApplicationContext()) + "").contains("true") ? "GPS true" : "GPS false");//GPS是否打开
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
