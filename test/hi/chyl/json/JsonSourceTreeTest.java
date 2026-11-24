package hi.chyl.json;

import org.junit.Test;
import static org.junit.Assert.*;

public class JsonSourceTreeTest {
    private JsonSourceTree.Node build(String text) {
        return JsonSourceTree.build(text, GraphNode.parse(text));
    }

    @Test public void mapsEscapedKeysStringsAndRepeatedValuesInCompactJson() {
        String text = "{\"a.b\":[{\"id\":\"same\"},{\"id\":\"same\"}],\"a\\\"b\":\"},]\\\\\\\"🍵\"}";
        JsonSourceTree.Node root = build(text);
        JsonSourceTree.Node second = root.at(text.lastIndexOf("same"));
        assertEquals("[1]", ((JsonSourceTree.Node) second.getParent()).key);
        assertEquals("\"id\":\"same\"", text.substring(second.start, second.end));
        assertSame(second, root.at(text.lastIndexOf("id")));
        JsonSourceTree.Node escaped = root.at(text.indexOf("🍵"));
        assertEquals("a\"b", escaped.key);
        assertEquals(text.substring(text.indexOf("\"a\\\"b"), text.length() - 1), text.substring(escaped.start, escaped.end));
    }

    @Test public void indentCommaAndClosingBraceIdentifyTheirOwningField() {
        String text = "{\r\n  \"items\": [\r\n    {\r\n      \"value\": null\r\n    },\r\n    []\r\n  ]\r\n}";
        JsonSourceTree.Node root = build(text);
        JsonSourceTree.Node value = root.at(text.indexOf("      "));
        assertEquals("value", value.key);
        assertSame(value, root.at(text.indexOf("\r", text.indexOf("null"))));
        JsonSourceTree.Node obj = root.at(text.indexOf("},"));
        assertSame(obj, root.at(text.indexOf(',')));
        assertEquals("{\r\n      \"value\": null\r\n    }", text.substring(obj.start, obj.end));
        assertEquals("[1]", root.at(text.indexOf("[]") + 1).key);
        assertEquals("items", root.at(text.lastIndexOf(']')).key);
        assertSame(root, root.at(text.lastIndexOf('}')));
    }

    @Test public void rootArraysAndScalarsHaveRealSourceRanges() {
        String array = " \n[true,-2.5e2,{},null]\n";
        JsonSourceTree.Node root = build(array);
        assertEquals(array.trim(), array.substring(root.start, root.end));
        assertEquals("[1]", root.at(array.indexOf('e', array.indexOf('-'))).key);
        assertEquals("[2]", root.at(array.indexOf('}')).key);
        JsonSourceTree.Node literal = build("  false\n");
        assertSame(literal, literal.at(3));
        assertEquals(2, literal.start);
        assertEquals(7, literal.end);
    }
}
