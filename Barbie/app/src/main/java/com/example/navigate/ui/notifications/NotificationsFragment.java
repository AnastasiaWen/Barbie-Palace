package com.example.navigate.ui.notifications;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.navigate.MainActivity;
import com.example.navigate.R;
import com.example.navigate.databinding.FragmentNotificationsBinding;
import com.example.navigate.ui.adapter.LeDeviceListAdapter;
import com.example.navigate.ui.home.HomeFragment;
import com.example.navigate.ui.others.BluetoothLeClass;
import com.example.navigate.ui.service.BLEService;
import com.example.navigate.ui.utilInfo.ClsUtils;
import com.example.navigate.ui.view.waveView;

import java.util.List;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;

    private final static String TAG = NotificationsFragment.class.getSimpleName();
    public BluetoothDevice nowSelectDevice;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    /** ??????BLE?????? */
    private BluetoothAdapter mBluetoothAdapter;
    /** ??????BLE?????? */
    public BluetoothLeClass mBLE;
    private boolean mScanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 3000;
    private ImageView imgSearch = null;
    private ProgressDialog dialog;
    private ListView blelv = null;
    private TextView restv = null;
    private static NotificationsFragment deviceScanActivity;
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    public static final int REQUEST_LOCATION_PERMISSION = 2;
    private RotateAnimation roanimation;
    public static NotificationsFragment getInstance() {
        return deviceScanActivity;
    }
    private View root;
    private float battery;


    @SuppressLint("MissingPermission")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel homeViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        Log.i(TAG,"Service State"+HomeFragment.isServiceRunning(getContext(),"BLEService"));
        deviceScanActivity = this;
        mHandler = new Handler();
        initlayout();

        final TextView textView = binding.textHome;
        final ImageView begin=binding.begin;
        final ImageView power=binding.powerOff;
        final ImageView planet=binding.planet;
        final waveView waveview=binding.waveview;

        //waveview.setBackgroundColor(Color.BLACK);
        if(MainActivity.note_ctl==true)
        {
            textView.setText("Device Has Connected!");
            textView.setClickable(false);
            power.setVisibility(View.VISIBLE);
            waveview.setVisibility(View.VISIBLE);
            planet.setVisibility(View.INVISIBLE);
            if(MainActivity.listBat.size()!=0){
                battery= (float) (0.3086*MainActivity.listBat.get(0)-689.2);
                waveview.begainAnimation(battery);
            }

        }
        else
        {
            textView.setText("Scan Devices");
            textView.setClickable(true);
            power.setVisibility(View.INVISIBLE);
            waveview.setVisibility(View.INVISIBLE);
            planet.setVisibility(View.VISIBLE);
        }

        power.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBLE.disconnect();
                Intent intent = new Intent(getActivity(), BLEService.class);
                getContext().stopService(intent);
                Log.e(TAG,"Service State"+HomeFragment.isServiceRunning(getContext(),"BLEService"));
                MainActivity.note_ctl=false;
                textView.setText("Scan Devices");
                textView.setClickable(true);
                power.setVisibility(View.INVISIBLE);
                waveview.setVisibility(View.INVISIBLE);
                planet.setVisibility(View.VISIBLE);
                if(MainActivity.mediaPlayer!=null){
                    MainActivity.mediaPlayer.stop();
                    MainActivity.mediaPlayer.release();
                }

                MainActivity.mediaPlayer_ctl=false;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "??????????????????",Toast.LENGTH_LONG).show();
                    }
                });

            }
        });

        //region Description
        if (!getActivity().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getActivity(), R.string.ble_not_supported, Toast.LENGTH_SHORT)
                    .show();
            getActivity().finish();
        }
        //endregion
        final BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        //???????????????????????????
        //region Description
        if (mBluetoothAdapter == null) {
            Toast.makeText(getActivity(), R.string.error_bluetooth_not_supported,
                    Toast.LENGTH_SHORT).show();
            getActivity().finish();
            //return ;
        }
        // ????????????
        if(!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }

        if (Build.VERSION.SDK_INT >= 23) {//?????? API level ??????????????? 23(Android 6.0) ???
            //????????????????????????
            if (PackageManager.PERMISSION_GRANTED != getActivity().checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                //????????????
                getActivity().requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CODE_ACCESS_COARSE_LOCATION);
            }
        }

        //?????????????????????????????????ble??????????????????
        if (Build.VERSION.SDK_INT >= 23 && !isLocationOpen(getActivity().getApplicationContext())) {
            Intent enableLocate = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(enableLocate, REQUEST_LOCATION_PERMISSION);
        }
        //endregion

        mBLE = new BluetoothLeClass(getActivity(),mBluetoothAdapter);
        dialog = new ProgressDialog(getActivity());
        dialog.setCanceledOnTouchOutside(false);
        dialog.setMessage("????????????????????????...");

        receiveBLEBroadcast();



        mScanning = true;
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Animation translateAnimation = new TranslateAnimation(0, 0, 0, -600);
                translateAnimation.setDuration(1000);
                translateAnimation.setFillEnabled(true);//?????????????????????????????????????????????
                translateAnimation.setFillAfter(true);//?????????????????????
                begin.setAnimation(translateAnimation);
                textView.setAnimation(translateAnimation);
                begin.startAnimation(translateAnimation);
                textView.startAnimation(translateAnimation);
                textView.setText("Connect Devices");
                textView.setTextSize(40);
                planet.setVisibility(View.INVISIBLE);
                waveview.setVisibility(View.INVISIBLE);

                scanLeDevice(mScanning);

            }
        });

        return root;
    }

    private void receiveBLEBroadcast() {
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);//??????????????????
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//????????????
        intent.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);//????????????

        getActivity().registerReceiver(searchDevices, intent);
    }

    public static boolean isLocationOpen(final Context context){
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        //gps??????
        boolean isGpsProvider = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        //????????????
        boolean isNetWorkProvider = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return isGpsProvider|| isNetWorkProvider;
    }

    void initlayout() {

        blelv = binding.blelv;

        mLeDeviceListAdapter = new LeDeviceListAdapter(getActivity());
        blelv.setAdapter(mLeDeviceListAdapter);
        blelv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                scanLeDevice(false);
                nowSelectDevice = mLeDeviceListAdapter.getDevice(position);
                dialog.show();
                mBLE.connect(nowSelectDevice,OnConnectListener);
            }
        });

    }


    BluetoothLeScanner scanner = null;
    /** ??????/?????? ???????????? */
    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
