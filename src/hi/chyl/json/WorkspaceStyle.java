package hi.chyl.json;

import java.awt.Color;
import java.awt.Insets;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.UIManager;

/** Shared colors and density for the document workspace. */
final class WorkspaceStyle {
    static final Color INK = new Color(0x273247), MUTED = new Color(0x58677E), DISABLED = new Color(0x96A0AE);
    static final Color CHROME = new Color(0xF1F3F8), PAPER = new Color(0xF8F9FC), LINE = new Color(0xD7DEE8);
    static final Color ACCENT = new Color(0x5546B1), SELECTION = new Color(0xE8E7FC);
    static final Color TEAL = new Color(0x146E62), AMBER = new Color(0x98501B), VIOLET = new Color(0x824B91);

    static void install() {
        UIManager.put("Component.accentColor", ACCENT);
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Button.margin", new Insets(4, 9, 4, 9));
        UIManager.put("TabbedPane.underlineColor", ACCENT);
        UIManager.put("TabbedPane.inactiveUnderlineColor", new Color(0xA196D9));
        UIManager.put("TabbedPane.focusColor", SELECTION);
        UIManager.put("TabbedPane.selectedBackground", Color.WHITE);
        UIManager.put("TabbedPane.contentAreaColor", LINE);
        UIManager.put("TabbedPane.tabInsets", new Insets(0, 10, 0, 10));
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
        UIManager.put("Tree.rowHeight", 23);
        UIManager.put("Tree.showsRootHandles", true);
        UIManager.put("Tree.selectionBackground", SELECTION);
        UIManager.put("Tree.selectionForeground", INK);
        UIManager.put("Tree.selectionInactiveBackground", SELECTION);
        UIManager.put("Tree.selectionInactiveForeground", INK);
        UIManager.put("ToggleButton.toolbar.selectedBackground", SELECTION);
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", INK);
        UIManager.put("PopupMenu.borderCornerRadius", 8);
        UIManager.put("MenuItem.selectionType", "default");
        UIManager.put("MenuItem.selectionBackground", SELECTION);
        UIManager.put("MenuItem.selectionForeground", INK);
        UIManager.put("MenuItem.acceleratorSelectionForeground", MUTED);
        UIManager.put("MenuItem.margin", new Insets(4, 8, 4, 8));
        UIManager.put("SplitPane.dividerSize", 5);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
    }

    static void toolButton(AbstractButton button, String label) {
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setMargin(new Insets(4, 6, 4, 6));
        button.setToolTipText(label);
        button.getAccessibleContext().setAccessibleName(label);
        button.setForeground(INK);
    }

    private WorkspaceStyle() {}
}
