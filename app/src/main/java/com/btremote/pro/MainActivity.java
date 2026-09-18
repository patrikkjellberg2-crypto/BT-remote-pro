package com.btremote.pro;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothProfile;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
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

    private static final int BG = Color.rgb(9, 11, 15);
    private static final int PANEL = Color.rgb(18, 21, 27);
    private static final int BUTTON = Color.rgb(27, 31, 39);
    private static final int TEXT = Color.rgb(244, 244, 246);
    private static final int MUTED = Color.rgb(142, 148, 158);
    private static final int ACCENT = Color.rgb(245, 132, 94);

    private static final byte[] HID_DESC = new byte[] {
        0x05,0x0C, 0x09,0x01, (byte)0xA1,0x01,
        (byte)0x85,0x01, 0x15,0x00, 0x25,0x01,
        0x09,(byte)0xE9, 0x09,(byte)0xEA, 0x09,(byte)0xE2, 0x09,(byte)0xB0,
        0x09,(byte)0xB5, 0x09,(byte)0xB6, 0x09,(byte)0xCD, 0x09,(byte)0xB7,
        0x09,(byte)0xB8, 0x09,(byte)0xB9, 0x09,(byte)0xBA, 0x09,(byte)0xBB,
        0x75,0x01, (byte)0x95,0x0C, (byte)0x81,0x02, (byte)0xC0
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        adapter = BluetoothAdapter.getDefaultAdapter();
        buildUi();
        if (adapter == null) {
            setStatus("Bluetooth is not available");
            return;
        }
        requestBluetoothPermissions();
        if (Build.VERSION.SDK_INT < 31 ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            startBluetooth();
        }
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= 31 &&
            (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
             checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE}, REQ_BT);
        }
    }

    @Override public void onRequestPermissionsResult(int r, String[] p, int[] g) {
        super.onRequestPermissionsResult(r,p,g);
        if (r == REQ_BT && (Build.VERSION.SDK_INT < 31 ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)) {
            startBluetooth();
        } else if (r == REQ_BT) {
            setStatus("Bluetooth permission is required");
        }
    }

    private GradientDrawable rounded(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView label(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private TextView labelLeft(String s, float size, int color) {
        TextView t = label(s,size,color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private Button action(String text, int w, int h) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setMinWidth(0); b.setMinHeight(0);
        b.setPadding(0,0,0,0);
        b.setBackground(rounded(BUTTON, 32));
        b.setLayoutParams(new LinearLayout.LayoutParams(w,h));
        return b;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 16, 18, 16);
        root.setBackgroundColor(BG);

        LinearLayout header = row();
        Button menu = action("☰", 48, 48);
        header.addView(menu);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(Gravity.CENTER);
        TextView title = label("BT REMOTE PRO", 20, TEXT);
        TextView subtitle = label("ANDROID TV REMOTE", 10, ACCENT);
        titles.addView(title);
        titles.addView(subtitle);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,56,1);
        tp.setMargins(8,0,8,0);
        header.addView(titles,tp);

        Button settings = action("⚙", 48, 48);
        header.addView(settings);
        root.addView(header);

        LinearLayout card = row();
        card.setPadding(14, 8, 8, 8);
        card.setBackground(rounded(PANEL, 22));
        devicesSpinner = new Spinner(this);
        card.addView(devicesSpinner, new LinearLayout.LayoutParams(0,52,1));
        Button connect = action("CONNECT", 105, 44);
        card.addView(connect);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,68);
        cp.setMargins(0,14,0,6);
        root.addView(card,cp);

        status = labelLeft("Bluetooth HID • waiting for permission", 12, MUTED);
        root.addView(status, new LinearLayout.LayoutParams(-1,28));

        FrameLayout remote = new FrameLayout(this);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1,0,1);
        rp.setMargins(0,8,0,8);

        LinearLayout padPanel = new LinearLayout(this);
        padPanel.setOrientation(LinearLayout.VERTICAL);
        padPanel.setGravity(Gravity.CENTER);
        padPanel.setPadding(10,18,10,18);
        padPanel.setBackground(rounded(PANEL, 30));

        LinearLayout padMid = row();
        Button up = action("▲",78,62);
        Button down = action("▼",78,62);
        Button left = action("◀",78,62);
        Button right = action("▶",78,62);
        Button ok = action("OK",82,66);
        LinearLayout top = row(); top.addView(up);
        LinearLayout.LayoutParams centerWrap = new LinearLayout.LayoutParams(-2,-2);
        centerWrap.gravity=Gravity.CENTER;
        padPanel.addView(top,centerWrap);
        padMid.addView(left); padMid.addView(ok); padMid.addView(right);
        padPanel.addView(padMid,centerWrap);
        LinearLayout bottom = row(); bottom.addView(down);
        padPanel.addView(bottom,centerWrap);

        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(0,-1,1);
        pp.setMargins(0,0,10,0);
        remote.addView(padPanel,pp);

        LinearLayout volume = new LinearLayout(this);
        volume.setOrientation(LinearLayout.VERTICAL);
        volume.setGravity(Gravity.CENTER);
        volume.setPadding(8,18,8,18);
        volume.setBackground(rounded(PANEL,30));
        Button volUp = action("+",64,58);
        Button volDown = action("−",64,58);
        TextView volText = label("VOL",10,MUTED);
        SeekBar seek = new SeekBar(this);
        seek.setMax(100); seek.setProgress(55);
        seek.setRotation(270);
        volume.addView(volUp);
        volume.addView(seek,new LinearLayout.LayoutParams(64,150));
        volume.addView(volText);
        volume.addView(volDown);
        FrameLayout.LayoutParams vp = new FrameLayout.LayoutParams(82,-1);
        vp.gravity=Gravity.RIGHT;
        remote.addView(volume,vp);

        root.addView(remote,rp);

        LinearLayout media = row();
        Button back = action("BACK",76,52);
        Button home = action("HOME",76,52);
        Button prev = action("⏮",62,52);
        Button play = action("▶ / ⏸",92,52);
        Button next = action("⏭",62,52);
        media.addView(back); media.addView(home); media.addView(prev); media.addView(play); media.addView(next);
        root.addView(media);

        setContentView(root);

        up.setOnClickListener(v->sendConsumer(0x001));
        down.setOnClickListener(v->sendConsumer(0x002));
        left.setOnClickListener(v->sendConsumer(0x004));
        right.setOnClickListener(v->sendConsumer(0x008));
        ok.setOnClickListener(v->sendConsumer(0x010));
        volUp.setOnClickListener(v->sendConsumer(0x020));
        volDown.setOnClickListener(v->sendConsumer(0x040));
        back.setOnClickListener(v->sendConsumer(0x080));
        home.setOnClickListener(v->sendConsumer(0x100));
        prev.setOnClickListener(v->sendConsumer(0x200));
        play.setOnClickListener(v->sendConsumer(0x400));
        next.setOnClickListener(v->sendConsumer(0x800));
        connect.setOnClickListener(v->connectSelectedDevice());
        menu.setOnClickListener(v->Toast.makeText(this,"BT Remote Pro",Toast.LENGTH_SHORT).show());
        settings.setOnClickListener(v->Toast.makeText(this,"Bluetooth settings are managed by Android",Toast.LENGTH_SHORT).show());
    }

    private void setStatus(String s) {
        if (status != null) status.setText(s);
    }

    private void startBluetooth() {
        loadBondedDevices();
        setupHid();
    }

    private void loadBondedDevices() {
        if (adapter == null || (Build.VERSION.SDK_INT >=31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)) return;
        bonded.clear();
        try {
            bonded.addAll(adapter.getBondedDevices());
            List<String> names = new ArrayList<>();
            for (BluetoothDevice d: bonded) {
                String n;
                try { n=d.getName(); } catch(SecurityException e) { n=null; }
                names.add(n == null || n.isEmpty() ? d.getAddress() : n);
            }
            if (names.isEmpty()) names.add("No paired devices");
            devicesSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        } catch(SecurityException e) { setStatus("Bluetooth permission required"); }
    }

    private void setupHid() {
        if (adapter == null || Build.VERSION.SDK_INT < 28) { setStatus("Bluetooth HID requires Android 9+"); return; }
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
            BluetoothHidDeviceAppSdpSettings s = new BluetoothHidDeviceAppSdpSettings(
                "BT Remote Pro","Android TV Remote","BT Remote Pro",(byte)0x80,HID_DESC);
            hid.registerApp(s,null,null,Executors.newSingleThreadExecutor(),new BluetoothHidDevice.Callback() {
                @Override public void onAppStatusChanged(BluetoothDevice d,boolean registered) {
                    runOnUiThread(()->setStatus(registered ? "Bluetooth HID • ready" : "Bluetooth HID • not registered"));
                }
                @Override public void onConnectionStateChanged(BluetoothDevice d,int state) {
                    runOnUiThread(()->{
                        if(state==BluetoothProfile.STATE_CONNECTED) { host=d; setStatus("Connected • Android TV"); }
                        else if(state==BluetoothProfile.STATE_DISCONNECTED) { if(host==d) host=null; setStatus("Android TV • disconnected"); }
                    });
                }
            });
        } catch(SecurityException e) { setStatus("Bluetooth permission required"); }
    }

    private void connectSelectedDevice() {
        if(hid==null) { setStatus("Bluetooth HID • not ready"); return; }
        if(bonded.isEmpty()) { setStatus("Pair Android TV in Android Bluetooth settings first"); return; }
        int pos=devicesSpinner.getSelectedItemPosition();
        if(pos<0 || pos>=bonded.size()) { setStatus("Select a Bluetooth device"); return; }
        try {
            BluetoothDevice d=bonded.get(pos);
            boolean ok=hid.connect(d);
            if(ok) { host=d; setStatus("Connecting • Android TV"); }
            else setStatus("Connection request failed");
        } catch(SecurityException e) { setStatus("Bluetooth permission required"); }
    }

    private void sendConsumer(int mask) {
        if(hid==null || host==null) { setStatus("Connect to Android TV first"); return; }
        try {
            hid.sendReport(host,1,new byte[]{(byte)(mask&255),(byte)((mask>>8)&255)});
            hid.sendReport(host,1,new byte[]{0,0});
        } catch(SecurityException e) { setStatus("Bluetooth permission required"); }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if(adapter!=null && hid!=null) {
            try { adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE,hid); } catch(SecurityException ignored) {}
        }
        hid=null; host=null;
    }
}
