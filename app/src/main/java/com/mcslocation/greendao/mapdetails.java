package com.mcslocation.greendao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by ly on 2017/11/12.
 */
@Entity
public class mapdetails {
    @Id
    private Long id;

    //定位是否成功+用户是否想主动退出。Success Fail Quit
    private String Success;
    //手机设备名称
    private String PhoneName;
    //手机号码
    private String PhoneNumber;
    //手机端请求服务器定位的时间
    private String ClientTime;
    //服务端出本次结果的时间:
    private String ServerTime;
    // 定位类型
    private int LocType;
    // 纬度
    private double Latitude;
    // 经度
    private double Longitude;
    // 半径
    private float Radius;
    // 城市
    private String City;
    // 区
    private String District;
    // 街道
    private String Street;
    // 地址信息
    private String AddrStr;
    // *****返回用户室内外判断结果*****
    private int UserIndoorState;
    // 位置语义化信息
    private String LocationDescribe;
    // POI信息
    private String PoiList;
    // 运营商信息
    private int Operators;
    //定位描述
    private String describe;
    //是否有网络
    private String isNetAble;
    //wifi是否打开
    private String isWifiAble;
    //GPS是否打开
    private String GPSStatus;
    @Generated(hash = 741531125)
    public mapdetails(Long id, String Success, String PhoneName, String PhoneNumber,
            String ClientTime, String ServerTime, int LocType, double Latitude,
            double Longitude, float Radius, String City, String District,
            String Street, String AddrStr, int UserIndoorState,
            String LocationDescribe, String PoiList, int Operators, String describe,
            String isNetAble, String isWifiAble, String GPSStatus) {
        this.id = id;
        this.Success = Success;
        this.PhoneName = PhoneName;
        this.PhoneNumber = PhoneNumber;
        this.ClientTime = ClientTime;
        this.ServerTime = ServerTime;
        this.LocType = LocType;
        this.Latitude = Latitude;
        this.Longitude = Longitude;
        this.Radius = Radius;
        this.City = City;
        this.District = District;
        this.Street = Street;
        this.AddrStr = AddrStr;
        this.UserIndoorState = UserIndoorState;
        this.LocationDescribe = LocationDescribe;
        this.PoiList = PoiList;
        this.Operators = Operators;
        this.describe = describe;
        this.isNetAble = isNetAble;
        this.isWifiAble = isWifiAble;
        this.GPSStatus = GPSStatus;
    }
    @Generated(hash = 1849985267)
    public mapdetails() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getSuccess() {
        return this.Success;
    }
    public void setSuccess(String Success) {
        this.Success = Success;
    }
    public String getPhoneName() {
        return this.PhoneName;
    }
    public void setPhoneName(String PhoneName) {
        this.PhoneName = PhoneName;
    }
    public String getPhoneNumber() {
        return this.PhoneNumber;
    }
    public void setPhoneNumber(String PhoneNumber) {
        this.PhoneNumber = PhoneNumber;
    }
    public String getClientTime() {
        return this.ClientTime;
    }
    public void setClientTime(String ClientTime) {
        this.ClientTime = ClientTime;
    }
    public String getServerTime() {
        return this.ServerTime;
    }
    public void setServerTime(String ServerTime) {
        this.ServerTime = ServerTime;
    }
    public int getLocType() {
        return this.LocType;
    }
    public void setLocType(int LocType) {
        this.LocType = LocType;
    }
    public double getLatitude() {
        return this.Latitude;
    }
    public void setLatitude(double Latitude) {
        this.Latitude = Latitude;
    }
    public double getLongitude() {
        return this.Longitude;
    }
    public void setLongitude(double Longitude) {
        this.Longitude = Longitude;
    }
    public float getRadius() {
        return this.Radius;
    }
    public void setRadius(float Radius) {
        this.Radius = Radius;
    }
    public String getCity() {
        return this.City;
    }
    public void setCity(String City) {
        this.City = City;
    }
    public String getDistrict() {
        return this.District;
    }
    public void setDistrict(String District) {
        this.District = District;
    }
    public String getStreet() {
        return this.Street;
    }
    public void setStreet(String Street) {
        this.Street = Street;
    }
    public String getAddrStr() {
        return this.AddrStr;
    }
    public void setAddrStr(String AddrStr) {
        this.AddrStr = AddrStr;
    }
    public int getUserIndoorState() {
        return this.UserIndoorState;
    }
    public void setUserIndoorState(int UserIndoorState) {
        this.UserIndoorState = UserIndoorState;
    }
    public String getLocationDescribe() {
        return this.LocationDescribe;
    }
    public void setLocationDescribe(String LocationDescribe) {
        this.LocationDescribe = LocationDescribe;
    }
    public String getPoiList() {
        return this.PoiList;
    }
    public void setPoiList(String PoiList) {
        this.PoiList = PoiList;
    }
    public int getOperators() {
        return this.Operators;
    }
    public void setOperators(int Operators) {
        this.Operators = Operators;
    }
    public String getDescribe() {
        return this.describe;
    }
    public void setDescribe(String describe) {
        this.describe = describe;
    }
    public String getIsNetAble() {
        return this.isNetAble;
    }
    public void setIsNetAble(String isNetAble) {
        this.isNetAble = isNetAble;
    }
    public String getIsWifiAble() {
        return this.isWifiAble;
    }
    public void setIsWifiAble(String isWifiAble) {
        this.isWifiAble = isWifiAble;
    }
    public String getGPSStatus() {
        return this.GPSStatus;
    }
    public void setGPSStatus(String GPSStatus) {
        this.GPSStatus = GPSStatus;
    }

}
