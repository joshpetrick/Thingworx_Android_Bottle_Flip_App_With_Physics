package com.thingworx.sdk.android.bottleflip;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.BarometerBosch;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.communications.client.things.VirtualThingPropertyChangeEvent;
import com.thingworx.communications.client.things.VirtualThingPropertyChangeListener;
import com.thingworx.sdk.android.activity.PreferenceActivity;
import com.thingworx.sdk.android.activity.ThingworxActivity;

import bolts.Continuation;
import bolts.Task;


/**
 * This class creates an Activity that manages one or more VirtualThings. It gets all of its
 * ThingWorx specific features from its base class and focuses on the creation of your
 * virtual things, in this case, a single Steam Sensor and the creation of the UI. It also binds
 * the values of the Steam Sensor VirtualThing directly to the controls in this activity. It also
 * provides a generic settings UI to configure the ThingWorx client connection.
 */
public class MainActivity extends ThingworxActivity implements ServiceConnection {

    private final static String logTag = MainActivity.class.getSimpleName();
    public final static class TOGGLE{
        static final boolean ON = true;
        static final boolean OFF = false;
    };

    public static final int POLLING_RATE = 250;
    private BtleService.LocalBinder serviceBinder;

    public Accelerometer accelerometer;
    public GyroBmi160 gyro;
    public BarometerBosch barometer;
    public MagnetometerBmm150 magnometer;
    public Led led;



    private final String TAG = MainActivity.class.getName();
    private MetaWearBoard board;

    private BottleFlipRemoteThing bottle1;

    private CheckBox checkBoxConnected;
    private CheckBox sensorCheckBox;


