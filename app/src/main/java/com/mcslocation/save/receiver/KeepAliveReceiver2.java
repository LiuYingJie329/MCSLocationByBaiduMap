package com.mcslocation.save.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;

import com.mcslocation.activity.MainActivity;
import com.mcslocation.save.utils.Contants;
import com.mcslocation.save.utils.SystemUtils;
import com.vondear.rxtools.RxBroadcastTool;
import com.vondear.rxtools.RxNetTool;

/**
 * Created by ly on 2017/11/12.
 */

public class KeepAliveReceiver2 extends BroadcastReceiver {

    private static final String TAG = KeepAliveReceiver2.class.getSimpleName();
    /**
     * 网络状态改变广播
     */

    @Override
    public void onReceive(Context context, Intent intent) {
        RxNetTool.getNetWorkType(context);
        if(SystemUtils.isAppAlive(context, Contants.PACKAGE_NAME)){
            Log.i(TAG,"MCS-->KeepAliveReceiver-->AliveBroadcastReceiver---->APP还是活着的");
            return;
        }
        Intent intentAlive = new Intent(context, MainActivity.class);
        intentAlive.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intentAlive);
        Log.i(TAG,"MCS-->KeepAliveReceiver-->AliveBroadcastReceiver---->复活进程(APP)");
    }

    /**
     * 注册监听网络状态的广播
     * @param context
     * @return
     */
    public static RxBroadcastTool.BroadcastReceiverNetWork initRegisterReceiverNetWork(Context context) {
        // 注册监听网络状态的服务
        RxBroadcastTool.BroadcastReceiverNetWork mReceiverNetWork = new RxBroadcastTool.BroadcastReceiverNetWork();
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(mReceiverNetWork, mFilter);
        return mReceiverNetWork;
    }
}
