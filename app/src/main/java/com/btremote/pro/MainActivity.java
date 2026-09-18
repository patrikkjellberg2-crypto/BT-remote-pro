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
    private Button connectButton;

    private final List<BluetoothDevice> bonded = new ArrayList<>();

    // HID keyboard-style consumer control report
    private static final byte[] HID_DESC = new byte[] {
        0x05, 0x0C,
        0x09, 0x01,
        (byte) 0xA1, 0x01,

        (byte) 0x85, 0x01,
        0x15, 0x00,
        0x25, 0x01,

        0x09, (byte) 0xE9,
        0x09, (byte) 0xEA,
        0x09, (byte) 0xE2,
        0x09, (byte) 0xB0,

        0x09, (byte) 0xB5,
        0x09, (byte) 0xB6,
        0x09, (byte) 0xCD,

        0x09, (byte) 0xB7,
        0x09, (byte) 0xB8,

        0x75, 0x01,
        (byte) 0x95, 0x09,

        (byte) 0x81, 0x02,
        (byte) 0xC0
    };

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().setStatusBarColor(Color.rgb(11, 12, 15));

        adapter = BluetoothAdapter.getDefaultAdapter();

        if (Build.VERSION.SDK_INT >= 31 &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_ADVERTISE
                    },
                    REQ_BT
            );
        }

        buildUi();
        setupHid();
    }

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    private TextView tv(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private Button btn(String s, int w, int h) {
        Button b = new Button(this);

        b.setText(s);
        b.setTextSize(20);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);

        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(0, 0, 0, 0);

        b.setLayoutParams(
                new LinearLayout.LayoutParams(w, h)
        );

        b.setBackground(
                bg(Color.rgb(31, 34, 42), 80)
        );

        return b;
    }

    private void buildUi() {

        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 18, 18, 18);
        root.setBackgroundColor(
                Color.rgb(13, 15, 18)
        );

        // Header
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);

        Button menu = btn("☰", 72, 72);
        head.addView(menu);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setGravity(Gravity.CENTER);

        TextView title =
                tv("BT Remote Pro", 23, Color.WHITE);

        TextView sub =
                tv(
                        "Connected: Android TV",
                        14,
                        Color.rgb(241, 154, 114)
                );

        titleBox.addView(title);
        titleBox.addView(sub);

        head.addView(
                titleBox,
                new LinearLayout.LayoutParams(
                        0,
                        72,
                        1
                )
        );

        Button settings = btn("⚙", 72, 72);
        head.addView(settings);

        root.addView(head);

        // Connection area
        LinearLayout conn = new LinearLayout(this);
        conn.setPadding(0, 12, 0, 8);

        devicesSpinner = new Spinner(this);

        connectButton =
                btn("Connect", 120, 52);

        conn.addView(
                devicesSpinner,
                new LinearLayout.LayoutParams(
                        0,
                        52,
                        1
                )
        );

        conn.addView(connectButton);

        root.addView(conn);

        status =
                tv(
                        "Bluetooth HID: starting…",
                        13,
                        Color.GRAY
                );

        root.addView(status);

        // Remote area
        FrameLayout remoteRow =
                new FrameLayout(this);

        LinearLayout.LayoutParams rr =
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                );

        remoteRow.setLayoutParams(rr);

        TextView dpad =
                tv(
                        "⌃\n\n   ‹        OK        ›\n\n⌄",
                        28,
                        Color.LTGRAY
                );

        dpad.setGravity(Gravity.CENTER);

        dpad.setBackground(
                bg(Color.rgb(20, 23, 29), 300)
        );

        FrameLayout.LayoutParams dp =
                new FrameLayout.LayoutParams(
                        0,
                        -1,
                        1
                );

        dp.setMargins(0, 18, 12, 18);

        remoteRow.addView(dpad, dp);

        // Volume
        LinearLayout vol =
                new LinearLayout(this);

        vol.setOrientation(
                LinearLayout.VERTICAL
        );

        vol.setGravity(Gravity.CENTER);

        Button plus =
                btn("+", 70, 65);

        Button minus =
                btn("−", 70, 65);

        SeekBar seek =
                new SeekBar(this);

        seek.setMax(100);
        seek.setProgress(55);
        seek.setRotation(270);

        vol.addView(plus);

        vol.addView(
                seek,
                new LinearLayout.LayoutParams(
                        90,
                        190
                )
        );

        vol.addView(minus);

        FrameLayout.LayoutParams vp =
                new FrameLayout.LayoutParams(
                        95,
                        -1,
                        Gravity.RIGHT
                );

        vp.setMargins(0, 18, 0, 18);

        remoteRow.addView(vol, vp);

        root.addView(remoteRow);

        // Media controls
        LinearLayout media =
                new LinearLayout(this);

        media.setGravity(Gravity.CENTER);
        media.set
