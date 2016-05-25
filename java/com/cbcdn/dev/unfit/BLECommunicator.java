package com.cbcdn.dev.unfit;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.IBinder;
import android.util.Log;

import com.cbcdn.dev.unfit.helpers.ConstMapper.Command;
import com.cbcdn.dev.unfit.helpers.ConstMapper.Characteristic;

import java.util.HashMap;
import java.util.Map;

public class BLECommunicator extends Service {
    private DatabaseManager db;
    private Map<String,BLEDevice> devices = new HashMap<>();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BLE service", "Received intent: " + intent.getAction());
            switch(intent.getAction()){
                //case "android.provider.Telephony.SMS_RECEIVED":
                //    break;
                case "com.cbcdn.dev.unfit.vibration.start":
                    Log.d("BLE service", "Starting vibration");
                    for(BLEDevice device : devices.values()){
                        device.requestWrite(Characteristic.VIBRATION, Command.VIBRATE2.getCommand());
                        //device.requestWrite(Characteristic.CONTROL_POINT, Command.TEST_COMMAND.getCommand());
                    }
                    break;
                case "com.cbcdn.dev.unfit.vibration.stop":
                    Log.d("BLE service", "Stopping vibration");
                    for(BLEDevice device : devices.values()){
                        device.requestWrite(Characteristic.VIBRATION, Command.VIBRATION_STOP.getCommand());
                    }
                    break;
                case "com.cbcdn.dev.unfit.selftest":
                    Log.d("BLE service", "Self-testing all devices");
                    for(BLEDevice device : devices.values()){
                        device.requestWrite(Characteristic.TEST, Command.SELF_TEST.getCommand());
                    }
                    break;
                case "com.cbcdn.dev.unfit.request.gather":
                    Log.d("BLE service", "Gathering passive data");
                    for(BLEDevice device : devices.values()){
                        device.requestPassiveDataRead();
                    }
                    break;
                case "com.cbcdn.dev.unfit.request.heartrate":
                    break;
            }
        }
    };

    public BLECommunicator() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseManager(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.cbcdn.dev.unfit.vibration.start");
        filter.addAction("com.cbcdn.dev.unfit.vibration.stop");
        filter.addAction("com.cbcdn.dev.unfit.selftest");
        filter.addAction("com.cbcdn.dev.unfit.request.gather");
        filter.addAction("com.cbcdn.dev.unfit.request.heartrate");
        registerReceiver(receiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("BLE service", "Start command received");
        Cursor deviceCursor = db.getReadableDatabase().rawQuery("SELECT mac FROM devices;", null);
        while(deviceCursor.moveToNext()){
            if(devices.get(deviceCursor.getString(0)) == null){
                Log.d("BLE service", "Creating BLE device " + deviceCursor.getString(0));
                devices.put(deviceCursor.getString(0), new BLEDevice(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceCursor.getString(0))));
                devices.get(deviceCursor.getString(0)).connect(this);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO service binding
        return null;
    }
}
