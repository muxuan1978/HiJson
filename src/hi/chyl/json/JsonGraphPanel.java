package hi.chyl.json;

import com.google.gson.JsonElement;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/** Binds one tab's source document to its editable diagram. All mutations run on the EDT. */
final class JsonGraphPanel extends JPanel {
    private final RSyntaxTextArea source;
    private final JsonGraphCanvas canvas;
    private final JLabel status = new JLabel();
    private final JButton scale = new JButton("100%");
    private final Timer refreshTimer;
    private GraphNode snapshot;
    private JsonSourceTree.Node sourceRoot;
    private boolean active, writing, selectingSource, dirty = true;

    JsonGraphPanel(RSyntaxTextArea source) {
        super(new BorderLayout());
        this.source = source;
        canvas = new JsonGraphCanvas(this::selectSource, this::edit, this::move);
        JScrollPane scroll = new JScrollPane(canvas);
        scroll.getVerticalScrollBar().setUnitIncrement(28);
        scroll.getHorizontalScrollBar().setUnitIncrement(28);
        add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(WorkspaceStyle.PAPER);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, WorkspaceStyle.LINE));
        status.setForeground(WorkspaceStyle.MUTED);
        status.setFont(status.getFont().deriveFont(status.getFont().getSize2D() - 1));
        status.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 4));
        footer.add(status, BorderLayout.CENTER);
        JPanel zoom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 2));
        zoom.setOpaque(false);
        JButton minus = new JButton(WorkspaceIcon.MINUS);
        JButton plus = new JButton(WorkspaceIcon.PLUS);
        WorkspaceStyle.toolButton(minus, "缩小");
        WorkspaceStyle.toolButton(plus, "放大");
        WorkspaceStyle.toolButton(scale, "恢复原始大小");
        minus.addActionListener(e -> zoomTo(canvas.zoom() - 0.25));
        plus.addActionListener(e -> zoomTo(canvas.zoom() + 0.25));
        scale.addActionListener(e -> zoomTo(1));
        JButton arrange = new JButton("自动排列", WorkspaceIcon.ARRANGE);
        WorkspaceStyle.toolButton(arrange, "恢复本标签页的默认卡片位置和大小");
        arrange.addActionListener(e -> canvas.resetLayout());
        zoom.add(arrange);
        zoom.add(minus);
        zoom.add(scale);
        zoom.add(plus);
        footer.add(zoom, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        setMinimumSize(new Dimension(120, 90));

        for (int shortcut : new int[]{InputEvent.META_DOWN_MASK, InputEvent.CTRL_DOWN_MASK}) {
            registerKeyboardAction(e -> source.undoLastAction(), KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut),
                    WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            registerKeyboardAction(e -> source.redoLastAction(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut | InputEvent.SHIFT_DOWN_MASK),
                    WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        }
        refreshTimer = new Timer(240, e -> refresh());
        refreshTimer.setRepeats(false);
        source.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
        });
        source.addCaretListener(e -> revealCaret());
        source.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (!active || !SwingUtilities.isLeftMouseButton(e)) return;
                if (dirty) refresh();
                else revealCaret();
            }
        });
    }

    private void revealCaret() {
        if (active && !dirty && !writing && !selectingSource && sourceRoot != null) {
            canvas.reveal(sourceRoot.at(source.getCaretPosition()).indexPath());
        }
    }

    private JsonSourceTree.Node rangeOf(GraphNode node) {
        if (node.parent == null) return sourceRoot;
        return (JsonSourceTree.Node) rangeOf(node.parent).getChildAt(node.index);
    }

    private void selectSource(GraphNode node) {
        if (!active || writing || dirty || sourceRoot == null) return;
        selectingSource = true;
        try {
            rangeOf(node).selectIn(source);
        } finally {
            selectingSource = false;
        }
    }

    private void zoomTo(double zoom) {
        canvas.zoomTo(zoom);
        scale.setText(Math.round(canvas.zoom() * 100) + "%");
    }

    private void changed() {
        if (writing) return;
        dirty = true;
        if (active) {
            if (snapshot != null) canvas.display(null, "正在更新图形…");
            snapshot = null;
            sourceRoot = null;
            refreshTimer.restart();
        }
    }

    void setActive(boolean active) {
        this.active = active;
        refreshTimer.stop();
        if (active && dirty) refresh();
        else if (active) revealCaret();
    }

    private void refresh() {
        refreshTimer.stop();
        dirty = false;
        String text = source.getText();
        if (text.trim().isEmpty()) {
            showMessage("在左侧输入或粘贴 JSON");
            return;
        }
        try {
            display(text, GraphNode.parse(text));
        } catch (IllegalArgumentException ex) {
            showMessage("请修正左侧 JSON 后继续编辑");
            status.setText(errorText(ex));
            status.setToolTipText(status.getText());
        }
    }

    private void showMessage(String message) {
        snapshot = null;
        sourceRoot = null;
        canvas.display(null, message);
        status.setText(" ");
        status.setToolTipText(null);
    }

    private void display(String text, JsonElement value) {
        sourceRoot = JsonSourceTree.build(text, value);
        snapshot = new GraphNode(value);
        canvas.display(snapshot, null);
        revealCaret();
        status.setText("单击定位原文 · 双击编辑");
        status.setToolTipText("标题箭头展开或折叠，拖动标题移动卡片，拖右下角调整大小，拖动字段行更换父节点");
    }

    private void edit(GraphNode node) {
        GraphNode original = snapshot;
        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        JTextArea key = new JTextArea(node.key, 1, 24);
        if (node.key != null) {
            fields.add(new JLabel("键名"));
            fields.add(new JScrollPane(key));
        }
        fields.add(new JLabel("值（JSON，字符串需加双引号）"));
        JTextArea value = new JTextArea(GraphNode.JSON.toJson(node.value), 9, 34);
        value.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        value.setLineWrap(true);
        value.setCaretPosition(0);
        fields.add(new JScrollPane(value));
        confirm(original, "编辑节点 " + node.label(), fields,
                () -> node.edit(key.getText(), GraphNode.parse(value.getText())));
    }

    private void move(GraphNode node, GraphNode target) {
        if (target.value.isJsonObject()) {
            JTextArea key = new JTextArea(node.key, 1, 24);
            confirm(snapshot, "移入对象 " + target.label(), new Object[]{"目标键名", new JScrollPane(key)},
                    () -> node.move(target, key.getText(), -1));
        } else {
            int maxIndex = target.value.getAsJsonArray().size() - (target == node.parent ? 1 : 0);
            JSpinner index = new JSpinner(new SpinnerNumberModel(maxIndex, 0, maxIndex, 1));
            confirm(snapshot, "移入数组 " + target.label(), new Object[]{"移动后的下标（从 0 开始）", index},
                    () -> {
                        try {
                            index.commitEdit();
                        } catch (java.text.ParseException ex) {
                            throw new IllegalArgumentException("请输入有效的数组下标。");
                        }
                        return node.move(target, null, (Integer) index.getValue());
                    });
        }
    }

    private void confirm(GraphNode original, String title, Object fields, Supplier<JsonElement> change) {
        while (JOptionPane.showConfirmDialog(this, fields, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            // A modal dialog has its own event loop. Text may have changed while it was open.
            if (!active || original != snapshot) {
                JOptionPane.showMessageDialog(this, "原文已更新，请重新选择节点。");
                return;
            }
            try {
                write(change.get());
                return;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, errorText(ex), title, JOptionPane.WARNING_MESSAGE);
                if (original != snapshot) return;
            }
        }
    }

    private void write(JsonElement updated) {
        String json = GraphNode.JSON.toJson(updated);
        JsonElement checked;
        try {
            // A subtree move or replacement can exceed the parser's nesting limit.
            checked = GraphNode.parse(json);
        } catch (IllegalArgumentException ex) {
            refresh();
            throw ex;
        }
        writing = true;
        try {
            source.replaceRange(json, 0, source.getDocument().getLength());
        } finally {
            writing = false;
        }
        display(json, checked);
    }

    private static String errorText(IllegalArgumentException ex) {
        String message = ex.getMessage();
        return message == null ? "JSON 格式无效" : message.split("\n", 2)[0];
    }

    @Override public void removeNotify() {
        refreshTimer.stop();
        super.removeNotify();
    }
}
