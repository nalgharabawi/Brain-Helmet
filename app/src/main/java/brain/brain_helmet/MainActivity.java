package brain.brain_helmet;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    Button connectBluetooth;
    Button navigation;

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

    }


}
