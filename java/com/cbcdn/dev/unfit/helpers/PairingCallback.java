package com.cbcdn.dev.unfit.helpers;

import android.util.Log;
import com.cbcdn.dev.unfit.BLEDevice;
import com.cbcdn.dev.unfit.helpers.ConstMapper.Characteristic;
import com.cbcdn.dev.unfit.helpers.ConstMapper.Command;

public class PairingCallback extends BLECallback {
    private enum PairingState {
        STARTED,
        CHECK;
    }

    PairingState pairState = PairingState.STARTED;

    @Override
    public void writeCompleted(BLEDevice self, Characteristic characteristic, int status) {
        switch(pairState){
            case STARTED:
                Log.d("PairingCallback", "Pair write done, reading back characteristic");
                self.requestPriorityRead(Characteristic.PAIR, this);
                pairState = PairingState.CHECK;
                return;
        }

        Log.e("PairingCallback", "Invalid state in write callback");
    }

    @Override
    public void readCompleted(BLEDevice self, Characteristic characteristic, int status, byte[] data) {
        if(data.length < 1) {
            Log.e("PairingCallback", "Invalid characteristic data length");
            return;
        }
        switch(pairState){
            case STARTED:
                if(data[0] == 0xFF){
                    Log.d("PairingCallback", "Unpaired device detected, writing pair command");
                    self.requestPriorityWrite(Characteristic.PAIR, Command.PAIR.getCommand(), this);
                    return;
                }
                break;
            case CHECK:
                if(data[0] == 0x2){
                    Log.d("PairingCallback", "Successfully paired");
                }
                else{
                    Log.d("PairingCallback", "Pairing failed");
                }
        }

        Log.d("PairingCallback", "Job done");
    }
}
