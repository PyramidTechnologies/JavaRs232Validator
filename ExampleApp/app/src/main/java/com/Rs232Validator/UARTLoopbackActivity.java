package com.Rs232Validator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.util.List;

import PTI.Rs232Validator.BillValidators.BillValidator;
import PTI.Rs232Validator.CustomEvent;
import PTI.Rs232Validator.Loggers.LogLevel;
import PTI.Rs232Validator.Loggers.Logger;
import PTI.Rs232Validator.Messages.Requests.Rs232RequestMessage;
import PTI.Rs232Validator.Messages.Responses.Rs232ResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetSerialNumberResponseMessage;
import PTI.Rs232Validator.Rs232Configuration;
import PTI.Rs232Validator.Utility.ByteExtensions;


public class UARTLoopbackActivity extends Activity {

	/* declare a FT311 UART interface variable */
	public FT311UARTInterface uartInterface;

	/* graphical objects */
	Button pollingButton;
	EditText outputText;
	EditText bill1Value;
	EditText bill2Value;
	EditText bill3Value;
	EditText bill4Value;
	EditText bill5Value;
	EditText bill6Value;
	EditText bill7Value;

	int baudRate; /* baud rate */
	byte stopBit; /* 1:1stop bits, 2:2 stop bits */
	byte dataBit; /* 8:8bit, 7: 7bit */
	byte parity; /* 0: none, 1: odd, 2: even, 3: mark, 4: space */
	byte flowControl; /* 0:none, 1: flow control(CTS,RTS) */
	public Context global_context;
	public boolean bConfiged = false;
	public SharedPreferences sharePrefSettings;
	public String act_string;

	public BillValidator billValidator;
	public AndroidLogger logger;
	public boolean isPolling;

	private int bill1 = 0;
	private int bill2 = 0;
	private int bill3 = 0;
	private int bill4 = 0;
	private int bill5 = 0;
	private int bill6 = 0;
	private int bill7 = 0;

	public CustomEvent OnBillStacked = new CustomEvent();
	public CustomEvent OnCommunicationAttempted = new CustomEvent();
	public CustomEvent OnConnectionLost = new CustomEvent();
	public CustomEvent OnStateChanged = new CustomEvent();
	public CustomEvent OnEventReported = new CustomEvent();
	public CustomEvent OnCashboxAttached = new CustomEvent();
	public CustomEvent OnCashboxRemoved = new CustomEvent();
	public CustomEvent OnBillEscrowed = new CustomEvent();
	public CustomEvent OnBarcodeDetected = new CustomEvent();

	/** Called when the activity is first created. */
	@SuppressLint("SuspiciousIndentation")
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		sharePrefSettings = getSharedPreferences("UARTLBPref", 0);
		cleanPreference();

		outputText = (EditText) findViewById(R.id.logOutputView);

		global_context = this;

		baudRate = 9600;
		stopBit = 1;
		dataBit = 7;
		parity = 2;
		flowControl = 0;

		bill1Value = (EditText) findViewById(R.id.bill1Value);
		bill2Value = (EditText) findViewById(R.id.bill2Value);
		bill3Value = (EditText) findViewById(R.id.bill3Value);
		bill4Value = (EditText) findViewById(R.id.bill4Value);
		bill5Value = (EditText) findViewById(R.id.bill5Value);
		bill6Value = (EditText) findViewById(R.id.bill6Value);
		bill7Value = (EditText) findViewById(R.id.bill7Value);

		pollingButton = (Button) findViewById(R.id.PollingButton);

		act_string = getIntent().getAction();
		if( -1 != act_string.indexOf("android.intent.action.MAIN")){
			restorePreference();
		}			
		else if( -1 != act_string.indexOf("android.hardware.usb.action.USB_ACCESSORY_ATTACHED")){
			cleanPreference();
		}

		/* handle write click */
        // @Override
        pollingButton.setOnClickListener(v -> {

            if(!bConfiged){
                bConfiged = true;
                uartInterface.SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
                billValidator._serialProvider = uartInterface;
                savePreference();
            }
            try {
                if (isPolling) {
                    billValidator.StopPollingLoop();
                    isPolling = false;
                    logger.LogInfo("Stopped Polling");
					pollingButton.setText(R.string.start_polling);
                } else {
                    isPolling = billValidator.StartPollingLoop();
                    logger.LogInfo("Started Polling");

					pollingButton.setText(R.string.stop_polling);

                    GetSerialNumberResponseMessage serialNumberResponseMessage = (GetSerialNumberResponseMessage) billValidator.GetSerialNumberAsync().join();
                    logger.LogInfo(serialNumberResponseMessage.getSerialNumber());
                }
            } catch (Exception e){
                e.getMessage();
            }
        });

