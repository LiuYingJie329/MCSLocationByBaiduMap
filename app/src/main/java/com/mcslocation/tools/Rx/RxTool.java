package com.mcslocation.tools.Rx;

import android.content.Context;
import android.os.Handler;

import com.mcslocation.interfaces.OnDelayListener;

public class RxTool {

    private static Context context;
    private static long lastClickTime;

    /**
     * 初始化工具类
     *
     * @param context 上下文
     */
    public static void init(Context context) {
        RxTool.context = context.getApplicationContext();
        RxCrashTool.init(context);
    }

    /**
     * 在某种获取不到 Context 的情况下，即可以使用才方法获取 Context
     * <p>
     * 获取ApplicationContext
     *
     * @return ApplicationContext
     */
    public static Context getContext() {
        if (context != null) return context;
        throw new NullPointerException("请先调用init()方法");
    }
    //==============================================================================================延时任务封装 end

    //----------------------------------------------------------------------------------------------延时任务封装 start
    public static void delayToDo(long delayTime, final OnDelayListener onDelayListener) {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                //execute the task
                onDelayListener.doSomething();
            }
        }, delayTime);
    }



}
