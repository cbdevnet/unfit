package com.cbcdn.dev.unfit.callbacks;

import com.cbcdn.dev.unfit.BLEDevice;
import com.cbcdn.dev.unfit.helpers.ConstMapper;

public class FirmwareUpdateCallback extends BLECallback {
    public FirmwareUpdateCallback(BLECallback continuation) {
        super(continuation);
    }

    public FirmwareUpdateCallback(){
        super();
    }

    @Override
    public void start(BLEDevice device) {
    }

    @Override
    public void writeCompleted(BLEDevice self, ConstMapper.Characteristic characteristic, int status) {
    }

    @Override
    public void readCompleted(BLEDevice self, ConstMapper.Characteristic characteristic, int status, byte[] data) {
    }
}
