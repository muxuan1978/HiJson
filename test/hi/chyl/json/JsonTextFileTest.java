package hi.chyl.json;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class JsonTextFileTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void preservesUtf8BomAndGraphEdits() throws Exception {
        File file = folder.newFile();
        String text = "{\"猫\":\"☕\"}";
        Files.write(file.toPath(), ("\uFEFF" + text).getBytes(StandardCharsets.UTF_8));
        JsonTextFile document = new JsonTextFile();
        GraphNode root = new GraphNode(GraphNode.parse(document.read(file)));
        String updated = GraphNode.JSON.toJson(root.children().get(0).edit("茶", GraphNode.parse("\"🍵\"")));
        document.write(file, updated);
        assertEquals("\uFEFF" + updated, new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        assertEquals(updated, new JsonTextFile().read(file));
    }

    @Test public void readsLegacyGbkAndPreservesNewUnicodeOnSave() throws Exception {
        File file = folder.newFile();
        Charset gbk = Charset.forName("GBK");
        Files.write(file.toPath(), "\"中文\"".getBytes(gbk));
        JsonTextFile document = new JsonTextFile();
        assertEquals("\"中文\"", document.read(file));
        document.write(file, "\"文本\"");
        assertArrayEquals("\"文本\"".getBytes(gbk), Files.readAllBytes(file.toPath()));
        document.write(file, "\"文本🍵\"");
        assertEquals("\"文本🍵\"", new JsonTextFile().read(file));
    }
}
