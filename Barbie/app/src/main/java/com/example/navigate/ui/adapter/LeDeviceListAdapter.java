package com.example.navigate.ui.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.navigate.R;

import java.util.ArrayList;
import java.util.HashMap;

public class LeDeviceListAdapter extends BaseAdapter {

	// Adapter for holding devices found through scanning.

	private ArrayList<BluetoothDevice> mLeDevices;
	private HashMap<String,String> rssiHashMap = new HashMap<>();
	private LayoutInflater mInflator;
	private Activity mContext;

	public LeDeviceListAdapter(Activity c) {
		super();
		mContext = c;
		mLeDevices = new ArrayList<BluetoothDevice>();
		mInflator = mContext.getLayoutInflater();
	}

	public void addDevice(BluetoothDevice device,String rssi) {
		if (!mLeDevices.contains(device)) {
			mLeDevices.add(device);
			rssiHashMap.put(device.getAddress(),rssi);
		}
		notifyDataSetChanged();
	}
	public void updateDevice(BluetoothDevice device) {
		for (int i = 0;i<mLeDevices.size();i++) {
			if (mLeDevices.get(i).getAddress().equalsIgnoreCase(device.getAddress())) {
				mLeDevices.set(i,device);
				break;
			}
		}
		notifyDataSetChanged();
	}

	public BluetoothDevice getDevice(int position) {
		return mLeDevices.get(position);
	}

	public void clear() {
		mLeDevices.clear();
	}

	@Override
	public int getCount() {
		return mLeDevices.size();
	}

	@Override
	public Object getItem(int i) {
		return mLeDevices.get(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		ViewHolder viewHolder;
		// General ListView optimization code.
		if (view == null) {
			view = mInflator.inflate(R.layout.listitem_device, null);
			viewHolder = new ViewHolder();
			viewHolder.deviceAddress = (TextView) view
					.findViewById(R.id.device_address);
			viewHolder.deviceName = (TextView) view
					.findViewById(R.id.device_name);
			viewHolder.img = (ImageView) view.findViewById(R.id.img);
			viewHolder.tvRssi = (TextView) view.findViewById(R.id.tvRssi);
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}

		BluetoothDevice device = mLeDevices.get(i);
		@SuppressLint("MissingPermission") final String deviceName = device.getName();
		if (deviceName != null && deviceName.length() > 0)
			viewHolder.deviceName.setText(deviceName);
		else
			viewHolder.deviceName.setText(R.string.unknown_device);
		viewHolder.deviceAddress.setText(device.getAddress());

		@SuppressLint("MissingPermission") int iBondState = device.getBondState();
		viewHolder.tvRssi.setTextColor(mContext.getResources().getColor(R.color.hui));
		String strBondState = "";
		// ?????????
		if (iBondState == BluetoothDevice.BOND_NONE) {
			strBondState = "?????????";
		}
		// ?????????
		else if (iBondState == BluetoothDevice.BOND_BONDING) {
			strBondState = "?????????";
		}
		// ?????????
		else if (iBondState == BluetoothDevice.BOND_BONDED) {
			strBondState = "?????????";
			viewHolder.tvRssi.setTextColor(mContext.getResources().getColor(R.color.green));
		}
		viewHolder.tvRssi.setText(rssiHashMap.get(device.getAddress())+"\n"+strBondState);
//		viewHolder.tvRssi.setText(strBondState);

		return view;
	}

	class ViewHolder {
		TextView deviceName;
		TextView deviceAddress;
		TextView tvRssi;
		ImageView img;
	}
}
