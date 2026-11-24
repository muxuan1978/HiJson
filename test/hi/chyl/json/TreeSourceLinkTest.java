package hi.chyl.json;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreeSourceLinkTest {
    @Test public void linkPreservesCaretAndInvalidatesStaleRangesForEachTab() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RSyntaxTextArea source = new RSyntaxTextArea();
            JTree tree = new JTree();
            TreeSourceLink link = new TreeSourceLink(source, tree, () -> { });
            String text = "[{\"a\":false},{\"a\":false}]";
            source.setText(text);
            link.display(text, GraphNode.parse(text));
            int offset = text.lastIndexOf("false") + 1;
            source.setCaretPosition(offset);
            JsonSourceTree.Node node = (JsonSourceTree.Node) tree.getLastSelectedPathComponent();
            assertEquals("a", node.key);
            assertEquals("[1]", ((JsonSourceTree.Node) node.getParent()).key);
            assertEquals(offset, source.getSelectionStart());
            assertEquals(offset, source.getSelectionEnd());
            TreePath object = tree.getSelectionPath().getParentPath();
            tree.setSelectionPath(object);
            assertEquals("{\"a\":false}", source.getSelectedText());
            assertEquals(text.lastIndexOf('{'), source.getSelectionStart());

            source.setText("{");
            tree.setSelectionPath(object.getParentPath());
            assertNull(source.getSelectedText());
            link.setActive(false);
            source.setText("[null]");
            link.setActive(true);
            source.setCaretPosition(2);
            assertTrue(((JsonSourceTree.Node) tree.getLastSelectedPathComponent()).value.isJsonNull());
            link.setActive(false);
        });
    }
}
