package com.example.testble;


import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;

public class BluetoothControlAcitvity extends Activity {
    private final static String TAG = BluetoothControlAcitvity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;
    private HolloBluetooth mble;
    private Context context;

    private ScrollView scrollView;

    private Handler mHandler;
    public String received;
    public String buttonValue;
    public String bendValue;
    public String forceValue;
    public String thermValue;
    int cut = 1;
    private static final int MSG_DATA_CHANGE = 0x11;

    private PdUiDispatcher dispatcher;

    private void initPD() throws IOException {
        int sampleRate = AudioParameters.suggestSampleRate();
        PdAudio.initAudio(sampleRate, 0, 2, 8, true);

        dispatcher = new PdUiDispatcher();
        PdBase.setReceiver(dispatcher);
    }

    private void initGUI() {
        //  Switch initSynthSwitch = (Switch) findViewById(R.id.switch1);

        //  initSynthSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        //     @Override
        //     public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //         // Log.i("INIT","SWITCH CHANGED" + String.valueOf(isChecked));
        //        float val = (isChecked) ?  1.0f : 0.0f;
        //        sendFloatPD("onOff", val);
        //    }
        //   });
    }

    public void sendPatchData(String receive, String value) {

        //addLogText(value,Color.BLACK,value.length());


        sendFloatPD(receive, Float.parseFloat(value));

        Log.e(receive, value);

    }

    public void sendFloatPD(String receiver, Float value) {
        PdBase.sendFloat(receiver, value);
    }

    public void sendBangPD(String receiver) {
        PdBase.sendBang(receiver);
    }


    private void loadPDPatch(String patchName) throws IOException {
        File dir = getFilesDir();
        try {
            IoUtils.extractZipResource(getResources().openRawResource(R.raw.synth), dir, true);
            File pdPatch = new File(dir, patchName);
            PdBase.openPatch(pdPatch.getAbsolutePath());
        } catch (IOException e) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_control);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        context = this;
        scrollView = (ScrollView) findViewById(R.id.scroll);


        mble = HolloBluetooth.getInstance(getApplicationContext());


        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_DATA_CHANGE:
                        int color = msg.arg1;
                        String strData = (String) msg.obj;
                        SpannableStringBuilder builder = new SpannableStringBuilder(strData);

                        //ForegroundColorSpan ，BackgroundColorSpan
                        ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
                        String string;
                        int num;
                        switch (color) {
                            case Color.BLUE: //send

                                builder.setSpan(colorSpan, 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                            case Color.RED:    //error
                                builder.setSpan(colorSpan, 0, strData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                            case Color.BLACK: //tips
                                builder.setSpan(colorSpan, 0, strData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;

                            default: //receive
                                addLogText(strData, Color.BLACK, strData.length());
                                received = strData.substring(9);
                                if (received != null && received != "") {
                                    for (int i = 1; i < received.length(); i++) {
                                        if (received.charAt(i) == 'A' || received.charAt(i) == 'B' || received.charAt(i) == 'C' || received.charAt(i) == 'D') {
                                            cut = i;
                                            i = received.length();
                                        } else {
                                            cut = received.length();
                                        }
                                    }

                                    if(cut != 1) {

                                        if (received.charAt(0) == 'A') {
                                            bendValue = received.substring(1, cut);
                                            sendPatchData("bend",bendValue);
                                          //  Log.e("BEND", bendValue);
                                        } else if (received.charAt(0) == 'B') {
                                            thermValue = received.substring(1, cut);
                                           // Log.e("THERM", thermValue);
                                            sendPatchData("therm", thermValue);
                                        } else if (received.charAt(0) == 'C') {
                                            forceValue = received.substring(1, cut);
                                            sendPatchData("force",forceValue);
                                          //  Log.e("FORCE", forceValue);
                                        } else if (received.charAt(0) == 'D') {
                                            buttonValue = received.substring(1, cut);
                                            sendPatchData("button",buttonValue);
                                          //  Log.e("BUTTON", buttonValue);
                                        } else {
                                            sendPatchData("freq","100");
                                        }
                                    }
                                    else
                                    {
                                        sendPatchData("freq","100");
                                    }

                                }

                                builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                        }

                        TextView tView = new TextView(context);
                        tView.setText(builder);
                        LinearLayout layout = (LinearLayout) findViewById(R.id.scroll_layout);
                        layout.addView(tView);
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        break;

                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };

        Button clearBt = (Button) findViewById(R.id.clear_log);
        clearBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout layout = (LinearLayout) findViewById(R.id.scroll_layout);
                layout.removeAllViews();

            }
        });


        new Handler().post(new Runnable() {
            @Override
            public void run() {

                int i;
                for (i = 0; i < 5; i++) {
                    if (mble.connectDevice(mDeviceAddress, bleCallBack))
                        break;

                    try {
                        Thread.sleep(10, 0);
                    } catch (Exception e) {

                    }
                }
                if (i == 5) {

                    return;
                }

                try {
                    Thread.sleep(10, 0);
                } catch (Exception e) {

                }


                if (mble.wakeUpBle()) {

                } else {

                }

            }
        });

        try {
            initPD();
            loadPDPatch("synth.pd"); // This is the name of the patch in the zip
            sendFloatPD("onOff", 1.0f);

            new Handler().post(new Runnable() {
                @Override
                public void run() {

                    if (!mble.sendData("start")) {
                    }

                }
            });


        } catch (IOException e) {
            finish();
        }

    }


    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        return super.onMenuItemSelected(featureId, item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        menu.findItem(R.id.menu_refresh).setActionView(null);

        return super.onCreateOptionsMenu(menu);
    }


    void addLogText(final String log, final int color, int byteLen) {
        Message message = new Message();
        message.what = MSG_DATA_CHANGE;
        message.arg1 = color;
        message.arg2 = byteLen;
        message.obj = log;
        mHandler.sendMessage(message);
    }

    HolloBluetooth.OnHolloBluetoothCallBack bleCallBack = new HolloBluetooth.OnHolloBluetoothCallBack() {

        @Override
        public void OnHolloBluetoothState(int state) {
            if (state == HolloBluetooth.HOLLO_BLE_DISCONNECTED) {
                onBackPressed();
            }
        }

        @Override
        public void OnReceiveData(byte[] recvData) {
            addLogText("Received：" + ConvertData.bytesToHexString(recvData, false), Color.rgb(139, 0, 255), recvData.length);
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PdAudio.startAudio(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PdAudio.stopAudio();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mble.disconnectDevice();
        Log.d(TAG, "destroy");
        mble.disconnectLocalDevice();
        Log.d(TAG, "destroyed");
    }


}
