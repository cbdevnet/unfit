package com.cbcdn.dev.unfit;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

public class DebugScreen extends Activity {
    private BLECommunicator.CommunicatorBinder serviceBinder = null;
    private String currentMAC;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("MainScreen", "Communicator service bound");
            serviceBinder = (BLECommunicator.CommunicatorBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d("MainScreen", "Communicator service detached");
            serviceBinder = null;
        }
    };

    public void runFirmwareUpdate(View v){
        if(serviceBinder != null){
            serviceBinder.updateFirmware();
        }
    }

    public void startService(View v){
        getApplicationContext().startService(new Intent(this.getApplicationContext(), BLECommunicator.class));
    }

    public void testBand(View v){
        if(serviceBinder != null){
            serviceBinder.deviceSelftest();
        }
    }

    public void toggleVibration(View v){
        if(serviceBinder != null){
            serviceBinder.startVibration(currentMAC);
        }
    }

    public void runPairing(View v){
        if(serviceBinder != null){
            serviceBinder.pairDevice(currentMAC);
        }
    }

    public void runSync(View v){
        if(serviceBinder != null){
            serviceBinder.syncBand(currentMAC, PreferenceManager.getDefaultSharedPreferences(this));
        }
    }

    public void rebootBand(View v){
        if(serviceBinder != null){
            serviceBinder.rebootBand(currentMAC);
        }
    }

    public void resetBand(View v){
        if(serviceBinder != null){
            serviceBinder.resetBand(currentMAC);
        }
    }

    public void doThings(View v){
        if(serviceBinder != null){
            serviceBinder.runTestCommand(currentMAC);
        }
    }

    public void fetchData(View v){
        if(serviceBinder != null){
            serviceBinder.gatherPassive();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, BLECommunicator.class), serviceConnection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(serviceBinder != null){
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        currentMAC = getIntent().getStringExtra("MAC");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_screen);
    }
}
