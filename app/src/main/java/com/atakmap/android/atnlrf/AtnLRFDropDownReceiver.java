
package com.atakmap.android.atnlrf;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.atnlrf.plugin.AtnLRFLifecycle;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.importexport.CotEventFactory;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.coords.GeoPoint;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.atnlrf.plugin.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class AtnLRFDropDownReceiver extends DropDownReceiver implements
        OnStateListener {

    public static final String TAG = "AtnLRFDropDownReceiver";

    public static final String SHOW_PLUGIN = "com.atakmap.android.atnlrf.SHOW_PLUGIN";
    private final View templateView;
    private MapView mapView;
    private final Context pluginContext;
    private Button connect;
    private TextView heartRateTV, name;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic;
    public final BluetoothGattCallback mGattCallback;
    private BroadcastReceiver gattUpdateReceiver;
    private BluetoothLeScanner scanner;
    private static int[] data = new int[8];
    private static String[] strData = new String[8];
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String ATN_LRF_MEASUREMENT = "0000ffe1-0000-1000-8000-00805f9b34fb";

    /**************************** CONSTRUCTOR *****************************/

    public AtnLRFDropDownReceiver(final MapView mapView,
            final Context context) {
        super(mapView);
        this.pluginContext = context;
        this.mapView = mapView;

        // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
        // In this case, using it is not necessary - but I am putting it here to remind
        // developers to look at this Inflator
        templateView = PluginLayoutInflater.inflate(context,
                R.layout.main_layout, null);

        final int[] count = {0};
        this.mGattCallback = new BluetoothGattCallback() {
            @Override  // android.bluetooth.BluetoothGattCallback
            public void onCharacteristicChanged(BluetoothGatt arg2, BluetoothGattCharacteristic characteristic) {
                int flag = characteristic.getProperties();
                Log.d(TAG, String.valueOf(flag));
                final byte[] data = characteristic.getValue();
                StringBuilder sb = new StringBuilder();
                for (byte b: data) {
                    sb.append(String.format("%02X", b));
                    sb.append(" ");
                }
                // atn: 10 01 A1 FF 58 00 00 07
                Log.d(TAG, sb.toString());
                parseData(sb.toString());
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onCharacteristicRead(BluetoothGatt arg1, BluetoothGattCharacteristic characteristic, int arg3) {
                Log.d(TAG, "onCharacteristicRead " + arg3);
                if(arg3 == 0) {
                }
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onCharacteristicWrite(BluetoothGatt arg2, BluetoothGattCharacteristic arg3, int arg4) {
            }

            @SuppressLint("ResourceAsColor")
            @Override  // android.bluetooth.BluetoothGattCallback
            public void onConnectionStateChange(BluetoothGatt arg5, int arg6, int arg7) {
                Log.d(TAG, "onConnectionStateChange status: " + arg6);
                Log.d(TAG, "onConnectionStateChange newState: " + arg7);
                if(arg7 == 0) {
                    Log.i(TAG, "Disconnected from GATT server.");
                    AtnLRFLifecycle.getActivity().runOnUiThread(() -> {
                        connect.setBackgroundResource(R.color.red);
                        connect.setText("*NOT* Connected");
                        connect.setVisibility(View.VISIBLE);
                    });
                }

                if(arg7 == 2) {
                    AtnLRFLifecycle.getActivity().runOnUiThread(() -> {
                        connect.setBackgroundResource(R.color.green);
                        connect.setText("Connected");
                        connect.setVisibility(View.VISIBLE);
                    });

                    mBluetoothGatt.discoverServices();
                }

                super.onConnectionStateChange(arg5, arg6, arg7);
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onDescriptorRead(BluetoothGatt arg1, BluetoothGattDescriptor arg2, int arg3) {
                super.onDescriptorRead(arg1, arg2, arg3);
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onDescriptorWrite(BluetoothGatt arg3, BluetoothGattDescriptor arg4, int arg5) {
                Intent i = new Intent("com.atakmap.app.civ.ATNLRF");
                mapView.getContext().sendBroadcast(i);
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onMtuChanged(BluetoothGatt arg2, int arg3, int arg4) {
                super.onMtuChanged(arg2, arg3, arg4);
                if(arg4 == 0) {
                    boolean v2 = mBluetoothGatt.discoverServices();
                    Log.i(TAG, "5.0 attempting to start service discovery: " + v2);
                    return;
                }

                Log.i(TAG, "Failed to set MTU.");
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onReadRemoteRssi(BluetoothGatt arg2, int arg3, int arg4) {
                Log.i(TAG, "rssi = " + arg3);
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onReliableWriteCompleted(BluetoothGatt arg1, int arg2) {
                super.onReliableWriteCompleted(arg1, arg2);
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onServicesDiscovered(BluetoothGatt arg3, int arg4) {
                Log.w(TAG, "onServicesDiscovered received: " + arg4);

                List<BluetoothGattService> s = mBluetoothGatt.getServices();
                if (s == null) {
                    mBluetoothGatt.disconnect();
                    return;
                }

                for (BluetoothGattService bgs: s) {
                    for (BluetoothGattCharacteristic bgc: bgs.getCharacteristics()) {
                        Log.d(TAG, bgc.getUuid().toString());
                        if (bgc.getUuid().toString().equals(ATN_LRF_MEASUREMENT)) {
                            mBluetoothGattCharacteristic = bgc;
                            mBluetoothGatt.setCharacteristicNotification(mBluetoothGattCharacteristic, true);
                            BluetoothGattDescriptor d = mBluetoothGattCharacteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                            d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            mBluetoothGatt.writeDescriptor(d);
                        }
                    }
                }


            }
        };

        gattUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action.equals("com.atakmap.app.civ.ATNLRF")) {
                    mBluetoothGatt.readCharacteristic(mBluetoothGattCharacteristic);
                }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction("com.atakmap.app.civ.READ_HEART_RATE");
        mapView.getContext().registerReceiver(gattUpdateReceiver, f);

        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.d(TAG, result.getDevice().getName());
                if (result != null && result.getDevice() != null && result.getDevice().getName() != null && result.getDevice().getName().startsWith("ATN")) {
                    mBluetoothGatt = result.getDevice().connectGatt(mapView.getContext(), true, mGattCallback);
                    mBluetoothGatt.connect();
                }

            }
        };

        connect = templateView.findViewById(R.id.connect);
        connect.setOnClickListener(v -> {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            scanner = bluetoothAdapter.getBluetoothLeScanner();
            scanner.startScan(scanCallback);
        });


    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
        getMapView().getContext().unregisterReceiver(gattUpdateReceiver);
    }

    private static String flipBinBits(String arg4) {
        android.util.Log.i(TAG, ">> original bin: " + arg4);
        StringBuilder v0 = new StringBuilder();
        int v1;
        for(v1 = 0; v1 < arg4.length(); ++v1) {
            v0.append(((char)(arg4.charAt(v1) == 0x30 ? 49 : 0x30)));
        }

        String v4 = v0.toString();
        android.util.Log.i(TAG, ">> inverted bin: " + v4);
        return v4;
    }
    private static String hexToBin(String arg2) {
        return String.format("%8s", new BigInteger(arg2, 16).toString(2)).replace(" ", "0");
    }

    public void parseData(String arg12) {
        float v12_1;
        int v1 = 0;
        while(arg12.length() > 0) {
            android.util.Log.i(TAG, "space_index: " + 2);
            String v2 = arg12.substring(0, 2);
            arg12 = arg12.substring(3);
            android.util.Log.i(TAG, "k: " + v1 + ", value: " + v2 + ", result: " + arg12);
            if(v2.length() <= 0) {
                continue;
            }

            data[v1] = Integer.parseInt(v2.trim(), 16);
            strData[v1] = v2;
            android.util.Log.w(TAG, "data[" + v1 + "]: " + data[v1]);
            ++v1;
        }

        float v12 = (float)Math.round(((double)Integer.parseInt(strData[3] + strData[4].trim(), 16)) * 0.5);
        android.util.Log.w(TAG, "distance: " + v12);
        double actualDistance = v12 >= 5.0f && v12 <= 1500.0f ? ((double) v12) : -1.0;
        if(String.valueOf(((char)strData[5].charAt(0))).equals("0")) {
            v12_1 = (float)Math.round(((double)Integer.parseInt(strData[5] + strData[6].trim(), 16)) * 0.1);
            android.util.Log.e(TAG, "--> hex : " + strData[5] + strData[6].trim());
            android.util.Log.e(TAG, "--> parseInt 16: " + Integer.parseInt(strData[5] + strData[6].trim(), 16));
        }
        else {
            String v12_2 = hexToBin(strData[5] + strData[6]);
            android.util.Log.e(TAG, "--> bin16 : " + v12_2 + " / dec: " + Integer.parseInt(v12_2, 2));
            String v12_3 = flipBinBits(v12_2);
            int v1_1 = Integer.parseInt(v12_3, 2) + 1;
            android.util.Log.e(TAG, "--> bin16 inverted: " + v12_3 + " / dec (+ 1): " + v1_1);
            v12_1 = (float)(Math.round(((double)v1_1) * 0.1) * -1L);
        }

        android.util.Log.w(TAG, "angle: " + v12_1);
        if(v12_1 >= -90.0f && v12_1 <= 90.0f) {
            double pitchAngle = (double) v12_1;
        }

        double bearing = mapView.getSelfMarker().getTrackHeading();
        android.util.Log.i(TAG, String.format("Bearing: %f", bearing));


        CotEvent cotEvent = new CotEvent();

        CoordinatedTime time = new CoordinatedTime();
        cotEvent.setTime(time);
        cotEvent.setStart(time);
        cotEvent.setStale(time.addMinutes(90));

        cotEvent.setUID(UUID.randomUUID().toString());

        cotEvent.setType("u-rb-a");

        cotEvent.setHow("m-g");

        CotPoint cotPoint = new CotPoint(mapView.getSelfMarker().getPoint().getLatitude(), mapView.getSelfMarker().getPoint().getLongitude(), CotPoint.UNKNOWN,
                CotPoint.UNKNOWN, CotPoint.UNKNOWN);
        cotEvent.setPoint(cotPoint);

        CotDetail cotDetail = new CotDetail("detail");
        cotEvent.setDetail(cotDetail);

        CotDetail cotRemark = new CotDetail("remarks");
        cotRemark.setAttribute("source", "ATN LRF Plugin");
        cotRemark.setInnerText(String.format(Locale.US, "Distance: %f\nAngle: %f\nBearing: %f", v12, v12_1, bearing));

        CotDetail range = new CotDetail("range");
        range.setAttribute("value", String.valueOf(v12));
        cotDetail.addChild(range);

        CotDetail bearingCot = new CotDetail("bearing");
        bearingCot.setAttribute("value", String.valueOf(bearing));
        cotDetail.addChild(bearingCot);

        CotDetail rangeUnits = new CotDetail("rangeUnits");
        rangeUnits.setAttribute("value", String.valueOf(1));
        cotDetail.addChild(rangeUnits);

        CotDetail bearingUnits = new CotDetail("bearingUnits");
        rangeUnits.setAttribute("value", String.valueOf(0));
        cotDetail.addChild(bearingUnits);

        CotDetail northRef = new CotDetail("northRef");
        rangeUnits.setAttribute("value", String.valueOf(1));
        cotDetail.addChild(northRef);

        CotDetail color = new CotDetail("color");
        rangeUnits.setAttribute("value", String.valueOf(-65535));
        cotDetail.addChild(color);

        CotDetail strokeWeight = new CotDetail("strokeWeight");
        rangeUnits.setAttribute("value", String.valueOf(3.0));
        cotDetail.addChild(strokeWeight);

        CotDetail strokeStyle = new CotDetail("strokeStyle");
        rangeUnits.setAttribute("value", "1");
        cotDetail.addChild(strokeStyle);

        cotDetail.addChild(cotRemark);

        if (cotEvent.isValid())
            CotMapComponent.getInternalDispatcher().dispatch(cotEvent);
        else
            android.util.Log.e(TAG, "cotEvent was not valid");

        String v12_4 = hexToBin(strData[2].trim());
        android.util.Log.w(TAG, "Data descriptor hex: " + strData[2].trim());
        android.util.Log.w(TAG, "Data descriptor bin: " + v12_4);
        int dataUnit = Integer.parseInt(String.valueOf(((char) v12_4.charAt(6))) + ((char) v12_4.charAt(7)));
        android.util.Log.i(TAG, "0-1......" + ((char)v12_4.charAt(6)) + "- - -" + ((char)v12_4.charAt(7)));
        android.util.Log.w(TAG, "0-1......" + (v12_4.charAt(6) + v12_4.charAt(7)));
        int v12_5 = dataUnit;

        if(v12_5 == 0) {
            android.util.Log.i(TAG, "--> METRIC (METERS)" + dataUnit);
            return;
        }

        if(v12_5 == 1) {
            android.util.Log.i(TAG, "--> STANDARD (YARDS)" + dataUnit);
            return;
        }

        android.util.Log.i(TAG, "--> Unknown data unit value " + dataUnit);




    }
    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(SHOW_PLUGIN)) {

            Log.d(TAG, "showing plugin drop down");
            showDropDown(templateView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false, this);
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

}
