package com.cbcdn.dev.unfit.helpers;

import android.bluetooth.BluetoothProfile;
import android.content.SharedPreferences;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.prefs.InvalidPreferencesFormatException;

public final class ConstMapper {
    private ConstMapper(){
    }

    public enum ChargeState {
        UNKNOWN(0, "Unknown state"), //encountered after reboot
        CHARGING_LOW(1, "Charging (low)"),
        CHARGING_MEDIUM(2, "Charging (medium)"),
        CHARGING_FULL(3, "Charging (full)"),
        DISCHARGING(4, "Discharging");

        private int value;
        private String desc;
        private static Map<Integer, ChargeState> valueMap = new HashMap<>();

        ChargeState(int value, String desc){
            this.value = value;
            this.desc = desc;
        }

        public static ChargeState getByValue(int value){
            return valueMap.get(Integer.valueOf(value));
        }

        @Override
        public String toString(){
            return desc;
        }

        public int getValue(){
            return value;
        }

        static {
            for(ChargeState state : ChargeState.values()){
                valueMap.put(state.value, state);
            }
        }

    }

    public enum BTLEState {
        CONNECTED("Connected", BluetoothProfile.STATE_CONNECTED),
        CONNECTING("Connecting", BluetoothProfile.STATE_CONNECTING),
        DISCONNECTED("Disconnected", BluetoothProfile.STATE_DISCONNECTED),
        DISCONNECTING("Disconnecting", BluetoothProfile.STATE_DISCONNECTING);

        private String name;
        private int value;
        private static Map<Integer, BTLEState> valueMap = new HashMap<>();

        BTLEState(String name, int value){
            this.name = name;
            this.value = value;
        }

