package com.cbcdn.dev.unfit.helpers;
import com.cbcdn.dev.unfit.BLECommunicator;
import com.cbcdn.dev.unfit.R;
import com.cbcdn.dev.unfit.helpers.ConstMapper.Characteristic;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValueListAdapter extends BaseAdapter implements ListAdapter {
    private List<Characteristic> characteristics;
    private Map<Characteristic, byte[]> characteristicData = new HashMap<>();

    public ValueListAdapter(List<Characteristic> characteristics){
        //FIXME might want to deep copy here
        this.characteristics = characteristics;
    }

    @Override
    public int getCount() {
        return characteristics.size();
    }

    @Override
    public Object getItem(int position) {
        return characteristics.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            Log.d("Adapter", "Inflating new Layout");
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.characteristic_display, parent, false);
        }
        else{
            Log.d("Adapter", "Reusing old Layout");
        }

        TextView name = (TextView)convertView.findViewById(R.id.characteristicName);
        TextView value = (TextView)convertView.findViewById(R.id.characteristicValue);

        if(name != null){
            name.setText(characteristics.get(position).toString());
        }

        if(value != null && characteristicData.get(characteristics.get(position)) != null){
            value.setText(characteristics.get(position).interpret(characteristicData.get(characteristics.get(position))));
        }

        return convertView;
    }

    public void update(Characteristic characteristic, byte[] data){
        int location = characteristics.indexOf(characteristic);
        if(location < 0){
            Log.e("ValueListAdapter", "Update on unlisted characteristic " + characteristic + " requested");
            return;
        }
        Log.d("ValueListAdapter", "Characteristic data for " + characteristic + " updated");
        characteristicData.put(characteristic, data);
        notifyDataSetChanged();
    }

    public void update(BLECommunicator.CommunicatorBinder serviceBinder, String mac) {
        for(Characteristic characteristic : characteristics){
            if(serviceBinder.queryCachedData(mac, characteristic) != null){
                characteristicData.put(characteristic, serviceBinder.queryCachedData(mac, characteristic));
            }
        }
        notifyDataSetChanged();
    }
}
