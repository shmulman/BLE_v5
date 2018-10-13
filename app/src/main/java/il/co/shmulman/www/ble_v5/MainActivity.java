package il.co.shmulman.www.ble_v5;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // GUI Variables
    Button startScanButton, stopScanButton, connectBLE, sendData;
    TextView realTimeScan, devicesView, connectionView;

    // BLE Variables
    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    BluetoothGatt mBluetoothGatt;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";

    // Data processing Variables
    ArrayList<String> listOfDevices = new ArrayList<>();
    int i_scan = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // GUI items initiation Buttons and TextViews
        startScanButton = findViewById(R.id.StartScanButton);
        stopScanButton = findViewById(R.id.StopScanButton);
        connectBLE = findViewById(R.id.ConnectBLE);
        sendData = findViewById(R.id.SendData);
        realTimeScan = findViewById(R.id.RealTimeScan);
        devicesView = findViewById(R.id.DevicesView);
        devicesView.setMovementMethod(new ScrollingMovementMethod());
        connectionView = findViewById(R.id.ConnectionView);
        connectionView.setMovementMethod(new ScrollingMovementMethod());

        // BLE items initiation
        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }
        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        // Button click definitions
        startScanButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                realTimeScan.setText("startScanButton\n");
                startScanning();
            }
        });

        stopScanButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                realTimeScan.setText("stopScanButton\n");
                stopScanning();
            }
        });

        connectBLE.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                devicesView.append("connectBLE\n");
            }
        });

        sendData.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                devicesView.append("sendData\n");
            }
        });
    }

    public void startScanning() {
        System.out.println("Start scanning\n");
        realTimeScan.setText("Start scanning\n");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning() {
        System.out.println("Stopping scanning\n");
        realTimeScan.setText("Stopped scanning\n");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            String deviceName = result.getDevice().getName();

            // Real time data
            realTimeScan.setText("Real time scan -> Device Name: " + deviceName + " rssi: " + result.getRssi() + "\n");

            // Data management
            if (!listOfDevices.contains(deviceName)) {
                listOfDevices.add(deviceName);
                devicesView.append(listOfDevices.get(i_scan) + "\n");

                // Find the right device which is more then 10 characters and starts with "easysense"
                if(deviceName!=null) {
                    if (deviceName.length()>9){
                        if (deviceName.substring(0, 9).equals("easysense")) {
                            devicesView.append("Connecting to: " + deviceName + "\n");
                            connectToDeviceSelected(result.getDevice());
                        }
                    } // String length is more than ...
                } // device not null

                i_scan++;
            } // if the name of the device is already exist

            // auto scroll for text view
            final int scrollAmount = devicesView.getLayout().getLineTop(devicesView.getLineCount()) - devicesView.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0)
                devicesView.scrollTo(0, scrollAmount);
        }
    };

    public void connectToDeviceSelected(BluetoothDevice ble_device) {
        connectionView.append("Trying to connect to device: " + ble_device.getName() + "\n");
        connectionView.append("String (MAC): " + ble_device.toString() + "\n");
        ble_device.createBond(); // Needed ?????????????????????????????????????????????????????
        //int deviceSelected = Integer.parseInt(deviceIndexInput.getText().toString());
        //bluetoothGatt = devicesDiscovered.get(deviceSelected).connectGatt(this, false, btleGattCallback);

        mBluetoothGatt = ble_device.connectGatt(this, false, btleGattCallback);
    }

    // Device connect call back
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    connectionView.append("device read or wrote to\n");
                    connectionView.append("BluetoothGattCallback initiated:\n");
                }
            });
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            // this will get called when a device connects or disconnects
            System.out.println(newState);
            switch (newState) {
                case 0:
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            connectionView.append("device disconnected\n");
                        }
                    });
                    break;
                case 2:
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            connectionView.append("device connected\n");
                        }
                    });

                    // discover services and characteristics for this device
                    mBluetoothGatt.discoverServices();

                    break;
                default:
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            connectionView.append("we encountered an unknown state, uh oh\n");
                        }
                    });
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a            BluetoothGatt.discoverServices() call
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    connectionView.append("device services have been discovered\n");
                }
            });
            displayGattServices(mBluetoothGatt.getServices());
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
    };

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {

        System.out.println(characteristic.getUuid());
        connectionView.append("broadcastUpdate characteristic.getUuid(): " + characteristic.getUuid() +"\n");
    }


    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {

            final String uuid = gattService.getUuid().toString();
            System.out.println("Service discovered: " + uuid);
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    connectionView.append("Service discovered: "+uuid+"\n");
                }
            });
            new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic :
                    gattCharacteristics) {

                final String charUuid = gattCharacteristic.getUuid().toString();
                System.out.println("Characteristic discovered for service: " + charUuid);
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        connectionView.append("Characteristic discovered for service: "+charUuid+"\n");
                    }
                });

            }
        }
    }
}