package com.xbx123.freedom.receive;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.xbx123.freedom.jsonparse.UtilParse;
import com.xbx123.freedom.ui.activity.OrderPayActivity;
import com.xbx123.freedom.utils.constant.Constant;
import com.xbx123.freedom.utils.tool.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import cn.jpush.android.api.JPushInterface;

/**
 * Created by EricYuan on 2016/6/1.
 */
public class JPushReceive extends BroadcastReceiver {
    private static final String TAG = "JPush";
    private NotificationManager notiManaer = null;
    private LocalBroadcastManager lBManager = null;

    @Override
    public void onReceive(Context context, Intent intent) {
//        context.get
        Bundle bundle = intent.getExtras();
        notiManaer = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        lBManager = LocalBroadcastManager.getInstance(context);
        Log.i(TAG, "[Eric] onReceiveAction - " + intent.getAction() + ", extras: " + printBundle(bundle));
        if (JPushInterface.ACTION_REGISTRATION_ID.equals(intent.getAction())) {
            String regId = bundle.getString(JPushInterface.EXTRA_REGISTRATION_ID);
            Log.i(TAG, "[MyReceiver] 接收Registration Id : " + regId);
        } else if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
            Log.i(TAG, "[MyReceiver] 接收到推送下来的自定义消息: " + bundle.getString(JPushInterface.EXTRA_MESSAGE) + "\n" + bundle.getString(JPushInterface.EXTRA_EXTRA));
        } else if (JPushInterface.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
            int notifactionId = bundle.getInt(JPushInterface.EXTRA_NOTIFICATION_ID);
            String customMsg = bundle.getString(JPushInterface.EXTRA_EXTRA);
            Log.i(TAG, "[MyReceiver] 接收到推送下来的通知携带了msg:" + customMsg + "\n通知ID:" + notifactionId);
            if (Util.isNull(customMsg))
                return;
            processCustomMessage(context, bundle,notifactionId);
            lBManager.sendBroadcast(new Intent().setAction(Constant.actionOverOrder));
        } else if (JPushInterface.ACTION_NOTIFICATION_OPENED.equals(intent.getAction())) {
            Log.i(TAG, "[MyReceiver] 用户点击打开了通知");
        } else if (JPushInterface.ACTION_RICHPUSH_CALLBACK.equals(intent.getAction())) {
            Log.i(TAG, "[MyReceiver] 用户收到到RICH PUSH CALLBACK: " + bundle.getString(JPushInterface.EXTRA_EXTRA));
            //在这里根据 JPushInterface.EXTRA_EXTRA 的内容处理代码，比如打开新的Activity， 打开一个网页等..
        } else if (JPushInterface.ACTION_CONNECTION_CHANGE.equals(intent.getAction())) {
            boolean connected = intent.getBooleanExtra(JPushInterface.EXTRA_CONNECTION_CHANGE, false);
            Log.i(TAG, "[MyReceiver]" + intent.getAction() + " connected state change to " + connected);
        } else {
            Log.i(TAG, "[MyReceiver] Unhandled intent - " + intent.getAction());
        }
    }

    // 打印所有的 intent extra 数据
    private static String printBundle(Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        for (String key : bundle.keySet()) {
            if (key.equals(JPushInterface.EXTRA_NOTIFICATION_ID)) {
                sb.append("\nkeynId:" + key + ", value:" + bundle.getInt(key));
            } else if (key.equals(JPushInterface.EXTRA_CONNECTION_CHANGE)) {
                sb.append("\nkeyExChange:" + key + ", value:" + bundle.getBoolean(key));
            }
            if (key.equals(JPushInterface.EXTRA_EXTRA)) {
                if (bundle.getString(JPushInterface.EXTRA_EXTRA).isEmpty()) {
                    Log.i(TAG, "This message has no Extra data");
                    continue;
                }
                try {
                    JSONObject json = new JSONObject(bundle.getString(JPushInterface.EXTRA_EXTRA));
                    Iterator<String> it = json.keys();
                    while (it.hasNext()) {
                        String myKey = it.next().toString();
                        sb.append("\nkey:" + key + ", value: [" +
                                myKey + " - " + json.optString(myKey) + "]");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Get message extra JSON error!");
                }
            } else {
                sb.append("\nkey:" + key + ", value:" + bundle.getString(key));
            }
        }
        return sb.toString();
    }

    //send msg to MainActivity
    private void processCustomMessage(Context context, Bundle bundle,int notificationId) {
        Intent intent = new Intent();
        String customMsg = bundle.getString(JPushInterface.EXTRA_EXTRA);
        Util.pLog("customMsg:" + customMsg);
        if(Util.isNull(customMsg))
            return;
        String ordernum = "";
        int serverType = 0;
        try {
            JSONObject jsonObject = new JSONObject(customMsg);
            if (UtilParse.checkTag(jsonObject, "server_type"))
                serverType = jsonObject.getInt("server_type");
            if(serverType != 201)
                return;
            if(Util.isTopActivy("com.xbx123.freedom.ui.activity.OrderPayActivity",context))
                return;
            if (UtilParse.checkTag(jsonObject, "order_number"))
                ordernum = jsonObject.getString("order_number");
            intent.putExtra("PayOrderNum", ordernum);
            intent.putExtra("JPushNotificationId", bundle.getInt(JPushInterface.EXTRA_NOTIFICATION_ID));
            intent.setClass(context, OrderPayActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
