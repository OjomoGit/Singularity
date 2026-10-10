package singularity.ui.utils;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

public class Windows11Blur {

    public interface DwmApi extends com.sun.jna.Library {
        DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class, W32APIOptions.DEFAULT_OPTIONS);
        int DwmSetWindowAttribute(HWND hwnd, int dwAttribute, IntByReference pvAttribute, int cbAttribute);
    }

    public static void applyAcrylic(String windowTitle) {
        try {
            HWND hwnd = User32.INSTANCE.FindWindow(null, windowTitle);

            if (hwnd != null) {
                DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, 20, new IntByReference(1), 4);
                DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, 38, new IntByReference(3), 4);
                DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, 33, new IntByReference(2), 4);
            }
        } catch (Throwable t) {
            System.out.println("Nativer Blur nicht unterstützt. Nutze CSS-Fallback.");
        }
    }
}
