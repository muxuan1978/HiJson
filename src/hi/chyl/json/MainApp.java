/*
 * MainApp.java
 */

package hi.chyl.json;

import com.formdev.flatlaf.util.SystemInfo;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import java.awt.Desktop;
import java.awt.Window;
import java.awt.Frame;
import java.beans.EventHandler;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The main class of the application.
 */
public class MainApp extends SingleFrameApplication {

    private static final int SOCKET_PORT = 19527;
    private static MainView mainView;

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
            // 强制刷新所有已创建的顶级窗口
            for (Window w : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(w);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        show(new MainView(this));
        mainView = (MainView) getMainView();
        if (isMac()) {
            installMacAboutMenu();
            startSocketServer();
        }
    }

    private void installMacAboutMenu() {
        // The About handler moved from Apple's Java 8 API to java.awt.desktop in Java 9.
        try {
            Class<?> api = SystemInfo.isJava_9_orLater ? Desktop.class : Class.forName("com.apple.eawt.Application");
            Object application = SystemInfo.isJava_9_orLater ? Desktop.getDesktop() : api.getMethod("getApplication").invoke(null);
            Class<?> handler = Class.forName(SystemInfo.isJava_9_orLater
                    ? "java.awt.desktop.AboutHandler" : "com.apple.eawt.AboutHandler");
            api.getMethod("setAboutHandler", handler).invoke(application,
                    EventHandler.create(handler, mainView, "showAboutBox"));
        } catch (ReflectiveOperationException | SecurityException ex) {
            System.err.println("Could not register the macOS About menu: " + ex);
        }
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of MainApp
     */
    public static MainApp getApplication() {
        return Application.getInstance(MainApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        if (isMac()) System.setProperty("apple.laf.useScreenMenuBar", "true");
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception ignored) {}
        launch(MainApp.class, args);
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").startsWith("Mac");
    }

    private void startSocketServer() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    ServerSocket server = new ServerSocket(SOCKET_PORT, 50, java.net.InetAddress.getByName("127.0.0.1"));
                    System.out.println("HiJson socket server listening on port " + SOCKET_PORT);
                    while (true) {
                        try {
                            final Socket client = server.accept();
                            InputStream is = client.getInputStream();
                            byte[] buf = new byte[65536];
                            int len = is.read(buf);
                            if (len > 0) {
                                final String text = new String(buf, 0, len, "UTF-8");
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        receiveText(text);
                                    }
                                });
                            }
                            // 回复确认
                            OutputStream os = client.getOutputStream();
                            os.write("OK".getBytes("UTF-8"));
                            os.flush();
                            client.close();
                        } catch (Exception ignored) {}
                    }
                } catch (Exception e) {
                    System.err.println("Socket server failed: " + e.getMessage());
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void receiveText(String text) {
        if (mainView != null) {
            mainView.receiveExternalText(text);
        }
        // 激活窗口
        Frame f = getMainFrame();
        if (f != null) {
            f.setExtendedState(Frame.NORMAL);
            f.toFront();
            f.requestFocus();
        }
    }
}