		logger = new AndroidLogger("Android Logger", LogLevel.Trace);
		uartInterface = new FT311UARTInterface(this, sharePrefSettings, logger);
		billValidator = new BillValidator(logger, uartInterface, new Rs232Configuration());
		isPolling = false;

		OnBillStacked.addListener(args -> runOnUiThread(() -> {
			switch ((byte) args[0]){
				case 1:
					bill1++;
					bill1Value.setText(Integer.toString(bill1));
					break;

				case 2:
					bill2++;
					bill2Value.setText(Integer.toString(bill2));
					break;

				case 3:
					bill3++;
					bill3Value.setText(Integer.toString(bill3));
					break;

				case 4:
					bill4++;
					bill4Value.setText(Integer.toString(bill4));
					break;

				case 5:
					bill5++;
					bill5Value.setText(Integer.toString(bill5));
					break;

				case 6:
					bill6++;
					bill6Value.setText(Integer.toString(bill6));
					break;

				case 7:
					bill7++;
					bill7Value.setText(Integer.toString(bill7));
					break;
			}
		}));
		billValidator.OnBillStacked = OnBillStacked;

		OnCommunicationAttempted.addListener(args -> runOnUiThread(() -> {
			Rs232RequestMessage requestMessage = (Rs232RequestMessage) args[0];
			Rs232ResponseMessage responseMessage = (Rs232ResponseMessage) args[1];
			logger.LogInfo("Request Payload: %s | %s", ByteExtensions.ConvertToHexString(requestMessage.getPayload(), false, true), requestMessage.toString());
			logger.LogInfo("Response Payload: %s | %s", ByteExtensions.ConvertToHexString(responseMessage.getPayload(), false, true), responseMessage.toString());
		}));
		billValidator.OnCommunicationAttempted = OnCommunicationAttempted;

		

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	public class AndroidLogger extends Logger {

		public AndroidLogger(String name, LogLevel minLogLevel){
			super(name, minLogLevel);
		}

		@Override
		protected void Log(String name, LogLevel logLevel, String format, Object... args) {
			runOnUiThread(() -> {
				outputText.append(String.format("\n\n[%s] [%s]: ", name, logLevel.name()) + String.format(format, args));
			});
		}
	}
	
	protected void cleanPreference(){
		SharedPreferences.Editor editor = sharePrefSettings.edit();
		editor.remove("configed");
		editor.remove("baudRate");
		editor.remove("stopBit");
		editor.remove("dataBit");
		editor.remove("parity");
		editor.remove("flowControl");
		editor.commit();
	}

	protected void savePreference() {
		if(true == bConfiged){
			sharePrefSettings.edit().putString("configed", "TRUE").commit();
			sharePrefSettings.edit().putInt("baudRate", baudRate).commit();
			sharePrefSettings.edit().putInt("stopBit", stopBit).commit();
			sharePrefSettings.edit().putInt("dataBit", dataBit).commit();
			sharePrefSettings.edit().putInt("parity", parity).commit();			
			sharePrefSettings.edit().putInt("flowControl", flowControl).commit();			
		}
		else{
			sharePrefSettings.edit().putString("configed", "FALSE").commit();
		}
	}
	
	protected void restorePreference() {
		String key_name = sharePrefSettings.getString("configed", "");
		if(true == key_name.contains("TRUE")){
			bConfiged = true;
		}
		else{
			bConfiged = false;
        }
		
		baudRate = sharePrefSettings.getInt("baudRate", 9600);
		stopBit = (byte)sharePrefSettings.getInt("stopBit", 1);
		dataBit = (byte)sharePrefSettings.getInt("dataBit", 7);
		parity = (byte)sharePrefSettings.getInt("parity", 2);
		flowControl = (byte)sharePrefSettings.getInt("flowControl", 0);

	}

	//@Override
	public void onHomePressed() {
		//onBackPressed();
	}

	/*
	public void onBackPressed(){
		super.onBackPressed();
	}
	*/

	
	@Override
	protected void onResume() {
		// Ideally should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onResume();		
		if( 2 == uartInterface.ResumeAccessory() )
		{
			cleanPreference();
			restorePreference();
			billValidator._serialProvider = uartInterface;
		}
		if(billValidator == null || billValidator._serialProvider == null){
			billValidator = new BillValidator(logger, uartInterface, new Rs232Configuration());
		}
	}

	@Override
	protected void onPause() {
		// Ideally should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onPause();
	}

	@Override
	protected void onStop() {
		// Ideally should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		uartInterface.DestroyAccessory(bConfiged);
		super.onDestroy();
	}
}
