package brain.brain_helmet;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class MyApp extends Application {

    public MyApp() {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        // this method fires once as well as constructor
        // but also application has context here

        Log.i("MyAppActivity", "onCreate fired");


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();

        // final Intent intent = getIntent();
        editor.putString(PreferenceValuesEnum.SAVENAME.name(), "").apply();
        editor.putString(PreferenceValuesEnum.SAVEADDRESS.name(), "").apply();
        editor.putString(PreferenceValuesEnum.SAVEBLUETOOTH.name(),"").apply();
        Log.i("MyAppActivity", "result: "+ PreferenceValuesEnum.SAVEBLUETOOTH.name());


    }
}