    private void retrieveBoard(String macAddr) {
        final BluetoothManager btManager=
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothDevice remoteDevice=
                btManager.getAdapter().getRemoteDevice(macAddr);

        // Create a MetaWear board object for the Bluetooth Device
        board= serviceBinder.getMetaWearBoard(remoteDevice);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");


        // Build User Interface
        setContentView(R.layout.activity_main);
        setTitle("Android Steam Thing");

        checkBoxConnected = (CheckBox) findViewById(R.id.checkBoxConnected);
        sensorCheckBox = (CheckBox) findViewById(R.id.sensorCheckBox);

        // Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, Context.BIND_AUTO_CREATE);

        // Create your Virtual Thing and bind it to your android controls
        try {


            bottle1 = new BottleFlipRemoteThing("BottleRemoteThing_BottleFlip_PTC", "Bottle Sensor Remote Thing", "Bottle2", client);

            /* Adding a property change listener to your VirtualThing is a convenient way to directly
             * bind your android controls to property values. They will get updated
             * as soon as they are changed, either on the server or locally                       */
            bottle1.addPropertyChangeListener(new VirtualThingPropertyChangeListener() {
                @Override
                public void propertyChangeEventReceived(final VirtualThingPropertyChangeEvent evt) {
                    final String propertyName = evt.getProperty().getPropertyDefinition().getName();
                    runOnUiThread(new Runnable() { // Always update your controls on the UI thread
                        @Override
                        public void run() {

                        }
                    });
                }
            });

            // If you don't have preferences, display the dialog to get them.
            if (!hasConnectionPreferences()) {
                // Show Preferences Activity
                connectionState = ConnectionState.DISCONNECTED;
                Intent i = new Intent(this, PreferenceActivity.class);
                startActivityForResult(i, 1);
                return;
            }

            // You only need to do this once, no matter how many things your add
            startProcessScanRequestThread(POLLING_RATE, new ConnectionStateObserver() {
                @Override
                public void onConnectionStateChanged(final boolean connected) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            checkBoxConnected.setChecked(connected);
                        }
                    });
                }
            });

            connect(new VirtualThing[]{bottle1});


        } catch (Exception e) {
            Log.e(TAG, "Failed to initalize with error.", e);
            onConnectionFailed("Failed to initalize with error : " + e.getMessage());
        }

    }

    /**
     * Resume will be called each time this activity becomes active.
     * Check your connection state and try to establish a connection.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume() called.");
        if(getConnectionState() == ConnectionState.DISCONNECTED) {
            try {
                connect(new VirtualThing[]{bottle1});

                //check if already connected
                if (!board.isConnected()){
                    connectBTLE();
                }
                else
                {
                    //show connection in app or something?
                    toggleModules(TOGGLE.ON);
                }
            } catch (Exception e) {
                Log.e(TAG, "Restart with new settings failed.", e);
            }
        }
    }

    private void toggleModules(boolean toggle) {


        if(toggle)
        {
            if (gyro != null) {
                gyro.configure()
                        .odr(GyroBmi160.OutputDataRate.ODR_25_HZ)
                        .range(GyroBmi160.Range.values()[0])
                        .commit();
                gyro.angularVelocity().addRouteAsync(source -> source.stream((data, env) -> {
                    final AngularVelocity value = data.value(AngularVelocity.class);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //rawData.setText(value.toString());
                        }
                    });
                })).continueWith(new Continuation<Route, Object>() {
                    @Override
                    public Object then(Task<Route> task) throws Exception {
                        System.out.println("Starting Gyro");

                        gyro.angularVelocity().start();
                        gyro.start();

                        return null;
                    }
                });

            }
            if (accelerometer != null) {
                accelerometer.configure().odr(25f).commit();
                accelerometer.acceleration().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                Acceleration tempD = data.value(Acceleration.class);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //dataText.setText(tempD.toString());
                                    }
                                });

                            }
                        });
                    }
                }).continueWith(new Continuation<Route, Object>() {

                    @Override
                    public Object then(Task<Route> task) throws Exception {

                        System.out.println("Starting Accelerometer");
                        accelerometer.acceleration().start();
                        accelerometer.start();
                        return null;
                    }
                });
            }

            if (barometer != null) {
                barometer.configure()
                        .pressureOversampling(BarometerBosch.OversamplingMode.ULTRA_HIGH)
                        .filterCoeff(BarometerBosch.FilterCoeff.OFF)
                        .standbyTime(0.5f)
                        .commit();
                barometer.altitude().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                System.out.println("Getting Data");
                                Float tempD = data.value(Float.class);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //textView2.setText("Altitude (m) "+String.valueOf(tempD));
                                    }
                                });
                            }
                        });
                    }
                }).continueWith(new Continuation<Route, Object>() {

                    @Override
                    public Object then(Task<Route> task) throws Exception {

                        barometer.altitude().start();
                        barometer.start();


                        System.out.println("Starting Barometer");
                        return null;
                    }
                });
            }
        }
        else
        {
            if(gyro != null) {
                gyro.stop();
                gyro.angularVelocity().stop();
                System.out.println("Stopping Gyro");
            }

            if(accelerometer != null) {
                accelerometer.stop();
                accelerometer.acceleration().stop();
                System.out.println("Stopping Accelerometer");
            }

            if(barometer != null) {
                barometer.stop();
                barometer.altitude().stop();
                System.out.println("Stopping Barometer");
            }

            board.tearDown();
        }
    }

    /**
     * This function will be called from the base class to allow you to set
     * values on your virtual thing that are not configured in your aspect defaults or to perform
     * any other UI changes in response to becoming connected to the server.
     */
    @Override
    protected void onConnectionEstablished() {
        super.onConnectionEstablished();

     }

    /**** Support for Settings Menu ****/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_settings was selected
            case R.id.action_settings:
                disconnect();
                Intent i = new Intent(this, PreferenceActivity.class);
                startActivityForResult(i, 1);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        toggleModules(TOGGLE.OFF);
        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);

    }
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // Typecast the binder to the service's LocalBinder class
        serviceBinder = (BtleService.LocalBinder) service;
        System.out.println(name.toString()+" Service Connected");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        System.out.println(name.toString()+" Service Disconnected");
    }

    public void connectBTLE()
    {
        final String macAddr = sharedPrefs.getString("prefMacAddress", "");
        retrieveBoard(macAddr);
        //attempt to connect
        sensorCheckBox.setChecked(board != null);
        if(board != null)
        {

            board.connectAsync().continueWithTask(new Continuation<Void, Task<Void>>() {
                @Override
                public Task<Void> then(Task<Void> task) throws Exception {
                    System.out.println("Board: "+board.isConnected());
                    if (task.isCancelled()) {
                        System.out.println("Board Task Failed: "+board.isConnected());
                        return task;

                    }
                    return task.isFaulted() ? reconnect(board) : task;
                }
            }).continueWith(new Continuation<Void, Object>() {


                @Override
                public Object then(Task<Void> task) throws Exception {
                    if(!task.isCancelled())
                    {
                        System.out.println("Board: "+board.isConnected());
                        System.out.println("Board is connected?");

                        gyro=board.getModule(GyroBmi160.class);
                        accelerometer=board.getModule(Accelerometer.class);
                        barometer=board.getModule(BarometerBosch.class);
                        magnometer=board.getModule(MagnetometerBmm150.class);
                        led=board.getModule(Led.class);


                        bottle1.createAccelerometerStream(accelerometer);

                    }
                    return null;
                }
            });


        }
        //in main loop read data and update textfield or syso at min
    }

    public static Task<Void> reconnect(final MetaWearBoard board) {
        return board.connectAsync().continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {
                return task.isFaulted() ? reconnect(board) : task;
            }
        });
    }
}
