package hi.chyl.json;

import com.formdev.flatlaf.util.UIScale;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import javax.swing.Icon;

/** Vector outlines from the bundled Lucide 0.468.0 subset, including for native menu rendering. */
enum WorkspaceIcon implements Icon {
    DOCUMENT('\ue36e'),
    NEW('\ue0ce'),
    OPEN('\ue246'),
    SAVE('\ue150'),
    CLOSE('\ue1b1'),
    FORMAT('\ue041'),
    EXPAND('\ue442'),
    TREE('\ue40c'),
    GRAPH('\ue429'),
    OBJECT('\ue36d', WorkspaceStyle.ACCENT),
    ARRAY('\ue447', WorkspaceStyle.TEAL),
    NUMBER('\ue0f2', WorkspaceStyle.AMBER),
    STRING('\ue197', WorkspaceStyle.TEAL),
    BOOLEAN('\ue18a', WorkspaceStyle.VIOLET),
    NULL('\ue406', WorkspaceStyle.MUTED),
    KEY('\ue4a7'),
    VALUE('\ue264'),
    CONVERT('\ue1f1'),
    MORE('\ue0ba'),
    SEARCH('\ue154'),
    FIND_NODE('\ue464'),
    INSPECTOR('\ue435'),
    LAYOUT('\ue12f'),
    PASTE('\ue3eb'),
    CLEAR('\ue28e'),
    NEWLINE('\ue247'),
    SLASH('\ue521'),
    RENAME('\ue1f8'),
    WINDOW('\ue42a'),
    TOOLS('\ue244'),
    HELP('\ue082'),
    ABOUT('\ue0fe'),
    EXIT('\ue111'),
    COPY('\ue0a2'),
    PAIR('\ue08a'),
    PATH('\ue542'),
    RELATED('\ue0d3'),
    CODE('\ue462'),
    UNDO('\ue2a0'),
    REDO('\ue29f'),
    CUT('\ue151'),
    DELETE('\ue18d'),
    SELECT_ALL('\ue50d'),
    FOLD('\ue440'),
    COLLAPSE_ALL('\ue227'),
    EXPAND_ALL('\ue210'),
    ARRANGE('\ue128'),
    MINUS('\ue11f'),
    PLUS('\ue140');

    private final Shape shape;
    private final Color tint;

    WorkspaceIcon(char glyph) { this(glyph, null); }

    WorkspaceIcon(char glyph, Color tint) {
        this.tint = tint;
        Shape outline = Glyphs.FONT.createGlyphVector(Glyphs.CONTEXT, new char[]{glyph}).getOutline();
        Rectangle2D box = outline.getBounds2D();
        AffineTransform center = AffineTransform.getTranslateInstance(8 - box.getCenterX(), 8 - box.getCenterY());
        shape = center.createTransformedShape(outline);
    }

    private static final class Glyphs {
        static final FontRenderContext CONTEXT = new FontRenderContext(null, true, true);
        static final Font FONT = load();

        private static Font load() {
            try (InputStream in = WorkspaceIcon.class.getResourceAsStream("resources/icons/lucide-subset.ttf")) {
                return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(15f);
            } catch (java.io.IOException | java.awt.FontFormatException ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }
    }

    public int getIconWidth() { return UIScale.scale(16); }
    public int getIconHeight() { return UIScale.scale(16); }

    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, y);
            UIScale.scaleGraphics(g);
            Color ink = tint != null ? tint : (component == null ? WorkspaceStyle.INK : component.getForeground());
            g.setColor(component != null && !component.isEnabled() ? WorkspaceStyle.DISABLED : ink);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.fill(shape);
        } finally {
            g.dispose();
        }
    }
}
