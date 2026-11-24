package hi.chyl.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** An address in one parsed snapshot. Keys and array indices never depend on display text. */
final class GraphNode {
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().serializeNulls()
            .disableHtmlEscaping().create();

    final GraphNode parent;
    final String key;
    final int index;
    final JsonElement value;

    GraphNode(JsonElement root) {
        this(null, null, -1, root);
    }

    private GraphNode(GraphNode parent, String key, int index, JsonElement value) {
        this.parent = parent;
        this.key = key;
        this.index = index;
        this.value = value;
    }

    static JsonElement parse(String text) {
        return parse(text, Strictness.STRICT);
    }

    static JsonElement parse(String text, Strictness mode) {
        try (JsonReader reader = new JsonReader(new StringReader(text))) {
            reader.setStrictness(mode);
            JsonElement result = readValue(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IllegalArgumentException("JSON 后面还有其他内容");
            }
            return result;
        } catch (IOException | com.google.gson.JsonParseException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private static JsonElement readValue(JsonReader reader) throws IOException {
        switch (reader.peek()) {
            case BEGIN_OBJECT:
                JsonObject obj = new JsonObject();
                reader.beginObject();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    if (obj.has(key)) throw new IllegalArgumentException("对象内有重名字段：" + reader.getPath());
                    obj.add(key, readValue(reader));
                }
                reader.endObject();
                return obj;
            case BEGIN_ARRAY:
                JsonArray arr = new JsonArray();
                reader.beginArray();
                while (reader.hasNext()) arr.add(readValue(reader));
                reader.endArray();
                return arr;
            case STRING:
            case NUMBER:
            case BOOLEAN:
            case NULL:
                return JsonParser.parseReader(reader);
            default:
                throw new IllegalArgumentException("缺少 JSON 值：" + reader.getPath());
        }
    }

    boolean isContainer() {
        return value.isJsonObject() || value.isJsonArray();
    }

    String label() {
        if (parent == null) return "$";
        if (key == null) return "[" + index + "]";
        return key.isEmpty() ? "\"\"" : key;
    }

    List<GraphNode> children() {
        List<GraphNode> result = new ArrayList<>();
        if (value.isJsonObject()) {
            for (Map.Entry<String, JsonElement> member : value.getAsJsonObject().entrySet()) {
                result.add(new GraphNode(this, member.getKey(), result.size(), member.getValue()));
            }
        } else if (value.isJsonArray()) {
            for (JsonElement item : value.getAsJsonArray()) {
                result.add(new GraphNode(this, null, result.size(), item));
            }
        }
        return result;
    }

    String summary() {
        if (value.isJsonObject()) return "{ " + value.getAsJsonObject().size() + " }";
        if (value.isJsonArray()) return "[ " + value.getAsJsonArray().size() + " ]";
        return value.toString();
    }

    JsonElement edit(String newKey, JsonElement replacement) {
        if (parent == null) return replacement;
        if (key == null) {
            parent.value.getAsJsonArray().set(index, replacement);
        } else {
            JsonObject obj = parent.value.getAsJsonObject();
            if (key.equals(newKey)) {
                obj.add(key, replacement);
            } else {
                requireFreeKey(obj, newKey);
                // Reinsert in original order so renaming a field leaves its siblings in place.
                Map<String, JsonElement> members = new LinkedHashMap<>(obj.asMap());
                obj.asMap().clear();
                members.forEach((name, item) -> obj.add(name.equals(key) ? newKey : name,
                        name.equals(key) ? replacement : item));
            }
        }
        return root().value;
    }

    boolean canMoveInto(GraphNode target) {
        if (parent == null || target == null || !target.isContainer()) return false;
        for (GraphNode ancestor = target; ancestor != null; ancestor = ancestor.parent) {
            if (ancestor.value == value) return false;
        }
        return target.root() == root();
    }

    /** Position is the final zero-based array index, including for same-array moves. */
    JsonElement move(GraphNode target, String newKey, int position) {
        if (!canMoveInto(target)) throw new IllegalArgumentException("请选择当前节点及其子节点以外的对象或数组。");
        if (target.value.isJsonObject()) {
            if (target.value == parent.value) return edit(newKey, value);
            requireFreeKey(target.value.getAsJsonObject(), newKey);
        } else {
            int max = target.value.getAsJsonArray().size() - (target.value == parent.value ? 1 : 0);
            if (position < 0 || position > max) throw new IllegalArgumentException("数组下标超出范围。");
        }

        if (key == null) parent.value.getAsJsonArray().remove(index);
        else parent.value.getAsJsonObject().remove(key);
        if (target.value.isJsonObject()) target.value.getAsJsonObject().add(newKey, value);
        else target.value.getAsJsonArray().asList().add(position, value);
        return root().value;
    }

    private static void requireFreeKey(JsonObject obj, String key) {
        if (key == null || obj.has(key)) throw new IllegalArgumentException("目标对象已有同名字段，请更换键名。");
    }

    private GraphNode root() {
        GraphNode node = this;
        while (node.parent != null) node = node.parent;
        return node;
    }
}
