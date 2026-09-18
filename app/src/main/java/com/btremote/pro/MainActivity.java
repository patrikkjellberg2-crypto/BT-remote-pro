package com.btremote.pro;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothProfile;
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
    private final List<BluetoothDevice> bonded = new ArrayList<>();

    private static final int BG = Color.rgb(9,10,13);
    private static final int PANEL = Color.rgb(23,25,31);
    private static final int PANEL2 = Color.rgb(31,34,42);
    private static final int TEXT = Color.rgb(244,242,239);
    private static final int MUTED = Color.rgb(160,156,154);
    private static final int ACCENT = Color.rgb(229,154,115);

    private static final byte[] HID_DESC = new byte[] {
        0x05,0x0C,0x09,0x01,(byte)0xA1,0x01,(byte)0x85,0x01,
        0x15,0x00,0x25,0x01,0x09,(byte)0xE9,0x09,(byte)0xEA,
        0x09,(byte)0xE2,0x09,(byte)0xB0,0x09,(byte)0xB5,0x09,(byte)0xB6,
        0x09,(byte)0xCD,0x09,(byte)0xB7,0x09,(byte)0xB8,0x09,(byte)0xB9,
        0x09,(byte)0xBA,0x09,(byte)0xBB,0x75,0x01,(byte)0x95,0x0C,
        (byte)0x81,0x02,(byte)0xC0
    };

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        adapter = BluetoothAdapter.getDefaultAdapter();
        buildUi();
        if (adapter == null) { setStatus("Bluetooth is not available"); return; }
        requestBluetoothPermissions();
        if (Build.VERSION.SDK_INT < 31 ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) startBluetooth();
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= 31 &&
            (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
             checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE}, REQ_BT);
        }
    }

    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g) {
        super.onRequestPermissionsResult(r,p,g);
        if (r == REQ_BT && (Build.VERSION.SDK_INT < 31 ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)) startBluetooth();
        else if (r == REQ_BT) setStatus("Bluetooth permission is required");
    }

    private GradientDrawable bg(int color,float radius) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }

    private TextView text(String s,float size,int color) {
        TextView t=new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER); return t;
    }

    private Button button(String s,int w,int h) {
        Button b=new Button(this);
        b.setText(s); b.setTextSize(16); b.setTextColor(TEXT); b.setAllCaps(false);
        b.setMinWidth(0); b.setMinHeight(0); b.setPadding(0,0,0,0);
        b.setBackground(bg(PANEL2,28));
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(w),dp(h)));
        return b;
    }

    private LinearLayout row() {
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER);
        return l;
    }

    private LinearLayout col() {
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL); l.setGravity(Gravity.CENTER);
        return l;
    }

    private void margin(View v,int l,int t,int r,int b) {
        if(v.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)v.getLayoutParams();
            p.setMargins(dp(l),dp(t),dp(r),dp(b)); v.setLayoutParams(p);
        }
    }

    private void buildUi() {
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root=col();
        root.setPadding(dp(16),dp(12),dp(16),dp(20));
        root.setGravity(Gravity.TOP);

        // Header
        LinearLayout header=row();
        Button menu=button("☰",46,46);
        Button settings=button("⚙",46,46);
        LinearLayout titles=col();
        TextView title=text("BT REMOTE PRO",19,TEXT);
        title.setTypeface(null,1);
        TextView sub=text("Connected: Android TV",12,ACCENT);
        titles.addView(title,new LinearLayout.LayoutParams(-1,dp(27)));
        titles.addView(sub,new LinearLayout.LayoutParams(-1,dp(21)));
        LinearLayout.LayoutParams titleP=new LinearLayout.LayoutParams(0,dp(50),1);
        titleP.setMargins(dp(8),0,dp(8),0);
        header.addView(menu);
        header.addView(titles,titleP);
        header.addView(settings);
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(56)));

        // Device selector
        LinearLayout device=row();
        device.setPadding(dp(12),0,dp(8),0);
        device.setBackground(bg(PANEL,20));
        devicesSpinner=new Spinner(this);
        device.addView(devicesSpinner,new LinearLayout.LayoutParams(0,dp(50),1));
        Button connect=button("CONN",72,42);
        device.addView(connect);
        LinearLayout.LayoutParams deviceP=new LinearLayout.LayoutParams(-1,dp(58));
        deviceP.setMargins(0,dp(8),0,0);
        root.addView(device,deviceP);

        status=text("Connect to Android TV first",12,MUTED);
        status.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        root.addView(status,new LinearLayout.LayoutParams(-1,dp(30)));

        // Main remote panel
        LinearLayout remote=row();
        remote.setGravity(Gravity.CENTER);
        remote.setPadding(0,dp(8),0,dp(8));

        FrameLayout dpad=new FrameLayout(this);
        dpad.setBackground(bg(PANEL,150));
        dpad.setElevation(dp(5));
        int size=250;

        Button up=button("⌃",70,62), down=button("⌄",70,62);
        Button left=button("‹",62,70), right=button("›",62,70);
        Button ok=button("OK",88,88);
        ok.setTextSize(18); ok.setTypeface(null,1);
        ok.setBackground(bg(Color.rgb(26,28,34),50));

        FrameLayout.LayoutParams u=new FrameLayout.LayoutParams(dp(70),dp(62),Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        u.topMargin=dp(22);
        FrameLayout.LayoutParams d=new FrameLayout.LayoutParams(dp(70),dp(62),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        d.bottomMargin=dp(22);
        FrameLayout.LayoutParams l=new FrameLayout.LayoutParams(dp(62),dp(70),Gravity.CENTER_VERTICAL|Gravity.LEFT);
        l.leftMargin=dp(22);
        FrameLayout.LayoutParams rr=new FrameLayout.LayoutParams(dp(62),dp(70),Gravity.CENTER_VERTICAL|Gravity.RIGHT);
        rr.rightMargin=dp(22);
        FrameLayout.LayoutParams o=new FrameLayout.LayoutParams(dp(88),dp(88),Gravity.CENTER);
        dpad.addView(up,u); dpad.addView(down,d); dpad.addView(left,l); dpad.addView(right,rr); dpad.addView(ok,o);

        LinearLayout.LayoutParams dpadP=new LinearLayout.LayoutParams(dp(size),dp(size));
        dpadP.setMargins(0,0,dp(12),0);
        remote.addView(dpad,dpadP);

        LinearLayout volume=col();
        volume.setPadding(dp(8),dp(10),dp(8),dp(10));
        volume.setBackground(bg(PANEL,30));
        Button plus=button("+",58,50), minus=button("−",58,50);
        SeekBar seek=new SeekBar(this);
        seek.setMax(100); seek.setProgress(55); seek.setRotation(270);
        volume.addView(plus);
        LinearLayout.LayoutParams seekP=new LinearLayout.LayoutParams(dp(58),dp(150));
        seekP.setMargins(0,dp(6),0,dp(2));
        volume.addView(seek,seekP);
        volume.addView(text("VOL",10,MUTED),new LinearLayout.LayoutParams(-1,dp(20)));
        volume.addView(minus);
        LinearLayout.LayoutParams volP=new LinearLayout.LayoutParams(dp(78),dp(250));
        remote.addView(volume,volP);

        root.addView(remote,new LinearLayout.LayoutParams(-1,dp(286)));

        // Media controls
        LinearLayout media=row();
        Button prev=button("◀",48,48), rewind=button("|◀",48,48);
        Button play=button("▶",58,58), next=button("▶|",48,48), back=button("↶",48,48);
        play.setTextSize(22); play.setBackground(bg(ACCENT,50));
        for(Button b:new Button[]{prev,rewind,play,next,back}) media.addView(b,new LinearLayout.LayoutParams(0,dp(b==play?58:48),1));
        root.addView(media,new LinearLayout.LayoutParams(-1,dp(70)));

        View divider=new View(this); divider.setBackgroundColor(Color.rgb(40,42,48));
        LinearLayout.LayoutParams divP=new LinearLayout.LayoutParams(-1,dp(1));
        divP.setMargins(0,dp(4),0,dp(10)); root.addView(divider,divP);

        // Tools
        LinearLayout tools=col();
        String[] names={"◷\nSleep Timer","▣\nFull Screen","⚙\nSettings","♩\nVoice Search",
                        "☀\nBrightness","〽\nSound Mode","↗\nInput","▦\nApps"};
        for(int r=0;r<2;r++){
            LinearLayout tr=row();
            for(int c=0;c<4;c++){
                final String name=names[r*4+c];
                Button tb=button(name.split("\\n")[0],50,50);
                TextView lab=text(name.split("\\n")[1],10,MUTED);
                LinearLayout cell=col();
                cell.addView(tb); cell.addView(lab,new LinearLayout.LayoutParams(-1,dp(24)));
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(82),1);
                cp.setMargins(dp(3),0,dp(3),0); tr.addView(cell,cp);
                tb.setOnClickListener(v->Toast.makeText(this,name.replace("\\n"," • "),Toast.LENGTH_SHORT).show());
            }
            tools.addView(tr,new LinearLayout.LayoutParams(-1,dp(82)));
        }
        root.addView(tools);

        TextView footer=text("Bluetooth Remote • Android TV HID",10,Color.rgb(100,96,96));
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(30));
        fp.setMargins(0,dp(6),0,0); root.addView(footer,fp);

        scroll.addView(root);
        setContentView(scroll);

        up.setOnClickListener(v->sendConsumer(0x001));
        down.setOnClickListener(v->sendConsumer(0x002));
        left.setOnClickListener(v->sendConsumer(0x004));
        right.setOnClickListener(v->sendConsumer(0x008));
        ok.setOnClickListener(v->sendConsumer(0x010));
        plus.setOnClickListener(v->sendConsumer(0x020));
        minus.setOnClickListener(v->sendConsumer(0x040));
        prev.setOnClickListener(v->sendConsumer(0x200));
        rewind.setOnClickListener(v->sendConsumer(0x080));
        play.setOnClickListener(v->sendConsumer(0x400));
        next.setOnClickListener(v->sendConsumer(0x800));
        back.setOnClickListener(v->sendConsumer(0x100));
        connect.setOnClickListener(v->connectSelectedDevice());
        menu.setOnClickListener(v->Toast.makeText(this,"BT Remote Pro",Toast.LENGTH_SHORT).show());
        settings.setOnClickListener(v->Toast.makeText(this,"Android Bluetooth settings",Toast.LENGTH_SHORT).show());
    }

    private void setStatus(String s) { if(status!=null) status.setText(s); }

    private void startBluetooth() { loadBondedDevices(); setupHid(); }

    private void loadBondedDevices() {
        if(adapter==null || (Build.VERSION.SDK_INT>=31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)) return;
        bonded.clear();
        try {
            bonded.addAll(adapter.getBondedDevices());
            List<String> names=new ArrayList<>();
            for(BluetoothDevice d:bonded) {
                String n=null; try { n=d.getName(); } catch(SecurityException ignored) {}
                names.add(n==null||n.isEmpty()?d.getAddress():n);
            }
            if(names.isEmpty()) names.add("No paired devices");
            devicesSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
        } catch(SecurityException e) { setStatus("Bluetooth permission required"); }
    }

    private void setupHid() {
        if(adapter==null || Build.VERSION.SDK_INT<28) { setStatus("Bluetooth HID requires Android 9+"); return; }
        try {
            adapter.getProfileProxy(this,new BluetoothProfile.ServiceListener() {
                @Override public void onServiceConnected(int profile,BluetoothProfile proxy) {
                    if(profile==BluetoothProfile.HID_DEVICE) { hid=(BluetoothHidDevice)proxy; registerHidApp(); }
                }
                @Override public void onServiceDisconnected(int profile) {
                    if(profile==BluetoothProfile.HID_DEVICE) { hid=null; host=null; setStatus("Bluetooth HID disconnected"); }
                }
            },BluetoothProfile.HID_DEVICE);
        } catch(SecurityException e) { setStatus("Bluetooth permission required"); }
    }

    private void registerHidApp() {
        if(hid==null) return;
        try {
            BluetoothHidDeviceAppSdpSettings s=new BluetoothHidDeviceAppSdpSettings(
                "BT Remote Pro","Android TV Remote","BT Remote Pro",(byte)0x80,HID_DESC);
            hid.registerApp(s,null,null,Executors.newSingleThreadExecutor(),new BluetoothHidDevice.Callback() {
                @Override public void onAppStatusChanged(BluetoothDevice d,boolean registered) {
                    runOnUiThread(()->setStatus(registered?"Bluetooth HID • ready":"Bluetooth HID • not registered"));
                }
                @Override public void onConnectionStateChanged(BluetoothDevice d,int state) {
                    runOnUiThread(()->{
                        if(state==BluetoothProfile.STATE_CONNECTED){host=d;setStatus("Connected • Android TV");}
                        else if(state==BluetoothProfile.STATE_DISCONNECTED){if(host==d)host=null;setStatus("Android TV • disconnected");}
                    });
                }
            });
        } catch(SecurityException e) { setStatus("Bluetooth permission required"); }
    }

    private void connectSelectedDevice() {
        if(hid==null){setStatus("Bluetooth HID • not ready");return;}
        if(bonded.isEmpty()){setStatus("Pair Android TV in Android Bluetooth settings first");return;}
        int pos=devicesSpinner.getSelectedItemPosition();
        if(pos<0||pos>=bonded.size()){setStatus("Select a Bluetooth device");return;}
        try {
            BluetoothDevice d=bonded.get(pos);
            boolean ok=hid.connect(d);
            if(ok){host=d;setStatus("Connecting • Android TV");}
            else setStatus("Connection request failed");
        } catch(SecurityException e){setStatus("Bluetooth permission required");}
    }

    private void sendConsumer(int mask) {
        if(hid==null||host==null){setStatus("Connect to Android TV first");return;}
        try {
            hid.sendReport(host,1,new byte[]{(byte)(mask&255),(byte)((mask>>8)&255)});
            hid.sendReport(host,1,new byte[]{0,0});
        } catch(SecurityException e){setStatus("Bluetooth permission required");}
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if(adapter!=null&&hid!=null){try{adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE,hid);}catch(SecurityException ignored){}}
        hid=null; host=null;
    }
}
