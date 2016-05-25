package com.cbcdn.dev.unfit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainScreen extends Activity {
    public final static int REQUEST_MAC = 1;
    public final static int REQUEST_CONFIG = 2;
    private DatabaseManager db;
    private boolean vibrating = false;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MainScreen", "Activity result: " + requestCode + " " + resultCode);
        if(requestCode == REQUEST_MAC) {
            if(resultCode == RESULT_OK) {
                Log.d("MainScreen", "Configuration screen returned device MAC " + data.getStringExtra("MAC"));
                db.getWritableDatabase().execSQL("INSERT ON CONFLICT IGNORE INTO devices (mac, is_def) VALUES (?, ?);", new Object[]{data.getStringExtra("MAC"), 0});
                startActivity(new Intent(this, SettingsActivity.class));
            }
        }
    }

    public void runGATTDump(View v){

    }

    public void startService(View v){
        getApplicationContext().startService(new Intent(this.getApplicationContext(), BLECommunicator.class));
    }

    public void testBand(View v){
        this.sendBroadcast(new Intent().setAction("com.cbcdn.dev.unfit.selftest"));
    }

    public void gatherPassive(View v){
        this.sendBroadcast(new Intent().setAction("com.cbcdn.dev.unfit.request.gather"));
    }

    public void toggleVibration(View v){
        if(vibrating){
            this.sendBroadcast(new Intent().setAction("com.cbcdn.dev.unfit.vibration.stop"));
        }
        else{
            this.sendBroadcast(new Intent().setAction("com.cbcdn.dev.unfit.vibration.start"));
        }
        //vibrating = !vibrating;
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
            startActivityForResult(new Intent(this, PairActivity.class), REQUEST_MAC);
        }
        getApplicationContext().startService(new Intent(this.getApplicationContext(), BLECommunicator.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pair_device:
                startActivityForResult(new Intent(this, PairActivity.class), REQUEST_MAC);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
