package com.robotia.approbotsoccergame;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
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
    static final String SOCKET_NAME = "comm socket";
    private static final String TAG = "MY_APP_DEBUG_TAG";

    //string id's
    static final String ARDUINO_MAC_ADDRESS = "00:00";
    static final String DEVICE_MAC_ADDRESS = "00:00";
    static final String HEADPHONES_MAC_ADDRESS = "22:22:22:67:0E:00";
    final UUID ARDUINO_PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //need this
    final UUID HEADSET_PORT_UUID = UUID.fromString("0000111E-0000-1000-8000-00805F9B34FB");
    final UUID DEVICE_PORT_UUID_1 = UUID.fromString("0000110a-0000-1000-8000-00805f9b34fb");
    final UUID DEVICE_PORT_UUID_2 = UUID.fromString("0000111f-0000-1000-8000-00805f9b34fb");
    final UUID DEVICE_PORT_UUID_3 = UUID.fromString("00001112-0000-1000-8000-00805f9b34fb");

    //Bluetooth
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public BluetoothSocket arduinoBluetoothSocket = null;
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
        if(arduinoBluetoothSocket != null && arduinoBluetoothSocket.isConnected()){
            try {
                arduinoBluetoothSocket.close();
            } catch (IOException e2) {
                //insert code to deal with this
                Toast.makeText(instance, "Closing error!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void createServerConnection(){
        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();
    }

    private void connected(BluetoothSocket mmSocket) {
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }
    //arduino conn
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        public ConnectThread(BluetoothDevice device, UUID MY_UUID) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                Log.e("","trying fallback...");
                try {
                    mmSocket =(BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(mmDevice,1);
                    mmSocket.connect();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException e) { }
            }
            connected(mmSocket);
        }
        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
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
        void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream

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
                // Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
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

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket commandVoiceBluetoothSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to commandVoiceBluetoothSocket
            // because commandVoiceBluetoothSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is th eapp's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(SOCKET_NAME, DEVICE_PORT_UUID_3);
                Log.d(TAG, "AcceptThread: Setting up Server using: " + DEVICE_PORT_UUID_3);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
            }
            commandVoiceBluetoothSocket = tmp;
        }

//        public void run() {
//            Log.d(TAG, "run: AcceptThread Running.");
//            BluetoothSocket socket = null;
//            // Keep listening until exception occurs or a socket is returned.
//            while (socket == null) {
//                try {
//                    socket = commandVoiceBluetoothSocket.accept();
//                } catch (IOException e) {
//                    Log.e("bad", "Socket's accept() method failed", e);
//                    break;
//                }
//
//                if (socket != null) {
//                    // A connection was accepted. Perform work associated with
//                    // the connection in a separate thread.
//                    Toast.makeText(BluetoothDevicesService.this, "Trying write", Toast.LENGTH_SHORT).show();
//                    connected(socket);
//                }
//            }
//        }

        public void run(){
            Log.d(TAG, "run: AcceptThread Running.");
            BluetoothSocket socket = null;
 //           while (true){
                try{
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    Log.d(TAG, "run: RFCOM server socket start.....");
                    socket = commandVoiceBluetoothSocket.accept();
                    Log.d(TAG, "run: RFCOM server socket accepted connection.");

                }catch (IOException e){
                    Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
                }

                if(socket != null){
                    connected(socket);
                    try {
                        commandVoiceBluetoothSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
   //             }
                Log.i(TAG, "END mAcceptThread ");
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try {
                commandVoiceBluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage() );
            }
        }
    }
}
