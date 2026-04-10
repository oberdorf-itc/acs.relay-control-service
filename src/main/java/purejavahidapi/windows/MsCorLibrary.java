package purejavahidapi.windows;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public class MsCorLibrary {

	static MsCorLibraryInterface INSTANCE = Native.load("mscorlib", MsCorLibraryInterface.class, W32APIOptions.UNICODE_OPTIONS);

	interface MsCorLibraryInterface extends StdCallLibrary {
		int SystemDefaultCharSize();
	}

	public static int SystemDefaultCharSize() {
		return INSTANCE.SystemDefaultCharSize();
	}
}
