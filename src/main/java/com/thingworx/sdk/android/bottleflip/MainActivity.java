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

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.BarometerBosch;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.sdk.android.activity.PreferenceActivity;
import com.thingworx.sdk.android.activity.ThingworxActivity;

import java.util.ArrayList;
import java.util.List;

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
    public static final int POLLING_RATE = 250;
    private BtleService.LocalBinder serviceBinder;
    private final String TAG = MainActivity.class.getName();
    private MetaWearBoard board;

    private BluetoothManager btManager;
    private BottleFlipRemoteThing bottle;

    private CheckBox checkBoxConnected;
    private CheckBox sensorCheckBox;


    private MetaWearBoard retrieveBoard(String macAddr)
    {
        BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(macAddr);

        // Create a MetaWear board object for the Bluetooth Device
        return serviceBinder.getMetaWearBoard(remoteDevice);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        if(btManager == null)
        {
            btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        }
        // Build User Interface
        setContentView(R.layout.activity_main);
        setTitle("Bottle Flip Remote App");

        checkBoxConnected = (CheckBox) findViewById(R.id.checkBoxConnected);
        sensorCheckBox = (CheckBox) findViewById(R.id.sensorCheckBox);

        // Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, Context.BIND_AUTO_CREATE);

        // Create your Virtual Thing and bind it to your android controls
        try {

            // If you don't have preferences, display the dialog to get them.
            if (!hasConnectionPreferences()) {
                // Show Preferences Activity
                connectionState = ConnectionState.DISCONNECTED;
                Intent i = new Intent(this, PreferenceActivity.class);
                startActivityForResult(i, 1);
                return;
            }

            final String identifier = sharedPrefs.getString("prefRemoteIdentifier", "");

            bottle = new BottleFlipRemoteThing("BottleRemoteThing_BottleFlip_PTC", "Bottle Sensor Remote Thing", identifier, client);
            // You only need to do this once, no matter how many things your add
            startProcessScanRequestThread(POLLING_RATE, connected -> runOnUiThread(() -> checkBoxConnected.setChecked(connected)));

            connect(new VirtualThing[]{bottle});


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
        if(getConnectionState() == ConnectionState.DISCONNECTED)
        {
            try
            {
                if(bottle == null)
                {
                    final String identifier = sharedPrefs.getString("prefRemoteIdentifier", "");

                    bottle = new BottleFlipRemoteThing("BottleRemoteThing_BottleFlip_PTC", "Bottle Sensor Remote Thing", identifier, client);
                }
                connect(new VirtualThing[]{bottle});
                connectBTLE();
            } catch (Exception e)
            {
                Log.e(TAG, "Restart with new settings failed.", e);
            }
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

        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);

        //add board disconnect

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

    /**
     *     Bluetooth connect to the board, and on success, initialize all modules.
     */

    public void connectBTLE()
    {
        final String macAddr = sharedPrefs.getString("prefMacAddress", "");
        board = retrieveBoard(macAddr);
        //attempt to connect
        if(board != null)
        {
            board.connectAsync().onSuccessTask(new Continuation<Void, Task<Void>>()
            {
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
                public Object then(Task<Void> task) throws Exception
                {
                    if(!task.isCancelled()) {
                        //connected successfully

 //                       GyroBmi160 gyro = board.getModule(GyroBmi160.class);
                        Accelerometer accelerometer = board.getModule(Accelerometer.class);
//                        BarometerBosch barometer = board.getModule(BarometerBosch.class);
 //                       MagnetometerBmm150 magnetometer = board.getModule(MagnetometerBmm150.class);
                        List<MetaWearBoard.Module> moduleList = new ArrayList<MetaWearBoard.Module>();
                        moduleList.add(accelerometer);
//                        moduleList.add(barometer);
//                        moduleList.add(gyro);
//                        moduleList.add(magnetometer);


                        //probs should pass in all modules (board)
                        bottle.addBoard_InitiModules(board, moduleList);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                sensorCheckBox.setChecked(board != null);
                            }
                        });
                    }
                    return null;
                }
            });

            board.onUnexpectedDisconnect(status -> Log.i("MainActivity", "Unexpectedly lost connection: " + status));
        }
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
