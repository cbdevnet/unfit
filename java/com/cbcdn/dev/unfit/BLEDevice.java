package com.cbcdn.dev.unfit;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.cbcdn.dev.unfit.callbacks.BLECallback;
import com.cbcdn.dev.unfit.helpers.ConstMapper.Command;
import com.cbcdn.dev.unfit.helpers.ConstMapper.BTLEState;
import com.cbcdn.dev.unfit.helpers.ConstMapper.Service;
import com.cbcdn.dev.unfit.helpers.ConstMapper.Characteristic;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.InvalidPreferencesFormatException;

public class BLEDevice {
    private static String dumpBytes(byte[] data){
        StringBuilder rv = new StringBuilder();
        for(byte b : data){
            rv.append(String.format("%02X", b));
        }
        return rv.toString();
    }

    private BluetoothDevice device;
    private BluetoothGatt gatt;
    private BTLEState state = BTLEState.DISCONNECTED;
    private BLEDevice self = this;
    private Map<Characteristic, byte[]> recentData = new HashMap<>();

    public BTLEState getConnectionStatus() {
        return state;
    }

    public void registerGenericCallback(BLECallback callback){
        notificationListeners.add(callback);
        Log.d("BLEDevice", "Currently at " + notificationListeners.size() + " callbacks");
    }

    public void unregisterGenericCallback(BLECallback callback){
        notificationListeners.remove(callback);
        Log.d("BLEDevice", "Currently at " + notificationListeners.size() + " callbacks");
    }

    private class RWQEntry {
        private Characteristic characteristic;
        private boolean write = false;
        private byte[] data;
        private BLECallback callback;
        public boolean inProgress = false;

        public RWQEntry(Characteristic characteristic, BLECallback callback){
            this(characteristic);
            this.callback = callback;
        }

        public RWQEntry(Characteristic characteristic){
            this.characteristic = characteristic;
        }

        public RWQEntry(Characteristic characteristic, byte[] data, BLECallback callback){
            this(characteristic, data);
            this.callback = callback;
        }

        public RWQEntry(Characteristic characteristic, byte[] data){
            this.characteristic = characteristic;
            this.data = data;
            this.write = true;
        }

        public boolean matches(Characteristic characteristic, boolean write){
            return this.characteristic == characteristic && this.write == write;
        }

        public void dispatch(int status, byte[] data){
            if(callback != null){
                if(write){
                    callback.writeCompleted(self, characteristic, status);
                }
                else{
                    callback.readCompleted(self, characteristic, status, data);
                }
            }
        }

        public void perform(){
            inProgress = true;
            if(write){
                self.performWrite(characteristic, data);
            }
            else{
                self.performRead(characteristic);
            }
        }
    }

    private List<RWQEntry> gattQueue = new LinkedList<>();
    private Set<BLECallback> notificationListeners = new HashSet<>();
    private BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            state = BTLEState.getByValue(newState);
            Log.d("BLE callback", device.getAddress() + ": Connection state changed to " + state + ", status " + status);
            if(newState == BTLEState.CONNECTED.getValue()){
                Log.d("BLE callback", "Connection established, starting service discovery");
                gatt.discoverServices();
                //upon first connection, run callback only when services discovered
                return;
            }

