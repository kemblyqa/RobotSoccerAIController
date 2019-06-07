package com.robotia.approbotsoccergame;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.net.URISyntaxException;
import java.util.Set;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {
    //UI elements
    TextView commandReceived, switchBt;
    Button button;
    //Handler
    static Handler handler;
    static final int HANDLER_STATE = 0;//used to identify handler message
    //Bluetooth
    private BluetoothDevicesService bluetoothDevicesService = BluetoothDevicesService.getInstance();
    private BluetoothAdapter bluetoothAdapter;
    //Socket
    Socket socket;
    private static final String BASE_URI = "http://172.24.29.56:3000";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        commandReceived = findViewById(R.id.commandTxt);
        switchBt = findViewById(R.id.switchBt);
        button = findViewById(R.id.button);
        bluetoothAdapter = bluetoothDevicesService.getBluetoothAdapter();
        makeBluetoothConnection();
        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), BluetoothDevicesService.REQUEST_BLUETOOTH_DISCOVERABLE);
        try {
            socket = IO.socket(BASE_URI);
            socket.connect();
            socket.emit("join","GAME");
        } catch (URISyntaxException e) {
            finish();
        }
        socket.on("test_response", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String data = (String) args[0];
                        Toast.makeText(MainActivity.this,data,Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        socket.on("comm", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String data = (String) args[0];
                        commandReceived.setText("Command: "+data);
                        if(bluetoothDevicesService.isSocketAlive()) bluetoothDevicesService.sendWord(data.charAt(0));
                        else Toast.makeText(MainActivity.this,"Bluetooth socket not connected",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
            if(msg.what == HANDLER_STATE){
                Toast.makeText(bluetoothDevicesService, "Mensaje de App", Toast.LENGTH_SHORT).show();
                String readMessage = (String) msg.obj;// msg.arg1 = bytes from connect thread
                commandReceived.setText(readMessage);
            }
            super.handleMessage(msg);
            }
        };
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case BluetoothDevicesService.REQUEST_BLUETOOTH:{
                if(resultCode == Activity.RESULT_OK){
                    //here a toggle image or something
                    Toast.makeText(this, "Bluetooth connected...", Toast.LENGTH_SHORT).show();
                }
                else if (resultCode == Activity.RESULT_CANCELED) {
                    finish();
                    System.exit(0);
                }
                break;
            }
            case BluetoothDevicesService.REQUEST_BLUETOOTH_DISCOVERABLE:{
                if(resultCode > 0){
                    //here a toggle image or something
                    Toast.makeText(this, "Bluetooth now is discoverable...", Toast.LENGTH_SHORT).show();
                }
                else if (resultCode == Activity.RESULT_CANCELED) {
                    finish();
                    System.exit(0);
                }
                else {
                    Toast.makeText(this, "Bluetooth something wrong discoverable....", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private void makeBluetoothConnection(){
        if (!bluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), BluetoothDevicesService.REQUEST_BLUETOOTH);
        } else {
            Toast.makeText(this, "Bluetooth actually connected...", Toast.LENGTH_SHORT).show();
        }
    }
    public void createNewBluetoothConnection(View view){
        bluetoothDevicesService.closeConnection();
        switchBt.setText("OFF");
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for(BluetoothDevice bt : pairedDevices){
            if(bt.getAddress().equals(BluetoothDevicesService.ARDUINO_MAC_ADDRESS)){
                if(bluetoothDevicesService.createConnection(bt, BluetoothDevicesService.MY_UUID)) switchBt.setText("ON");
                else Toast.makeText(bluetoothDevicesService, "Wrong device", Toast.LENGTH_SHORT).show();
            }
            else Toast.makeText(bluetoothDevicesService, "Couldn't connect device", Toast.LENGTH_SHORT).show();
        }
    }
//    public void getUUIDs(){
//        try {
//            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
//            Method getUuidsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);
//            ParcelUuid[] uuids = (ParcelUuid[]) getUuidsMethod.invoke(adapter, null);
//
//            if(uuids != null) {
//                for (ParcelUuid uuid : uuids) {
//                    Log.d("my.....", "UUID: " + uuid.getUuid().toString());
//                }
//            }else{
//                Log.d("my", "Uuids not found, be sure to enable Bluetooth!");
//            }
//
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
//    }
}
