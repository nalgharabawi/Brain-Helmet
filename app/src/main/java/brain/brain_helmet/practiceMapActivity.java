package brain.brain_helmet;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class practiceMapActivity extends AppCompatActivity {
    private static final String LOG_TAG = practiceMapActivity.class.getSimpleName();

    // permissions request code
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    private double location = 0.0;
    private double destination = 0.0;
    private boolean paused = false;

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



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
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
    }

    public void onPause() {
        if (positioningManager != null) {
            positioningManager.stop();
        }
        super.onPause();
        paused = true;
    }

    public void onDestroy() {
        if (positioningManager != null) {
            // Cleanup
            positioningManager.removeListener(
                    positionListener);
        }
        map = null;
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
                mapRoute = new MapRoute(routeResult.get(0).getRoute());
                map.addMapObject(mapRoute);
                map.setMapScheme(Map.Scheme.CARNAV_DAY_GREY);
                NavigationManager navigationManager = NavigationManager.getInstance();
                navigationManager.setMap(map);
                navigationManager.startNavigation(routeResult.get(0).getRoute());
                map.getPositionIndicator().setVisible(true);

            }
            else {
// Display a message indicating route calculation failure
            }
        }
    }
    private PositioningManager.OnPositionChangedListener positionListener = new
            PositioningManager.OnPositionChangedListener() {


                public void onPositionUpdated(PositioningManager.LocationMethod method,
                                              GeoPosition position, boolean isMapMatched) {
                    // set the center only when the app is in the foreground
                    // to reduce CPU consumption
                    if (!paused) {
                        map.setCenter(position.getCoordinate(),
                                Map.Animation.NONE);
                    }
                }

                public void onPositionFixChanged(PositioningManager.LocationMethod method,
                                                 PositioningManager.LocationStatus status) {
                }
            };

}

