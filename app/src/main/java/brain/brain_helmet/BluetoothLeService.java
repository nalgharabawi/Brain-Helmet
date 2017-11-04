package brain.brain_helmet;

/**
 * Created by Bassem Felemban on 7/30/2017.
 */

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG2 = "life";
    private final static String tag = BluetoothLeService.class.getSimpleName();
    boolean running = false;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    SharedPreferences prefs;
    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    boolean check = true;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "seniordesign.harambe.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "seniordesign.harambe.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "seniordesign.harambe.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "seniordesign.harambe.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "seniordesign.harambe.EXTRA_DATA";

    public final static UUID UUID_PROJECT_ZERO =
            UUID.fromString("F0001110-0451-4000-B000-000000000000");
    public final static UUID DATA_SERVICE = UUID.fromString("F0001130-0451-4000-B000-000000000000");
    //  UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);
    //"F0001111-0451-4000-B000-000000000000";

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(tag, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(tag, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(tag, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(tag, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                Log.w(tag, "money " + characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_PROJECT_ZERO.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();

            Log.d(tag, String.format("Received heart rate: %d", 1));
            //     intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                //  Log.i("datadata  ", data[0] + "");
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());

            }
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(tag, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(tag, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(tag, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(tag, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(tag, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(tag, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(tag, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(tag, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean writeCharacteristic(byte[] byteVal, String uuid, int f) {
        UUID here;
        if (f==0)
            here = UUID_PROJECT_ZERO;
        else
            here = DATA_SERVICE;
        //check mBluetoothGatt is available
        if (mBluetoothGatt == null) {
            Log.e(tag, "lost connection");
            return false;
        }
        BluetoothGattService Service = mBluetoothGatt.getService(here);
        if (Service == null) {
            Log.e(tag, "service not found!");
            return false;
        }
        UUID inputU =
                UUID.fromString(uuid);
        BluetoothGattCharacteristic charac = Service
                .getCharacteristic(inputU);
        //     BluetoothGattCharacteristic charac2 = Service
        //          .getCharacteristic(UUID.fromString(SampleGattAttributes.LED1));
        Log.i(tag, byteVal[0] + "");
        if (charac == null) {
            Log.e(tag, "char not found!");
            return false;
        }


        charac.setValue(byteVal);
        //charac2.setValue(byteVal);
        boolean status = mBluetoothGatt.writeCharacteristic(charac);
        //  mBluetoothGatt.writeCharacteristic(charac2);
        return status;
    }

    public BluetoothGattCharacteristic returnCharacteristic(UUID service, UUID input) {
        BluetoothGattService Service = mBluetoothGatt.getService(service);


        return Service.getCharacteristic(input);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */


    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                                 boolean enable) {

        mBluetoothGatt.setCharacteristicNotification(characteristic, enable);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[] { 0x01, 0x00 });
        descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : new byte[] { 0x01, 0x00 });
        return mBluetoothGatt.writeDescriptor(descriptor); //descriptor write operation successfully started?
    }

    public void SendDistance(int distance){
        UUID service = UUID.fromString("5f935bc0-1c05-4719-b7c5-87528f9eaf48");
        //UUID characteristic = UUID.fromString("00000048-0000-1000-8000-00805F9B34FB");
        UUID characteristic = UUID.fromString("5f93c1ad-1c05-4719-b7c5-87528f9eaf48");
        BluetoothGattCharacteristic charac = returnCharacteristic(service, characteristic);
        charac.setValue(integerToHex(distance));
        boolean status = mBluetoothGatt.writeCharacteristic(charac);
        Log.d("statuss", status + "   " + distance + " distance");
    }
    public void SendTurnDirections(int turn){
        UUID service = UUID.fromString("5F935BC0-1C05-4719-B7C5-87528F9EAF48");
        UUID characteristic = UUID.fromString("5F932429-1C05-4719-B7C5-87528F9EAF48");
        BluetoothGattCharacteristic charac = returnCharacteristic(service, characteristic);
        charac.setValue(integerToHex(turn));
        boolean status = mBluetoothGatt.writeCharacteristic(charac);
        Log.d("statuss", status + "   " + turn + " turn");
    }
    public void SendStreetName(String streetName){
        UUID service = UUID.fromString("5F935BC0-1C05-4719-B7C5-87528F9EAF48");
        UUID characteristic = UUID.fromString("5F930048-1C05-4719-B7C5-87528F9EAF48");
        BluetoothGattCharacteristic charac = returnCharacteristic(service, characteristic);
        boolean status = mBluetoothGatt.writeCharacteristic(charac);
        Log.d("statuss", status + "   " + streetName + " street");
    }
    public void SendVelocity(int avgVelocity){
        UUID service = UUID.fromString("5f935bc0-1c05-4719-b7c5-87528f9eaf48");
        UUID characteristic = UUID.fromString("5F93C1AD-1C05-4719-B7C5-87528F9EAF48");
        BluetoothGattCharacteristic charac = returnCharacteristic(service, characteristic);
        byte[] byteArray = integerToHex(avgVelocity);
        charac.setValue(integerToHex(avgVelocity));
        boolean status = mBluetoothGatt.writeCharacteristic(charac);
        Log.d("statuss", status + "   " + avgVelocity + " velocity");
    }
    public void SendArrivalTime(int arrivalTime){
        UUID service = UUID.fromString("5F935BC0-1C05-4719-B7C5-87528F9EAF48");
        UUID characteristic = UUID.fromString("5F93158D-1C05-4719-B7C5-87528F9EAF48");
        BluetoothGattCharacteristic charac = returnCharacteristic(service, characteristic);
        String hex = Integer.toHexString(arrivalTime);

        charac.setValue(integerToHex(arrivalTime));
        boolean status = mBluetoothGatt.writeCharacteristic(charac);
        Log.d("statuss", status + "   " + arrivalTime + " arrival");
    }
    public static byte[] hexStringToByteArray(String s) {
        Log.d("hex", s);
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    private byte[] integerToHex(int num){
        int origin = num;
        int one = 48;
        one = (num%10);
        byte first = turnToHex(one);
        num = (num > 0) ? num/10 : 0;

        int ten = 48;

        ten = (num%10);
        num = (num > 0) ? num/10 : 0;
        byte second = turnToHex(ten);

        int hundred = 48;

        hundred = (num%10);
        num = (num > 0) ? num/10 : 0;
        byte third = turnToHex(hundred);
        int thousand =  (num%10);
        byte fourth = turnToHex(thousand);


         Log.d("checkbyte","The Number is " + origin + " The bytes are " + thousand + " "  + hundred + " " + ten + " " + one);
         Log.d("checkbyte","The Number is " + origin + " The bytes are " + fourth + " "  + third + " " + second + " " + first);
        if (ten == 0 && hundred == 0 && thousand == 0){
            return new byte[]{48, first};
        }
        if (hundred == 0 && thousand == 0){
            return new byte[]{second, first};
        }
        if (thousand == 0){
            return new byte[]{third, second, first};
        }
        return new byte[]{fourth, third, second, first};
    }
    public static  byte[] my_int_to_bb_le(int myInteger){
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(myInteger).array();
    }

    public static int my_bb_to_int_le(byte [] byteBarray){
        return ByteBuffer.wrap(byteBarray).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static  byte[] my_int_to_bb_be(int myInteger){
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(myInteger).array();
    }

    public static int my_bb_to_int_be(byte [] byteBarray){
        return ByteBuffer.wrap(byteBarray).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    public byte turnToHex(int num){
        if (num==0){
            return 0x30;
        }
        if (num==1){
            return 0x31;
        }
        if (num==2){
            return 0x32;
        }
        if (num==3){
            return 0x33;
        }
        if (num==4){
            return 0x34;
        }
        if (num==5){
            return 0x35;
        }
        if (num==6){
            return 0x36;
        }
        if (num==7){
            return 0x37;
        }
        if (num==8){
            return 0x38;
        }
        if (num==9){
            return 0x39;
        }
        else
            return 0x30;
    }

}
