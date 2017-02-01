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
import android.widget.Toast;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdReceiver;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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
    private static final int MSG_DATA_CHANGE = 0x11;

    StringBuilder output = new StringBuilder();

    private PdUiDispatcher dispatcher;

    private void initPD() throws IOException {
        int sampleRate = AudioParameters.suggestSampleRate();
        PdAudio.initAudio(sampleRate, 1, 2, 8, true);

        dispatcher = new PdUiDispatcher();

        PdBase.setReceiver(dispatcher);

        dispatcher.addListener("android",receiver);
        PdBase.subscribe("android");

    }


    public void sendPatchData(String receive, String value) {

        sendFloatPD(receive, Float.parseFloat(value));

        Log.e(receive, value);

    }

    public void sendFloatPD(String receiver, Float value)
    {
        PdBase.sendFloat(receiver, value);
    }

    public void sendBangPD(String receiver)
    {
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


                                    for (int i = 0; i < strData.length(); i++) {
                                        if (strData.charAt(i) == 'A' || strData.charAt(i) == 'B' || strData.charAt(i) == 'C' || strData.charAt(i) == 'D') {
                                            received = output.toString();
                                            sensorParse();
                                            output.delete(0,output.length());
                                            output.append(strData.charAt(i));

                                        } else {
                                           output.append(strData.charAt(i));
                                        }
                                    }

                                break;
                        }

                        break;

                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };

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

    void sensorParse()
    {
        if(received.length()>0) {
            if (received.charAt(0) == 'A') {
                bendValue = received.substring(1);
                sendPatchData("A0", bendValue);
            }
            else if (received.charAt(0) == 'B') {
                thermValue = received.substring(1);
                sendPatchData("A1", thermValue);
            }
            else if (received.charAt(0) == 'C') {
                forceValue = received.substring(1);
                sendPatchData("A2", forceValue);
            }
            else if (received.charAt(0) == 'D') {
                buttonValue = received.substring(1);
                sendPatchData("A3", buttonValue);
            }
        }
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
            addLogText(ConvertData.bytesToHexString(recvData, false), Color.rgb(139, 0, 255), recvData.length);


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

    private PdReceiver receiver = new PdReceiver() {

        private void pdPost(final String msg) {
            Log.e("RECEIVED:", msg);

            new Handler().post(new Runnable() {
                @Override
                public void run() {

                    if (!mble.sendData(msg)) {
                    }

                }
            });
        }

        @Override
        public void print(String s) {
            Log.i("PRINT",s);
        }

        @Override
        public void receiveBang(String source) {
            pdPost("bang");
        }

        @Override
        public void receiveFloat(String source, float x) {
            pdPost("float: " + x);
        }

        @Override
        public void receiveList(String source, Object... args) {
            pdPost("list: " + Arrays.toString(args));
        }

        @Override
        public void receiveMessage(String source, String symbol, Object... args) {
            pdPost("message: " + Arrays.toString(args));
        }

        @Override
        public void receiveSymbol(String source, String symbol) {
            pdPost("symbol: " + symbol);
        }
    };



}
