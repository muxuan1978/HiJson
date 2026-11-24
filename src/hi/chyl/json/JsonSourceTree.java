package hi.chyl.json;

import com.google.gson.JsonElement;
import java.util.Map;
import javax.swing.tree.DefaultMutableTreeNode;

/** Builds the Swing tree and UTF-16 editor ranges together from the same validated document. */
final class JsonSourceTree {
    static final class Node extends DefaultMutableTreeNode {
        final JsonElement value;
        final String key, summary;
        final int start, hitStart;
        int end, hitEnd;

        Node(String key, JsonElement value, int start, int hitStart) {
            this.key = key;
            this.value = value;
            this.start = start;
            this.hitStart = hitStart;
            String type;
            if (value.isJsonObject()) {
                type = Kit.sObj;
                summary = Kit.object;
            } else if (value.isJsonArray()) {
                type = Kit.sArr;
                summary = Kit.array;
            } else if (value.isJsonNull()) {
                type = Kit.sNull;
                summary = "<null>";
            } else {
                type = value.getAsJsonPrimitive().isString() ? Kit.sStr
                        : value.getAsJsonPrimitive().isBoolean() ? Kit.sBool : Kit.sNum;
                summary = value.getAsString();
            }
            String label = type + key;
            if (!value.isJsonObject() && !value.isJsonArray()) {
                label += Kit.split + (type.equals(Kit.sStr) ? "\"" + summary + "\"" : summary);
            }
            setUserObject(label);
        }

        Node at(int offset) {
            int lo = 0, hi = getChildCount() - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                Node child = (Node) getChildAt(mid);
                if (offset < child.hitStart) hi = mid - 1;
                else if (offset >= child.hitEnd) lo = mid + 1;
                else return child.at(offset);
            }
            return this;
        }
    }

    private final String text;
    private int pos;

    private JsonSourceTree(String text) {
        this.text = text;
        if (text.startsWith("\uFEFF")) pos++;
    }

    static Node build(String text, JsonElement parsed) {
        return new JsonSourceTree(text).read("JSON", parsed, false);
    }

    private Node read(String key, JsonElement value, boolean member) {
        int indentation = skipSpace();
        Node node = new Node(key, value, pos, indentation);
        if (member) {
            skipString();
            skipSpace();
            pos++; // colon
            skipSpace();
        }
        if (value.isJsonObject() || value.isJsonArray()) {
            pos++; // opening delimiter
            if (value.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                    node.add(read(entry.getKey(), entry.getValue(), true));
                }
            } else {
                int index = 0;
                for (JsonElement item : value.getAsJsonArray()) node.add(read(Kit.fkey(index++), item, false));
            }
            skipSpace();
            pos++; // closing delimiter
        } else if (text.charAt(pos) == '"') {
            skipString();
        } else {
            while (pos < text.length() && ",]} \t\r\n".indexOf(text.charAt(pos)) < 0) pos++;
        }
        node.end = pos;
        skipSpace();
        node.hitEnd = node.end;
        if (pos < text.length() && text.charAt(pos) == ',') node.hitEnd = ++pos;
        int trailing = node.hitEnd;
        while (trailing < text.length() && (text.charAt(trailing) == ' ' || text.charAt(trailing) == '\t')) trailing++;
        if (trailing < text.length() && (text.charAt(trailing) == '\n' || text.charAt(trailing) == '\r')) {
            node.hitEnd = trailing + 1;
        }
        return node;
    }

    private void skipString() {
        pos++; // opening quote
        while (true) {
            char c = text.charAt(pos++);
            if (c == '"') return;
            if (c == '\\') pos++;
        }
    }

    private int skipSpace() {
        int lineStart = pos;
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (" \t\r\n".indexOf(c) < 0) break;
            pos++;
            if (c == '\n' || c == '\r') lineStart = pos;
        }
        return lineStart;
    }
}
