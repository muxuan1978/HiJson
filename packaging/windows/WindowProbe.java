import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

/** Build-only probe. Observe the packaged launcher's real Swing window on the runner desktop. */
public final class WindowProbe {
    public static void premain(String resultFile) {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event.getID() != WindowEvent.WINDOW_OPENED) return;
            if (!(event.getSource() instanceof JFrame)) return;
            JFrame window = (JFrame) event.getSource();
            if (!window.isShowing() || !hasEditorTabs(window)) return;
            EventQueue.invokeLater(() -> {
                try {
                    Files.write(Paths.get(resultFile), window.getTitle().getBytes(StandardCharsets.UTF_8));
                    System.exit(0);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.exit(1);
                }
            });
        }, AWTEvent.WINDOW_EVENT_MASK);
    }

    private static boolean hasEditorTabs(Component component) {
        if (component instanceof JTabbedPane) {
            JTabbedPane tabs = (JTabbedPane) component;
            if (tabs.indexOfTab("树状") >= 0 && tabs.indexOfTab("图形") >= 0) return true;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                if (hasEditorTabs(child)) return true;
            }
        }
        return false;
    }
}
