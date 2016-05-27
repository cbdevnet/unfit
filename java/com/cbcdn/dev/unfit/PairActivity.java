package com.cbcdn.dev.unfit;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.cbcdn.dev.unfit.helpers.ContextRunnable;
import com.cbcdn.dev.unfit.helpers.DeviceListAdapter;

import java.util.List;

public class PairActivity extends Activity {

    private boolean currentlyScanning = false;
    private Activity self = this;
    private static final long SCAN_PERIOD = 15000;
    private DeviceListAdapter scanList;

    private ScanCallback deviceDetected = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d("Scanner", "Scanner found result: " + result.toString());
            Log.d("Scanner", "MAC " + result.getDevice().getAddress() + " Device name: " + result.getScanRecord().getDeviceName());
            scanList.add(result.getScanRecord(), result.getDevice());

            super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for(ScanResult result : results){
                Log.d("Scanner", "Scanner found result (batched): " + result.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            self.findViewById(R.id.scanDevices).setVisibility(View.VISIBLE);
            self.findViewById(R.id.scanningSpinner).setVisibility(View.INVISIBLE);
            //TODO print message
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(currentlyScanning){
            Log.w("Scanner", "Scanning stopped due to activity end");
            BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner().stopScan(deviceDetected);
            currentlyScanning = false;
        }
    }

    public void scanForDevices(View v){
        scanList.clear();

        if(currentlyScanning){
            return;
        }

        new Handler().postDelayed(
                new ContextRunnable(this) {
                    @Override
                    public void run() {
                        if(currentlyScanning) {
                            BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner().stopScan(deviceDetected);
                            currentlyScanning = false;
                            BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner().flushPendingScanResults(deviceDetected);
                            ((Activity) passedContext).findViewById(R.id.scanningSpinner).setVisibility(View.INVISIBLE);
                            ((Activity) passedContext).findViewById(R.id.scanDevices).setVisibility(View.VISIBLE);
                            Log.d("Scanner", "Scan stopped with timeout");
                        }
                    }
                }, SCAN_PERIOD);

        v.setVisibility(View.INVISIBLE);
        self.findViewById(R.id.scanningSpinner).setVisibility(View.VISIBLE);

        //TODO use extended scan start
        BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner().startScan(deviceDetected);

        currentlyScanning = true;
        Log.d("Scanner", "Scan started");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);
        scanList = new DeviceListAdapter();
        ((ListView)this.findViewById(R.id.deviceSelection)).setAdapter(scanList);
        ((ListView)this.findViewById(R.id.deviceSelection)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Pair<ScanRecord, BluetoothDevice> device = (Pair<ScanRecord, BluetoothDevice>)parent.getItemAtPosition(position);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("MAC", device.second.getAddress());
                self.setResult(Activity.RESULT_OK, resultIntent);
                self.finish();
            }
        });
    }
}
