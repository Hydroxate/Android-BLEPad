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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class BluetoothControlAcitvity extends Activity
{
    private final static String TAG = BluetoothControlAcitvity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    
    private String mDeviceName;
    private String mDeviceAddress;
    private HolloBluetooth mble;
    private Context context;
    private EditText sendEdit; 
    private ScrollView scrollView;
    private Button sendBt;
    private Handler mHandler;
    public String received;
	private static final int MSG_DATA_CHANGE = 0x11;  
    
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bluetooth_control);
		
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        
        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        context = this;
        scrollView = (ScrollView)findViewById(R.id.scroll);
        sendBt = (Button)findViewById(R.id.send);
        sendEdit = (EditText)findViewById(R.id.sendData);
        sendBt.setEnabled(false);
        
        mble = HolloBluetooth.getInstance(getApplicationContext());
        
        mHandler = new Handler()
        {  
            @Override  
            public void handleMessage(Message msg) 
            {  
                switch (msg.what) {  
                case MSG_DATA_CHANGE:  
                	int color = msg.arg1;
                	String strData = (String)msg.obj;
                	SpannableStringBuilder builder = new SpannableStringBuilder(strData);
                	  
                	  //ForegroundColorSpan ，BackgroundColorSpan
                	ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
                	String string;
                	int num;
                	switch (color) {
					case Color.BLUE: //send

						builder.setSpan(colorSpan, 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						break;
					case Color.RED:	//error
						builder.setSpan(colorSpan, 0, strData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						break;
					case Color.BLACK: //tips
						builder.setSpan(colorSpan, 0, strData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						break;

					default: //receive
                        addLogText(strData,Color.BLACK,strData.length());
                        received = strData;
						builder.setSpan(colorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						break;
					}
                	TextView tView = new TextView(context);
					tView.setText(builder);
					LinearLayout layout = (LinearLayout)findViewById(R.id.scroll_layout);
					layout.addView(tView);
					scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    break;  
  
                default:  
                    break;  
                }  
                super.handleMessage(msg);  
            }  
        }; 
        
        Button clearBt = (Button)findViewById(R.id.clear_log);
        clearBt.setOnClickListener(new View.OnClickListener() 
        {
			@Override
			public void onClick(View v) 
			{
				LinearLayout layout = (LinearLayout)findViewById(R.id.scroll_layout);
				layout.removeAllViews();

			}
		});
        
        sendBt.setOnClickListener(new View.OnClickListener() 
        {
			@Override
			public void onClick(View v) 
			{
				new Handler().post(new Runnable()
				{
					@Override
					public void run()
					{
						String sendString = sendEdit.getText().toString();
						if(sendString == null || sendString.isEmpty())
							return ;
						sendEdit.setText("");

						addLogText("Sent："+ sendString,Color.BLUE, sendString.length());
						if(!mble.sendData(sendString)){}
							//addLogText("SENT！",Color.RED,0);
					}
				});

			}
		});


        
        new Handler().post(new Runnable() 
        {
            @Override
            public void run() 
            {    

            	int i;
            	for (i = 0; i < 5; i++) 
            	{
            		 if(mble.connectDevice(mDeviceAddress,bleCallBack))
                     	break;

					try {
						Thread.sleep(500,0);//200ms
					}
					catch (Exception e){

					}
				}
            	if(i == 5)
            	{

            		return ;
            	}

				try {
					Thread.sleep(200,0);//200ms
				}
				catch (Exception e){

				}



            	if(mble.wakeUpBle())
            	{

            		sendBt.setEnabled(true);
            	}
            	else
            	{

				}
            	
            }
        });
        
	}


	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) 
	{

		return super.onMenuItemSelected(featureId, item);
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.main, menu);

		menu.findItem(R.id.menu_refresh).setActionView(null);
		
		return super.onCreateOptionsMenu(menu);
	}



	void addLogText(final String log, final int color, int byteLen)
	{
		Message message = new Message();
        message.what = MSG_DATA_CHANGE;  
        message.arg1 = color;
        message.arg2 = byteLen;
        message.obj = log;
        mHandler.sendMessage(message);
	}
	
	HolloBluetooth.OnHolloBluetoothCallBack bleCallBack = new HolloBluetooth.OnHolloBluetoothCallBack()
	{
		
		@Override
		public void OnHolloBluetoothState(int state)
		{
			if(state == HolloBluetooth.HOLLO_BLE_DISCONNECTED)
			{
				onBackPressed();
			}
		}

		@Override
		public void OnReceiveData(byte[] recvData)
		{
			addLogText("Received："+ConvertData.bytesToHexString(recvData, false),Color.rgb(139, 0, 255),recvData.length);
		}
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
        case android.R.id.home:
            onBackPressed();
            return true;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		mble.disconnectDevice();
		Log.d(TAG, "destroy");
		mble.disconnectLocalDevice();
		Log.d(TAG, "destroyed");
	}


}
