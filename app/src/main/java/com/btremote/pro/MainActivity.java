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

    // Android Consumer Control HID descriptor
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

        if (adapter == null) {
            Toast.makeText(
                    this,
                    "Bluetooth is not available",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        requestBluetoothPermissions();

        buildUi();
        loadBondedDevices();
        setupHid();
    }

    private void requestBluetoothPermissions() {

        if (Build.VERSION.SDK_INT >= 31) {

            if (checkSelfPermission(
                    Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(
                    Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[] {
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_ADVERTISE
                        },
                        REQ_BT
                );
            }
        }
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

        titleBox.setOrientation(
                LinearLayout.VERTICAL
        );

        titleBox.setGravity(Gravity.CENTER);

        TextView title =
                tv(
                        "BT Remote Pro",
                        23,
                        Color.WHITE
                );

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

        conn.setGravity(Gravity.CENTER_VERTICAL);
        conn.setPadding(0, 12, 0, 8);

        devicesSpinner = new Spinner(this);

        connectButton =
                btn(
                        "Connect",
                        120,
                        52
                );

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

        // D-pad
        LinearLayout dpad =
                new LinearLayout(this);

        dpad.setOrientation(
                LinearLayout.VERTICAL
        );

        dpad.setGravity(Gravity.CENTER);

        Button up = btn("▲", 100, 65);

        LinearLayout middle =
                new LinearLayout(this);

        middle.setGravity(Gravity.CENTER);

        Button left =
                btn("◀", 100, 65);

        Button ok =
                btn("OK", 110, 65);

        Button right =
                btn("▶", 100, 65);

        middle.addView(left);
        middle.addView(ok);
        middle.addView(right);

        Button down =
                btn("▼", 100, 65);

        dpad.addView(up);
        dpad.addView(middle);
        dpad.addView(down);

        dpad.setBackground(
                bg(
                        Color.rgb(20, 23, 29),
                        300
                )
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

        Button back =
                btn("Back", 95, 58);

        Button home =
                btn("Home", 95, 58);

        Button previous =
                btn("⏮", 75, 58);

        Button play =
                btn("▶ / ⏸", 105, 58);

        Button next =
                btn("⏭", 75, 58);

        media.addView(back);
        media.addView(home);
        media.addView(previous);
        media.addView(play);
        media.addView(next);

        root.addView(media);

        setContentView(root);

        // D-pad actions
        up.setOnClickListener(
                v -> sendConsumer(0x01)
        );

        down.setOnClickListener(
                v -> sendConsumer(0x02)
        );

        left.setOnClickListener(
                v -> sendConsumer(0x04)
        );

        right.setOnClickListener(
                v -> sendConsumer(0x08)
        );

        ok.setOnClickListener(
                v -> sendConsumer(0x10)
        );

        plus.setOnClickListener(
                v -> sendConsumer(0x20)
        );

        minus.setOnClickListener(
                v -> sendConsumer(0x40)
        );

        back.setOnClickListener(
                v -> sendConsumer(0x80)
        );

        home.setOnClickListener(
                v -> sendConsumer(0x100)
        );

        previous.setOnClickListener(
                v -> sendConsumer(0x200)
        );

        play.setOnClickListener(
                v -> sendConsumer(0x400)
        );

        next.setOnClickListener(
                v -> sendConsumer(0x800)
        );

        connectButton.setOnClickListener(
                v -> connectSelectedDevice()
        );
    }

    private void loadBondedDevices() {

        if (adapter == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 31 &&
                checkSelfPermission(
                        Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        bonded.clear();

        try {

            bonded.addAll(
                    adapter.getBondedDevices()
            );

            List<String> names =
                    new ArrayList<>();

            for (BluetoothDevice device : bonded) {

                String name;

                try {
                    name = device.getName();
                } catch (SecurityException e) {
                    name = null;
                }

                if (name == null || name.length() == 0) {
                    name = device.getAddress();
                }

                names.add(name);
            }

            if (names.isEmpty()) {
                names.add("No paired devices");
            }

            ArrayAdapter<String> spinnerAdapter =
                    new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            names
                    );

            devicesSpinner.setAdapter(
                    spinnerAdapter
            );

        } catch (SecurityException e) {

            status.setText(
                    "Bluetooth permission required"
            );
        }
    }

    private void setupHid() {

        if (adapter == null) {
            return;
        }

        if (Build.VERSION.SDK_INT < 28) {

            status.setText(
                    "Bluetooth HID requires Android 9+"
            );

            return;
        }

        try {

            adapter.getProfileProxy(
                    this,
                    new BluetoothProfile.ServiceListener() {

                        @Override
                        public void onServiceConnected(
                                int profile,
                                BluetoothProfile proxy
                        ) {

                            if (profile ==
                                    BluetoothProfile.HID_DEVICE) {

                                hid =
                                        (BluetoothHidDevice) proxy;

                                registerHidApp();
                            }
                        }

                        @Override
                        public void onServiceDisconnected(
                                int profile
                        ) {

                            if (profile ==
                                    BluetoothProfile.HID_DEVICE) {

                                hid = null;

                                status.setText(
                                        "Bluetooth HID disconnected"
                                );
                            }
                        }
                    },
                    BluetoothProfile.HID_DEVICE
            );

        } catch (SecurityException e) {

            status.setText(
                    "Bluetooth permission required"
            );
        }
    }

    private void registerHidApp() {

        if (hid == null) {
            return;
        }

        try {

            BluetoothHidDeviceAppSdpSettings settings =
                    new BluetoothHidDeviceAppSdpSettings(
                            "BT Remote Pro",
                            "Android TV Remote",
                            "BT Remote Pro",
                            (byte) 0x80,
                            HID_DESC
                    );

            boolean registered =
                    hid.registerApp(
                            settings,
                            null,
                            null,
                            Executors.newSingleThreadExecutor(),
                            new BluetoothHidDevice.Callback() {

                                @Override
                                public void onAppStatusChanged(
                                        BluetoothDevice pluggedDevice,
                                        boolean registered
                                ) {

                                    runOnUiThread(() -> {

                                        if (registered) {

                                            status.setText(
                                                    "Bluetooth HID ready"
                                            );

                                        } else {

                                            status.setText(
                                                    "Bluetooth HID not registered"
                                            );
                                        }
                                    });
                                }

                                @Override
                                public void onConnectionStateChanged(
                                        BluetoothDevice device,
                                        int state
                                ) {

                                    runOnUiThread(() -> {

                                        if (state ==
                                                BluetoothProfile.STATE_CONNECTED) {

                                            host = device;

                                            status.setText(
                                                    "Connected to Android TV"
                                            );

                                        } else if (state ==
                                                BluetoothProfile.STATE_DISCONNECTED) {

                                            if (host == device) {
                                                host = null;
                                            }

                                            status.setText(
                                                    "Android TV disconnected"
                                            );
                                        }
                                    });
                                }
                            }
                    );

            if (!registered) {

                status.setText(
                        "Could not register Bluetooth HID"
                );
            }

        } catch (SecurityException e) {

            status.setText(
                    "Bluetooth permission required"
            );
        }
    }

    private void connectSelectedDevice() {

        if (hid == null) {

            status.setText(
                    "Bluetooth HID is not ready"
            );

            return;
        }

        if (bonded.isEmpty()) {

            status.setText(
                    "No paired Bluetooth device"
            );

            return;
        }

        int position =
                devicesSpinner.getSelectedItemPosition();

        if (position < 0 ||
                position >= bonded.size()) {

            status.setText(
                    "Select a Bluetooth device"
            );

            return;
        }

        BluetoothDevice device =
                bonded.get(position);

        try {

            boolean result =
                    hid.connect(device);

            if (result) {

                host = device;

                status.setText(
                        "Connecting to Android TV…"
                );

            } else {

                status.setText(
                        "Connection request failed"
                );
            }

        } catch (SecurityException e) {

            status.setText(
                    "Bluetooth permission required"
            );
        }
    }

    private void sendConsumer(int mask) {

        if (hid == null || host == null) {

            status.setText(
                    "Connect to Android TV first"
            );

            return;
        }

        byte low =
                (byte) (mask & 0xFF);

        byte high =
                (byte) ((mask >> 8) & 0xFF);

        byte[] report =
                new byte[] {
                        low,
                        high
                };

        try {

            hid.sendReport(
                    host,
                    1,
                    report
            );

            // Release button
            hid.sendReport(
                    host,
                    1,
                    new byte[] {
                            0x00,
                            0x00
                    }
            );

        } catch (SecurityException e) {

            status.setText(
                    "Bluetooth permission required"
            );
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (adapter != null &&
                hid != null) {

            try {

                adapter.closeProfileProxy(
                        BluetoothProfile.HID_DEVICE,
                        hid
                );

            } catch (SecurityException ignored) {
            }
        }

        hid = null;
        host = null;
    }
}
