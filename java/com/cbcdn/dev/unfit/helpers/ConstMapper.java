package com.cbcdn.dev.unfit.helpers;

import android.bluetooth.BluetoothProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ConstMapper {
    private ConstMapper(){
    }

    public enum BTLEState {
        CONNECTED("Connected", BluetoothProfile.STATE_CONNECTED),
        CONNECTING("Connecting", BluetoothProfile.STATE_CONNECTING),
        DISCONNECTED("Disconnected", BluetoothProfile.STATE_DISCONNECTED),
        DISCONNECTING("Disconnecting", BluetoothProfile.STATE_DISCONNECTING);

        private String name;
        private int value;
        private static Map<Integer, BTLEState> valueMap = new HashMap<Integer, BTLEState>();

        private BTLEState(String name, int value){
            this.name = name;
            this.value = value;
        }

        public static BTLEState getByValue(int value){
            return valueMap.get(new Integer(value));
        }

        @Override
        public String toString(){
            return name;
        }

        public int getValue(){
            return value;
        }

        static {
            for(BTLEState state : BTLEState.values()){
                valueMap.put(state.value, state);
            }
        }
    }

    public enum Service {
        MILI("Main Service", "0000fee0-0000-1000-8000-00805f9b34fb"),
        VIBRATE("Vibration", "00001802-0000-1000-8000-00805f9b34fb"),
        HEARTRATE("Heart Rate", "0000180d-0000-1000-8000-00805f9b34fb"),
        GAP_SERVICE("GAP Service", "00001800-0000-1000-8000-00805f9b34fb"),
        GAP("Generic Attribute Profile", "00001801-0000-1000-8000-00805f9b34fb"),

        UNKNOWN_1("Unknown 1", "0000fee1-0000-1000-8000-00805f9b34fb");

        private String ident;
        private UUID uuid;
        private static Map<UUID, Service> uuidMap = new HashMap<>();

        private Service(String name, String uuid){
            this.ident = name;
            this.uuid = UUID.fromString(uuid);
        }

        public UUID getUUID(){
            return uuid;
        }

        public static Service fromUUID(UUID uuid){
            return uuidMap.get(uuid);
        }

        @Override
        public String toString(){
            return ident;
        }

        static {
            for(Service service : Service.values()){
                uuidMap.put(service.getUUID(), service);
            }
        }
    }

    public enum Characteristic {
        DEVICE_INFO(Service.MILI, "Device info", "0000ff01-0000-1000-8000-00805f9b34fb"),
        DEVICE_NAME(Service.MILI, "Device name", "0000ff02-0000-1000-8000-00805f9b34fb"),
        NOTIFICATION(Service.MILI, "Notification?", "0000ff03-0000-1000-8000-00805f9b34fb"),
        USER_INFO(Service.MILI, "User info", "0000ff04-0000-1000-8000-00805f9b34fb"),
        CONTROL(Service.MILI, "Control", "0000ff05-0000-1000-8000-00805f9b34fb"),
        REALTIME_STEPS(Service.MILI, "Realtime steps", "0000ff06-0000-1000-8000-00805f9b34fb"),
        ACTIVITY(Service.MILI, "Activity?", "0000ff07-0000-1000-8000-00805f9b34fb"),
        FIRMWARE(Service.MILI, "Firmware data", "0000ff08-0000-1000-8000-00805f9b34fb"),
        BLE_PARAMS(Service.MILI, "BLE parameters", "0000ff09-0000-1000-8000-00805f9b34fb"),
        TIME(Service.MILI, "Time", "0000ff0a-0000-1000-8000-00805f9b34fb"),
        STATISTICS(Service.MILI, "Statistics", "0000ff0b-0000-1000-8000-00805f9b34fb"),
        BATTERY(Service.MILI, "Battery", "0000ff0c-0000-1000-8000-00805f9b34fb"),
        TEST(Service.MILI, "Device test", "0000ff0d-0000-1000-8000-00805f9b34fb"),
        SENSOR(Service.MILI, "Sensor data?", "0000ff0e-0000-1000-8000-00805f9b34fb"),
        PAIR(Service.MILI, "Pairing", "0000ff0f-0000-1000-8000-00805f9b34fb"),

        HEARTRATE_NOTIFICATION(Service.HEARTRATE, "Heart rate notification", "00002a37-0000-1000-8000-00805f9b34fb"),
        HEARTRATE(Service.HEARTRATE, "Heart rate", "00002a39-0000-1000-8000-00805f9b34fb"),

        GENERIC_DEVICE_NAME(Service.GAP_SERVICE, "Device name", "00002a00-0000-1000-8000-00805f9b34fb"),
        GENERIC_DEVICE_APPEARANCE(Service.GAP_SERVICE, "Device appearance", "00002a01-0000-1000-8000-00805f9b34fb"),
        PERIPHERAL_PRIVACY(Service.GAP_SERVICE, "Peripheral privacy", "00002a02-0000-1000-8000-00805f9b34fb"),
        CONNECTION_PARAMS(Service.GAP_SERVICE, "Preferred connection parameters", "00002a04-0000-1000-8000-00805f9b34fb"),

        SERVICE_CHANGED(Service.GAP, "GAP Service changed", "00002a05-0000-1000-8000-00805f9b34fb"),


        VIBRATION(Service.VIBRATE, "Vibration", "00002a06-0000-1000-8000-00805f9b34fb");

        private Service parent;
        private String ident;
        private UUID uuid;
        private static Map<UUID, Characteristic> uuidMap = new HashMap<>();

        private Characteristic(Service parent, String name, String uuid){
            this.parent = parent;
            this.ident = name;
            this.uuid = UUID.fromString(uuid);
        }

        public UUID getUUID(){
            return uuid;
        }

        public Service getParent(){
            return parent;
        }

        @Override
        public String toString(){
            return ident;
        }

        public static Characteristic fromUUID(UUID uuid){
            return uuidMap.get(uuid);
        }

        static {
            for(Characteristic characteristic : Characteristic.values()){
                uuidMap.put(characteristic.getUUID(), characteristic);
            }
        }
    }

    public enum Command {
        PAIR(new byte[]{2}),
        SELF_TEST(new byte[]{2}),
        VIBRATE2(new byte[]{4}),
        VIBRATE2_LED2(new byte[]{3}),
        VIBRATE10_LED(new byte[]{2}),
        VIBRATE2_LED(new byte[]{1}),
        VIBRATION_STOP(new byte[]{0}),

        TEST_COMMAND(new byte[]{8, 2});

        private byte[] command;

        private Command(byte[] command){
            this.command = command;
        }

        public byte[] getCommand(){
            return command;
        }
    }
}
