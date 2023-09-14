package com.atakmap.android.atnlrf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import com.atakmap.android.compassring.CompassRingMapComponent;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.location.LocationMapComponent;
import com.atakmap.android.mapcompass.CompassArrowMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.math.BigInteger;
import java.util.Locale;
import java.util.UUID;


public class Receiver extends BroadcastReceiver {

    private static String TAG = "AtnBallistics";
    private static int[] data = new int[8];
    private static String[] strData = new String[8];

    private MapView mapView;

    private Location currentLocation;

    public Receiver() {

    }
    public Receiver(MapView mapView) {
        this.mapView = mapView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("com.example.bluetooth.le.ACTION_BUFFER_COMPLETE")) {
            String s = intent.getStringExtra("com.example.bluetooth.le.EXTRA_DATA").split("\n")[1];
            parseData(s);
        }


    }

    private static String flipBinBits(String arg4) {
        Log.i(TAG, ">> original bin: " + arg4);
        StringBuilder v0 = new StringBuilder();
        int v1;
        for(v1 = 0; v1 < arg4.length(); ++v1) {
            v0.append(((char)(arg4.charAt(v1) == 0x30 ? 49 : 0x30)));
        }

        String v4 = v0.toString();
        Log.i(TAG, ">> inverted bin: " + v4);
        return v4;
    }
    private static String hexToBin(String arg2) {
        return String.format("%8s", new BigInteger(arg2, 16).toString(2)).replace(" ", "0");
    }

    public void parseData(String arg12) {
        float v12_1;
        int v1 = 0;
        while(arg12.length() > 0) {
            Log.i(TAG, "space_index: " + 2);
            String v2 = arg12.substring(0, 2);
            arg12 = arg12.substring(3);
            Log.i(TAG, "k: " + v1 + ", value: " + v2 + ", result: " + arg12);
            if(v2.length() <= 0) {
                continue;
            }

            data[v1] = Integer.parseInt(v2.trim(), 16);
            strData[v1] = v2;
            Log.w(TAG, "data[" + v1 + "]: " + data[v1]);
            ++v1;
        }

        float v12 = (float)Math.round(((double)Integer.parseInt(strData[3] + strData[4].trim(), 16)) * 0.5);
        Log.w(TAG, "distance: " + v12);
        double actualDistance = v12 >= 5.0f && v12 <= 1500.0f ? ((double) v12) : -1.0;
        if(String.valueOf(((char)strData[5].charAt(0))).equals("0")) {
            v12_1 = (float)Math.round(((double)Integer.parseInt(strData[5] + strData[6].trim(), 16)) * 0.1);
            Log.e(TAG, "--> hex : " + strData[5] + strData[6].trim());
            Log.e(TAG, "--> parseInt 16: " + Integer.parseInt(strData[5] + strData[6].trim(), 16));
        }
        else {
            String v12_2 = hexToBin(strData[5] + strData[6]);
            Log.e(TAG, "--> bin16 : " + v12_2 + " / dec: " + Integer.parseInt(v12_2, 2));
            String v12_3 = flipBinBits(v12_2);
            int v1_1 = Integer.parseInt(v12_3, 2) + 1;
            Log.e(TAG, "--> bin16 inverted: " + v12_3 + " / dec (+ 1): " + v1_1);
            v12_1 = (float)(Math.round(((double)v1_1) * 0.1) * -1L);
        }

        Log.w(TAG, "angle: " + v12_1);
        if(v12_1 >= -90.0f && v12_1 <= 90.0f) {
            double pitchAngle = (double) v12_1;
        }

        double bearing = mapView.getSelfMarker().getTrackHeading();
        Log.i(TAG, String.format("Bearing: %f", bearing));


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
        rangeUnits.setAttribute("value", "solid");
        cotDetail.addChild(strokeStyle);

        cotDetail.addChild(cotRemark);

        if (cotEvent.isValid())
            CotMapComponent.getInternalDispatcher().dispatch(cotEvent);
        else
            Log.e(TAG, "cotEvent was not valid");

        String v12_4 = hexToBin(strData[2].trim());
        Log.w(TAG, "Data descriptor hex: " + strData[2].trim());
        Log.w(TAG, "Data descriptor bin: " + v12_4);
        int dataUnit = Integer.parseInt(String.valueOf(((char) v12_4.charAt(6))) + ((char) v12_4.charAt(7)));
        Log.i(TAG, "0-1......" + ((char)v12_4.charAt(6)) + "- - -" + ((char)v12_4.charAt(7)));
        Log.w(TAG, "0-1......" + (v12_4.charAt(6) + v12_4.charAt(7)));
        int v12_5 = dataUnit;

        if(v12_5 == 0) {
            Log.i(TAG, "--> METRIC (METERS)" + dataUnit);
            return;
        }

        if(v12_5 == 1) {
            Log.i(TAG, "--> STANDARD (YARDS)" + dataUnit);
            return;
        }

        Log.i(TAG, "--> Unknown data unit value " + dataUnit);




    }
}