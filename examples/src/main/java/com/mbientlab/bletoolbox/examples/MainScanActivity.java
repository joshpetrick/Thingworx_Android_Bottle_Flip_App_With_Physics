/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 */

package com.mbientlab.bletoolbox.examples;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainScanActivity extends AppCompatActivity {
    public static final String EXTRA_BLE_DEVICE= "com.mbientlab.bletoolbox.examples.MainScanActivity.EXTRA_BLE_DEVICE";
    private final static int REQUEST_ENABLE_BT= 0, SCAN_DEVICE=1;
    public static final String DEVICE = "Device";

    private static BluetoothDevice device;
    private Boolean resumed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_activity);

        BluetoothAdapter btAdapter= ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        if (btAdapter == null) {
            new AlertDialog.Builder(this).setTitle(R.string.error_title)
                    .setMessage(R.string.error_no_bluetooth)
                    .setCancelable(false)
                    .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MainScanActivity.this.finish();
                        }
                    })
                    .create()
                    .show();
        } else if (!btAdapter.isEnabled()) {
            final Intent enableIntent= new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_CANCELED) {
                    finish();
                }
                break;
            case SCAN_DEVICE:
                if (data != null) {
                    device = data.getParcelableExtra(MainScanActivity.EXTRA_BLE_DEVICE);
                    Toast.makeText(this, "Device selected: " + device.getAddress(), Toast.LENGTH_LONG).show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startBleScanActivity(View v) {
        Intent bleScanIntent= new Intent(this, ScannerActivity.class);
        startActivityForResult(bleScanIntent, SCAN_DEVICE);

    }

    public static BluetoothDevice getBT(){
        return device;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(resumed) {
            resumed = false;
            Intent scanIntent = new Intent();
            scanIntent.putExtra(DEVICE, device);
            setResult(1, scanIntent);
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = true;
    }
}
