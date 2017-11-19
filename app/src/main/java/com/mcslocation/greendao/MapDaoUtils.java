package com.mcslocation.greendao;

import android.content.Context;
import android.util.Log;

import com.mcslocation.mapdetailsDao;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

/**
 * Created by ly on 2017/10/12.
 */

public class MapDaoUtils {
    private static final String TAG = MapDaoUtils.class.getSimpleName();
    private MapDaoManager mManager;

    public MapDaoUtils(Context context) {
        mManager = MapDaoManager.getInstance();
        mManager.init(context);
    }

    /**
     * 完成Location记录的插入，如果表未创建，先创建Location表
     *
     * @param Location
     * @return
     */
    public boolean insertLocation(mapdetails Location) {
        boolean flag = false;

        flag = mManager.getDaoSession().getMapdetailsDao().insert(Location) == -1 ? false : true;
        Log.e(TAG, "insert Location :" + flag + "-->" + Location.toString());

        return flag;
    }

    /**
     * 插入多条数据，在子线程操作
     * @param LocationList
     * @return
     */
    public boolean insertMultLocation(final List<mapdetails> LocationList) {
        boolean flag = false;
        try {
            mManager.getDaoSession().runInTx(new Runnable() {
                @Override
                public void run() {
                    for (mapdetails Location : LocationList) {
                        mManager.getDaoSession().insertOrReplace(Location);
                    }
                }
            });
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 修改一条数据
     * @param Location
     * @return
     */
    public boolean updateLocation(mapdetails Location){
        boolean flag = false;
        try {
            mManager.getDaoSession().update(Location);
            flag = true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 删除单条记录
     * @param Location
     * @return
     */
    public boolean deleteLocation(mapdetails Location){
        boolean flag = false;
        try {
            //按照id删除
            mManager.getDaoSession().delete(Location);
            flag = true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 删除所有记录
     * @return
     */
    public boolean deleteAll(){
        boolean flag = false;
        try {
            //按照id删除
            mManager.getDaoSession().deleteAll(mapdetails.class);
            flag = true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 查询所有记录
     * @return
     */
    public List<mapdetails> queryAllLocation(){
        return mManager.getDaoSession().loadAll(mapdetails.class);
    }

    /**
     * 根据主键id查询记录
     * @param key
     * @return
     */
    public mapdetails queryLocationById(long key){
        return mManager.getDaoSession().load(mapdetails.class, key);
    }

    /**
     * 使用native sql进行查询操作
     */
    public List<mapdetails> queryLocationByNativeSql(String sql, String[] conditions){
        return mManager.getDaoSession().queryRaw(mapdetails.class, sql, conditions);
    }

    /**
     * 使用queryBuilder进行查询
     * @return
     */
    public List<mapdetails> queryLocationByQueryBuilder(long id){
        QueryBuilder<mapdetails> queryBuilder = mManager.getDaoSession().queryBuilder(mapdetails.class);
        return queryBuilder.where(mapdetailsDao.Properties.Id.eq(id)).list();
    }
}
