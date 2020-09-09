package com.example.ble_train;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.util.Output;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

//1. Bluetooth on/off
//2. Discoverable / Find device
//3. Pairing device
//4. Send data

public class MainActivity extends AppCompatActivity{
    Button btnBluetoothOn;
    Button btnBluetoothOff;

    TextView tvBluetoothStatus;
    Button btnConnect;

    BluetoothAdapter mBluetoothAdapter;

    Button btnDiscover;
    public ArrayList<BluetoothDevice> mBTdevices;
    public DeviceListAdapter mDeviceListAdapter;
    ListView newDevicesList;
    Button btnFindDiscover;

    //Mode
    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("tag", String.valueOf(Build.VERSION.SDK_INT));

        //initialize
        btnBluetoothOn = findViewById(R.id.btnBluetoothOn);
        btnBluetoothOff = findViewById(R.id.btnBluetoothOff);
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus);
        btnConnect = findViewById(R.id.btnConnect);
        btnDiscover = findViewById(R.id.btnDiscover);
        btnFindDiscover = findViewById(R.id.btnFindDiscover);
        newDevicesList = (ListView) findViewById(R.id.newDevicesList);

        //get Defualt of blooth adapter | get Permitted(Manifest)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //1. Bluetooth on/off
        btnBluetoothOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                blueToothOn();
            }
        });

        btnBluetoothOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                blueToothOff();
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               bluetoothDiscovery();
            }
        });

        btnFindDiscover.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(mBluetoothAdapter.isEnabled()){
                    mBTdevices = new ArrayList<>();
                    bluetoothDiscover();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Turn on bluetooth.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Broadcasts when bond state changes (ie.pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadCastReceiver,filter);
        newDevicesList.setOnItemClickListener(mItemClickListener);

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkBTPermissions(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            if(permissionCheck!=0){
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},1001);
            }
            else{
                Log.d("checkPermission", "No need to check permissions. SDK version < LoLLIPOP");
            }
        }
    }

    // Create a BroadcastReceiver
    private final BroadcastReceiver mBroadCastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        Toast.makeText(getApplicationContext(), "Bluetooth On", Toast.LENGTH_SHORT).show();
                        tvBluetoothStatus.setText("Active");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        Toast.makeText(getApplicationContext(), "Bluetooth Off", Toast.LENGTH_SHORT).show();
                        tvBluetoothStatus.setText("NonActive");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Toast.makeText(getApplicationContext(), "Bluetooth turning On", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Toast.makeText(getApplicationContext(), "Bluetooth turning Off", Toast.LENGTH_SHORT).show();
                        break;
                }
            }//end if

            else if (action.equals(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Toast.makeText(getApplicationContext(), "Discoverability enabled", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Toast.makeText(getApplicationContext(), "Discoverability Disabled. Able to receive connections", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Toast.makeText(getApplicationContext(), "Discoverability Disabled. Not able to receive connections", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Toast.makeText(getApplicationContext(), "Connected.",Toast.LENGTH_SHORT).show();
                        break;

                }
            }//end else if

            else if(action.equals(BluetoothDevice.ACTION_FOUND)){
                //get devices
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mBTdevices.add(device);
                    //Toast.makeText(getApplicationContext(), device.getName()+" : "+device.getAddress(), Toast.LENGTH_SHORT).show();
                    //attach device to adapter & set list
                    mDeviceListAdapter = new DeviceListAdapter(context,R.layout.device_adapter_view, mBTdevices);
                    newDevicesList.setAdapter(mDeviceListAdapter);
            }//end else if

            else if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(getApplicationContext(),"Already bonded", Toast.LENGTH_SHORT).show();
                }
                else if(mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Toast.makeText(getApplicationContext(),"Creating bond", Toast.LENGTH_SHORT).show();
                }
                else if(mDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Toast.makeText(getApplicationContext(),"Breaking a bond", Toast.LENGTH_SHORT).show();
                }
            }

        }//end onReceive
    };


    public void blueToothOn() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "This device doesn't support bluetooth service", Toast.LENGTH_SHORT).show();
            tvBluetoothStatus.setText("NonActive");
        } else if (mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Already On", Toast.LENGTH_SHORT).show();
        } else {
            //Toast.makeText(getApplicationContext(), "Bluetooth On",Toast.LENGTH_SHORT).show();
            //Ask user
            Intent intentBluetoothEnable = new Intent(mBluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE);//BT_REQUEST_ENABLE : ModeId

            //Intercept Status changed (by.broadcast)
            IntentFilter BTIntent = new IntentFilter(mBluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadCastReceiver, BTIntent);

        }
    }

    public void blueToothOff() {
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Already OFF", Toast.LENGTH_SHORT).show();
        } else if (mBluetoothAdapter.isEnabled()) {
            //Toast.makeText(getApplicationContext(), "Bluetooth Off",Toast.LENGTH_SHORT).show();
            mBluetoothAdapter.disable();
        }
    }

    public void bluetoothDiscovery(){
        Toast.makeText(getApplicationContext(), "Making device discoverable for 300 seconds.",Toast.LENGTH_SHORT).show();

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
        startActivity(discoverableIntent);

        //get scanmode change
        IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadCastReceiver,intentFilter);
    }

    public void bluetoothDiscover(){
        if(mBluetoothAdapter.isDiscovering()){ //already discovering > cancel
            mBluetoothAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(), "Canceling discovery", Toast.LENGTH_SHORT).show();

            checkBTPermissions(); //check for permissions

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadCastReceiver,discoverDevicesIntent);
        }

        else if(!mBluetoothAdapter.isDiscovering()){
            checkBTPermissions();
            Toast.makeText(getApplicationContext(), "Starting discovery", Toast.LENGTH_SHORT).show();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadCastReceiver,discoverDevicesIntent);
        }
    }

    @Override
    protected void onDestroy() {
        Toast.makeText(getApplicationContext(), "onDestroy called", Toast.LENGTH_SHORT).show();
        Log.d("onDestroy", "onDestroy called");
        super.onDestroy();
        unregisterReceiver(mBroadCastReceiver);
    }

    //ListView onClicklistener
    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            //becasue it's memeory intensive
            mBluetoothAdapter.cancelDiscovery();

            String deviceName = mBTdevices.get(i).getName();
            String deviceAddress = mBTdevices.get(i).getAddress();

            //create the bond
            //requires API 18+ (maybe JEllybeanMr2)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBTdevices.get(i).createBond();
            }
        }
    };
}