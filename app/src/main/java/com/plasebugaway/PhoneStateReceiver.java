package com.plasebugaway;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.github.kevinsawicki.http.HttpRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * Created by pxy on 8/13/15.
 */
public class PhoneStateReceiver extends BroadcastReceiver {

    private static final String TAG = "PhoneStateReceiver";

    private static final String SBProvince = "福建";

    @Override
    public void onReceive(Context context, Intent intent) {
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Service.TELEPHONY_SERVICE);
        switch (tm.getCallState()) {
            case TelephonyManager.CALL_STATE_RINGING:
                String phno = intent.getStringExtra("incoming_number");
                if (phno == null) {
                    return;
                }
                Log.i(TAG, "RING :" + phno);
                String location = callerLocation(phno);
                if (location.equals(SBProvince)) {
                    Toast toast = Toast.makeText(context, "Fuck Fujian SB", Toast.LENGTH_LONG);
                    toast.show();
                    killCall(context);
                }
        }
    }

    private String callerLocation(String phno)  {
        String api = "https://tcc.taobao.com/cc/json/mobile_tel_segment.htm?tel="+phno;
        HttpRequest request = HttpRequest.get(api);
        request.trustAllCerts();
        request.trustAllHosts();
        request.acceptJson();
        if (!request.ok()) {
            Log.w(TAG, "request failed: " + request.code() + " " + request.body() + "\n" + request.headers().toString());
            return "";
        }
        try {
            String body = request.body();
            body = body.split("=")[1];
            JSONObject json = new JSONObject(body);
            return json.getString("province");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    private boolean killCall(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Class classTelephony = Class.forName(tm.getClass().getName());
            Method methodGetITelephony = classTelephony.getDeclaredMethod("getITelephony");

            methodGetITelephony.setAccessible(true);

            Object telephonyInterface = methodGetITelephony.invoke(tm);

            Class telephonyInterfaceClass =
                    Class.forName(telephonyInterface.getClass().getName());
            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");

            methodEndCall.invoke(telephonyInterface);
        } catch (Exception e) {
            e.printStackTrace();
            Log.w(TAG, "PhoneStateReceiver **" + e.toString());
            return false;
        }
        return true;
    }
}