        public static BTLEState getByValue(int value){
            return valueMap.get(Integer.valueOf(value));
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
        ACCESS("Generic Access", "00001800-0000-1000-8000-00805f9b34fb"),
        ATTRIBUTES("Generic Attribute", "00001801-0000-1000-8000-00805f9b34fb"),

        UNKNOWN_1("Unknown 1", "0000fee1-0000-1000-8000-00805f9b34fb");

        private String ident;
        private UUID uuid;
        private static Map<UUID, Service> uuidMap = new HashMap<>();

        Service(String name, String uuid){
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
        DEVICE_INFO(Service.MILI, "Device info", "0000ff01-0000-1000-8000-00805f9b34fb", true, true){
            @Override
            public String interpret(byte[] data) {
                if(data.length ==16 || data.length == 20){
                    StringBuilder rv = new StringBuilder("Magic ");
                    for(int i = 0; i < 4; i++){
                        rv.append(String.format("%02X", data[i]));
                    }

                    rv.append(", Features " + data[4] + ", ");
                    rv.append("Appearance " + data[5] + ", ");
                    rv.append("Hardware version " + data[6] + ", ");
                    rv.append("Header checksum " + String.format("%02X", data[7]) + ", ");
                    rv.append("Profile version " + data[11] + "." + data[10] + "." + data[9] + "." + data[8] + ", ");
                    rv.append("Firmware version " + data[15] + "." + data[14] + "." + data[13] + "." + data[12]);
                    if(data.length == 20) {
                        rv.append(", Secondary firmware version " + data[19] + "." + data[18] + "." + data[17] + "." + data[16]);
                    }

                    return rv.toString();
                }
                else{
                    return "Data invalid";
                }
            }
        },
        DEVICE_NAME(Service.MILI, "Device name", "0000ff02-0000-1000-8000-00805f9b34fb", false, false){
            @Override
            public String interpret(byte[] data) {
                return new String(data);
            }
        },
        NOTIFICATION(Service.MILI, "Notification", "0000ff03-0000-1000-8000-00805f9b34fb", true, false){
            @Override
            public String interpret(byte[] data) {
                if(data.length > 0) {
                    switch (data[0]) {
                        case 5:
                            return "Authentication OK";
                        case 8:
                            return "Latency parameters OK";
                        case 10:
                            return "Authentication OK (Reset)";
                    }
                }
                else{
                    return "Zero length";
                }

                return "Unknown notification byte";
            }
        },
        USER_INFO(Service.MILI, "User info", "0000ff04-0000-1000-8000-00805f9b34fb", true, false),
        CONTROL(Service.MILI, "Control", "0000ff05-0000-1000-8000-00805f9b34fb", false, false),
        REALTIME_STEPS(Service.MILI, "Steps", "0000ff06-0000-1000-8000-00805f9b34fb", true, true){
            @Override
            public String interpret(byte[] data) {
                ByteBuffer bb = ByteBuffer.wrap(data);
                return Integer.reverseBytes(bb.getInt()) + " steps";
            }
        },
        ACTIVITY(Service.MILI, "Activity?", "0000ff07-0000-1000-8000-00805f9b34fb", true, false),
        FIRMWARE(Service.MILI, "Firmware data", "0000ff08-0000-1000-8000-00805f9b34fb", false, false),
        BLE_PARAMS(Service.MILI, "BLE parameters", "0000ff09-0000-1000-8000-00805f9b34fb", true, false),
        TIME(Service.MILI, "Time", "0000ff0a-0000-1000-8000-00805f9b34fb", true, true){
            @Override
            public String interpret(byte[] data) {
                if((data.length % 6) != 0){
                    return "Invalid time data length";
                }

                if(data.length == 0){
                    return "No data";
                }

                StringBuilder rv = new StringBuilder();

                for(int date = 0; date < data.length / 6; date++) {
                    rv.append("Date #" + (date + 1) + " ");

                    rv.append((data[(date * 6) + 0] + 2000) + "-");
                    rv.append((data[(date * 6) + 1] + 1) + "-");
                    rv.append((data[(date * 6) + 2]) + " ");

                    rv.append((data[(date * 6) + 3]) + ":");
                    rv.append((data[(date * 6) + 4]) + ":");
                    rv.append((data[(date * 6) + 5]) + " ");
                }

                return rv.toString();
            }
        },
        STATISTICS(Service.MILI, "Statistics", "0000ff0b-0000-1000-8000-00805f9b34fb", true, true),
        BATTERY(Service.MILI, "Battery", "0000ff0c-0000-1000-8000-00805f9b34fb", true, true){
            @Override
            public String interpret(byte[] data) {
                if(data.length != 10){
                    return "Invalid data length";
                }

                StringBuilder rv = new StringBuilder();

                rv.append(data[0] + "% charged, ");
                rv.append("Last charge: " + (data[1] + 2000) + "-" + (data[2] + 1) + "-" + data[3] + " ");
                rv.append(data[4] + ":" + data[5] + ":" + data[6] + ", ");
                rv.append((data[7] | data[8] << 8) + " charge cycles, ");
                rv.append("Status: " + ChargeState.getByValue(data[9]));

                return rv.toString();
            }
        },
        TEST(Service.MILI, "Device test", "0000ff0d-0000-1000-8000-00805f9b34fb", false, false),
        SENSOR(Service.MILI, "Sensor data?", "0000ff0e-0000-1000-8000-00805f9b34fb", true, true),
        PAIR(Service.MILI, "Pairing", "0000ff0f-0000-1000-8000-00805f9b34fb", false, false){
            @Override
            public String interpret(byte[] data) {
                if(data.length < 1) {
                    return "Invalid pairing data length";
                }

                if(data[0] == 2){
                    return "Paired";
                }
                if(data[0] == (byte)0xFF){
                    return "Not paired";
                }

                return "Unknown pairing data value";
            }
        },

        HEARTRATE_NOTIFICATION(Service.HEARTRATE, "Heart rate notification", "00002a37-0000-1000-8000-00805f9b34fb", false, false),
        HEARTRATE(Service.HEARTRATE, "Heart rate", "00002a39-0000-1000-8000-00805f9b34fb", false, true),

        GENERIC_DEVICE_NAME(Service.ACCESS, "Device name", "00002a00-0000-1000-8000-00805f9b34fb", true, true){
            @Override
            public String interpret(byte[] data) {
                return new String(data);
            }
        },
        GENERIC_DEVICE_APPEARANCE(Service.ACCESS, "Device appearance", "00002a01-0000-1000-8000-00805f9b34fb", false, false),
        PERIPHERAL_PRIVACY(Service.ACCESS, "Peripheral privacy", "00002a02-0000-1000-8000-00805f9b34fb", true, false),
        CONNECTION_PARAMS(Service.ACCESS, "Preferred connection parameters", "00002a04-0000-1000-8000-00805f9b34fb", true, false),

        SERVICE_CHANGED(Service.ATTRIBUTES, "GAP Service changed", "00002a05-0000-1000-8000-00805f9b34fb", false, false),

        VIBRATION(Service.VIBRATE, "Vibration", "00002a06-0000-1000-8000-00805f9b34fb", false, false);

        private Service parent;
        private String ident;
        private UUID uuid;
        private static Map<UUID, Characteristic> uuidMap = new HashMap<>();
        private boolean passive;
        private boolean displayed;

        Characteristic(Service parent, String name, String uuid, boolean passive, boolean displayed){
            this.parent = parent;
            this.ident = name;
            this.uuid = UUID.fromString(uuid);
            this.passive = passive;
            this.displayed = displayed;
        }

        public boolean isPassive() {
            return passive;
        }

        public boolean isDisplayed(){
            return displayed;
        }

        public String interpret(byte[] data){
            return "not implemented";
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
        PAIR(Characteristic.PAIR ,new byte[]{2}),
        SELF_TEST(Characteristic.TEST, new byte[]{2}),
        VIBRATE2(Characteristic.VIBRATION, new byte[]{4}),
        VIBRATE2_LED2(Characteristic.VIBRATION, new byte[]{3}),
        VIBRATE10_LED(Characteristic.VIBRATION, new byte[]{2}),
        VIBRATE2_LED(Characteristic.VIBRATION, new byte[]{1}),
        VIBRATION_STOP(Characteristic.VIBRATION, new byte[]{0}),

        FACTORY_RESET(Characteristic.CONTROL, new byte[]{9}),

        SET_GOAL(Characteristic.CONTROL, new byte[]{5, 0}){
            @Override
            public byte[] getCommand(String mac, SharedPreferences preferences) throws InvalidPreferencesFormatException {
                if(!preferences.contains("goal")){
                    throw new InvalidPreferencesFormatException("Missing goal preference");
                }

                ByteBuffer command = ByteBuffer.allocate(4);
                command.put(this.command);
                command.putShort(Short.reverseBytes(Short.parseShort(preferences.getString("goal", "5000"))));

                return command.array();
            }
        },
        SET_TIME(Characteristic.TIME, null){
            @Override
            public byte[] getCommand(String mac, SharedPreferences preferences) throws InvalidPreferencesFormatException {
                ByteBuffer data = ByteBuffer.allocate(12);
                Calendar calendar = Calendar.getInstance();

                data.put((byte) (calendar.get(Calendar.YEAR) - 2000));
                data.put((byte) calendar.get(Calendar.MONTH));
                data.put((byte) calendar.get(Calendar.DATE));

                data.put((byte) calendar.get(Calendar.HOUR_OF_DAY));
                data.put((byte) calendar.get(Calendar.MINUTE));
                data.put((byte) calendar.get(Calendar.SECOND));

                data.putShort((short) 0x0F0F);
                data.putShort((short) 0x0F0F);
                data.putShort((short) 0x0F0F);

                return data.array();
            }
        },
        SET_LOCATION(Characteristic.CONTROL, new byte[]{15}){
            @Override
            public byte[] getCommand(String mac, SharedPreferences preferences) {
                return new byte[]{this.command[0], Byte.parseByte(preferences.getString("side", "0"))};
            }
        },

        SET_USER_DATA(Characteristic.USER_INFO, null) {
            @Override
            public byte[] getCommand(String mac, SharedPreferences preferences) throws InvalidPreferencesFormatException {

                if(!preferences.contains("gender") || !preferences.contains("age")
                        ||!preferences.contains("height") || !preferences.contains("weight")){
                    throw new InvalidPreferencesFormatException("Missing data");
                }

                ByteBuffer data = ByteBuffer.allocate(20);

                data.putInt(0xFEEDFEFE);
                //data.putInt(0xDEADBEEF);
                data.put(Byte.parseByte(preferences.getString("gender", "0")));
                data.put(Byte.parseByte(preferences.getString("age", "0")));
                //FIXME these are ugly because Java is really picky about bytes
                data.put((byte) (Integer.parseInt(preferences.getString("height", "0")) & 0xFF));
                data.put((byte) (Integer.parseInt(preferences.getString("weight", "0")) & 0xFF));
                data.put((byte) 0);
                data.put((byte) 4);
                data.put((byte) 0);
                data.put("cbdevrox".getBytes());

                //calculate the checksum
                //Log.d("User info", "CRC is " + String.format("%02X", crc(data.array(), 0, 19)));
                data.put((byte) (crc(data.array(), 0, 19) ^ Integer.parseInt(mac.substring(mac.length() - 2), 16)));

                return data.array();
            }

            private byte crc(byte[] data, int offset, int length) {
                byte crc = 0x00;

                for (int i = 0; i < length; i++) {
                    byte temp = data[i + offset];

                    //process byte
                    for (int j = 0; j < 8; j++) {
                        int parity = (crc ^ temp) & 0x01;
                        crc = (byte) ((crc & 0xFF) >>> 1);

                        if (parity != 0) {
                            crc = (byte) (crc ^ 0x8c);
                        }

                        temp = (byte) ((temp & 0xFF) >>> 1);
                    }
                }

                return crc;
            }
        },
        REBOOT(Characteristic.CONTROL, new byte[]{12});

        protected byte[] command;
        private Characteristic endpoint;

        Command(Characteristic endpoint, byte[] command){
            this.endpoint = endpoint;
            this.command = command;
        }

        public byte[] getCommand(String mac, SharedPreferences preferences) throws InvalidPreferencesFormatException{
            return command;
        }

        public Characteristic getEndpoint(){
            return endpoint;
        }
    }
}
