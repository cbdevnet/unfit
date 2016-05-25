package com.cbcdn.dev.unfit;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import com.cbcdn.dev.unfit.helpers.ConstMapper.Command;
import com.cbcdn.dev.unfit.helpers.ConstMapper.BTLEState;
import com.cbcdn.dev.unfit.helpers.ConstMapper.Service;
import com.cbcdn.dev.unfit.helpers.ConstMapper.Characteristic;

import java.lang.reflect.Array;
import java.util.Arrays;

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

    private BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            state = BTLEState.getByValue(newState);
            Log.d("BTLE callback", device.getAddress() + ": Connection state changed to " + state + ", status " + status);
            if(newState == BTLEState.CONNECTED.getValue()){
                Log.d("BLE callback", "Connection established, starting service discovery");
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d("BLE callback", "Service discovery successful");
            for(BluetoothGattService service : gatt.getServices()){
                Log.d("BLE callback", "Service discovered: " + (Service.fromUUID(service.getUuid()) == null ? service.getUuid() : Service.fromUUID(service.getUuid()).toString()));
            }

            Log.d("BLE callback", "Testing for device bond");
            self.requestRead(Characteristic.PAIR);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d("BTLE Callback", "Characteristic write: "
                    + status
                    + " " + (Characteristic.fromUUID(characteristic.getUuid()) == null ?
                    characteristic.getUuid() :
                    Characteristic.fromUUID(characteristic.getUuid()).toString())
                    + ": " + dumpBytes(characteristic.getValue()));

            if(Characteristic.fromUUID(characteristic.getUuid()) == Characteristic.PAIR){
                Log.d("BLE callback", "Detected write of pairing characteristic, rechecking");
                self.requestRead(Characteristic.PAIR);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d("BLE callback", "Characteristic read: "
                    + status
                    + " " + (Characteristic.fromUUID(characteristic.getUuid()) == null ?
                    characteristic.getUuid() :
                    Characteristic.fromUUID(characteristic.getUuid()).toString())
                    + ": " + dumpBytes(characteristic.getValue()));

            if(Characteristic.fromUUID(characteristic.getUuid()) == Characteristic.PAIR) {
                if (Arrays.equals(characteristic.getValue(), new byte[]{(byte) 0xFF, (byte) 0xFF})) {
                    Log.d("BLE callback", "Unpaired device detected, trying to pair");
                    self.requestWrite(Characteristic.PAIR, Command.PAIR.getCommand());
                } else {
                    Log.d("BLE callback", "Paired device detected");
                }
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
            Log.d("BTLE Callback", "Descriptor write: " + status + " " + descriptor.toString() + " " + dumpBytes(descriptor.getValue()));
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.d("BTLE Callback", "Reliable write done: " + status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.d("BTLE Callback", "RSSI changed: " + status + " " + rssi);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.d("BTLE Callback", "MTU changed: " + status + " " + mtu);
        }
    };

    public BLEDevice(BluetoothDevice device){
        this.device = device;
    }

    public void dumpEndpoints(){
        if(gatt != null){
            Log.d("BLE Dump", "Invoked on " + gatt.toString() + " " + device.getAddress());

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

    public boolean requestRead(Characteristic characteristic){
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
            Log.d("BLE read", "Characteristic read request failed");
            return false;
        }

        Log.d("BLE read", "Read request sent");
        return true;
    }

    public boolean requestWrite(Characteristic characteristic, byte[] data){
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
            Log.d("BLE write", "Characteristic write returned false");
            return false;
        }

        Log.d("BLE write", "Write request sent successfully");
        return true;
    }
}
