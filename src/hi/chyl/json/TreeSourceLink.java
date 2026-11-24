package hi.chyl.json;

import com.google.gson.JsonElement;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/** Bidirectional selection for one tab. Incoming navigation must not trigger the reverse action. */
final class TreeSourceLink {
    private final RSyntaxTextArea source;
    private final JTree tree;
    private final Runnable modelReset;
    private final Timer rebuild;
    private JsonSourceTree.Node root;
    private boolean active = true, dirty = true, syncing;

    TreeSourceLink(RSyntaxTextArea source, JTree tree, Runnable modelReset) {
        this.source = source;
        this.tree = tree;
        this.modelReset = modelReset;
        rebuild = new Timer(240, e -> refresh());
        rebuild.setRepeats(false);
        source.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
        });
        source.addCaretListener(e -> revealTreeNode());
        source.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (!active || !SwingUtilities.isLeftMouseButton(e)) return;
                if (dirty) refresh();
                else revealTreeNode();
            }
        });
        tree.addTreeSelectionListener(e -> selectSource());
        tree.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                // Clicking an already selected row doesn't fire a TreeSelectionEvent.
                if (SwingUtilities.isLeftMouseButton(e) && tree.getPathForLocation(e.getX(), e.getY()) != null) {
                    selectSource();
                }
            }
        });
    }

    private void changed() {
        dirty = true;
        if (active) rebuild.restart();
    }

    void setActive(boolean active) {
        this.active = active;
        rebuild.stop();
        if (active) {
            if (dirty) refresh();
            else revealTreeNode();
        }
    }

    private void refresh() {
        String text = source.getText();
        JsonElement parsed = null;
        if (!text.trim().isEmpty()) {
            try {
                parsed = GraphNode.parse(text);
            } catch (IllegalArgumentException ignored) {
                // A document being typed can be incomplete. No stale offsets are safe to use.
            }
        }
        display(text, parsed);
    }

    void display(String text, JsonElement value) {
        rebuild.stop();
        dirty = false;
        syncing = true;
        try {
            root = value == null ? null : JsonSourceTree.build(text, value);
            tree.putClientProperty("jsonRoot", value);
            ((DefaultTreeModel) tree.getModel()).setRoot(root == null ? Kit.objNode("JSON") : root);
            modelReset.run();
        } finally {
            syncing = false;
        }
        revealTreeNode();
    }

    private void revealTreeNode() {
        if (!active || dirty || syncing || root == null) return;
        JsonSourceTree.Node node = root.at(source.getCaretPosition());
        TreePath path = new TreePath(node.getPath());
        syncing = true;
        try {
            tree.setSelectionPath(path);
            tree.expandPath(path);
            tree.scrollPathToVisible(path);
        } finally {
            syncing = false;
        }
    }

    private void selectSource() {
        Object selected = tree.getLastSelectedPathComponent();
        if (!active || dirty || syncing || !(selected instanceof JsonSourceTree.Node)) return;
        JsonSourceTree.Node node = (JsonSourceTree.Node) selected;
        syncing = true;
        try {
            source.getFoldManager().ensureOffsetNotInClosedFold(node.start);
            source.getFoldManager().ensureOffsetNotInClosedFold(node.end - 1);
            source.setCaretPosition(node.end);
            source.moveCaretPosition(node.start);
            source.getCaret().setSelectionVisible(true);
            Rectangle firstLine = source.modelToView(node.start);
            if (firstLine != null) source.scrollRectToVisible(firstLine);
        } catch (BadLocationException ex) {
            throw new IllegalStateException(ex);
        } finally {
            syncing = false;
        }
    }
}
