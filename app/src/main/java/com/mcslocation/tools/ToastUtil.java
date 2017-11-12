package com.mcslocation.tools;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mcslocation.R;
import com.mcslocation.application.MapBaseApplication;


public class ToastUtil {

	private static Handler baseHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0: {
				Bundle bundle = msg.getData();
				String text = bundle.getString("text");
				int duration = bundle.getInt("duration");
				makeText(MapBaseApplication.getMapBaseApplicationInstance(), text, duration).show();
			}break;
			default: {

			}break;
			}
		}
	};

	public static void toast(String str) {
		Message msg = baseHandler.obtainMessage(0);
		Bundle bundle = new Bundle();
		bundle.putString("text", str);
		bundle.putInt("duration", Toast.LENGTH_SHORT);
		msg.setData(bundle);
		baseHandler.sendMessage(msg);
	}

	public static void toast(String str, int duration) {
		Message msg = baseHandler.obtainMessage(0);
		Bundle bundle = new Bundle();
		bundle.putString("text", str);
		bundle.putInt("duration", duration);
		msg.setData(bundle);
		baseHandler.sendMessage(msg);
	}

	private static Toast makeText(Context context, CharSequence text,
                                  int duration) {

		if (null == context) {
			return null;
		}

		Toast result = new Toast(context);

		View v = LayoutInflater.from(context).inflate(R.layout.color_toast, null);
		result.setView(v);
		TextView tv = (TextView) v.findViewById(R.id.message);
		if(tv != null){
			tv.setText(text);
		}
		result.setGravity(17, 0, 0);
		result.setDuration(duration);

		return result;
	}
}
