package brain.brain_helmet;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    private final String TAG = "MainActivity";
    Button connectBluetooth;
    Button navigation;
    SharedPreferences prefs = null;
    private BluetoothLeService mBluetoothLeService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectBluetooth = (Button)findViewById(R.id.connectBluetooth);
        navigation = (Button) findViewById(R.id.navigation);
        connectBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent nextActivity = new Intent(MainActivity.this,ConnectBluetoothActivity.class);
                startActivity(nextActivity);


            }
        });
        navigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent nextActivity = new Intent(MainActivity.this,practiceMapActivity.class);
                startActivity(nextActivity);
            }
        });
        prefs = getPreference();
    }

    private SharedPreferences getPreference(){
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return  prefs;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

            Log.d(TAG, mBluetoothLeService +  " sir");
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            Log.d(TAG, mBluetoothLeService +  " sir");
            String mDeviceAddress = getPreferencesString(PreferenceValuesEnum.SAVEADDRESS.name());
            String mDeviceName =  getPreferencesString(PreferenceValuesEnum.SAVENAME.name());

            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
        @Override
        public void onResume(){
            if (getPreferencesString(PreferenceValuesEnum.SAVEBLUETOOTH.name()).equals(PreferenceValuesEnum.SAVEBLUETOOTH.name())) {
                Intent i = new Intent(MainActivity.this, BluetoothLeService.class);
                bindService(i, mServiceConnection, BIND_AUTO_CREATE);
            }
            super.onResume();
}

    private String getPreferencesString(String get){
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        //   prefs = getPreference();
        if (get.equals(PreferenceValuesEnum.SAVENAME.name())) {
            return prefs.getString(get, "");
        }
        if (get.equals(PreferenceValuesEnum.SAVEADDRESS.name())) {
            return prefs.getString(get, "");
        }
        if (get.equals(PreferenceValuesEnum.SAVEBLUETOOTH.name())) {
            return prefs.getString(get, "");
        }
        return null;
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        //  unbindService(mServiceConnection);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //    unbindService(mServiceConnection);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //  safePreferences();
        Log.i(TAG, "On destroy");
        if (mBluetoothLeService!=null)
            unbindService(mServiceConnection);
    }
}
