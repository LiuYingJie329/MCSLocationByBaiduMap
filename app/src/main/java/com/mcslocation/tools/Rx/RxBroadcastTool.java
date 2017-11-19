package com.mcslocation.tools.Rx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

/**
 * Created by Vondear on 2017/3/15.
 */

public class RxBroadcastTool {

    private static final String TAG = RxBroadcastTool.class.getSimpleName();
    private static boolean disnet = false;
    /**
     * 注册监听网络状态的广播
     * @param context
     * @return
     */
    public static BroadcastReceiverNetWork initRegisterReceiverNetWork(Context context) {
        // 注册监听网络状态的服务
        BroadcastReceiverNetWork mReceiverNetWork = new BroadcastReceiverNetWork();
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(mReceiverNetWork, mFilter);
        return mReceiverNetWork;
    }

    /**
     * 网络状态改变广播
     */
    public static class BroadcastReceiverNetWork extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            RxNetTool.getNetWorkType(context);
            RxNetTool.isWifi(context);
        }
    }
}