//			mHandler.postDelayed(new Runnable() {
//				@Override
//				public void run() {
//					scanLeDevice(false);
//				}
//			}, SCAN_PERIOD);
            //imgSearch.startAnimation(roanimation);
            mScanning = !enable;
            //restv.setText("Searching...");
            mLeDeviceListAdapter.clear();
//			mBluetoothAdapter.startDiscovery();
            mBluetoothAdapter.startLeScan(leScanCallback);
            if (scanner == null)
                scanner = mBluetoothAdapter.getBluetoothLeScanner();
            scanner.startScan(scanCallback);
//			scanner.startScan(null,null,scanCallback);


        } else {
            mScanning = !enable;
            //imgSearch.clearAnimation();
            //restv.setText("Stop the search");
//			mBluetoothAdapter.cancelDiscovery();
            mBluetoothAdapter.stopLeScan(leScanCallback);

            if (scanner != null)
                scanner.stopScan(scanCallback);

        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice bluetoothDevice = result.getDevice();
            Log.e(TAG, "onScanResult: "+result.toString());
            @SuppressLint("MissingPermission") String strName = bluetoothDevice.getName();
            if (strName != null && strName.length() > 0) {
                mLeDeviceListAdapter.addDevice(bluetoothDevice,result.getRssi()+"");
            }
//			mLeDeviceListAdapter.addDevice(bluetoothDevice,result.getRssi()+"");

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            @SuppressLint("MissingPermission") String strName = device.getName();
            if (strName != null && strName.length() > 0) {
                mLeDeviceListAdapter.addDevice(device,rssi+"");
            }
//			mLeDeviceListAdapter.addDevice(device,rssi+"");
        }
    };


    // ??????????????? ???????????????????????????
    private BluetoothLeClass.OnConnectListener OnConnectListener = new BluetoothLeClass.OnConnectListener() {
        @Override
        public void onConnectting(BluetoothGatt gatt, int status, int newState) {
            Log.e(TAG,"status: "+status+",newState:"+newState);
        }

        @Override
        public void onConnected(BluetoothGatt gatt, int status, int newState) {
            Log.e(TAG,"status: "+status+",newState:"+newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG,"YES");
                dialog.dismiss();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intentOne = new Intent(getActivity(), BLEService.class);
                        getActivity().startService(intentOne);
                        Navigation.findNavController(root).navigate(R.id.navigation_home);
                        MainActivity.note_ctl=true;
                        MainActivity.lineChart_ctl=true;
                        Log.i(TAG, "note_ctl"+MainActivity.note_ctl);
                        Toast.makeText(getActivity(), "????????????",Toast.LENGTH_LONG).show();

                        //startActivity(new Intent(getActivity(),DeviceHome.class));
                    }
                });

            }
        }

        @Override
        public void onDisconnect(BluetoothGatt gatt, int status, int newState) {
            Log.e(TAG,"status: "+status+",newState:"+newState);
            dialog.dismiss();
        }
    };



    String PIN = "0000";
    /**
     * ??????????????????
     */
    private BroadcastReceiver searchDevices = new BroadcastReceiver() {
        //??????
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle b = intent.getExtras();
            Object[] lstName = b.keySet().toArray();

            // ???????????????????????????????????????
            for (int i = 0; i < lstName.length; i++) {
                String keyName = lstName[i].toString();
                Log.e("bluetooth", keyName + ">>>" + String.valueOf(b.get(keyName)));
            }
            BluetoothDevice device;
            // ????????????????????????????????????????????????????????????????????????????????????????????????
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String strRSSI = b.get(BluetoothDevice.EXTRA_RSSI)+"";
                String strName = device.getName();
                if (strName != null && strName.length() > 0) {
                    mLeDeviceListAdapter.addDevice(device,strRSSI);
                }
//				mLeDeviceListAdapter.addDevice(device,strRSSI);

            }
            //???????????????
            else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDING://????????????
                        Log.e("BlueToothTestActivity", "????????????......");
                        Toast.makeText(getActivity(),"????????????......",Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothDevice.BOND_BONDED://????????????
                        Log.e("BlueToothTestActivity", "????????????");
                        Toast.makeText(getActivity(),"????????????",Toast.LENGTH_SHORT).show();
//						mBLE.connect(device,OnConnectListener);
                        break;
                    case BluetoothDevice.BOND_NONE://????????????/?????????
                        Log.e("BlueToothTestActivity", "????????????/?????????......");
                        Toast.makeText(getActivity(),"????????????",Toast.LENGTH_SHORT).show();
//						mBLE.connect(device,OnConnectListener);
                    default:
                        break;
                }
                mLeDeviceListAdapter.updateDevice(device);
            }
            // ????????????
            else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.e("BlueToothTestActivity", "????????????......");
                try {
                    //1.????????????
                    ClsUtils.setPairingConfirmation(device.getClass(), device, true);
                    //2.??????????????????
//					Log.e("order...", "isOrderedBroadcast:"+isOrderedBroadcast()+",isInitialStickyBroadcast:"+isInitialStickyBroadcast());
//					abortBroadcast();//???????????????????????????????????????????????????????????????????????????
                    //3.??????setPin??????????????????...
                    boolean ret = ClsUtils.setPin(device.getClass(), device, PIN);

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }
    };



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}