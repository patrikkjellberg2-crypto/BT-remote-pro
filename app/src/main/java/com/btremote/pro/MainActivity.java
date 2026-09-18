package com.btremote.pro;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothHidDeviceAppQosSettings;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQ_BT = 42;
    private BluetoothAdapter adapter;
    private BluetoothHidDevice hid;
    private BluetoothDevice host;
    private Spinner devicesSpinner;
    private TextView status;
    private Button connectButton;
    private final List<BluetoothDevice> bonded = new ArrayList<>();

    // HID keyboard-style consumer control report:
    // modifier, reserved, key1, key2, key3, key4, key5, key6
    private static final byte[] HID_DESC = new byte[] {
        0x05,0x0C, 0x09,0x01, (byte)0xA1,0x01,
        (byte)0x85,0x01, 0x15,0x00, 0x25,0x01,
        0x09,0xE9,0x09,0xEA,0x09,(byte)0xE2,0x09,(byte)0xB0,
        0x09,(byte)0xB5,0x09,(byte)0xB6,0x09,(byte)0xCD,
        0x09,(byte)0xB7,0x09,(byte)0xB8,0x75,0x01,0x95,0x09,
        (byte)0x81,0x02, (byte)0xC0
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(11,12,15));
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE}, REQ_BT);
        }
        buildUi();
        setupHid();
    }

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(radius); return g;
    }

    private TextView tv(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        t.setGravity(Gravity.CENTER); return t;
    }

    private Button btn(String s, int w, int h) {
        Button b = new Button(this);
        b.setText(s); b.setTextSize(20); b.setTextColor(Color.WHITE);
        b.setAllCaps(false); b.setMinHeight(0); b.setMinWidth(0);
        b.setPadding(0,0,0,0);
        b.setLayoutParams(new LinearLayout.LayoutParams(w,h));
        b.setBackground(bg(Color.rgb(31,34,42), 80));
        return b;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18,18,18,18);
        root.setBackgroundColor(Color.rgb(13,15,18));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        Button menu = btn("☰", 72, 72);
        head.addView(menu);
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setGravity(Gravity.CENTER);
        TextView title = tv("BT Remote Pro", 23, Color.WHITE);
        TextView sub = tv("Connected: Android TV", 14, Color.rgb(241,154,114));
        titleBox.addView(title); titleBox.addView(sub);
        head.addView(titleBox, new LinearLayout.LayoutParams(0,72,1));
        Button settings = btn("⚙", 72,72);
        head.addView(settings);
        root.addView(head);

        LinearLayout conn = new LinearLayout(this);
        conn.setPadding(0,12,0,8);
        devicesSpinner = new Spinner(this);
        connectButton = btn("Connect", 120, 52);
        conn.addView(devicesSpinner, new LinearLayout.LayoutParams(0,52,1));
        conn.addView(connectButton);
        root.addView(conn);

        status = tv("Bluetooth HID: starting…", 13, Color.GRAY);
        root.addView(status);

        FrameLayout remoteRow = new FrameLayout(this);
        LinearLayout.LayoutParams rr = new LinearLayout.LayoutParams(-1, 0, 1);
        remoteRow.setLayoutParams(rr);

        TextView dpad = tv("⌃\n\n   ‹        OK        ›\n\n⌄", 28, Color.LTGRAY);
        dpad.setGravity(Gravity.CENTER);
        dpad.setBackground(bg(Color.rgb(20,23,29), 300));
        FrameLayout.LayoutParams dp = new FrameLayout.LayoutParams(0,-1,1);
        dp.setMargins(0,18,12,18);
        remoteRow.addView(dpad, dp);

        LinearLayout vol = new LinearLayout(this);
        vol.setOrientation(LinearLayout.VERTICAL);
        vol.setGravity(Gravity.CENTER);
        Button plus = btn("+", 70,65);
        Button minus = btn("−",70,65);
        SeekBar seek = new SeekBar(this);
        seek.setMax(100); seek.setProgress(55);
        seek.setRotation(270);
        vol.addView(plus);
        vol.addView(seek, new LinearLayout.LayoutParams(90,190));
        vol.addView(minus);
        FrameLayout.LayoutParams vp = new FrameLayout.LayoutParams(95,-1,Gravity.RIGHT);
        vp.setMargins(0,18,0,18);
        remoteRow.addView(vol,vp);
        root.addView(remoteRow);

        LinearLayout media = new LinearLayout(this);
        media.setGravity(Gravity.CENTER); media.setWeightSum(5);
        String[] keys={"◀","|◀","▶","▶|","↶"};
        for(String k:keys){
            Button b=btn(k,0,60);
            media.addView(b,new LinearLayout.LayoutParams(0,60,1));
            if(k.equals("▶")) b.setBackground(bg(Color.rgb(241,139,101),80));
            b.setOnClickListener(v -> sendConsumer(k));
        }
        root.addView(media);

        GridLayout tools = new GridLayout(this);
        tools.setColumnCount(4); tools.setRowCount(2);
        String[] names={"◷\nSleep Timer","▣\nFull Screen","⚙\nSettings","🎙\nVoice Search",
                        "☀\nBrightness","〽\nSound Mode","↪\nInput","▦\nApps"};
        for(String n:names){
            Button b=btn(n,0,88);
            b.setTextSize(12);
            GridLayout.LayoutParams gp=new GridLayout.LayoutParams();
            gp.width=0; gp.height=88; gp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
            tools.addView(b,gp);
            b.setOnClickListener(v -> sendConsumer(n));
        }
        root.addView(tools);

        setContentView(root);
        connectButton.setOnClickListener(v -> connectSelected());
        menu.setOnClickListener(v -> toast("Meny"));
        settings.setOnClickListener(v -> toast("Inställningar"));
        dpad.setOnTouchListener((v,e)->false);
        // Separate directional hit areas are added below for reliable touch.
        addDpadTouchTargets(remoteRow, dpad);
    }

    private void addDpadTouchTargets(FrameLayout row, View dpad) {
        String[] labels={"UP","LEFT","OK","RIGHT","DOWN"};
        int[] gravity={Gravity.TOP,Gravity.CENTER_VERTICAL,Gravity.CENTER,Gravity.CENTER_VERTICAL,Gravity.BOTTOM};
        for(int i=0;i<labels.length;i++){
            Button b=btn(labels[i],80,70);
            b.setAlpha(0.01f);
            FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(100,100);
            if(i==0){p.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;}
            if(i==1){p.gravity=Gravity.CENTER_VERTICAL|Gravity.LEFT;}
            if(i==2){p.gravity=Gravity.CENTER;}
            if(i==3){p.gravity=Gravity.CENTER_VERTICAL|Gravity.RIGHT;p.rightMargin=100;}
            if(i==4){p.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;}
            row.addView(b,p);
            b.setOnClickListener(v->sendNavigation(((Button)v).getText().toString()));
        }
    }

    private void setupHid() {
        if(adapter==null){ status.setText("Bluetooth saknas på den här enheten."); return; }
        adapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if(profile==BluetoothProfile.HID_DEVICE){
                    hid=(BluetoothHidDevice)proxy;
                    BluetoothHidDeviceAppSdpSettings sdp =
                        new BluetoothHidDeviceAppSdpSettings("BT Remote Pro","Android TV Remote","BT Remote Pro",0x80,HID_DESC);
                    if (Build.VERSION.SDK_INT >= 28) {
                        hid.registerApp(sdp, null, null, Executors.newSingleThreadExecutor(), new BluetoothHidDevice.Callback(){
                            @Override public void onAppStatusChanged(BluetoothDevice d, boolean registered){
                                runOnUiThread(()->status.setText(registered ? "Bluetooth HID redo — välj Android TV och tryck Connect." : "Bluetooth HID kunde inte registreras."));
                            }
                            @Override public void onConnectionStateChanged(BluetoothDevice d,int state){
                                runOnUiThread(()->status.setText("Bluetooth: "+stateText(state)));
                            }
                        });
                    }
                    refreshBonded();
                }
            }
            @Override public void onServiceDisconnected(int profile){ hid=null; }
        }, BluetoothProfile.HID_DEVICE);
    }

    private void refreshBonded() {
        bonded.clear();
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) return;
        bonded.addAll(adapter.getBondedDevices());
        List<String> names=new ArrayList<>();
        for(BluetoothDevice d:bonded) names.add(d.getName()+"\n"+d.getAddress());
        if(names.isEmpty()) names.add("Inga parkopplade enheter — parkoppla Android TV i Bluetooth-inställningar först.");
        devicesSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));
    }

    private void connectSelected(){
        if(hid==null){toast("Bluetooth HID är inte tillgängligt på den här telefonen.");return;}
        if(bonded.isEmpty()){toast("Parkoppla Android TV först.");return;}
        int i=devicesSpinner.getSelectedItemPosition();
        if(i<0||i>=bonded.size())return;
        host=bonded.get(i);
        if (Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},REQ_BT); return;
        }
        hid.connect(host);
    }

    private String stateText(int s){
        if(s==BluetoothProfile.STATE_CONNECTED)return "ansluten";
        if(s==BluetoothProfile.STATE_CONNECTING)return "ansluter…";
        if(s==BluetoothProfile.STATE_DISCONNECTED)return "frånkopplad";
        return "ändras";
    }

    private void sendNavigation(String key){
        // Android TV consumes these standard HID consumer/navigation usages.
        switch(key){
            case "UP": sendReport((byte)0x01); break;
            case "DOWN": sendReport((byte)0x02); break;
            case "LEFT": sendReport((byte)0x04); break;
            case "RIGHT": sendReport((byte)0x08); break;
            case "OK": sendReport((byte)0x10); break;
        }
        release();
    }

    private void sendConsumer(String key){
        if(key.contains("▶")) sendReport((byte)0x08);
        else if(key.contains("◀")) sendReport((byte)0x04);
        else if(key.contains("↶")) sendReport((byte)0x10);
        release();
    }

    private void sendReport(byte usage){
        if(hid==null||host==null)return;
        try{
            // Nine 1-bit usages, packed into two bytes.
            hid.sendReport(host,1,new byte[]{usage,0});
        }catch(Exception e){ runOnUiThread(()->status.setText("Bluetooth-fel: "+e.getMessage())); }
    }
    private void release(){
        if(hid==null||host==null)return;
        try{ hid.sendReport(host,1,new byte[]{0,0}); }catch(Exception ignored){}
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
