package brain.brain_helmet;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.guidance.VoiceCatalog;
import com.here.android.mpa.guidance.VoicePackage;
import com.here.android.mpa.guidance.VoiceSkin;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Maneuver;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class practiceMapActivity extends AppCompatActivity {
    private static final String TAG = "practice";

    // permissions request code
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    SharedPreferences prefs = null;
    private double location = 0.0;
    private double destination = 0.0;
    private boolean paused = false;
    private GeoBoundingBox m_geoBoundingBox;
    private VoiceCatalog voiceCatalog = null;
    private BluetoothLeService mBluetoothLeService;
    private List<Maneuver> turns = null;

    /**
     * Permissions that need to be explicitly requested from end user.
     */
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    // map embedded in the map fragment
    private Map map = null;

    // map fragment embedded in this activity
    private MapFragment mapFragment = null;

    MapRoute mapRoute = null;

    PositioningManager positioningManager = null;
    NavigationManager navigationManager = null;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
        prefs = getPreference();
        if (getPreferencesString(PreferenceValuesEnum.SAVEBLUETOOTH.name()).equals(PreferenceValuesEnum.SAVEBLUETOOTH.name())) {
            Intent i = new Intent(practiceMapActivity.this, BluetoothLeService.class);
            bindService(i, mServiceConnection, BIND_AUTO_CREATE);
        }
        //Log.v(TAG, "TestTest");
    }

    private SharedPreferences getPreference(){
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return  prefs;
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

    private void initialize() {
        setContentView(R.layout.activity_practice_map);

        // Search for the map fragment to finish setup by calling init().
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapfragment);
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();
                    // Set the map center to the Vancouver region (no animation)
                    map.setCenter(new GeoCoordinate(28.3722884, -81.4004225, 0.0),
                            Map.Animation.NONE);
                    // Set the zoom level to the average between min and max
                    map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
                    positioningManager = PositioningManager.getInstance();
                    positioningManager.addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(positionListener));
                    positioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK);
                    navigationManager = NavigationManager.getInstance();
                } else {
                    System.out.println("ERROR: Cannot initialize Map Fragment");
                }
            }
        });
    }

    public void onResume() {
        super.onResume();
        paused = false;
        if (positioningManager != null) {
            positioningManager.start(
                    PositioningManager.LocationMethod.GPS_NETWORK);
        }
        if (getPreferencesString(PreferenceValuesEnum.SAVEBLUETOOTH.name()).equals(PreferenceValuesEnum.SAVEBLUETOOTH.name())) {
            Intent i = new Intent(practiceMapActivity.this, BluetoothLeService.class);
            bindService(i, mServiceConnection, BIND_AUTO_CREATE);
        }
    }

    public void onPause() {
        if (positioningManager != null) {
            //positioningManager.stop();
        }
        super.onPause();
        //paused = true;
    }

    public void onDestroy() {
        if (positioningManager != null) {
            // Cleanup
            positioningManager.removeListener(
                    positionListener);
        }
        map = null;
        if (mBluetoothLeService!=null)
            unbindService(mServiceConnection);
        super.onDestroy();
    }

    /**
     * Checks the dynamically controlled permissions and requests missing permissions from end user.
     */
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                initialize();
                break;
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_location:
                this.getLocation();
                break;
            case R.id.menu_destination:
                this.getDestination();
                break;
            case R.id.menu_route:
                CoreRouter rm = new CoreRouter();
                RoutePlan routePlan = new RoutePlan();
            routePlan.addWaypoint(new RouteWaypoint(positioningManager.getLastKnownPosition().getCoordinate()));
            routePlan.addWaypoint(new RouteWaypoint(new GeoCoordinate(28.3811339, -81.3819982)));
