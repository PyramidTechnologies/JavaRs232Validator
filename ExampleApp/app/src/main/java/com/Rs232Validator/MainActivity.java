package com.Rs232Validator;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import PTI.Rs232Validator.BillValidators.BillValidator;
import PTI.Rs232Validator.CustomEvent;
import PTI.Rs232Validator.Loggers.LogLevel;
import PTI.Rs232Validator.Loggers.Logger;
import PTI.Rs232Validator.Rs232Configuration;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharePrefSettings;

    Button pollingButton;

    EditText outputText;
    EditText bill1Value;
    EditText bill2Value;
    EditText bill3Value;
    EditText bill4Value;
    EditText bill5Value;
    EditText bill6Value;
    EditText bill7Value;

    public BillValidator billValidator;
    public FT311UARTInterface connection;
    public CustomEvent OnBillStacked = new CustomEvent();

    public static Context global_context;
    public String act_string;
    public boolean bConfiged = false;
    public boolean isPolling = false;
    public final int baudRate = 9600;
    public final int stopBit = 1;
    public final int dataBit = 7;
    public final int parity = 2;
    public final int flowControl = 0;

    private int bill1 = 0;
    private int bill2 = 0;
    private int bill3 = 0;
    private int bill4 = 0;
    private int bill5 = 0;
    private int bill6 = 0;
    private int bill7 = 0;

    public AndroidLogger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        sharePrefSettings = getSharedPreferences("UARTLBPref", 0);
        global_context = this;

        bill1Value = (EditText) findViewById(R.id.bill1Value);
        bill2Value = (EditText) findViewById(R.id.bill2Value);
        bill3Value = (EditText) findViewById(R.id.bill3Value);
        bill4Value = (EditText) findViewById(R.id.bill4Value);
        bill5Value = (EditText) findViewById(R.id.bill5Value);
        bill6Value = (EditText) findViewById(R.id.bill6Value);
        bill7Value = (EditText) findViewById(R.id.bill7Value);

        pollingButton = (Button) findViewById(R.id.PollingButton);

        act_string = getIntent().getAction();
        if(-1 != act_string.indexOf("android.intent.action.MAIN")){
            restorePreference();
        } else if(-1 != act_string.indexOf("android.hardware.usb.action.USB_ACCESSORY_ATTACHED")){
            cleanPreference();
        }

        pollingButton.setOnClickListener(v -> {
            if(isPolling){
                isPolling = false;
                billValidator.StopPollingLoop();
            } else{
                isPolling = billValidator.StartPollingLoop();
            }
        });

        logger = new AndroidLogger("Android Logger", LogLevel.Trace);
        connection = new FT311UARTInterface(this, sharePrefSettings, logger);
        connection.SetConfig(baudRate, (byte) dataBit, (byte) stopBit, (byte) parity, (byte) flowControl);
        billValidator = new BillValidator(logger, connection, new Rs232Configuration());
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

    protected void savePreference(){
        if(bConfiged){
            sharePrefSettings.edit().putString("configed", "TRUE").commit();
            sharePrefSettings.edit().putInt("baudRate", baudRate).commit();
            sharePrefSettings.edit().putInt("stopBit", stopBit).commit();
            sharePrefSettings.edit().putInt("dataBit", dataBit).commit();
            sharePrefSettings.edit().putInt("parity", parity).commit();
            sharePrefSettings.edit().putInt("flowControl", flowControl).commit();
        } else{
            sharePrefSettings.edit().putString("configed", "FALSE").commit();
        }
    }

    protected void restorePreference(){
        String key_name = sharePrefSettings.getString("configed", "");
        if(key_name.contains("TRUE")){
            bConfiged = true;
        } else {
            bConfiged = false;
        }
    }

    @Override
    protected void onResume() {
        // Ideally should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onResume();
        if( 2 == connection.ResumeAccessory() )
        {
            cleanPreference();
            restorePreference();
            billValidator._serialProvider = connection;
        }
        if(billValidator == null || billValidator._serialProvider == null){
            billValidator = new BillValidator(logger, connection, new Rs232Configuration());
        }
    }

    @Override
    protected void onPause() {
        // Ideally should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        connection.DestroyAccessory(true);
        //android.os.Process.killProcess(android.os.Process.myPid());
        super.onDestroy();
    }

    public class AndroidLogger extends Logger {

        public AndroidLogger(String name, LogLevel minLogLevel){
            super(name, minLogLevel);
        }

        @Override
        protected void Log(String name, LogLevel logLevel, String format, Object... args) {

            runOnUiThread(() -> {
                outputText.append(String.format("[%s] [%s]", name, logLevel.name()) + String.format(format, args) + "\n\n");
            });
        }
    }
}