//User must modify the below package with their package name
package PTI.Rs232Validator.SerialProviders;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.*;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import java.io.*;

import PTI.Rs232Validator.Loggers.ILogger;


/**
 * An implementation of {@link ISerialProvider} that connects to the FTDI Chip
 */
public class FT311UARTInterface extends Activity implements ISerialProvider
{
	private final String ACTION_USB_PERMISSION;
	public UsbManager usbmanager;
	public UsbAccessory usbaccessory;
	public PendingIntent mPermissionIntent;
	public ParcelFileDescriptor filedescriptor = null;
	public FileInputStream inputstream = null;
	public FileOutputStream outputstream = null;
	public boolean mPermissionRequestPending = false;
	public read_thread readThread;

	private byte [] usbdata;
	private byte []	writeusbdata;
	private byte[] readBuffer; /*circular buffer*/
	private int readcount;
	private int totalBytes;
	private int writeIndex;
	private int readIndex;
	private byte status;
	final int maxnumbytes = 65536;

	public boolean datareceived = false;
	public volatile boolean READ_ENABLE = false;
	public boolean accessory_attached = false;

	public Context global_context;

	public static String ManufacturerString = "mManufacturer=FTDI";
	public static String ModelString1 = "mModel=FTDIUARTDemo";
	public static String ModelString2 = "mModel=Android Accessory FT312D";
	public static String VersionString = "mVersion=1.0";

	public SharedPreferences intsharePrefSettings;

	public ILogger _logger;

