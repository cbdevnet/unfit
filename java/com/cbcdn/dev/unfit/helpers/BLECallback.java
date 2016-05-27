package com.cbcdn.dev.unfit.helpers;

import com.cbcdn.dev.unfit.BLEDevice;
import com.cbcdn.dev.unfit.helpers.ConstMapper.Characteristic;

public abstract class BLECallback {
    public void writeCompleted(BLEDevice self, Characteristic characteristic, int status) {
    }

    public void readCompleted(BLEDevice self, Characteristic characteristic, int status, byte[] data) {
    }
}
