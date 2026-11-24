package hi.chyl.json;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Remembers the source file and its encoding for one editor tab. */
final class JsonTextFile {
    private static final Charset GBK = Charset.forName("GBK");
    private Charset charset = StandardCharsets.UTF_8;
    private boolean bom;
    private File file;

    File location() { return file; }

    String read(File selected) throws IOException {
        ByteBuffer bytes = ByteBuffer.wrap(Files.readAllBytes(selected.toPath()));
        Charset encoding = StandardCharsets.UTF_8;
        String text;
        try {
            text = encoding.newDecoder().decode(bytes).toString();
        } catch (CharacterCodingException ex) {
            bytes.rewind();
            encoding = GBK;
            text = encoding.newDecoder().decode(bytes).toString();
        }
        charset = encoding;
        file = selected;
        bom = text.startsWith("\uFEFF");
        return bom ? text.substring(1) : text;
    }

    void write(File selected, String text) throws IOException {
        Charset encoding = charset;
        if (encoding.equals(GBK) && !encoding.newEncoder().canEncode(text)) {
            encoding = StandardCharsets.UTF_8;
        }
        // Finish encoding before opening the destination, including on encoding errors.
        ByteBuffer encoded = encoding.newEncoder().encode(CharBuffer.wrap(bom ? "\uFEFF" + text : text));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        Files.write(selected.toPath(), bytes);
        file = selected;
        charset = encoding;
    }
}
