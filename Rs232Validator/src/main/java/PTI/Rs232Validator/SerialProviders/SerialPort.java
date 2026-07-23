//User must modify the below package with their package name
package PTI.Rs232Validator.SerialProviders;

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
import PTI.Rs232Validator.Loggers.NullLogger;


/**
 * An implementation of {@link ISerialProvider} that connects to the FTDI Chip
 */
public class SerialPort extends Activity
{

	/**
	 * String for requesting USB permission
	 */
	private static String ACTION_USB_PERMISSION;

	/**
	 * Manager for handling USB connections
	 */
	public UsbManager usbmanager;

	/**
	 * The USB accessory representing the connected device
	 */
	public UsbAccessory usbaccessory;

	/**
	 * Pending intent used for requesting USB permission from the user
	 */
	public PendingIntent mPermissionIntent;

	/**
	 * File descriptor for communicating with the USB accessory
	 */
	public ParcelFileDescriptor filedescriptor = null;

	/**
	 * Input stream for reading data from the USB accessory
	 */
	public FileInputStream inputstream;

	/**
	 * Output stream for writing data to the USB accessory
	 */
	public FileOutputStream outputstream;

	/**
	 * Flag to indicate whether a USB permission request is currently pending
	 */
	public boolean mPermissionRequestPending = false;

	/**
	 * Thread for reading data from the USB accessory
	 */
	public read_thread readThread;


	/**
	 * Data from the USB accessory
	 */
	private byte [] usbdata;

	/**
	 * Buffer for storing data to be written to the USB accessory
	 */
	private byte []	writeusbdata;

	/**
	 * Circular buffer for storing data from the USB accessory from usbdata buffer
	 */
	private byte  [] readBuffer; /*circular buffer*/

	/**
	 * The number of bytes read from the USB accessory
	 */
	private int readcount;

	/**
	 * The total number of bytes currently stored in the {@link #readBuffer} that have not been read
	 */
	private int totalBytes;

	/**
	 * Index for writing data to {@link #readBuffer}
	 */
	private int writeIndex;

	/**
	 * Index for reading data from {@link #readBuffer}
	 */
	private int readIndex;

	/**
	 * Status byte for indicating the status of send and read operations (0 for success, 1 for error)
	 */
	private byte status;

	/**
	 * The maximum number of bytes that can be stored in the {@link #readBuffer}
	 */
	final int  maxnumbytes = 65536;

	/**
	 * Flag to indicate whether reading from the USB accessory is currently enabled
	 */
	public boolean READ_ENABLE = false;
	/**
	 * Flag to indicate whether a USB accessory is currently attached
	 */
	public boolean accessory_attached = false;

	/**
	 * The global context of the application, used for accessing system services and resources
	 */
	public Context global_context;


	/**
	 * String for identifying the manufacturer of the USB accessory (FTDI)
	 */
	public static String ManufacturerString = "mManufacturer=FTDI";

	/**
	 * String for identifying the model of the USB accessory
	 */
	public static String ModelString1 = "mModel=FTDIUARTDemo";

	/**
	 * String for identifying the model of the USB accessory
	 */
	public static String ModelString2 = "mModel=Android Accessory FT312D";

	/**
	 * String for identifying the version of the USB accessory
	 */
	public static String VersionString = "mVersion=1.0";

	/**
	 * Flag to indicate what settings the FT311 chip is currently configured with.
	 * <p>0: not configured,</p>
	 * <p>1: Configured with default Phoenix settings (9600, 8, 1, 0, 0),</p>
	 * <p>2: Configured with default Reliance settings (19200, 8, 1, 0, 0)</p>
	 */
	public byte isConfiged = 0;

	public ILogger logger = new NullLogger();

	/**
	 * Constructor for the BaseSerialPort class. The SharedPreferences object will save config settings
	 * when the accessory is detached. If the SharedPreferences object is null, the settings will not be saved.
	 * @param context The context of the application
	 */
	public SerialPort(Context context){
		super();
		global_context = context;
		ACTION_USB_PERMISSION = context.getPackageName() + ".USB_PERMISSION";

		usbdata = new byte[1024];
		writeusbdata = new byte[256];
		readBuffer = new byte [maxnumbytes];


		readIndex = 0;
		writeIndex = 0;
		/***********************USB handling******************************************/

		usbmanager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		// Log.d("LED", "usbmanager" +usbmanager);

		if(Build.VERSION.SDK_INT>=0x17){
			mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0 | (PendingIntent.FLAG_IMMUTABLE));
		}else {
			mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
		}
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			context.registerReceiver(mUsbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
		} else {
			context.registerReceiver(mUsbReceiver, filter);
		}

