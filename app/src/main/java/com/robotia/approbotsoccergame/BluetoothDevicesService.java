package com.robotia.approbotsoccergame;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothDevicesService extends Application {
    private static BluetoothDevicesService instance;
    static BluetoothDevicesService getInstance() {
        return instance;
    }

    //flags
    static final int HANDLER_STATE = 0;             //used to identify handler message
    static final int REQUEST_BLUETOOTH = 1;
    static final int REQUEST_BLUETOOTH_DISCOVERABLE = 2;
    static final String SOCKET_NAME = "AppRobotIA";
    static final String TAG = "MY_APP_DEBUG_TAG";

    //string id's
    static final String ARDUINO_MAC_ADDRESS = "00:00";
    static final String DEVICE_MAC_ADDRESS = "00:00";
    static final String HEADPHONES_MAC_ADDRESS = "22:22:22:67:0E:00";
    final UUID ARDUINO_PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //need this
    static final UUID DEVICE_PORT_UUID = UUID.fromString("0000111E-0000-1000-8000-00805F9B34FB");
    static final UUID DEVICE_PORT_UUID_1 = UUID.fromString("0000110a-0000-1000-8000-00805f9b34fb");
    static final UUID DEVICE_PORT_UUID_2 = UUID.fromString("0000111f-0000-1000-8000-00805f9b34fb");
    static final UUID DEVICE_PORT_UUID_3 = UUID.fromString("00001112-0000-1000-8000-00805f9b34fb");
    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    //Bluetooth
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public BluetoothSocket bluetoothArduinoSocket = null;
    ConnectedThread mConnectedThread;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    BluetoothAdapter getBluetoothAdapter(){
        return bluetoothAdapter;
    }

    public void closeConnection(){
        if(bluetoothArduinoSocket != null && bluetoothArduinoSocket.isConnected()) mConnectedThread.cancel();
    }

    public boolean isSocketAlive(){
        return bluetoothArduinoSocket != null && bluetoothArduinoSocket.isConnected();
    }

    public boolean createConnection(BluetoothDevice device, UUID uuid){
        ConnectThread connectThread = new ConnectThread(device, uuid);
        connectThread.start();
        return true;
    }

    public void sendWord(char key){
        mConnectedThread.write(key);
    }

    private void connected(BluetoothSocket mmSocket) {
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }
    private class ConnectThread extends Thread {
        private final BluetoothDevice mmDevice;
        ConnectThread(BluetoothDevice device, UUID MY_UUID) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            // BluetoothSocket tmp = null;
            mmDevice = device;
            // BluetoothDevice actualDevice = bluetoothAdapter.getRemoteDevice(mmDevice.getAddress());
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                bluetoothArduinoSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            Log.d(TAG, "SOCKET CREATED SUCCESSFULLY");
            // bluetoothArduinoSocket = tmp;
        }
        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                bluetoothArduinoSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                this.cancel();
                return;
            }
            connected(bluetoothArduinoSocket);
        }
        // Closes the client socket and causes the thread to finish.
        void cancel() {
            try {
                bluetoothArduinoSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }
    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        byte[] mmBuffer; // mmBuffer store for the stream

        //creation of the connect thread
        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            mmBuffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(mmBuffer);         //read bytes from input buffer
                    String readMessage = new String(mmBuffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    MainActivity.handler.obtainMessage(MainActivity.HANDLER_STATE, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        void write(char input) {
            byte msgBuffer = (byte)input;           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
                mmOutStream.flush();
                MainActivity.handler.obtainMessage(MainActivity.HANDLER_STATE, -1, -1, msgBuffer).sendToTarget();
                // Toast.makeText(BluetoothDevicesService.this, input, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                //if you cannot write, close the application
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        MainActivity.handler.obtainMessage(MainActivity.HANDLER_STATE);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                MainActivity.handler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}
