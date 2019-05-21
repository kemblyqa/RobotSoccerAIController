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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    //UI elements
    TextView commandReceived;
    Button button;

    //Handler
    static Handler handler;
    static final int HANDLER_STATE = 0;//used to identify handler message

    //Bluetooth
    private BluetoothDevicesService bluetoothDevicesService = BluetoothDevicesService.getInstance();
    public ArrayList<BluetoothDevice> bluetoothDevices;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        commandReceived = findViewById(R.id.commandTxt);
        button = findViewById(R.id.button);
        bluetoothAdapter = bluetoothDevicesService.getBluetoothAdapter();
        makeBluetoothConnection();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // bluetoothDevicesService.createServerConnection();

            }
        });

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == HANDLER_STATE){
                    Toast.makeText(bluetoothDevicesService, "Por aquí", Toast.LENGTH_SHORT).show();
                    String readMessage = (String) msg.obj;// msg.arg1 = bytes from connect thread
                    commandReceived.setText("algooooo");
                    // commandReceived.setText(readMessage);
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
                    Toast.makeText(this, "Bluetooth connected....", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Bluetooth discoverable....", Toast.LENGTH_SHORT).show();
                   // bluetoothDevicesService.createServerConnection();
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
    public void requestForBluetoothService(View view){
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
            //here a toggle image or something
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BluetoothDevicesService.REQUEST_BLUETOOTH);
        }
    }
    private void makeBluetoothConnection(){
        if (!bluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), BluetoothDevicesService.REQUEST_BLUETOOTH);
        } else {
            Toast.makeText(this, "Bluetooth connected....", Toast.LENGTH_SHORT).show();
        }
        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                        .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300),
                BluetoothDevicesService.REQUEST_BLUETOOTH_DISCOVERABLE);
    }
}
