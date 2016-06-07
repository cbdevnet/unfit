package com.cbcdn.dev.unfit.callbacks;

import com.cbcdn.dev.unfit.BLEDevice;
import com.cbcdn.dev.unfit.helpers.ConstMapper.Characteristic;

public abstract class BLECallback {
    //TODO have callback chains automatically generate a progress indicator

    private BLECallback continuation = null;

    public BLECallback(BLECallback continuation){
        this.continuation = continuation;
    }

    public BLECallback(){
        this(null);
    }

    public abstract void start(BLEDevice device);

    public void writeCompleted(BLEDevice self, Characteristic characteristic, int status) {
    }

    public void readCompleted(BLEDevice self, Characteristic characteristic, int status, byte[] data) {
    }

    public void connectionChanged(BLEDevice self){
    }

    protected final void chain(BLEDevice self){
        if(continuation != null){
            continuation.start(self);
        }
    }
}
