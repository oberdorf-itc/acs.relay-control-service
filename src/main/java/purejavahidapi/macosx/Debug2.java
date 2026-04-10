package purejavahidapi.macosx;

import static purejavahidapi.macosx.CoreFoundationLibrary.CFRelease;
// import static purejavahidapi.macosx.CoreFoundationLibrary.CFRunLoopAddSource;
import static purejavahidapi.macosx.CoreFoundationLibrary.CFRunLoopGetCurrent;
import static purejavahidapi.macosx.CoreFoundationLibrary.CFRunLoopRunInMode;
// import static purejavahidapi.macosx.CoreFoundationLibrary.CFRunLoopSourceCreate;
// import static purejavahidapi.macosx.CoreFoundationLibrary.CFSTR;
import static purejavahidapi.macosx.CoreFoundationLibrary.CFSetGetCount;
import static purejavahidapi.macosx.CoreFoundationLibrary.CFSetGetValues;
import static purejavahidapi.macosx.CoreFoundationLibrary.CFStringCreateWithCString;
import static purejavahidapi.macosx.CoreFoundationLibrary.kCFAllocatorDefault;
import static purejavahidapi.macosx.CoreFoundationLibrary.kCFRunLoopDefaultMode;
import static purejavahidapi.macosx.CoreFoundationLibrary.kCFRunLoopRunFinished;
// import static purejavahidapi.macosx.CoreFoundationLibrary.kCFRunLoopRunHandledSource;
import static purejavahidapi.macosx.CoreFoundationLibrary.kCFRunLoopRunTimedOut;
import static purejavahidapi.macosx.CoreFoundationLibrary.kCFStringEncodingASCII;
// import static purejavahidapi.macosx.HidDevice.processPendingEvents;
// import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDDeviceRegisterInputReportCallback;
//import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDDeviceRegisterRemovalCallback;
// import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDDeviceScheduleWithRunLoop;
import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDManagerClose;
import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDManagerCopyDevices;
import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDManagerCreate;
import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDManagerScheduleWithRunLoop;
import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDManagerSetDeviceMatching;
// import static purejavahidapi.macosx.IOHIDManagerLibrary.kIOHIDMaxInputReportSizeKey;
import static purejavahidapi.macosx.IOHIDManagerLibrary.kIOHIDOptionsTypeNone;

import java.util.List;

// import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import purejavahidapi.DeviceRemovalListener;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.InputReportListener;
import purejavahidapi.PureJavaHidApi;
import purejavahidapi.macosx.CoreFoundationLibrary.CFRunLoopRef;
// import purejavahidapi.macosx.CoreFoundationLibrary.CFRunLoopSourceContext;
import purejavahidapi.macosx.CoreFoundationLibrary.CFRunLoopSourceRef;
import purejavahidapi.macosx.CoreFoundationLibrary.CFSetRef;
import purejavahidapi.macosx.CoreFoundationLibrary.CFStringRef;
import purejavahidapi.macosx.HidDevice.HidDeviceRemovalCallback;
import purejavahidapi.macosx.HidDevice.HidReportCallback;
//import purejavahidapi.macosx.HidDevice.PerformSignalCallback;
import purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDDeviceRef;
import purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDManagerRef;
import purejavahidapi.shared.SyncPoint;