		inputstream = null;
		outputstream = null;
	}

	/**
	 * Configures the serial port connections used by the FT311 chip
	 * @param baud Baud Rate of the connection
	 * @param dataBits Number of data bits in the connection
	 * @param stopBits Number of stop bits in the connection
	 * @param parity Parity setting for the connection
	 * @param flowControl Flow control setting for the connection
	 */
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


	/**
	 * Sends data to the USB accessory through the FT311 chip. If the number of bytes to send exceeds 256,
	 * the data will be sent in multiple packets with a delay between each packet to avoid overwhelming the chip.
	 * @param numBytes The number of bytes to send
	 * @param buffer The buffer containing the data to send
	 * @return A byte indicating the status of the send operation (0 for success, 1 for error)
	 */
	public byte Write(int numBytes, byte[] buffer)
	{
		status = 0x00; /*success by default*/
		/*
		 * if num bytes are more than maximum limit
		 */
		if(numBytes < 1){
			/*return the status with the error in the command*/
			return status;
		}

		/*
		 * FTDI Chip can only handle 256 bytes of data at a time, so if the data is more than 256 bytes, we need to send it in multiple packets
		 * and add some delay between the packets to avoid overwhelming the chip
		 */
		if(numBytes > 256) {
			int packets = (numBytes / 256) + 1;

			for (int count = 0; count < packets; count++) {
				int offset = count * 256;
				int bytesToSend = Math.min(256, numBytes - offset);
				if (bytesToSend >= 0)
					System.arraycopy(buffer, offset, writeusbdata, 0, bytesToSend);
				SendPacket(bytesToSend);
				try{
					Thread.sleep(10);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			return status;
		}

		/*prepare the packet to be sent*/
		System.arraycopy(buffer, 0, writeusbdata, 0, numBytes);

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

		return status;
	}

	/**
	 * Reads a number of bytes of data from the readBuffer
	 * @param numBytes The number of bytes to read from the readBuffer
	 * @param buffer The buffer to store the read data
	 * @param actualNumBytes An array to store the actual number of bytes read from the readBuffer
	 * @return A byte indicating the status of the read operation (0 for success, 1 for error)
	 */
	public byte Read(int numBytes,byte[] buffer, int [] actualNumBytes)
	{
		status = 0x00; /*success by default*/

		/*should be at least one byte to read*/
		if((numBytes < 1) || (totalBytes == 0)){
			actualNumBytes[0] = 0;
			status = 0x01;
			return status;
		}

		/*check for max limit*/
		if(numBytes > totalBytes)
			numBytes = totalBytes;

		/*update the number of bytes available*/
		totalBytes -= numBytes;

		actualNumBytes[0] = numBytes;

		/*copy to the user buffer*/
		for(int count = 0; count<numBytes;count++)
		{
			buffer[count] = readBuffer[readIndex];
			readIndex++;
			/*shouldnt read more than what is there in the buffer,
			 * 	so no need to check the overflow
			 */
			readIndex %= maxnumbytes;
		}
		return status;
	}

	/**
	 * Sends a packet of data to the USB accessory through the FT311 chip
	 * @param numBytes The number of bytes to send from the writeusbdata buffer
	 */
	private void SendPacket(int numBytes)
	{
		try {
			if(outputstream != null){
				outputstream.write(writeusbdata, 0,numBytes);
			} else{
				logger.LogTrace("Output stream is null, cannot send packet");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * resume accessory
	 */
	public int ResumeAccessory()
	{
		// Intent intent = getIntent();
		if (inputstream != null && outputstream != null) {
			return 1;
		}

		UsbAccessory[] accessories = usbmanager.getAccessoryList();
		if(accessories != null)
		{
			Toast.makeText(global_context, "Accessory Attached", Toast.LENGTH_SHORT).show();
		}
		else
		{
			// return 2 for accessory detached case
			//Log.e(">>@@","ResumeAccessory RETURN 2 (accessories == null)");
			accessory_attached = false;
			return 2;
		}

		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			/*if(!accessory.toString().contains(ManufacturerString))
			{
				Toast.makeText(global_context, "Manufacturer is not matched!", Toast.LENGTH_SHORT).show();
				return 1;
			}

			if(!accessory.toString().contains(ModelString1) && !accessory.toString().contains(ModelString2))
			{
				Toast.makeText(global_context, "Model is not matched!", Toast.LENGTH_SHORT).show();
				return 1;
			}

			if(!accessory.toString().contains(VersionString))
			{
				Toast.makeText(global_context, "Version is not matched!", Toast.LENGTH_SHORT).show();
				return 1;
			}

			Toast.makeText(global_context, "Manufacturer, Model & Version are matched!", Toast.LENGTH_SHORT).show();*/
			accessory_attached = true;

			if (usbmanager.hasPermission(accessory)) {
				OpenAccessory(accessory);
			}
			else
			{
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						Toast.makeText(global_context, "Request USB Permission", Toast.LENGTH_SHORT).show();
						usbmanager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		}

		return 0;
	}

	/**
	 * destroy accessory
	 */
	public void DestroyAccessory(boolean bConfiged){

		if(bConfiged){
			READ_ENABLE = false;  // set false condition for handler_thread to exit waiting data loop
			writeusbdata[0] = 0;  // send dummy data for instream.read going
			SendPacket(1);
		}
		else
		{
			SetConfig(9600,(byte)8,(byte)1,(byte)0,(byte)0);  // send default setting data for config
			try{Thread.sleep(10);}
			catch(Exception e){}

			READ_ENABLE = false;  // set false condition for handler_thread to exit waiting data loop
			writeusbdata[0] = 0;  // send dummy data for instream.read going
			SendPacket(1);
		}

		try{Thread.sleep(10);}
		catch(Exception e){}
		CloseAccessory();
	}

	/*********************helper routines*************************************************/

	/**
	 * Opens a connection to the USB accessory and initializes the input and output streams for communication.
	 * @param accessory The USB accessory to connect to
	 */
	public void OpenAccessory(UsbAccessory accessory)
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

			SetConfig(9600,(byte)7,(byte)1,(byte)2,(byte)0);
			Toast.makeText(global_context, "Device configured!", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Closes the connection to the USB accessory and cleans up resources. This method also exits the application.
	 */
	private void CloseAccessory()
	{
		try{
			if(filedescriptor != null)
				filedescriptor.close();

		}catch (IOException e){}

		try {
			if(inputstream != null)
				inputstream.close();
		} catch(IOException e){}

		try {
			if(outputstream != null)
				outputstream.close();

		}catch(IOException e){}

		filedescriptor = null;
		inputstream = null;
		outputstream = null;
		isConfiged = 0;
		if (global_context instanceof Activity) {
			((Activity) global_context).finish();
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
					if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
						accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY, UsbAccessory.class);
					} else {
						accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					}
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
					{
						Toast.makeText(global_context, "Allow USB Permission", Toast.LENGTH_SHORT).show();
						OpenAccessory(accessory);
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
				DestroyAccessory(true);
			}else
			{
				Log.d("LED", "....");
			}
		}
	};

	/**
	 * Thread class for continuous reading of data from the USB accessory.
	 */
	private class read_thread  extends Thread
	{
		FileInputStream instream;

		/**
		 * Constructor for the read_thread class
		 * @param stream The FileInputStream to read data from the USB accessory
		 */
		read_thread(FileInputStream stream ){
			instream = stream;
			this.setPriority(Thread.MAX_PRIORITY);
		}

		/**
		 * Continuously reads data from the USB accessory and stores it in the readBuffer circular buffer until reading is disabled.
		 */
		public void run()
		{
			while(READ_ENABLE)
			{
				while(totalBytes > (maxnumbytes - 1024))
				{
					try
					{
						Thread.sleep(50);
					}
					catch (InterruptedException e) {e.printStackTrace();}
				}

				try
				{
					if(instream != null)
					{
						readcount = instream.read(usbdata,0,1024);
						if(readcount > 0)
						{
							for(int count = 0;count<readcount;count++)
							{
								readBuffer[writeIndex] = usbdata[count];
								writeIndex++;
								writeIndex %= maxnumbytes;
							}

							if(writeIndex >= readIndex)
								totalBytes = writeIndex-readIndex;
							else
								totalBytes = (maxnumbytes-readIndex)+writeIndex;

//					    		Log.e(">>@@","totalBytes:"+totalBytes);
						}
					}
				}
				catch (IOException e){e.printStackTrace();}
			}
		}
	}
}