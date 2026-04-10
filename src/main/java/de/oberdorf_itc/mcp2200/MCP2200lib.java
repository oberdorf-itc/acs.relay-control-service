package de.oberdorf_itc.mcp2200;

// libraries
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.InputReportListener;
import purejavahidapi.PureJavaHidApi;

/**
 * Class MCP2200lib - Public class to communicate with MCP2200 chips over USB, using the "PureJavaHidApi"<br>
 * Copyright by Michael Oberdorf IT-Consulting 2015,<br>
 * Date: 2016-07-21<br>
 * MCP2200 - USB2Serial converter processor library<br>
 * to communicate with the Denkovi USB Relay USB4v2<br>
 * the original library was taken from the Denkovi Java example<br>
 * @version 0.105
 * @see "http://www.oberdorf-itc.de/"
 * @see "https://denkovi.com/usb-relay-board-four-channels-for-home-automation-v2"
 * @see "http://denkovi.com/SoftwareExamples/usb4relay-v2/java/USB4v2_MCP2200.rar"
 * @see "https://github.com/nyholku/purejavahidapi"
 */
public class MCP2200lib
  {
  private final static Logger logger = LoggerFactory.getLogger(de.oberdorf_itc.mcp2200.MCP2200lib.class);
  // constants
  private static final short VENDOR_ID     = 0x04D8;
  private static final short PRODUCT_ID    = 0x00DF;
  private static final short SET_CLEAR_OUT = 0x08;
  private static final short CONFIGURE     = 0x10;
  private static final short READ_EE       = 0x20;
  private static final short WRITE_EE      = 0x40;
  private static final short READ_ALL      = 0x80;
  private static final int   HID_OK        = 0;
  private static final int   HID_ERROR     = 1;
  private static final int   READ          = 0;
  private static final int   WRITE         = 1;

  // default variables
  mcp2200_Packet            data          = new mcp2200_Packet();
  private  HidDevice        openedDevice;
  boolean                   ifready       = false;
  private byte[]            buf           = new byte[16];
  List<HidDeviceInfo>       devList;
  HidDeviceInfo             devInfo;

  /**
   * Interface of a MCP2200 communication package
   */
  private class mcp2200_Packet
    {
	byte CommandOpcode;
    byte eepAddr;
    byte eepReadVal;
    byte eepWriteVal;
    byte ioBmap;
    byte configAltPins;
    byte ioDefaultValBmap;
    byte configAllOptions;
    byte baudH;
    byte baudL;
    byte ioPortValBmap;
    byte setBmap;
    byte clearBmap;
    }

  /**
   * list_Vendor_Product - Locate all USB devices by VENDOR_ID and PRODUCT_ID
   * @param VENDOR_ID (number of the manufacturer)
   * @param PRODUCT_ID (Product Number)
   * @return List&lt;HidDeviceInfo&gt; (returns a list, arranged in order of increasing size of getSerialNumber String type HidDeviceInfo)
   */
  public List<HidDeviceInfo> list_Vendor_Product(int VENDOR_ID, int PRODUCT_ID)
    {
    logger.trace("Method list_Vendor_Product(int VENDORID={}, int PRODUCT_ID={})", VENDOR_ID, PRODUCT_ID);
    // get all HID devices
    logger.trace("List all HID devices");
    List <HidDeviceInfo> res = PureJavaHidApi.enumerateDevices();
    // define ArrayList of filtered HID devices
    List <HidDeviceInfo> res2 = new ArrayList<HidDeviceInfo>();

    // loop over HID devices
    logger.trace("Loop over detected HID devices and filter them by VendorID={} and ProductID={}", VENDOR_ID, PRODUCT_ID);
    for (HidDeviceInfo info : res)
      {
      logger.trace("Process device VendorID={}, ProductID={}", info.getVendorId(), info.getProductId());
      // check if the HID device is from given Vendor and Product and save device in new array res2
      if (info.getVendorId() == (short)VENDOR_ID && info.getProductId()== (short)PRODUCT_ID)
    	{
    	logger.trace("Device matches filter, add to array.");
    	res2.add(info);
    	}

      // sort the device list
      logger.trace("Sort list");
      Collections.sort(res2, new Comparator<HidDeviceInfo>()
    	{
    	public int compare(HidDeviceInfo o1, HidDeviceInfo o2)
    	  {
    	  return o1.getSerialNumberString().compareToIgnoreCase(o2.getSerialNumberString());
    	  }
    	});
   	  }
    logger.trace("Return device list: {}", res2);
    return res2;
    }

  /**
   * sendUSBPacket - Sends an USB packet and waits for a response
   * @param Read_Write (read (0) or write (1) the data)
   * @param in (data to send)
   * @return mcp2200_Packet (received data or null if none)
   * @throws Exception if the device can't be opened for communication
   */
  private mcp2200_Packet sendUSBPacket(int Read_Write, byte[] in) throws Exception
    {
    logger.trace("Method sendUSBPacket(int Read_Write={}, byte[] in={})", Read_Write, in);
    logger.trace("trigger openeddevice.setOutputReport");
    int res = openedDevice.setOutputReport((byte)0,in,in.length);
    if (Read_Write == READ)
      {
      logger.trace("READ mode - waiting till device is ready.");
      while (!ifready)
    	{
    	Thread.sleep(1);
    	}
      logger.trace("Device is ready.");
      }
    else { logger.trace("WRITE mode"); }
    logger.trace("Check result: {}", res);
    if (res!=16)
      {
      logger.error("Result: {}", res);
      throw new Exception();
      }
    logger.trace("Success - store data in class mcp2200_Packet and return data.");
    mcp2200_Packet response = new mcp2200_Packet();
    response.CommandOpcode=buf[0];
    logger.trace("CommandOpcode={}", response.CommandOpcode);
    response.eepAddr=buf[1];
    logger.trace("eepAddr={}", response.eepAddr);
    response.eepWriteVal=buf[2];
    logger.trace("eepWriteVal={}", response.eepWriteVal);
    response.eepReadVal=buf[3];
    logger.trace("eepReadVal={}", response.eepReadVal);
    response.ioBmap=buf[4];
    logger.trace("ioBmap={}", response.ioBmap);
    response.configAltPins=buf[5];
    logger.trace("configAltPins={}", response.configAltPins);
    response.ioDefaultValBmap=buf[6];
    logger.trace("ioDefaultValBmap={}", response.ioDefaultValBmap);
    response.configAllOptions=buf[7];
    logger.trace("configAllOptions={}", response.configAllOptions);
    response.baudH=buf[8];
    logger.trace("baudH={}", response.baudH);
    response.baudL=buf[9];
    logger.trace("baudL={}", response.baudL);
    response.ioPortValBmap=buf[10];
    logger.trace("ioPortValBmap={}", response.ioPortValBmap);
    return response;
    }

  /**
   * SET_CLEAR_OUTPUTS - Command output
   * @param in (mcp2200_Packet)
   * @return status (HID_ERROR (1) on error event or HID_OK (0) on success)
   */
  @SuppressWarnings("unused")
  private int SET_CLEAR_OUTPUTS(mcp2200_Packet in)
    {
    logger.trace("Method SET_CLEAR_OUTPUTS(mcp2200_Packet in={})", in);
    mcp2200_Packet buf = new mcp2200_Packet();
    try
      {
      logger.trace("Try to send command to write data to EEPROM. SET_CLEAR_OUT={}, ioBmap={}, clearBmap={}", SET_CLEAR_OUT, in.ioBmap, in.clearBmap);
      buf = sendUSBPacket (WRITE, new byte[] {
											(byte) SET_CLEAR_OUT,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
							                in.setBmap,
							                in.clearBmap
							                }
										);
      }
    catch (Exception e) { logger.error(e.toString()); return HID_ERROR; }
    return HID_OK;
    }

  /**
   * CONFIGURE - Command to CONFIGURE
   * @param in (mcp2200_Packet)
   * @return status (HID_ERROR (1) on error event or HID_OK (0) on success)
   */
  private int CONFIGURE(mcp2200_Packet in)
    {
    logger.trace("Method CONFIGURE(mcp2200_Packet in={})", in);
    @SuppressWarnings("unused")
    mcp2200_Packet buf = new mcp2200_Packet();
    try
      {
      logger.trace("Try to send command to write data to EEPROM. CONFIGURE={}, ioBmap={}, configAltPins={}, ioDefaultValBmap={}, configAllOptions={}, baudH={}, baudL={}", CONFIGURE, in.ioBmap, in.configAltPins, in.ioDefaultValBmap, in.configAllOptions, in.baudH, in.baudL);
      buf = sendUSBPacket (WRITE, new byte[] {
											(byte) CONFIGURE,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											in.ioBmap,
											in.configAltPins,
											in.ioDefaultValBmap,
											in.configAllOptions,
        									in.baudH,
        									in.baudL,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00
        									}
        								);
      }
    catch (Exception e) { logger.error(e.toString()); return HID_ERROR; }
    return HID_OK;
    }

  /**
   * READ_EE - Command for reading EEPROM
   * @param in (mcp2200_Packet, Address reading)
   * @return status (HID_ERROR (1) on error event or the returned value on success)
   */
  @SuppressWarnings("unused")
  private byte READ_EE(mcp2200_Packet in)
    {
    logger.trace("Method READ_EE(mcp2200_Packet in={})", in);
    mcp2200_Packet buf = new mcp2200_Packet();
    try
      {
      logger.trace("Try to send command to read data from EEPROM. READ_EE={}, eepAddr={}", READ_EE, in.eepAddr);
      buf = sendUSBPacket(READ, new byte[] {
        									(byte) READ_EE,
        									in.eepAddr,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00,
        									(byte) 0x00
                							}
        								);
      }
    catch (Exception e) { logger.error(e.toString()); return (byte) HID_ERROR; }
    logger.trace("Return eepReadVal={}", buf.eepReadVal);
    return buf.eepReadVal;
    }

  /**
   * WRITE_EE - Command for writing EEPROM
   * @param in (mcp2200_Packet, Data to write)
   * @return status (HID_ERROR (1) on error event or HID_OK (0) on success)
   */
  @SuppressWarnings("unused")
  private int WRITE_EE(mcp2200_Packet in)
    {
    logger.trace("Method WRITE_EE(mcp2200_Packet in={})", in);
    mcp2200_Packet buf = new mcp2200_Packet();
    try
      {
      logger.trace("Try to send command to write configuration data to EEPROM. WRITE_EE={}, eepAddr={}, eepWriteVal={}", WRITE_EE, in.eepAddr, in.eepWriteVal);
	  buf = sendUSBPacket(WRITE, new byte[] {
											(byte) WRITE_EE,
											in.eepAddr,
											in.eepWriteVal
											}
										);
      }
    catch (Exception e) { logger.error(e.toString()); return HID_ERROR; }
    return HID_OK;
    }

  /**
   * READ_ALL - Command for reading all the data
   * @return mcp2200_Packet (data or null)
   */
  private mcp2200_Packet READ_ALL()
   	{
    logger.trace("Method READ_ALL()");
    mcp2200_Packet buf = new mcp2200_Packet();
    try
      {
      logger.trace("Try to send command to read all configuration data from EEPROM. READ_ALL={}", READ_ALL);
      buf = sendUSBPacket(READ, new byte[] {
											(byte) READ_ALL,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00,
											(byte) 0x00
											}
										);
      }
    catch (Exception e) { logger.error(e.toString()); return null; }
    logger.trace("Return {}", buf);
    return buf;
    }

  /**
   * openBySerialID - Open connection to USB device, given by serial ID
   * @param serID (serial ID to identify USB device)
   * @return state (HID_ERROR (1) on error event or HID_OK (0) on success)
   */
  public int openBySerialID(String serID)
   	{
   	logger.debug("Method openBySerialID(String serID={})", serID);
    List<HidDeviceInfo> devList;
    HidDeviceInfo devInfo;
    boolean havedevice;
    try
      {
      // get device list connected by USB
      logger.trace("Try to get connected devices.");
      devList = list_Vendor_Product(VENDOR_ID, PRODUCT_ID);
      devInfo = null;
      havedevice=false;
      // loop over device list and try to identify device by serial ID
      logger.trace("Loop over device list and try to get device information from device.");
      for (HidDeviceInfo info : devList)
        {
        logger.trace("Check if device: {} matches {}", info.getSerialNumberString(), serID);
        if (info.getSerialNumberString().equalsIgnoreCase(serID))
          {
          logger.trace("Device found: {}", info.getSerialNumberString());
          devInfo = info;
          havedevice=true;
          break;
          }
        }
      if (devInfo == null || !havedevice) { logger.error("Device {} not found, seems it is not connected.", serID); return HID_ERROR; }
      else
    	{
        logger.trace("Device information: {}", devInfo);
        // open USB connection to the device
        String OS = System.getProperty("os.name");
        logger.trace("Identify Operating System: {} to open the device in the specific method.", OS);
        if (OS.toUpperCase().contains("WIN"))
          {
          logger.trace("Open USB Device {} with timeout 10", devInfo.getPath());
          logger.warn("Using deprecated method: PureJavaHidApi.openDeviceNonBlocking");
          openedDevice = PureJavaHidApi.openDevice(devInfo); // PureJavaHidApi v0.0.10
          //openedDevice = PureJavaHidApi.openDeviceNonBlocking(devInfo.getPath(),10); // PureJavaHidApi v0.0.1
          }
        else
          {
          logger.trace("Open USB Device {}", devInfo.getPath());
          openedDevice = PureJavaHidApi.openDevice(devInfo); // PureJavaHidApi v0.0.10
          //openedDevice = PureJavaHidApi.openDevice(devInfo.getPath());  // PureJavaHidApi v0.0.1
          }
        logger.trace("Activate the input report listener", OS);
        openedDevice.setInputReportListener(new InputReportListener()
          {
          @Override
          public void onInputReport(HidDevice source, byte Id, byte[] data, int len)
            {
            // change flag in order for waiting loop to finish
            ifready = true;
            buf = data;
            }
          });
        }
      }
    catch (Exception e) { logger.error(e.toString()); return HID_ERROR; }

    // read all data from EEPROM of MCP2200 device and save it in class variable data
    data = READ_ALL();
    return HID_OK;
    }

  /**
   * connectToFirstDevice - Open the first matching USB device
   * @return state (HID_ERROR (1) on error event or HID_OK (0) on success)
   */
  public int connectToFirstDevice()
    {
    logger.debug("Method connectToFirstDevice()");
    // return with error if already connected
    if (connectedToDevice() == true) { logger.warn("Device already connected"); return HID_ERROR; }
    HidDeviceInfo devInfo = null;
    logger.trace("Try to get connected devices.");
    List<HidDeviceInfo> devList = list_Vendor_Product(VENDOR_ID, PRODUCT_ID);
    logger.trace("Found {} connected devices.", devList.size());
    try
      {
      if (devList.size() >= 1)
        {
        logger.trace("Try to get device information from first device");
        devInfo = devList.get(0);
        }
      }
    catch (Exception e) { logger.error(e.toString()); return HID_ERROR; }
    if (devInfo == null)
      {
      logger.error("No device information found for the first device.");
      return HID_ERROR;
      }
    logger.trace("Try to open device by serial number {}", devInfo.getSerialNumberString());
    return openBySerialID(devInfo.getSerialNumberString());
    }

  /**
   * openByID - Open connection to USB device, given by ID
   * @param myID (USB ID to identify USB device)
   * @return status (HID_ERROR (1) on error event or HID_OK (0) on success)
   */
  public int openByID(int myID)
    {
    logger.debug("Method openByID(int myID={})", myID);
    List<HidDeviceInfo> devList;
    HidDeviceInfo devInfo;
    try
      {
      // get device list connected by USB
      logger.trace("Try to get connected devices.");
      devList = list_Vendor_Product(VENDOR_ID, PRODUCT_ID);
      devInfo = null;
      logger.trace("Try to get device information from device ID={}", myID);
      devInfo = devList.get(myID);
      if (devInfo == null)
        {
        logger.error("No device information found for ID={}", myID);
        return HID_ERROR;
        }
      else
       	{
        logger.trace("Device information: {}", devInfo);
        // open USB connection to the device
        String OS = System.getProperty("os.name");
        logger.trace("Identify Operating System: {} to open the device in the specific method.", OS);
        if (OS.toUpperCase().contains("WIN"))
          {
          logger.trace("Open USB Device {} with timeout 10", devInfo.getPath());
          logger.warn("Using deprecated method: PureJavaHidApi.openDeviceNonBlocking");
          openedDevice = PureJavaHidApi.openDevice(devInfo); // PureJavaHidApi v0.0.10
          // openedDevice = PureJavaHidApi.openDeviceNonBlocking(devInfo.getPath(),10); // PureJavaHidApi v0.0.1
          }
        else
          {
          logger.trace("Open USB Device {}", devInfo.getPath());
          openedDevice = PureJavaHidApi.openDevice(devInfo); // PureJavaHidApi v0.0.10
          // openedDevice = PureJavaHidApi.openDevice(devInfo.getPath());  // PureJavaHidApi v0.0.1
          }
        logger.trace("Activate the input report listener", OS);
        openedDevice.setInputReportListener(new InputReportListener()
          {
          @Override
          public void onInputReport(HidDevice source, byte Id, byte[] data, int len)
            {
            // change flag in order for waiting loop to finish
            ifready = true;
            buf = data;
            }
          });
       	}
      }
    catch (Exception e) { logger.error(e.toString()); return HID_ERROR;	}

    // read all data from EEPROM of MCP2200 device and save it in class variable data
    data = READ_ALL();
    return HID_OK;
  	}

  /**
   * close - Close connection from USB device
   * @return status (HID_ERROR (1) on error event or HID_OK (0) on success)
   */
  public int close()
    {
    logger.trace("Method close()");
    // close if openedDevice is initialized
    if (connectedToDevice() == true)
      {
      logger.trace("Device connected - trigger close");
      openedDevice.close();
      }
    else { logger.trace("Device not connected - skip close"); }
    return HID_OK;
    }

  /**
   * WriteOutputs - Trigger USB relays by writing to USB device
   * @param state (byte that represents the bits to trigger relayN)
   * @return status (HID_ERROR (1) on error event or HID_OK (0) on success)
   */
  private int WriteOutputs(byte state)
    {
    logger.trace("Method WriteOutputs(byte state={})", state);
    // set the bits
    logger.trace("Set ioDefaultValBmap={}", state);
    data.ioDefaultValBmap=state;
    // send it to device
    logger.trace("Trigger CONFIGURE");
    return CONFIGURE(data);
    }

  /**
   * ReadIO - Read the state of the USB relays (from class cache)
   * @return state (bits or HID_ERROR (1) if the configuration is not initialized in this class)
   */
  private byte ReadIO()
    {
    logger.trace("Method ReadIO()");
    if (data!=null)
      {
      logger.trace("Return ioDefaultValBmap={} from cached data.", data.ioDefaultValBmap);
      return data.ioDefaultValBmap;
      }
    logger.error("There is no cached data to return, assume that the device is not connected!");
    return HID_ERROR;
    }

  /**
   * WriteIODirection - unknown function
   * @param state (unknown)
   * @return status (HID_ERROR (1) on error event or HID_OK (0) on success)
   */
  @SuppressWarnings("unused")
  private int WriteIODirection(byte state)
   	{
   	logger.trace("Method WriteIODirection(byte state={})", state);
    if (data!=null)
   	  {
      logger.trace("Overwrite in cached data ioBmap={}", state);
      data.ioBmap = state;
      logger.trace("Trigger CONFIGURE");
      return CONFIGURE(data);
      }
    logger.error("There is no cached data, assume that the device is not connected!");
    return HID_ERROR;
    }

  /**
   * setSingleRelayState - Switch a single relay to on or off state
   * @param RelayN (integer, number of the relay [1..4])
   * @param State (boolean, the state to switch: on = true; off = false)
   * @return int (HID_ERROR (1) on error event or HID_OK (0) on success)
   */
  public int setSingleRelayState(int RelayN, boolean State)
   	{
    logger.trace("Method setSingleRelayState(int RelayN={}, boolean State={})", RelayN, State);
    try
      {
      // get the actual relay state
      byte io = ReadIO();
      logger.trace("Get actual relay state: {}", io);
      // generate a bitmask from requested relay number
      byte mask = (byte)(0b00000001 << (RelayN-1));
      logger.trace("generated mask: {}", mask);

      // dependent on requested state, process bitmask on relay state
      if (State==false) { io = (byte)(io & ~mask); }
      else { io = (byte)(io | mask); }

      // write to USB device
      logger.trace("Try to write generated output: {}", io);
      if (WriteOutputs(io)!=HID_ERROR)
        {
        logger.trace("Write was successful");
        // return new StringBuilder(String.format("%8s", Integer.toBinaryString(io & 0xFF)).replace(' ', '0')).reverse().toString();
        return HID_OK;
        }
      logger.error("Writing relay state was not successful!");
      return HID_ERROR;
      }
    catch (Exception e) { logger.error(e.toString()); return HID_ERROR; }
    }


  /**
   * setSingleRelayFlipFlop - Flip-Flop a single relay
   * @param RelayN (integer, number of the relay [1..4])
   * @param state (boolean, the first state to trigger: on = true; off = false)
   * @param sleepTime (integer, time in milliseconds to wait before switch the state)
   * @return int (HID_ERROR (1) on error event or HID_OK (0) on success)
   */
  public int setSingleRelayFlipFlop(int RelayN, boolean state, int sleepTime)
  	{
    logger.debug("Method setSingleRelayFlipFlop(int RelayN={}, boolean State={}, int sleepTime={})", RelayN, state, sleepTime);
    boolean otherState = true;
    int RES = 0;
    if (state == true) { otherState = false; }

    // set the first state flip
    logger.trace("Set relay no. {} to state {}.", RelayN, state);
    RES = setSingleRelayState(RelayN, state);
    if (RES == HID_ERROR) { return HID_ERROR; }

    // sleep time between flip-flop
    logger.trace("Sleep for {} milliseconds.", sleepTime);
    try { Thread.sleep(sleepTime); } catch (InterruptedException e) { return HID_ERROR; }

    // reverse the state - flop
    logger.trace("Set relay no. {} to state {}.", RelayN, otherState);
    RES = setSingleRelayState(RelayN, otherState);
    if (RES == HID_ERROR) { return HID_ERROR; }

    return HID_OK;
    }

  /**
   * connectedToDevice - check if we are connected to a device or not
   * @return state (boolean, state if we are connected: connected=true, not connected=false)
   */
  public boolean connectedToDevice()
    {
    logger.trace("Method connectedToDevice()");
    if (openedDevice != null)
      {
      logger.trace("USB device connected.");
      return true;
      }
    logger.trace("USB device not connected.");
    return false;
    }
  }