	/**
	 * Initializes a new instance of {@link FT311UARTInterface}
	 * @param context The global context of the Android App
	 * @param sharePrefSettings The app's shared preferences
	 * @param logger {@link ILogger}
	 * @param permissionString The usb permission string
	 */
	@SuppressLint({"UnspecifiedRegisterReceiverFlag", "UnspecifiedImmutableFlag"})
	public FT311UARTInterface(Context context, SharedPreferences sharePrefSettings, ILogger logger, String permissionString){
		super();
		global_context = context;
		intsharePrefSettings = sharePrefSettings;
		_logger = logger;
		ACTION_USB_PERMISSION = permissionString;
		/*shall we start a thread here or what*/
		usbdata = new byte[1024];
		writeusbdata = new byte[256];
		/*128(make it 256, but looks like bytes should be enough)*/
		readBuffer = new byte [maxnumbytes];


		readIndex = 0;
		writeIndex = 0;
		/***********************USB handling******************************************/

		usbmanager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		// Log.d("LED", "usbmanager" +usbmanager);

		if(Build.VERSION.SDK_INT>=0x17){
			mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0 | (PendingIntent.FLAG_IMMUTABLE));
		} else {
			mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
		}
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			context.registerReceiver(mUsbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
		} else {
			context.registerReceiver(mUsbReceiver, filter);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void SetConfig(int baud, byte dataBits, byte stopBits,
						  byte parity, byte flowControl)
	{
		/*prepare the baud rate buffer*/
		writeusbdata[0] = (byte)baud;
		writeusbdata[1] = (byte)(baud >> 8);
		writeusbdata[2] = (byte)(baud >> 16);
		writeusbdata[3] = (byte)(baud >> 24);

		/*data bits*/
		writeusbdata[4] = dataBits;
		/*stop bits*/
		writeusbdata[5] = stopBits;
		/*parity*/
		writeusbdata[6] = parity;
		/*flow control*/
		writeusbdata[7] = flowControl;

		/*send the UART configuration packet*/
		SendPacket((int)8);
	}


	private byte ReadData(int numBytes, byte[] buffer, int[] actualNumBytes) {
		status = 0x00;

		while ((numBytes < 1) || (totalBytes == 0)) {
			Thread.currentThread().interrupt();
			actualNumBytes[0] = 0;
			status = 0x01;
			return status;
		}
		if (numBytes > totalBytes)
			numBytes = totalBytes;
		totalBytes -= numBytes;
		actualNumBytes[0] = numBytes;
		for (int count = 0; count < numBytes; count++) {
			buffer[count] = readBuffer[readIndex];
			readIndex++;
			readIndex %= maxnumbytes;
		}
		return status;
	}


	private void SendPacket(int numBytes)
	{
		try {
			if(outputstream != null){
				outputstream.write(writeusbdata, 0,numBytes);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	@Override
	public int ResumeAccessory()
	{
		// Intent intent = getIntent();
		if (inputstream != null && outputstream != null) {
			return 1;
		}

		UsbAccessory[] accessories = usbmanager.getAccessoryList();
		UsbAccessory accessory = (accessories != null && accessories.length > 0) ? accessories[0] : null;
		if(accessory == null)
		{
			accessory_attached = false;
			return 2;
		}

		Toast.makeText(global_context, "Accessory Attached", Toast.LENGTH_SHORT).show();


		if( -1 == accessory.toString().indexOf(ManufacturerString))
		{
			Toast.makeText(global_context, "Manufacturer is not matched!", Toast.LENGTH_SHORT).show();
			return 1;
		}

		if( -1 == accessory.toString().indexOf(ModelString1) && -1 == accessory.toString().indexOf(ModelString2))
		{
			Toast.makeText(global_context, "Model is not matched!", Toast.LENGTH_SHORT).show();
			return 1;
		}

		if( -1 == accessory.toString().indexOf(VersionString))
		{
			Toast.makeText(global_context, "Version is not matched!", Toast.LENGTH_SHORT).show();
			return 1;
		}

		Toast.makeText(global_context, "Manufacturer, Model & Version are matched!", Toast.LENGTH_SHORT).show();
		accessory_attached = true;

		if (usbmanager.hasPermission(accessory)) {
			OpenAccessory(accessory);
		}
		else
		{
			if (!mPermissionRequestPending) {
				Toast.makeText(global_context, "Request USB Permission", Toast.LENGTH_SHORT).show();
				usbmanager.requestPermission(accessory, mPermissionIntent);
				mPermissionRequestPending = true;
			}
		}

		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void OnPause(){}

	private void DestroyAccessory(boolean bConfiged){
		if(true == bConfiged){
			READ_ENABLE = false;  // set false condition for handler_thread to exit waiting data loop
		}
		else
		{
			SetConfig(9600,(byte)7,(byte)1,(byte)2,(byte)0);  // send default setting data for config
			try{Thread.sleep(10);}
			catch(Exception e){}

			READ_ENABLE = false;  // set false condition for handler_thread to exit waiting data loop
			if(true == accessory_attached)
			{
				saveDefaultPreference();
			}
		}

		try{Thread.sleep(10);}
		catch(Exception e){}
		CloseAccessory();
	}

	/*********************helper routines*************************************************/

	private void OpenAccessory(UsbAccessory accessory)
	{
		filedescriptor = usbmanager.openAccessory(accessory);
		if(filedescriptor != null){
			usbaccessory = accessory;

			FileDescriptor fd = filedescriptor.getFileDescriptor();

			inputstream = new FileInputStream(fd);
			outputstream = new FileOutputStream(fd);
			/*check if any of them are null*/
			if(inputstream == null || outputstream==null){
				return;
			}

			if(!READ_ENABLE){
				READ_ENABLE = true;
				readThread = new read_thread(inputstream);
				readThread.start();
			}
		}
	}

	private void CloseAccessory()
	{
		try {
			if (filedescriptor != null)
				filedescriptor.close();
		} catch (IOException e) {}

		try {
			if (inputstream != null)
				inputstream.close();
		} catch(IOException e) {}

		try {
			if (outputstream != null) {
				outputstream.flush();
				outputstream.close();
			}
		} catch(IOException e) {}

		filedescriptor = null;
		inputstream = null;
		outputstream = null;

		System.exit(0);
	}

	protected void saveDetachPreference() {
		if(intsharePrefSettings != null)
		{
			intsharePrefSettings.edit()
					.putString("configed", "FALSE")
					.commit();
		}
	}

	protected void saveDefaultPreference() {
		if(intsharePrefSettings != null)
		{
			intsharePrefSettings.edit().putString("configed", "TRUE").commit();
			intsharePrefSettings.edit().putInt("baudRate", 9600).commit();
			intsharePrefSettings.edit().putInt("stopBit", 1).commit();
			intsharePrefSettings.edit().putInt("dataBit", 7).commit();
			intsharePrefSettings.edit().putInt("parity", 2).commit();
			intsharePrefSettings.edit().putInt("flowControl", 0).commit();
		}
	}

	/***********USB broadcast receiver*******************************************/
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action))
			{
				synchronized (this)
				{
					UsbAccessory accessory = null;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
						accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY, UsbAccessory.class);
					} else {
						accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					}
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
					{
						Toast.makeText(global_context, "Allow USB Permission", Toast.LENGTH_SHORT).show();
						if(accessory != null){
							OpenAccessory(accessory);
						}
					}
					else
					{
						Toast.makeText(global_context, "Deny USB Permission", Toast.LENGTH_SHORT).show();
						Log.d("LED", "permission denied for accessory "+ accessory);
					}
					mPermissionRequestPending = false;
				}
			}
			else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action))
			{
				saveDetachPreference();
				DestroyAccessory(true);
				//CloseAccessory();
			}
			else if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)){
				UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if(accessory != null){
					if(usbmanager.hasPermission(accessory)){
						OpenAccessory(accessory);
					} else {
						usbmanager.requestPermission(accessory, mPermissionIntent);
					}
				}
			}
			else
			{
				Log.d("LED", "....");
			}
		}
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] Read(long l) {
		status = 0x01;
		int[] actualNumBytes = new int[1];
		int attemtps = 0;
		while (status != 0x00 && attemtps < 4) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {}
			status = ReadData((int) l, readBuffer, actualNumBytes);
			attemtps++;
		}
		byte[] buffer = new byte[actualNumBytes[0]];
		for(int i=0; i<actualNumBytes[0]; i++) {
			buffer[i] = readBuffer[i];
		}
		return buffer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void Write(byte[] bytes) {
		int numBytes = bytes.length;
		if(numBytes < 1){
			return;
		}

		if(numBytes > 256){
			numBytes = 256;
		}

		for(int count = 0;count<numBytes;count++)
		{
			writeusbdata[count] = bytes[count];
		}

		if(numBytes != 64)
		{
			SendPacket(numBytes);
		}
		else
		{
			byte temp = writeusbdata[63];
			SendPacket(63);
			writeusbdata[0] = temp;
			SendPacket(1);
		}
	}

	/**
	 * {@inheritDoc}
	 * @implNote Closing the connection also closes the application
	 */
	@Override
	public void close() {
		DestroyAccessory(true);
	}

	/*usb input data handler*/
	private class read_thread extends Thread
	{
		FileInputStream instream;

		read_thread(FileInputStream stream ){
			instream = stream;
			this.setPriority(Thread.MAX_PRIORITY);
		}

		public void run() {
			while (READ_ENABLE) {
				while (totalBytes > (maxnumbytes - 1024)) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				try {
					if (instream != null) {
						readcount = instream.read(usbdata, 0, 1024);
						if (readcount > 0) {
							for (int count = 0; count < readcount; count++) {
								readBuffer[writeIndex] = usbdata[count];
								writeIndex++;
								writeIndex %= maxnumbytes;
							}
							if (writeIndex >= readIndex)
								totalBytes = writeIndex - readIndex;
							else
								totalBytes = (maxnumbytes - readIndex) + writeIndex;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					_logger.LogError(e.getMessage());
				}
			}
		}
	}
}