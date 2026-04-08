package purejavahidapi.macosx;

// import static purejavahidapi.macosx.CoreFoundationLibrary.CFRelease;
// import static purejavahidapi.macosx.CoreFoundationLibrary.CFRunLoopAddSource;
// import static purejavahidapi.macosx.CoreFoundationLibrary.CFRunLoopGetCurrent;
import static purejavahidapi.macosx.CoreFoundationLibrary.CFRunLoopRunInMode;
// import static purejavahidapi.macosx.CoreFoundationLibrary.CFRunLoopSourceCreate;
// import static purejavahidapi.macosx.CoreFoundationLibrary.CFSTR;
// import static purejavahidapi.macosx.CoreFoundationLibrary.CFSetGetCount;
// import static purejavahidapi.macosx.CoreFoundationLibrary.CFSetGetValues;
// import static purejavahidapi.macosx.CoreFoundationLibrary.CFStringCreateWithCString;
// import static purejavahidapi.macosx.CoreFoundationLibrary.kCFAllocatorDefault;
import static purejavahidapi.macosx.CoreFoundationLibrary.kCFRunLoopDefaultMode;
import static purejavahidapi.macosx.CoreFoundationLibrary.kCFRunLoopRunFinished;
// import static purejavahidapi.macosx.CoreFoundationLibrary.kCFRunLoopRunHandledSource;
import static purejavahidapi.macosx.CoreFoundationLibrary.kCFRunLoopRunTimedOut;
// import static purejavahidapi.macosx.CoreFoundationLibrary.kCFStringEncodingASCII;
// import static purejavahidapi.macosx.HidDevice.processPendingEvents;
// import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDDeviceRegisterInputReportCallback;
// import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDDeviceScheduleWithRunLoop;
// import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDManagerClose;
// import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDManagerCopyDevices;
// import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDManagerCreate;
// import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDManagerScheduleWithRunLoop;
// import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDManagerSetDeviceMatching;
// import static purejavahidapi.macosx.IOHIDManagerLibrary.kIOHIDMaxInputReportSizeKey;
// import static purejavahidapi.macosx.IOHIDManagerLibrary.kIOHIDOptionsTypeNone;

import java.util.List;

// import com.sun.jna.Memory;
// import com.sun.jna.Pointer;

import purejavahidapi.DeviceRemovalListener;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.InputReportListener;
import purejavahidapi.PureJavaHidApi;
// import purejavahidapi.macosx.CoreFoundationLibrary.CFRunLoopRef;
// import purejavahidapi.macosx.CoreFoundationLibrary.CFRunLoopSourceContext;
// import purejavahidapi.macosx.CoreFoundationLibrary.CFRunLoopSourceRef;
// import purejavahidapi.macosx.CoreFoundationLibrary.CFSetRef;
// import purejavahidapi.macosx.CoreFoundationLibrary.CFStringRef;
// import purejavahidapi.macosx.HidDevice.HidDeviceRemovalCallback;
// import purejavahidapi.macosx.HidDevice.HidReportCallback;
// import purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDDeviceRef;
// import purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDManagerRef;
// import purejavahidapi.shared.SyncPoint;

public class Debug {
	static int count = 0;

	static public void main(String[] args) {
		try {

			MacOsXBackend t = new MacOsXBackend();
			t.init();

			while (true) {
				HidDeviceInfo devInfo = null;

				List<purejavahidapi.HidDeviceInfo> list = t.enumerateDevices();
				//System.err.println(list.size());

				for (HidDeviceInfo info : list) {
					if (info.getVendorId() == (short) 0x0810 && info.getProductId() == (short) 0x0005) {
						//System.out.println("match " + info);
						devInfo = info;
						//break;
					}
				}

				if (devInfo != null) {
					//System.err.println("open");
					purejavahidapi.HidDevice dev = PureJavaHidApi.openDevice(devInfo);
					dev.setInputReportListener(new InputReportListener() {

						@Override
						public void onInputReport(HidDevice source, byte reportID, byte[] reportData, int reportLength) {
							// TODO Auto-generated method stub

						}
					});
					dev.setDeviceRemovalListener(new DeviceRemovalListener() {

						@Override
						public void onDeviceRemoval(HidDevice source) {
							System.out.println("onDeviceRemoval");
						}
					});
					if (dev != null) {
						Thread.sleep(100);
						System.err.println("close "+count++);
						try {
							dev.close();
							// if (false) {
							//	while (true) {
							//		byte[] x = new byte[1000000];
							//	}
							//}
						} catch (IllegalStateException e) {
							System.err.println(e.getMessage());
						}
					}
				}

				//System.exit(0);
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

}
