package hi.chyl.json;

import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class JsonGraphLayoutTest {
    private void mouse(JsonGraphCanvas canvas, int id, int x, int y, int clicks) {
        canvas.dispatchEvent(new MouseEvent(canvas, id, 0, 0, x, y, clicks, false, MouseEvent.BUTTON1));
    }

    private void drag(JsonGraphCanvas canvas, int x, int y, int toX, int toY) {
        mouse(canvas, MouseEvent.MOUSE_PRESSED, x, y, 1);
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, toX, toY, 0);
        mouse(canvas, MouseEvent.MOUSE_RELEASED, toX, toY, 1);
    }

    @Test public void layoutGesturesSurviveRefreshAndNeverReparent() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicReference<GraphNode> hit = new AtomicReference<>();
            JsonGraphCanvas canvas = new JsonGraphCanvas(hit::set, (from, to) -> fail("Layout wrote JSON"));
            JViewport viewport = new JViewport();
            viewport.setView(canvas);
            viewport.setSize(900, 600);
            canvas.setSize(900, 600);
            String json = "{\"group\":{\"first\":1,\"second\":2},\"last\":3}";
            GraphNode doc = new GraphNode(GraphNode.parse(json));
            canvas.display(doc, null);
            drag(canvas, 330, 42, 390, 142); // move group header
            drag(canvas, 590, 223, 710, 196); // enlarge width, shrink height to one visible row
            mouse(canvas, MouseEvent.MOUSE_CLICKED, 680, 168, 2);
            assertEquals("first", hit.get().key);
            mouse(canvas, MouseEvent.MOUSE_PRESSED, 407, 195, 1); // next page
            mouse(canvas, MouseEvent.MOUSE_RELEASED, 407, 195, 1);
            mouse(canvas, MouseEvent.MOUSE_CLICKED, 680, 168, 2);
            assertEquals("second", hit.get().key);
            canvas.display(null, "正在更新");
            canvas.display(new GraphNode(GraphNode.parse(json)), null);
            canvas.zoomTo(1.5);
            mouse(canvas, MouseEvent.MOUSE_CLICKED, 1020, 252, 2);
            assertEquals("first", hit.get().key);
            canvas.resetLayout();
            hit.set(null);
            mouse(canvas, MouseEvent.MOUSE_CLICKED, 1020, 252, 2);
            assertNull(hit.get());
            mouse(canvas, MouseEvent.MOUSE_CLICKED, 495, 63, 2);
            assertEquals("group", hit.get().key);
            assertEquals(json, doc.value.toString());
        });
    }
}
