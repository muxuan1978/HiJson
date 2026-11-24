package hi.chyl.json;

import com.formdev.flatlaf.util.SystemInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

/** Opens the node menu once per gesture, anywhere across the clicked row. */
final class TreePopupMouseListener extends MouseAdapter {
    private final JTree tree;
    private final BiConsumer<TreePath, Point> showMenu;
    private boolean handledOnPress;

    TreePopupMouseListener(JTree tree, BiConsumer<TreePath, Point> showMenu) {
        this.tree = tree;
        this.showMenu = showMenu;
    }

    @Override public void mousePressed(MouseEvent e) {
        handledOnPress = e.isPopupTrigger() || isMacControlClick(e);
        if (handledOnPress) open(e);
    }

    @Override public void mouseReleased(MouseEvent e) {
        // Some macOS Java runtimes deliver button 3 without setting isPopupTrigger.
        if (!handledOnPress && (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e) || isMacControlClick(e))) {
            open(e);
        }
        handledOnPress = false;
    }

    private boolean isMacControlClick(MouseEvent e) {
        return SystemInfo.isMacOS && e.isControlDown() && SwingUtilities.isLeftMouseButton(e);
    }

    private void open(MouseEvent e) {
        if (!tree.contains(e.getPoint())) return;
        TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
        Rectangle row = path == null ? null : tree.getPathBounds(path);
        if (row != null && e.getY() >= row.y && e.getY() < row.y + row.height) {
            showMenu.accept(path, e.getPoint());
        }
    }
}
