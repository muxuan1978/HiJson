package hi.chyl.json;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.junit.Test;
import static org.junit.Assert.*;

public class JsonGraphNavigationTest {
    @Test public void fieldMenuCopiesTheKeyRawValueAndTypedJsonPair() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GraphNode object = new GraphNode(GraphNode.parse("{\"\":\"a\\nb\",\"x\\\"雪\":[false,null,{\"n\":1}]}"));
            assertCopies(object.children().get(0), "", "a\nb", "\"\": \"a\\nb\"");
            GraphNode array = object.children().get(1);
            assertCopies(array, "x\"雪", "[false,null,{\"n\":1}]", "\"x\\\"雪\": [false,null,{\"n\":1}]");
            assertCopies(array.children().get(0), "[0]", "false", "\"[0]\": false");
            assertCopies(array.children().get(1), "[1]", "null", "\"[1]\": null");
            assertCopies(array.children().get(2), "[2]", "{\"n\":1}", "\"[2]\": {\"n\":1}");
            GraphNode numbers = new GraphNode(GraphNode.parse("{\"n\":987654321098765432109}"));
            assertCopies(numbers.children().get(0), "n", "987654321098765432109", "\"n\": 987654321098765432109");
        });
    }

    private static void assertCopies(GraphNode field, String key, String value, String pair) {
        AtomicReference<String> copied = new AtomicReference<>();
        JPopupMenu menu = new JPopupMenu();
        JsonGraphCanvas.addFieldCopyItems(menu, field, copied::set);
        String[] expected = {key, value, pair};
        for (int i = 0; i < expected.length; i++) {
            ((JMenuItem) menu.getComponent(i)).doClick(0);
            assertEquals(expected[i], copied.get());
        }
    }

    private static void click(JsonGraphCanvas canvas, int x, int y) {
        for (int id : new int[]{MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED}) {
            canvas.dispatchEvent(new MouseEvent(canvas, id, 1, 0, x, y, 1, false, MouseEvent.BUTTON1));
        }
    }

    @Test public void nestedFoldsSurviveParentFoldingAndOnlyTheLocatedBranchOpens() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String text = "{\"left\":{\"inner\":{\"same\":1}},\"right\":{\"same\":2}}";
            AtomicReference<GraphNode> selected = new AtomicReference<>();
            JsonGraphCanvas canvas = new JsonGraphCanvas(selected::set, selected::set, (a,b) -> fail("unexpected write"));
            JViewport view = new JViewport();
            view.setView(canvas);
            view.setSize(280, 150);
            GraphNode doc = new GraphNode(GraphNode.parse(text));
            canvas.display(doc, null);
            view.doLayout();
            click(canvas, 810, 41); // inner
            click(canvas, 520, 41); // left
            click(canvas, 520, 100); // right, now directly below left
            click(canvas, 230, 41); // root
            Dimension folded = canvas.getPreferredSize();
            assertEquals(82, folded.height);
            canvas.display(null, "updating");
            canvas.display(doc, null);
            assertEquals(folded, canvas.getPreferredSize());

            click(canvas, 230, 41);
            click(canvas, 520, 41);
            selected.set(null);
            click(canvas, 665, 72); // inner is still folded
            assertNull(selected.get());
            canvas.reveal(new int[]{0, 0, 0});
            canvas.getActionForKeyStroke(KeyStroke.getKeyStroke("ENTER")).actionPerformed(null);
            assertEquals(1, selected.get().value.getAsInt());
            assertTrue(view.getViewRect().intersects(new Rectangle(604, 58, 222, 27)));
            selected.set(null);
            click(canvas, 380, 170); // the right card is still folded
            assertNull(selected.get());
            assertEquals(text, doc.value.toString());
        });
    }

    @Test public void editorAndGraphSelectExactOccurrencesAtZoomAndUnfoldTheTarget() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String text = "{\n  \"items\": [{\"same\":17}, {\"same\":17, \"x/y~\":\"雪🍵\"}],\n  \"other\":false\n}";
            RSyntaxTextArea source = new RSyntaxTextArea();
            JsonGraphPanel panel = new JsonGraphPanel(source);
            JViewport view = ((JScrollPane) panel.getComponent(0)).getViewport();
            JsonGraphCanvas canvas = (JsonGraphCanvas) view.getView();
            view.setSize(280, 170);
            source.setText(text);
            source.setCaretPosition(0);
            panel.setActive(true);
            view.doLayout();
            click(canvas, 230, 41);
            assertEquals(text, source.getSelectedText());
            canvas.zoomTo(1.5);
            int second = text.lastIndexOf("17");
            source.setCaretPosition(second);
            assertNull(source.getSelectedText());
            assertTrue(view.getViewRect().intersects(new Rectangle(906, 238, 333, 41)));

            click(canvas, 960, 260); // second object's first field
            assertEquals("\"same\":17", source.getSelectedText());
            assertEquals(text.lastIndexOf("\"same\""), source.getSelectionStart());
            click(canvas, 960, 212); // second object header
            assertEquals("{\"same\":17, \"x/y~\":\"雪🍵\"}", source.getSelectedText());
            click(canvas, 960, 300);
            assertEquals("\"x/y~\":\"雪🍵\"", source.getSelectedText());
            assertEquals(text, source.getText());

            source.setText("{");
            click(canvas, 960, 260);
            assertEquals("{", source.getText());
            assertNull(source.getSelectedText());
            panel.setActive(false);
            source.setText("[null, true]");
            panel.setActive(true);
            click(canvas, 105, 149);
            assertEquals("true", source.getSelectedText());
        });
    }

    @Test public void locatingAPagedFieldRestoresTheManuallySizedCard() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RSyntaxTextArea source = new RSyntaxTextArea();
            JsonGraphPanel panel = new JsonGraphPanel(source);
            JViewport view = ((JScrollPane) panel.getComponent(0)).getViewport();
            JsonGraphCanvas canvas = (JsonGraphCanvas) view.getView();
            view.setSize(360, 240);
            String text = "[1, 2, 1, 2]";
            source.setText(text);
            panel.setActive(true);
            view.doLayout();
            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 1, 0, 241, 177, 1, false, MouseEvent.BUTTON1));
            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 2, MouseEvent.BUTTON1_DOWN_MASK, 296, 97, 0, false, 0));
            canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 3, 0, 296, 97, 1, false, MouseEvent.BUTTON1));
            Dimension resized = canvas.getPreferredSize();
            click(canvas, 286, 41);
            assertTrue(canvas.getPreferredSize().height < resized.height);
            source.setCaretPosition(text.lastIndexOf('2'));
            assertEquals(resized, canvas.getPreferredSize());
            click(canvas, 95, 72);
            assertEquals("2", source.getSelectedText());
            assertEquals(text.lastIndexOf('2'), source.getSelectionStart());
        });
    }
}
