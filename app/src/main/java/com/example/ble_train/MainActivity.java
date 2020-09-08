package com.example.ble_train;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.icu.util.Output;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    TextView mTvBluetoothStatus;
    TextView mTvReceiveData;
    TextView mTvSendData;
    Button mBtnBluetoothOn;
    Button mBtnBluetoothOff;
    Button mBtnConnect;
    Button mBtnSendData;

    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices;
    List<String> mListPairedDevices;

    Handler mBluetoothHandler;
    ConnectedBluetoothThread mThreadConnectedBluetooth;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    //스마트폰 - 스마트폰 간 통신
    final static UUID BT_UUID = UUID.fromString("8CE255C0-200A-11E0-AC64-0800200C9A66");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTvBluetoothStatus = (TextView)findViewById(R.id.tvBluetoothStatus);
        mTvReceiveData = (TextView)findViewById(R.id.tvReceiveData);
        mTvSendData =  (EditText) findViewById(R.id.tvSendData);
        mBtnBluetoothOn = (Button)findViewById(R.id.btnBluetoothOn);
        mBtnBluetoothOff = (Button)findViewById(R.id.btnBluetoothOff);
        mBtnConnect = (Button)findViewById(R.id.btnConnect);
        mBtnSendData = (Button)findViewById(R.id.btnSendData);

        //getDefaultAdapter : 해당 장치가 블루투스를 지원하는지 확인
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //블루투스 ON
        mBtnBluetoothOn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothOn();
            }
        });
        //블루투스 OFF
        mBtnBluetoothOff.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothOff();
            }
        });
        //블루투스 연결
        mBtnConnect.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                listPairedDevices();
            }
        });
        //블루투스 통신
        mBtnSendData.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mThreadConnectedBluetooth != null) {
                    try {
                        mThreadConnectedBluetooth.write(mTvSendData.getText().toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mTvSendData.setText("");
                }
            }
        });

        //수신 된 데이터를 receiveData 부분에 표시하기
        mBluetoothHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == BT_MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    void bluetoothOn(){
        if(mBluetoothAdapter==null){
            Toast.makeText(getApplicationContext(),"블루투스를 지원하지 않는 기기입니다.",Toast.LENGTH_LONG).show();
            finish();
        }
        else{
            if(mBluetoothAdapter.isEnabled()){
                Toast.makeText(getApplicationContext(), "블루투스가 이미 활성화 되어 있습니다.", Toast.LENGTH_LONG).show();
                mTvBluetoothStatus.setText("활성화");
            }
            else{
                Toast.makeText(getApplicationContext(), "블루투스가 활성화 되어있지 않습니다.", Toast.LENGTH_LONG).show();
                Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intentBluetoothEnable,BT_REQUEST_ENABLE); //onActivityResult 로 연결
            }
        }

    }

    void bluetoothOff(){
        if(!mBluetoothAdapter.isEnabled()){
            Toast.makeText(getApplicationContext(), "블루투스가 이미 비활성화 되어 있습니다.", Toast.LENGTH_LONG).show();
        }
        else{
            mBluetoothAdapter.disable();
            Toast.makeText(getApplicationContext(), "블루투스를 비활성화 합니다.",Toast.LENGTH_LONG).show();
            mTvBluetoothStatus.setText("비활성화");
        }
    }

    //bluetoothOn에서 비활성화 상태일 시 켜는 화면
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case BT_REQUEST_ENABLE: //enable 화면에서
                if(resultCode==RESULT_OK){ //활성화 확인
                    Toast.makeText(getApplicationContext(),"블루투스 활성화",Toast.LENGTH_LONG).show();
                    mTvBluetoothStatus.setText("활성화");
                }
                else if(resultCode==RESULT_CANCELED){
                    Toast.makeText(getApplicationContext(),"취소",Toast.LENGTH_LONG).show();
                    mTvBluetoothStatus.setText("비활성화");
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void listPairedDevices(){
        if(mBluetoothAdapter.isEnabled()){ //활성화 여부 확인
            if(mPairedDevices.size()>0){ //페어링 된 장치 여부 확인
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("장치 선택");

                mListPairedDevices = new ArrayList<>();
                for(BluetoothDevice d : mPairedDevices){
                    mListPairedDevices.add(d.getName());
                }
                final CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);

                builder.setItems(items, new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialogInterface, int item) {
                        connectSelectedDevice(items[item].toString());
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
            else{
                Toast.makeText(getApplicationContext(),"페어링된 장치가 없습니다.",Toast.LENGTH_LONG).show();
            }
        }
        else{
            Toast.makeText(getApplicationContext(),"블루투스가 비활성화 되어 있습니다.",Toast.LENGTH_LONG).show();
        }
    }

    //실제 블루투스 장치와 연결
    void connectSelectedDevice(String selectedDeviceName){
        //페어링 된 모든 장치를 검색하면서 그 장치의 주소 값 얻어옴
        for(BluetoothDevice tempDevice : mPairedDevices){
            if(selectedDeviceName.equals(tempDevice.getName())){
                mBluetoothDevice = tempDevice;
                break;
            }
        }
        try{
            //1. mBluetoothDevice를 통해 UUID를 호출하여 mBluetoothSocket을 가져온다.
            //2. mBluetoothSocket이 초기화 되며 connect한다.
            //3. 데이터 수신은 언제 이루어질지 모르기 때문에 데이터 수신을 위한 쓰레드를 따로 만들어 처리한다.
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            mBluetoothSocket.connect();
            mThreadConnectedBluetooth = new ConnectedBluetoothThread(mBluetoothSocket);
            mThreadConnectedBluetooth.start();
            mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS,1,-1).sendToTarget();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private class ConnectedBluetoothThread extends Thread{
        private BluetoothSocket mmSocket;
        private InputStream mmInStream=null;
        private OutputStream mmOutStream=null;

        public ConnectedBluetoothThread(BluetoothSocket socket) throws IOException {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //소켓 전송 처리하는 inputStream 및 outputStream 가져오기 (초기화)
            //데이터 전송 및 수신하는 길 만들어주기
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            //데이터는 언제 들어올지 모르니 항상 확인해야함
            //while문으로 데이터 읽어오기
            while(true){
                try {
                    bytes = mmInStream.available();
                    if(bytes != 0){
                        SystemClock.sleep(100);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer,0,bytes);
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
        //데이터 전송
        public void write(String str) throws IOException {
            byte[] bytes = str.getBytes();
            mmOutStream.write(bytes);
        }
        //소켓 닫기기
        public void cancel() throws IOException {
            mmSocket.close();
        }

    }


}