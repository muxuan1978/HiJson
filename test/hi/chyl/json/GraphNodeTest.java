package hi.chyl.json;

import com.google.gson.JsonElement;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class GraphNodeTest {
    private static GraphNode root(String text) {
        return new GraphNode(GraphNode.parse(text));
    }

    @Test public void editPreservesOrderTypesAndEscapes() {
        String source = "{\"before\":922337203685477580812345,\"a.b[0]\":0,\"\":null}";
        GraphNode root = root(source);
        String value = "[null,true,\"\\\"\\\\\\n雪\",9.000000000000000001e-23]";
        JsonElement updated = root.children().get(1).edit("a\n/b", GraphNode.parse(value));
        String expected = source.replace("\"a.b[0]\":0", "\"a\\n/b\":" + value);
        assertEquals(expected, updated.toString());
        assertEquals(expected, GraphNode.parse(GraphNode.JSON.toJson(updated)).toString());
    }

    @Test public void canReplaceRootAndArrayScalarsWithContainers() {
        GraphNode array = root("[42]");
        assertEquals("[{}]", array.children().get(0).edit(null, GraphNode.parse("{}")).toString());
        assertEquals("null", array.edit(null, GraphNode.parse("null")).toString());
        assertEquals("\"text\"", root("null").edit(null, GraphNode.parse("\"text\"")).toString());
    }

    @Test public void crossParentMovesPreserveSubtreeAndArrayOrder() {
        GraphNode root = root("{\"src\":[0,{\"x\":null},2],\"dst\":{}}");
        List<GraphNode> containers = root.children();
        JsonElement updated = containers.get(0).children().get(1).move(containers.get(1), "", -1);
        assertEquals("{\"src\":[0,2],\"dst\":{\"\":{\"x\":null}}}", updated.toString());
        containers = new GraphNode(updated).children();
        updated = containers.get(1).children().get(0).move(containers.get(0), null, 1);
        assertEquals("{\"src\":[0,{\"x\":null},2],\"dst\":{}}", updated.toString());
    }

    @Test public void sameArrayUsesFinalIndexInBothDirections() {
        GraphNode array = root("[1,2,3]");
        JsonElement updated = array.children().get(0).move(array, null, 2);
        assertEquals("[2,3,1]", updated.toString());
        array = new GraphNode(updated);
        assertEquals("[1,2,3]", array.children().get(2).move(array, null, 0).toString());
    }

    @Test public void rejectedChangesLeaveOriginalUntouched() {
        String json = "{\"a\":{\"b\":[]},\"b\":[]}";
        GraphNode root = root(json);
        GraphNode a = root.children().get(0), nested = a.children().get(0);
        assertThrows(IllegalArgumentException.class, () -> root.move(a, "root", 0));
        assertThrows(IllegalArgumentException.class, () -> a.move(nested, null, 0));
        assertThrows(IllegalArgumentException.class, () -> nested.move(root, "b", 0));
        assertThrows(IllegalArgumentException.class, () -> a.edit("b", GraphNode.parse("0")));
        assertThrows(IllegalArgumentException.class, () -> a.move(root.children().get(1), null, 2));
        assertThrows(IllegalArgumentException.class, () -> a.move(root("[1]"), null, 0));
        assertEquals(json, root.value.toString());
    }

    @Test public void graphRejectsAmbiguousOrIncompleteSource() {
        for (String text : new String[]{"", "{", "null true", "[]1", "{foo:1}", "bare",
                "[1,]", "{\"x\":1,\"x\":2}", "\"bad\\q\"", "[012]"}) {
            assertThrows(text, IllegalArgumentException.class, () -> GraphNode.parse(text));
        }
    }
}
