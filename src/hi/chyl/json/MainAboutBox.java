package hi.chyl.json;

import com.formdev.flatlaf.util.ScaledImageIcon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import org.jdesktop.application.ResourceMap;

public class MainAboutBox extends JDialog {
    private final ResourceMap resources = MainApp.getApplication().getContext().getResourceMap(MainAboutBox.class);

    public MainAboutBox(Frame parent) {
        super(parent, true);
        setTitle(resources.getString("title"));
        setName("aboutBox");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        content.add(createHeader(), BorderLayout.NORTH);

        JPanel details = new JPanel(new GridBagLayout());
        details.setOpaque(false);
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, WorkspaceStyle.LINE),
                BorderFactory.createEmptyBorder(12, 0, 0, 0)));
        addRow(details, 0, "contributorLabel", new JLabel(resources.getString("Application.originalContributor")));
        addRow(details, 1, "maintainerLabel", new JLabel(resources.getString("Application.maintainer")));
        addRow(details, 2, "originalProjectLabel", createLink(resources.getString("originalProjectUrl")));
        addRow(details, 3, "repositoryLabel", createLink(resources.getString("Application.homepage")));
        content.add(details, BorderLayout.CENTER);

        JButton close = new JButton(resources.getString("closeLabel"));
        close.setMargin(new Insets(5, 18, 5, 18));
        close.addActionListener(e -> dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        footer.setOpaque(false);
        footer.add(close);
        content.add(footer, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().setDefaultButton(close);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
    }

    private JPanel createHeader() {
        JLabel icon = new JLabel(new ScaledImageIcon(resources.getImageIcon("brandIcon"), 80, 80));
        JLabel name = new JLabel(resources.getString("Application.name"));
        name.setFont(name.getFont().deriveFont(Font.BOLD, name.getFont().getSize2D() + 14));
        name.setForeground(WorkspaceStyle.INK);
        JLabel description = new JLabel(resources.getString("headerDetail"));
        description.setForeground(WorkspaceStyle.MUTED);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(Box.createVerticalGlue());
        titles.add(name);
        titles.add(Box.createVerticalStrut(4));
        titles.add(description);
        titles.add(Box.createVerticalGlue());

        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setOpaque(false);
        header.add(icon, BorderLayout.LINE_START);
        header.add(titles, BorderLayout.CENTER);
        return header;
    }

    private void addRow(JPanel details, int row, String labelKey, JComponent value) {
        JLabel label = new JLabel(resources.getString(labelKey));
        label.setForeground(WorkspaceStyle.MUTED);
        label.setLabelFor(value);
        GridBagConstraints cell = new GridBagConstraints();
        cell.gridy = row;
        cell.gridx = 0;
        cell.insets = new Insets(8, 0, 8, 32);
        cell.anchor = GridBagConstraints.LINE_START;
        details.add(label, cell);
        cell.gridx = 1;
        cell.weightx = 1;
        cell.insets = new Insets(8, 0, 8, 0);
        details.add(value, cell);
    }

    private JButton createLink(String url) {
        JButton link = new JButton(url);
        link.setBorder(BorderFactory.createEmptyBorder());
        link.setBorderPainted(false);
        link.setContentAreaFilled(false);
        link.setFocusPainted(false);
        link.setDefaultCapable(false);
        link.setForeground(WorkspaceStyle.ACCENT);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setToolTipText(resources.getString("openLink"));
        Font normal = link.getFont();
        Font underlined = normal.deriveFont(Collections.singletonMap(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
        link.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { link.setFont(underlined); }
            @Override public void focusLost(FocusEvent e) { link.setFont(normal); }
        });
        link.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "pressed");
        link.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "released");
        link.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(URI.create(url));
            } catch (IOException | UnsupportedOperationException | SecurityException ex) {
                JOptionPane.showInputDialog(this, resources.getString("copyLink"), url);
            }
        });
        return link;
    }
}