//                //routePlan.addWaypoint(new GeoCoordinate(49.1947289, -123.1762924));
//                //routePlan.addWaypoint(new GeoCoordinate(49.1947289, -123.1762924));
            RouteOptions routeOptions = new RouteOptions();
            routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
            routeOptions.setRouteType(RouteOptions.Type.FASTEST);
            routeOptions.setRouteCount(1);
            routePlan.setRouteOptions(routeOptions);
            rm.calculateRoute(routePlan, new RouteListener());
            map.setCenter(new GeoCoordinate(28.3722884, -81.4004225,0.0), Map.Animation.NONE);
            map.setZoomLevel(map.getMaxZoomLevel());
            break;
            default:
                break;
        }
        return false;
    }


    public void getLocation() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.common_location, null);
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText input1 = (EditText) textEntryView.findViewById(R.id.location);
        alert.setIcon(R.drawable.dialog_background_light).setTitle("Write your location").setView(textEntryView).setPositiveButton("Okay",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        location = Double.parseDouble(input1.getText().toString());
                    }


                }).setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
     /*
     * User clicked cancel so do some stuff
     */
                    }
                });

        alert.show();
    }

    public void getDestination() {
        LayoutInflater factory = LayoutInflater.from(this);

        final View textEntryView = factory.inflate(R.layout.common_dest, null);
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText input1 = (EditText) textEntryView.findViewById(R.id.destination);
        alert.setIcon(R.drawable.dialog_background_light).setTitle("Write your destination").setView(textEntryView).setPositiveButton("Okay",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        destination = Double.parseDouble(input1.getText().toString());
                    }


                }).setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
     /*
     * User clicked cancel so do some stuff
     */
                    }
                });

        alert.show();
    }


    private class RouteListener implements CoreRouter.Listener {
        // Method defined in Listener
        public void onProgress(int percentage) {
// Display a message indicating calculation progress
        }
        // Method defined in Listener
        public void onCalculateRouteFinished(List<RouteResult> routeResult, RoutingError error) {
// If the route was calculated successfully
            if (error == RoutingError.NONE) {
// Render the route on the map
                Route route = routeResult.get(0).getRoute();
                mapRoute = new MapRoute(routeResult.get(0).getRoute());
                mapRoute.setManeuverNumberVisible(true);
                map.addMapObject(mapRoute);
                m_geoBoundingBox = route.getBoundingBox();
                map.zoomTo(m_geoBoundingBox, Map.Animation.NONE,
                        Map.MOVE_PRESERVE_ORIENTATION);

                //map.setMapScheme(Map.Scheme.CARNAV_DAY_GREY);
                turns = route.getManeuvers();
//                for (int i = 0; i<turns.size(); i++){
//                    Log.v(TAG, "We want to turn : " + turns.get(i).getTurn() + " on road " + turns.get(i).getNextRoadName() + " The distance is " + turns.get(i).getDistanceToNextManeuver() + " The value of the turn is " + turns.get(i).getTurn().value() + " The estimated time is: " + navigationManager.getEta(true, Route.TrafficPenaltyMode.OPTIMAL));
//                }
                map.getPositionIndicator().setVisible(true);
                navigationManager.setMap(map);
                updateInstalledVoices();
                downloadCatalogAndSkin();
                //setupVoice();
                navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW);
                //navigationManager.setRealisticViewMode(NavigationManager.RealisticViewMode.DAY);
                m_geoBoundingBox = route.getBoundingBox();
                navigationManager.getEta(false, Route.TrafficPenaltyMode.OPTIMAL);
                navigationManager.startNavigation(route);

                //Log.v(TAG, " The ETA is: " + navigationManager.getTta(Route.TrafficPenaltyMode.OPTIMAL, false) + " " + route.getTta(Route.TrafficPenaltyMode.OPTIMAL,0));

            }
            else {
// Display a message indicating route calculation failure
            }
        }
    }
    private void updateInstalledVoices() {

        // First get the voice catalog from the backend that contains all available languages (so called voiceskins) for download
        VoiceCatalog.getInstance().downloadCatalog(new VoiceCatalog.OnDownloadDoneListener() {
            @Override
            public void onDownloadDone(VoiceCatalog.Error error) {
                if (error != VoiceCatalog.Error.NONE) {
                    Toast.makeText(getApplicationContext(), "Failed to download catalog", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Catalog downloaded", Toast.LENGTH_LONG).show();

                    boolean result = false;
                    List<VoicePackage> packages = VoiceCatalog.getInstance().getCatalogList();
                    List<VoiceSkin> local = VoiceCatalog.getInstance().getLocalVoiceSkins();

                    // if successful, check for updated version in catalog compared to local installed ones
                    for (VoiceSkin voice : local) {
                        for (VoicePackage pkg : packages) {
                            if (voice.getId() == pkg.getId() && !voice.getVersion().equals(pkg.getVersion())) {
                                Toast.makeText(getApplicationContext(), "New version detected....downloading", Toast.LENGTH_LONG).show();
                                downloadVoice(voice.getId());
                                result = true;
                            }
                        }
                    }

                    if (!result)
                        Toast.makeText(getApplicationContext(), "No updates found", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void downloadCatalogAndSkin() {
        // First get the voice catalog from the backend that contains all available languages (so called voiceskins) for download
        VoiceCatalog.getInstance().downloadCatalog(new VoiceCatalog.OnDownloadDoneListener() {
            @Override
            public void onDownloadDone(VoiceCatalog.Error error) {
                if (error != VoiceCatalog.Error.NONE) {
                    Toast.makeText(getApplicationContext(), "Failed to download catalog", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Catalog downloaded", Toast.LENGTH_LONG).show();

                    // If catalog was successfully downloaded, you can iterate over it / show it to the user / select a skin for download
                    List<VoicePackage> packages = VoiceCatalog.getInstance().getCatalogList();
                    Log.i(TAG, "# of available packages: " + packages.size());

                    // debug print of the voice skins that are available
                    for (VoicePackage lang : packages)
                        Log.d(TAG, "Language name: " + lang.getLocalizedLanguage() + " is TTS: " + lang.isTts() + " ID: " + lang.getId());

                    // Return list of already installed voices on device
                    List<VoiceSkin> localInstalledSkins = VoiceCatalog.getInstance().getLocalVoiceSkins();

                    // debug print of the already locally installed skins
                    Log.d(TAG, "# of local skins: " + localInstalledSkins.size());
//                    for (VoiceSkin voice : localInstalledSkins) {
//                        Log.d(TAG, "ID: " + voice.getId() + " Language: " + voice.getLanguage());
//                    }

                    downloadVoice(206);
                }
            }
        });
    }

    private void downloadVoice(final long skin_id) {
        // kick off the download for a voice skin from the backend
        VoiceCatalog.getInstance().downloadVoice(skin_id, new VoiceCatalog.OnDownloadDoneListener() {
            @Override
            public void onDownloadDone(VoiceCatalog.Error error) {
                if (error != VoiceCatalog.Error.NONE) {
                    Toast.makeText(getApplicationContext(), "Failed downloading voice skin", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Voice skin downloaded and activated", Toast.LENGTH_LONG).show();
                    // set output for Nuance TTS
                    NavigationManager.getInstance().setTtsOutputFormat(NavigationManager.TtsOutputFormat.NUANCE);
                    // set usage of downloaded voice
                    NavigationManager.getInstance().setVoiceSkin(VoiceCatalog.getInstance().getLocalVoiceSkin(skin_id));
                }
            }
        });
    }
    private void setupVoice() {
        // Retrieve the VoiceCatalog and download the latest updates
        VoiceCatalog voiceCatalog = VoiceCatalog.getInstance();

        if (!voiceCatalog.isLocalCatalogAvailable()) {
            //if (DEBUG) Log.d(TAG, "Voice catalog is not available in local storage.");
            //Toast.makeText(mActivity.getApplicationContext(), "Voice catalog is not available in local storage.", Toast.LENGTH_LONG).show();

            voiceCatalog.downloadCatalog(new VoiceCatalog.OnDownloadDoneListener() {
                @Override
                public void onDownloadDone(VoiceCatalog.Error error) {
                    if (error == VoiceCatalog.Error.NONE) {
                        // catalog download successful
                        //if (DEBUG) Log.d(TAG, "Download voice catalog successfully.");

                        //Toast.makeText(mActivity.getApplicationContext(), "Voice catalog download successful.", Toast.LENGTH_LONG).show();
                    } else {
                        //if (DEBUG) Log.d(TAG, "Download voice catalog failed.");

                        //Toast.makeText(mActivity.getApplicationContext(), "Voice catalog download error.", Toast.LENGTH_LONG).show();
                    }

                    // Get the list of voice packages from the voice catalog list
                    List<VoicePackage> voicePackages =
                            VoiceCatalog.getInstance().getCatalogList();
                    if (voicePackages.size() == 0) {
                       //if (DEBUG) Log.d(TAG, "Voice catalog size is 0.");

                        //Toast.makeText(mActivity.getApplicationContext(), "Voice catalog size is 0.", Toast.LENGTH_LONG).show();
                    }

                    long id = -1;
                    // select
                    for (VoicePackage voicePackage : voicePackages) {
                        if (voicePackage.getMarcCode().compareToIgnoreCase("eng") == 0) {
                            //if (voicePackage.isTts()) // TODO: need to figure out why always return false
                            {
                                id = voicePackage.getId();
                                break;
                            }
                        }
                    }

                    if (!VoiceCatalog.getInstance().isLocalVoiceSkin(id)) {
                        final long finalId = id;
                        VoiceCatalog.getInstance().downloadVoice(id, new VoiceCatalog.OnDownloadDoneListener() {
                            @Override
                            public void onDownloadDone(VoiceCatalog.Error error) {
                                if (error == VoiceCatalog.Error.NONE) {
                                    //voice skin download successful
                                    //if (Log.d(TAG, "Download voice skin successfully.");

                                    //Toast.makeText(mActivity.getApplicationContext(), "Voice skin download successful.", Toast.LENGTH_LONG).show();

                                    // set the voice skin for use by navigation manager
                                    if (VoiceCatalog.getInstance().getLocalVoiceSkin(finalId) != null) {
                                        navigationManager.setVoiceSkin(VoiceCatalog.getInstance().getLocalVoiceSkin(finalId));
                                    } else {
                                        //if  Log.d(TAG, "Get local voice skin error.");

                                        //Toast.makeText(mActivity.getApplicationContext(), "Navi manager set voice skin error.", Toast.LENGTH_LONG).show();
                                    }

                                } else {
                                    //if (Log.d(TAG, "Download voice skin failed.");
                                    //Toast.makeText(mActivity.getApplicationContext(), "Voice skin download error.", Toast.LENGTH_LONG).show();
                                }

                            }
                        });
                    } else {
                        // set the voice skin for use by navigation manager
                        if (VoiceCatalog.getInstance().getLocalVoiceSkin(id) != null) {
                            navigationManager.setVoiceSkin(VoiceCatalog.getInstance().getLocalVoiceSkin(id));
                        } else {
                            //Toast.makeText(mActivity.getApplicationContext(), "Navi manager set voice skin error.", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
        }
    }

    private NavigationManager.NewInstructionEventListener instructListener
            = new NavigationManager.NewInstructionEventListener() {

        @Override
        public void onNewInstructionEvent() {
            // Interpret and present the Maneuver object as it contains
            // turn by turn navigation instructions for the user.
            navigationManager.getNextManeuver();
        }
    };

    private PositioningManager.OnPositionChangedListener positionListener = new
            PositioningManager.OnPositionChangedListener() {


                public void onPositionUpdated(PositioningManager.LocationMethod method,
                                              GeoPosition position, boolean isMapMatched) {
                    // set the center only when the app is in the foreground
                    // to reduce CPU consumption
                    if (!paused) {
                        map.setCenter(position.getCoordinate(),
                                Map.Animation.NONE);
                        position.getCoordinate();
                        position.getHeading();
                        position.getSpeed();
                        //navigationManager.getTta(Route.TrafficPenaltyMode.DISABLED, true);
                        //navigationManager.getDestinationDistance();
                        Date destinateTime = navigationManager.getEta(true, Route.TrafficPenaltyMode.OPTIMAL);
                        Date currentTime = Calendar.getInstance().getTime();
                        long diff = destinateTime.getTime() - currentTime.getTime();
                        long minutes = diff / 60000;
                        long hours = minutes / 60;
                        if (mBluetoothLeService != null && navigationManager.getNextManeuver() != null) {

                            mBluetoothLeService.SendArrivalTime((int) minutes);
                            mBluetoothLeService.SendDistance((int) navigationManager.getDestinationDistance());
                            mBluetoothLeService.SendStreetName(navigationManager.getNextManeuver().getNextRoadName());
                            mBluetoothLeService.SendTurnDirections(navigationManager.getNextManeuver().getTurn().value());
                            mBluetoothLeService.SendVelocity((int) navigationManager.getAverageSpeed());
                        }
                        //Log.v(TAG,  (int) minutes + "  " +  (int)navigationManager.getDestinationDistance() + "  " + navigationManager.getNextManeuver().getNextRoadName() + " " + navigationManager.getNextManeuver().getTurn().value() + "  " + (int)navigationManager.getAverageSpeed());
                        //Log.v(TAG, " The ETA is: " + navigationManager.getEta(true, Route.TrafficPenaltyMode.OPTIMAL));
                        Log.v(TAG, "Running while paused...");
                        //// navigationManager.repeatVoiceCommand();
                    }
                }

                public void onPositionFixChanged(PositioningManager.LocationMethod method,
                                                 PositioningManager.LocationStatus status) {
                }
            };

}

