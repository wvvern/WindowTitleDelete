package wvv.windowstitlebardelete;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

@Mod(
        modid = WindowsTitlebarDelete.MODID,
        name = WindowsTitlebarDelete.MODNAME,
        version = WindowsTitlebarDelete.VERSION
)
@SideOnly(Side.CLIENT)
public class WindowsTitlebarDelete {

    public static final String MODID = "windowstitlebardelete";
    public static final String MODNAME = "Windows Titlebar Delete";
    public static final String VERSION = "1.0.0";

    private boolean titlebarHidden = false;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        hideTitlebar();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !titlebarHidden) {
            hideTitlebar();
        }
    }

    private void hideTitlebar() {
        if (!isWindows()) {
            return;
        }

        try {
            WinDef.HWND hwnd = getMinecraftWindow();
            if (hwnd != null) {
                int currentStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_STYLE);
                int newStyle = currentStyle & ~(WinUser.WS_CAPTION | WinUser.WS_SYSMENU | 
                                               WinUser.WS_MINIMIZEBOX | WinUser.WS_MAXIMIZEBOX);
                newStyle |= WinUser.WS_MAXIMIZEBOX;
                
                User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_STYLE, newStyle);
                User32.INSTANCE.SetWindowPos(hwnd, null, 0, 0, 0, 0,
                    WinUser.SWP_FRAMECHANGED | WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE | WinUser.SWP_NOZORDER);
                
                titlebarHidden = true;
            }
        } catch (Exception e) {
            System.err.println("[Windows Titlebar Delete] Error hiding titlebar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private WinDef.HWND getMinecraftWindow() {
        try {
            // Try to get window by class name (LWJGL window class)
            WinDef.HWND hwnd = User32.INSTANCE.FindWindow("LWJGL", null);
            if (hwnd != null) {
                return hwnd;
            }
            
            // Fallback: try to find by window title containing "Minecraft"
            final WinDef.HWND[] result = new WinDef.HWND[1];
            User32.INSTANCE.EnumWindows((hwnd1, data) -> {
                char[] windowText = new char[512];
                User32.INSTANCE.GetWindowText(hwnd1, windowText, 512);
                String title = Native.toString(windowText);

                if (title.contains("Minecraft")) {
                    result[0] = hwnd1;
                    return false; // Stop enumeration
                }
                return true; // Continue enumeration
            }, null);
            
            return result[0];
        } catch (Exception e) {
            System.err.println("[Windows Titlebar Delete] Error finding window: " + e.getMessage());
            return null;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}