            for(BLECallback callback : notificationListeners){
                callback.connectionChanged(self);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d("BLE callback", "Service discovery successful");
            for(BluetoothGattService service : gatt.getServices()){
                Log.d("BLE callback", "Service discovered: " + (Service.fromUUID(service.getUuid()) == null ? service.getUuid() : Service.fromUUID(service.getUuid()).toString()));
            }

            gatt.setCharacteristicNotification(gatt.getService(Service.MILI.getUUID()).getCharacteristic(Characteristic.NOTIFICATION.getUUID()), true);

            for(BLECallback callback : notificationListeners){
                callback.connectionChanged(self);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d("BLE Callback", "Characteristic write: "
                    + status
                    + " " + (Characteristic.fromUUID(characteristic.getUuid()) == null ?
                    characteristic.getUuid() :
                    Characteristic.fromUUID(characteristic.getUuid()).toString())
                    + ": " + dumpBytes(characteristic.getValue()));

            RWQEntry head = gattQueue.size() > 0 ? gattQueue.get(0) : null;
            if(head != null && head.inProgress && head.matches(Characteristic.fromUUID(characteristic.getUuid()), true)){
                //The ordering of remove and dispatch is important, because callbacks may re-enqueue themselves
                gattQueue.remove(0);
                head.dispatch(status, characteristic.getValue());
                workQueue();
            }
            else{
                Log.e("BLE queue", "Invalid state: head not workable");
            }

            for(BLECallback callback : notificationListeners){
                callback.writeCompleted(self, Characteristic.fromUUID(characteristic.getUuid()), status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            Characteristic c = Characteristic.fromUUID(characteristic.getUuid());

            if(c == null){
                Log.d("BLE callback", "Characteristic read: "
                        + status
                        + " " + characteristic.getUuid() + ": " + dumpBytes(characteristic.getValue()));
            }
            else {
                Log.d("BLE callback", "Characteristic " + c + " read: "
                        + status + " " + dumpBytes(characteristic.getValue()) + ": " + c.interpret(characteristic.getValue()));

                recentData.put(c, characteristic.getValue());

                RWQEntry head = gattQueue.size() > 0 ? gattQueue.get(0) : null;
                if (head != null && head.inProgress && head.matches(Characteristic.fromUUID(characteristic.getUuid()), false)) {
                    gattQueue.remove(0);
                    head.dispatch(status, characteristic.getValue());
                    workQueue();
                }
                else{
                    Log.e("BLE queue", "Invalid state: head not workable");
                }
            }
            for(BLECallback callback : notificationListeners){
                callback.readCompleted(self, Characteristic.fromUUID(characteristic.getUuid()), status, characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d("BLE Callback", "Characteristic changed: "
                    + " " + (Characteristic.fromUUID(characteristic.getUuid()) == null ?
                    characteristic.getUuid() :
                    Characteristic.fromUUID(characteristic.getUuid()).toString())
                    + ": " + dumpBytes(characteristic.getValue()));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d("BLE Callback", "Descriptor read: " + status + " " + descriptor.toString() + " " + dumpBytes(descriptor.getValue()));
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d("BLE Callback", "Descriptor write: " + status + " " + descriptor.toString() + " " + dumpBytes(descriptor.getValue()));
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.d("BLE Callback", "Reliable write done: " + status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.d("BLE Callback", "RSSI changed: " + status + " " + rssi);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.d("BLE Callback", "MTU changed: " + status + " " + mtu);
        }
    };

    public BLEDevice(BluetoothDevice device){
        this.device = device;
    }

    public void updateFirmware(Context context){
        //This is horribly inefficient, but since InputStream does not provide a length,
        //we'll do it the hard way
        List<Byte> fwData = new ArrayList<>();

        try{
            InputStream fwStream = context.getAssets().open("mi1s.fw");

            for(int data = fwStream.read(); data >= 0; data = fwStream.read()){
                fwData.add((byte)data);
            }

            fwStream.close();
        }
        catch(IOException e){
            Log.e("BLE firmware update", "Failed to read firmware asset: " + e.getMessage());
        }

        Log.d("BLE firmware update", "Read " + fwData.size() + " bytes of firmware data");
        //TODO verify and write firmare
    }

    public void dumpEndpoints(){
        if(gatt != null){
            Log.d("BLE Dump", "Invoked on " + device.getAddress());

            for(BluetoothGattService service : gatt.getServices()){
                Log.d("BLE Dump", "Service " +
                        (Service.fromUUID(service.getUuid()) == null ? service.getUuid() + " " + service.getType():Service.fromUUID(service.getUuid()).toString()));
                for(BluetoothGattCharacteristic characteristic : service.getCharacteristics()){
                    Log.d("BLE Dump", "Characteristic " +
                            (Characteristic.fromUUID(characteristic.getUuid()) == null ?
                                    characteristic.getUuid() + " " + dumpBytes(characteristic.getValue()) + " " + characteristic.toString() : Characteristic.fromUUID(characteristic.getUuid()).toString())
                    );
                    for(BluetoothGattDescriptor descriptor : characteristic.getDescriptors()){
                        Log.d("BLE Dump", "Descriptor " + descriptor.getUuid() + " " + descriptor.toString());
                    }
                }
            }
        }
    }

    public void connect(Context context){
        this.gatt = device.connectGatt(context, false, callback);
    }

    public boolean requestPassiveDataRead(){
        /*return this.requestRead(Characteristic.ACTIVITY)
                && this.requestRead(Characteristic.BATTERY)
                && this.requestRead(Characteristic.USER_INFO)
                && this.requestRead(Characteristic.BLE_PARAMS)
                && this.requestRead(Characteristic.DEVICE_INFO)
                && this.requestRead(Characteristic.DEVICE_NAME)
                && this.requestRead(Characteristic.TIME)
                && this.requestRead(Characteristic.CONNECTION_PARAMS)
                && this.requestRead(Characteristic.GENERIC_DEVICE_APPEARANCE)
                && this.requestRead(Characteristic.GENERIC_DEVICE_NAME)
                && this.requestRead(Characteristic.NOTIFICATION)
                && this.requestRead(Characteristic.PERIPHERAL_PRIVACY)
                && this.requestRead(Characteristic.REALTIME_STEPS)
                && this.requestRead(Characteristic.SENSOR)
                && this.requestRead(Characteristic.STATISTICS);*/
        for(Characteristic characteristic : Characteristic.values()){
            if(characteristic.isPassive()){
                if(!this.requestRead(characteristic)){
                    return false;
                }
            }
        }
        return true;
    }

    public boolean requestPriorityRead(Characteristic characteristic, BLECallback callback){
        if(state != BTLEState.CONNECTED){
            Log.d("BLE read", "Device not connected");
            return false;
        }

        //TODO check for discovered services
        Log.d("BLE Queue", "Enqueueing priority read for " + characteristic);
        gattQueue.add(0, new RWQEntry(characteristic, callback));
        workQueue();
        return true;
    }

    public boolean requestRead(Characteristic characteristic){
        if(state != BTLEState.CONNECTED){
            Log.d("BLE read", "Device not connected");
            return false;
        }

        //TODO check for discovered services
        Log.d("BLE Queue", "Enqueueing read for " + characteristic);
        gattQueue.add(new RWQEntry(characteristic));
        workQueue();
        return true;
    }

    public boolean requestRead(Characteristic characteristic, BLECallback callback){
        if(state != BTLEState.CONNECTED){
            Log.d("BLE read", "Device not connected");
            return false;
        }

        //TODO check for discovered services
        Log.d("BLE Queue", "Enqueueing read for " + characteristic);
        gattQueue.add(new RWQEntry(characteristic, callback));
        workQueue();
        return true;
    }

    public boolean requestPriorityWrite(Command command, BLECallback callback, SharedPreferences preferences){
        try{
            return this.requestPriorityWrite(command.getEndpoint(), command.getCommand(this.device.getAddress(), preferences), callback);
        }
        catch(InvalidPreferencesFormatException e){
            return false;
        }
    }

    private boolean requestPriorityWrite(Characteristic characteristic, byte[] data, BLECallback callback){
        if(state != BTLEState.CONNECTED){
            Log.d("BLE read", "Device not connected");
            return false;
        }

        //TODO check for discovered services
        Log.d("BLE Queue", "Enqueueing priority write for " + characteristic);
        gattQueue.add(0, new RWQEntry(characteristic, data, callback));
        workQueue();
        return true;
    }

    public boolean requestWrite(Command command, SharedPreferences preferences){
        try {
            return requestWrite(command.getEndpoint(), command.getCommand(this.device.getAddress(), preferences));
        }
        catch(InvalidPreferencesFormatException e){
            return false;
        }
    }

    private boolean requestWrite(Characteristic characteristic, byte[] data){
        if(state != BTLEState.CONNECTED){
            Log.d("BLE read", "Device not connected");
            return false;
        }

        //TODO check for discovered services
        Log.d("BLE Queue", "Enqueueing write for " + characteristic);
        gattQueue.add(new RWQEntry(characteristic, data));
        workQueue();
        return true;
    }

    private void workQueue(){
        Log.d("BLE queue", "Running queue of size " + gattQueue.size());

        RWQEntry head = gattQueue.size() > 0 ? gattQueue.get(0) : null;
        if(head != null && !head.inProgress){
            head.perform();
        }
    }

    private boolean performRead(Characteristic characteristic){
        if(gatt == null){
            Log.d("BLE read", "Request failed, no connection");
            return false;
        }

        BluetoothGattService s = gatt.getService(characteristic.getParent().getUUID());
        if(s == null){
            Log.d("BLE read", "Service " + characteristic.getParent() + "not available, retrying discovery");
            gatt.discoverServices();
            return false;
        }

        BluetoothGattCharacteristic c = s.getCharacteristic(characteristic.getUUID());
        if(c == null){
            Log.d("BLE read", "Characteristic " + characteristic + " on service " + characteristic.getParent() + " does not exist");
            return false;
        }

        if(!gatt.readCharacteristic(c)){
            Log.d("BLE read", "Characteristic read request failed for " + characteristic);
            return false;
        }

        Log.d("BLE read", "Read request sent");
        return true;
    }

    private boolean performWrite(Characteristic characteristic, byte[] data){
        if(gatt == null){
            Log.d("BLE write", "Write request failed, no connection");
            return false;
        }

        BluetoothGattService s = gatt.getService(characteristic.getParent().getUUID());
        if(s == null){
            Log.d("BLE write", "Service " + characteristic.getParent() + " not available, retrying discovery");
            gatt.discoverServices();
            return false;
        }

        BluetoothGattCharacteristic c = s.getCharacteristic(characteristic.getUUID());
        if(c == null){
            Log.d("BLE write", "Characteristic " + characteristic + " on service " + characteristic.getParent() + " does not exist");
            return false;
        }

        if(!c.setValue(data)){
            Log.d("BLE write", "Failed to set local value on characteristic " + characteristic);
            return false;
        }

        if(!gatt.writeCharacteristic(c)){
            Log.d("BLE write", "Characteristic write request failed for " + characteristic);
            return false;
        }

        Log.d("BLE write", "Write request sent successfully");
        return true;
    }
}
