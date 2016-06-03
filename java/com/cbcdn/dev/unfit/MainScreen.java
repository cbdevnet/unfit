package com.cbcdn.dev.unfit;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cbcdn.dev.unfit.helpers.BLECallback;
import com.cbcdn.dev.unfit.helpers.ConstMapper.Characteristic;
import com.cbcdn.dev.unfit.helpers.ValueListAdapter;

import java.util.LinkedList;
import java.util.List;

public class MainScreen extends Activity {
    public final static int REQUEST_MAC = 1;
    private DatabaseManager db;
    private String currentMAC;
    private BLECommunicator.CommunicatorBinder serviceBinder = null;
    private ValueListAdapter listAdapter;

    private BLECallback updaterCallback = new BLECallback() {
        @Override
        public void start(BLEDevice device) {
        }

        @Override
        public void readCompleted(BLEDevice self, final Characteristic characteristic, int status, final byte[] data) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listAdapter.update(characteristic, data);
                    updateDisplay();
                }
            });
        }

        @Override
        public void connectionChanged(BLEDevice self) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateDisplay();
                }
            });
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MainScreen", "Activity result: " + requestCode + " " + resultCode);
        if(requestCode == REQUEST_MAC) {
            if(resultCode == RESULT_OK && data.hasExtra("MAC")) {
                currentMAC = data.getStringExtra("MAC");
                Log.d("MainScreen", "Pairing screen returned device MAC " + currentMAC);
                //TODO start pairing process + datetime update + firmware update
                //TODO update settings shared prefs
                try{
                    ContentValues cv = new ContentValues();
                    cv.put("mac", currentMAC);
                    long index = db.getWritableDatabase().insert("devices", null, cv);
                    getPreferences(Context.MODE_PRIVATE).edit().putLong("active_device", index).commit();
                    getApplicationContext().startService(new Intent(this.getApplicationContext(), BLECommunicator.class));
                }
                catch(SQLiteConstraintException e){
                    Toast.makeText(this, "Failed to store device address, probably already paired", Toast.LENGTH_SHORT).show();
                }

                startActivity(new Intent(this, SettingsActivity.class).putExtra("MAC", currentMAC));
            }
            else if(currentMAC == null){
                Toast.makeText(this, "Need a device to work with", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void updateDisplay(){
        ((TextView)findViewById(R.id.display_mac)).setText(" " + currentMAC);

        if(serviceBinder != null) {
            ((TextView) findViewById(R.id.display_connection)).setText(serviceBinder.getStatus(currentMAC).toString());
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("MainScreen", "Communicator service bound");
            serviceBinder = (BLECommunicator.CommunicatorBinder) service;
            serviceBinder.registerCallback(currentMAC, updaterCallback);
            updateDisplay();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d("MainScreen", "Communicator service detached");
            serviceBinder.unregisterCallback(currentMAC, updaterCallback);
            serviceBinder = null;
        }
    };

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
        db.close();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        //FIXME this loads the default values into the settings dialog
        //need to replace them with the current band settings when starting settings
        PreferenceManager.setDefaultValues(this, R.xml.device_settings, false);
        getApplicationContext().startService(new Intent(this.getApplicationContext(), BLECommunicator.class));

        db = new DatabaseManager(this);
        if(db.getReadableDatabase().rawQuery("SELECT * FROM devices;", null).getCount() < 1){
            Log.d("MainScreen", "No device configured, running pairing dialog");
            startActivityForResult(new Intent(this, PairActivity.class), REQUEST_MAC);
            return;
        }

        //Get active device MAC
        long device = getPreferences(Context.MODE_PRIVATE).getLong("active_device", -1);
        Cursor devices = null;
        if(device >= 0){
            devices = db.getReadableDatabase().rawQuery("SELECT mac FROM devices WHERE device = ?;", new String[]{"" + device});
            if(devices.getCount() < 1){
                devices.close();
                devices = null;
            }
        }

        if(devices == null){
            devices = db.getReadableDatabase().rawQuery("SELECT mac FROM devices;", null);
        }

        devices.moveToNext();
        currentMAC = devices.getString(0);
        Log.d("MainScreen", "Active device MAC " + currentMAC);

        devices.close();

        List<Characteristic> listedCharacteristics = new LinkedList<>();
        for(Characteristic c : Characteristic.values()){
            if(c.isDisplayed()){
                listedCharacteristics.add(c);
            }
        }

        listAdapter = new ValueListAdapter(listedCharacteristics);
        ((ListView)this.findViewById(R.id.display_scroller)).setAdapter(listAdapter);
        ((ListView)this.findViewById(R.id.display_scroller)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //TODO onclick handler
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //TODO when changing active device, reset the callback
        switch (item.getItemId()) {
            case R.id.pair_device:
                startActivityForResult(new Intent(this, PairActivity.class), REQUEST_MAC);
                return true;
            case R.id.setup_device:
                startActivity(new Intent(this, SettingsActivity.class).putExtra("MAC", currentMAC));
                return true;
            case R.id.debug_screen:
                startActivity(new Intent(this, DebugScreen.class).putExtra("MAC", currentMAC));
                return true;
            case R.id.fetch:
                if(serviceBinder != null){
                    serviceBinder.gatherPassive();
                    return true;
                }
                return false;
            case R.id.reconnect:
                if(serviceBinder != null){
                    serviceBinder.reconnectDevice(currentMAC);
                    return true;
                }
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