public class Debug2 {
	static public void main(String[] args) {
		try {

			MacOsXBackend t = new MacOsXBackend();
			t.init();

			while (true) {
				HidDeviceInfo devInfo = null;

				List<purejavahidapi.HidDeviceInfo> list = t.enumerateDevices();
				System.out.println(list.size());

				for (HidDeviceInfo info : list) {
					if (info.getVendorId() == (short) 0x0810 && info.getProductId() == (short) 0x0005) {
						System.out.println("match " + info);
						devInfo = info;
						//break;
					}
				}

				if (devInfo != null) {
					System.err.println("open");
					purejavahidapi.HidDevice dev = PureJavaHidApi.openDevice(devInfo);
					if (dev != null) {
						System.err.println("close");
						dev.close();
					}
				}

				Thread.sleep(1000);
				System.exit(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void processPendingEvents() {
		int res;
		do {
			res = CFRunLoopRunInMode(kCFRunLoopDefaultMode, 0.001, false);
		} while (res != kCFRunLoopRunFinished && res != kCFRunLoopRunTimedOut);
	}

	@SuppressWarnings("unused")
	static void test2() {
		int m_InternalIdGenerator = 0;
		boolean m_Open = true;
		HidDeviceInfo m_HidDeviceInfo;
		int m_InternalId = m_InternalIdGenerator++; // used when passing 'HidDevice' to Mac OS X callbacks
		IOHIDDeviceRef m_IOHIDDeviceRef;
		boolean m_Disconnected;
		CFStringRef m_CFRunLoopMode;
		CFRunLoopRef m_CFRunLoopRef;
		CFRunLoopSourceRef m_CFRunLoopSourceRef;
		Pointer m_InputReportBuffer;
		byte[] m_InputReportData;
		int m_MaxInputReportLength;
		InputReportListener m_InputReportListener;
		DeviceRemovalListener m_DeviceRemovalListener;
		Thread m_Thread;
		SyncPoint m_SyncStart;
		SyncPoint m_SyncShutdown;
		boolean m_StopThread;

		HidReportCallback m_HidReportCallBack;
		HidDeviceRemovalCallback m_HidDeviceRemovalCallback;
		//PerformSignalCallback m_PerformSignalCallback;

		IOHIDManagerRef m_HidManager = IOHIDManagerCreate(kCFAllocatorDefault, kIOHIDOptionsTypeNone);
		IOHIDManagerSetDeviceMatching(m_HidManager, null);
		IOHIDManagerScheduleWithRunLoop(m_HidManager, CFRunLoopGetCurrent(), kCFRunLoopDefaultMode);

		processPendingEvents();

		CFSetRef device_set = IOHIDManagerCopyDevices(MacOsXBackend.m_HidManager);

		int num_devices = (int) CFSetGetCount(device_set);
		Pointer[] device_array = new Pointer[(int) num_devices];

		CFSetGetValues(device_set, device_array);
		for (int i = 0; i < num_devices; i++) {
			IOHIDDeviceRef dev = new IOHIDDeviceRef(device_array[i]);
			short vid = 0;
			short pid = 0;
			if (vid == (short) 0x0810 && pid == (short) 0x0005) {

				m_IOHIDDeviceRef = dev;

				//m_PerformSignalCallback = new PerformSignalCallback();
				//m_DevFromCallback.put(m_PerformSignalCallback, this);

				m_HidReportCallBack = new HidReportCallback();
				//m_DevFromCallback.put(m_HidReportCallBack, this);

				m_HidDeviceRemovalCallback = new HidDeviceRemovalCallback();
				//m_DevFromCallback.put(m_HidDeviceRemovalCallback, this);

				m_SyncStart = new SyncPoint(2);
				m_SyncShutdown = new SyncPoint(2);
				//m_MaxInputReportLength = getIntProperty(dev, CFSTR(kIOHIDMaxInputReportSizeKey));
				//m_InputReportBuffer = new Memory(m_MaxInputReportLength);
				//m_InputReportData = new byte[m_MaxInputReportLength];

				//m_HidDeviceInfo = new HidDeviceInfo(dev);

				String str = String.format("HIDAPI_0x%08x", Pointer.nativeValue(dev.getPointer()));
				m_CFRunLoopMode = CFStringCreateWithCString(null, str, kCFStringEncodingASCII);

				//IOHIDDeviceRegisterInputReportCallback(dev, m_InputReportBuffer, m_MaxInputReportLength, m_HidReportCallBack, asPointerForPassingToCallback()); // shoudl pass dev

				//IOHIDDeviceRegisterRemovalCallback(dev, m_HidDeviceRemovalCallback, asPointerForPassingToCallback());

				m_SyncStart.waitAndSync();

			}

		}

		CFRelease(device_set);

		IOHIDManagerClose(m_HidManager, kIOHIDOptionsTypeNone);
		CFRelease(m_HidManager);
	}
}
