package com.cbcdn.dev.unfit.helpers;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.cbcdn.dev.unfit.R;
import java.util.LinkedList;
import java.util.List;

public class DeviceListAdapter extends BaseAdapter implements ListAdapter {

    private List<Pair<ScanRecord, BluetoothDevice>> members = new LinkedList<>();

    @Override
    public int getCount() {
        return members.size();
    }

    @Override
    public Object getItem(int position) {
        return members.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            Log.d("Adapter", "Inflating new Layout");
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_entry, null);
        }
        else{
            Log.d("Adapter", "Reusing old Layout");
        }

        TextView mac = (TextView)convertView.findViewById(R.id.deviceMac);
        TextView name = (TextView)convertView.findViewById(R.id.deviceName);

        if(mac != null){
            mac.setText(members.get(position).second.getAddress());
        }

        if(name != null){
            name.setText((members.get(position).first.getDeviceName() == null)? "<unnamed>":members.get(position).first.getDeviceName());
        }

        return convertView;
    }

    public void add(ScanRecord scanRecord, BluetoothDevice device) {
        members.add(new Pair<>(scanRecord, device));
        notifyDataSetChanged();
    }

    public void clear() {
        members.clear();
        notifyDataSetInvalidated();
    }
}
