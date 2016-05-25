package com.cbcdn.dev.unfit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainScreen extends Activity {
    public final static int REQUEST_MAC = 1;
    private DatabaseManager db;
    private boolean vibrating = false;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MainScreen", "Activity result: " + requestCode + " " + resultCode);
        if(requestCode == REQUEST_MAC) {
            if(resultCode == RESULT_OK) {
                Log.d("MainScreen", "Configuration screen returned device MAC " + data.getStringExtra("MAC"));
                db.getWritableDatabase().execSQL("INSERT INTO devices (mac, is_def) VALUES (?, ?);", new Object[]{data.getStringExtra("MAC"), 0});
            }
        }
    }

    //If no device registered, go to pairing activity
    //Menu: Unregister, Change active device, Pair new device,
    //Change setting
    public void runPairingActivity(View v){
        Log.d("Main", "Starting scanner view");
        startActivityForResult(new Intent(this, PairActivity.class), 1);
    }

    public void runGATTDump(View v){

    }

    public void startService(View v){
        getApplicationContext().startService(new Intent(this.getApplicationContext(), BLECommunicator.class));
    }

    public void testBand(View v){
        this.sendBroadcast(new Intent().setAction("com.cbcdn.dev.unfit.selftest"));
    }

    public void toggleVibration(View v){
        if(vibrating){
            this.sendBroadcast(new Intent().setAction("com.cbcdn.dev.unfit.vibration.stop"));
        }
        else{
            this.sendBroadcast(new Intent().setAction("com.cbcdn.dev.unfit.vibration.start"));
        }
        vibrating = !vibrating;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        db = new DatabaseManager(this);
        if(db.getReadableDatabase().rawQuery("SELECT * FROM devices;", null).getCount() < 1){
            Log.d("MainScreen", "No device configured, running pairing dialog");
            startActivityForResult(new Intent(this, PairActivity.class), 1);
        }
        getApplicationContext().startService(new Intent(this.getApplicationContext(), BLECommunicator.class));
    }
}
