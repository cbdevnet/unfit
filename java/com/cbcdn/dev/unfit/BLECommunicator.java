package com.cbcdn.dev.unfit;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import com.cbcdn.dev.unfit.helpers.BLECallback;
import com.cbcdn.dev.unfit.helpers.ConstMapper.Command;
import com.cbcdn.dev.unfit.helpers.ConstMapper.BTLEState;
import com.cbcdn.dev.unfit.helpers.PairingCallback;
import com.cbcdn.dev.unfit.helpers.SyncCallback;
import java.util.HashMap;
import java.util.Map;

public class BLECommunicator extends Service {
    private DatabaseManager db;
    private Map<String,BLEDevice> devices = new HashMap<>();
    private BLECommunicator self = this;
    private CommunicatorBinder binder = new CommunicatorBinder();

    public class CommunicatorBinder extends Binder {
        public void startVibration(String mac){
            BLEDevice device = devices.get(mac);
            if(device != null){
                Log.d("BLE service", "Starting vibration");
                device.requestWrite(Command.VIBRATE2, null);
            }
        }

        public void stopVibration(String mac){
            BLEDevice device = devices.get(mac);
            if(device != null){
                Log.d("BLE service", "Stopping vibration");
                device.requestWrite(Command.VIBRATION_STOP, null);
            }
        }

        public void deviceSelftest(){
            Log.d("BLE service", "Self-testing all devices");
            for(BLEDevice device : devices.values()){
                device.requestWrite(Command.SELF_TEST, null);
            }
        }

        public void gatherPassive(){
            Log.d("BLE service", "Gathering passive data");
            for(BLEDevice device : devices.values()){
                device.requestPassiveDataRead();
            }
        }

        public void reconnectDevice(String mac){
            BLEDevice device = devices.get(mac);
            if(device != null){
                Log.d("BLE service", "Trying to reconnect to device " + mac);
                device.connect(self);
            }
        }

        public void updateFirmware(){
            Log.d("BLE service", "Initiating firmware update");
            for(BLEDevice device : devices.values()) {
                device.updateFirmware(self);
            }
        }

        public void pairDevice(String mac){
            BLEDevice device = devices.get(mac);
            if(device != null){
                Log.d("BLE service", "Trying to pair device " + mac);
                new PairingCallback().start(device);
            }
            else{
                Log.e("BLE service", "Device " + mac + " not known to service");
            }
        }

        public void syncBand(String mac, SharedPreferences preferences){
            BLEDevice device = devices.get(mac);
            if(device != null){
                Log.d("BLE service", "Trying to sync device " + mac);
                new SyncCallback(PreferenceManager.getDefaultSharedPreferences(self)).start(device);
            }
        }

        public void rebootBand(String mac){
            BLEDevice device = devices.get(mac);
            if(device != null){
                Log.d("BLE service", "Rebooting device " + mac);
                device.requestWrite(Command.REBOOT, null);
            }
        }

        public void resetBand(String mac){
            BLEDevice device = devices.get(mac);
            if(device != null){
                Log.d("BLE service", "Resetting device " + mac);
                device.requestWrite(Command.FACTORY_RESET, null);
            }
        }

        public void runTestCommand(String mac){
            BLEDevice device = devices.get(mac);
            if(device != null){
                Log.d("BLE service", "Testing command on " + mac);
                device.requestWrite(Command.SET_USER_DATA, PreferenceManager.getDefaultSharedPreferences(self));
            }
        }

        public BTLEState getStatus(String mac) {
            BLEDevice device = devices.get(mac);
            if(device != null){
                return device.getConnectionStatus();
            }
            return BTLEState.DISCONNECTED;
        }

        public void registerCallback(String mac, BLECallback callback){
            BLEDevice device = devices.get(mac);
            if(device != null){
                Log.d("BLE service", "Registering callback on " + mac);
                device.registerGenericCallback(callback);
            }
        }

        public void unregisterCallback(String mac, BLECallback callback){
            BLEDevice device = devices.get(mac);
            if(device != null) {
                Log.d("BLE service", "Unregistering callback on " + mac);
                device.unregisterGenericCallback(callback);
            }
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BLE service", "Received intent: " + intent.getAction());
            //switch(intent.getAction()){
            //}
        }
    };

    public BLECommunicator() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseManager(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.deskclock.ALARM_ALERT");
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
        return binder;
    }
}
