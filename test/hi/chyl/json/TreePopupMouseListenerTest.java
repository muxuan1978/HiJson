package hi.chyl.json;

import com.formdev.flatlaf.util.SystemInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreePopupMouseListenerTest {
    private static final class Fixture {
        final JTree tree = new JTree();
        final List<TreePath> opened = new ArrayList<>();
        final TreePopupMouseListener listener = new TreePopupMouseListener(tree, (path, point) -> opened.add(path));
        final TreePath path;
        final Point rightOfLabel;

        Fixture() {
            tree.setSize(500, 400);
            tree.expandRow(0);
            path = tree.getPathForRow(1);
            Rectangle row = tree.getPathBounds(path);
            rightOfLabel = new Point(tree.getWidth() - 2, row.y + row.height / 2);
            assertTrue(rightOfLabel.x > row.x + row.width);
        }

        void click(Point point, int button, int modifiers, boolean pressTrigger, boolean releaseTrigger) {
            listener.mousePressed(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 1,
                    modifiers | InputEvent.getMaskForButton(button), point.x, point.y, 1, pressTrigger, button));
            listener.mouseReleased(new MouseEvent(tree, MouseEvent.MOUSE_RELEASED, 2,
                    modifiers, point.x, point.y, 1, releaseTrigger, button));
        }
    }

    @Test public void rightClickAnywhereOnTheRowOpensOnceWithOrWithoutNativeFlags() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Fixture f = new Fixture();
            for (boolean press : new boolean[]{false, true}) {
                for (boolean release : new boolean[]{false, true}) {
                    f.opened.clear();
                    f.click(f.rightOfLabel, MouseEvent.BUTTON3, 0, press, release);
                    assertEquals(Collections.singletonList(f.path), f.opened);
                }
            }
        });
    }

    @Test public void ignoresEmptySpaceAndUsesControlClickOnlyOnMac() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Fixture f = new Fixture();
            Rectangle last = f.tree.getRowBounds(f.tree.getRowCount() - 1);
            f.click(new Point(50, last.y + last.height + 5), MouseEvent.BUTTON3, 0, true, true);
            f.click(f.rightOfLabel, MouseEvent.BUTTON1, 0, false, false);
            assertTrue(f.opened.isEmpty());
            f.click(f.rightOfLabel, MouseEvent.BUTTON1, InputEvent.CTRL_DOWN_MASK, false, false);
            assertEquals(SystemInfo.isMacOS ? Collections.singletonList(f.path) : Collections.emptyList(), f.opened);
        });
    }
}
