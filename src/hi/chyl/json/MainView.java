package hi.chyl.json;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.application.Application;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;

public class MainView extends FrameView {
    private static final int DEFAULT_FONT_SIZE = 13;
    private int currentFontSize = DEFAULT_FONT_SIZE;
    private final EnumMap<WorkspaceIcon, Action> commands = new EnumMap<>(WorkspaceIcon.class);
    private int documentCount;

    private JDialog aboutBox;
    private JTabbedPane tabbedPane;
    private boolean isTxtFindDlgOpen = false;
    private boolean isTreeFinDlgdOpen = false;
    private List<TreePath> treePathLst = new ArrayList<TreePath>();
    private int curPos = 0;
    private ResourceMap resourceMap;
    
    public MainView(SingleFrameApplication app) {
        super(app);
        resourceMap = Application.getInstance(MainApp.class).getContext().getResourceMap(MainView.class);
        initUI();
    }


    private void initUI() {
        WorkspaceStyle.install();
        getFrame().setIconImage(new ImageIcon(getClass().getResource("resources/json.png")).getImage());
        registerCommands();
        tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setBackground(WorkspaceStyle.CHROME);
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_HEIGHT, 30);
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true);
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK, (IntConsumer) this::closeTab);
        tabbedPane.addChangeListener(e -> treePathLst.clear());
        newDocument();
        setMenuBar(createMenuBar());
        setToolBar(createToolBar());
        setComponent(tabbedPane);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        getFrame().setPreferredSize(new Dimension(Math.min(screen.width, 1160), Math.min(screen.height, 740)));
    }

    private void registerCommands() {
        command(WorkspaceIcon.NEW, "新建标签页", KeyEvent.VK_N, this::newDocument);
        command(WorkspaceIcon.OPEN, "打开文件…", KeyEvent.VK_O, () -> chooseFile(FileDialog.LOAD));
        command(WorkspaceIcon.SAVE, "保存文件…", KeyEvent.VK_S, () -> chooseFile(FileDialog.SAVE));
        command(WorkspaceIcon.CLOSE, "关闭标签页", 0, () -> closeTab(getTabIndex()));
        command(WorkspaceIcon.RENAME, "重命名标签页…", 0, () -> rename(false));
        command(WorkspaceIcon.WINDOW, "修改窗口标题…", 0, () -> rename(true));
        command(WorkspaceIcon.FORMAT, "格式化", KeyEvent.VK_F, this::formatJson);
        command(WorkspaceIcon.EXPAND, "展开嵌套 JSON", KeyEvent.VK_E, this::expandAllJson);
        command(WorkspaceIcon.PASTE, "粘贴并格式化", 0, () -> { getTextArea().paste(); formatJson(); });
        command(WorkspaceIcon.CLEAR, "清空文本", KeyEvent.VK_D, () -> getTextArea().setText(""));
        command(WorkspaceIcon.NEWLINE, "移除换行符", 0, () -> getTextArea().setText(getTextArea().getText().replace("\n", "")));
        command(WorkspaceIcon.SLASH, "移除反斜杠", 0, () -> getTextArea().setText(getTextArea().getText().replace("\\", "")));
        command(WorkspaceIcon.SEARCH, "查找文本…", 0, () -> {
            if (!isTxtFindDlgOpen) showFindDialog(1, "查找文本");
        });
        command(WorkspaceIcon.FIND_NODE, "查找树节点…", 0, () -> {
            if (isTreeFinDlgdOpen) return;
            selectedTab().modes.setSelectedIndex(0);
            showFindDialog(2, "查找树节点");
        });
        command(WorkspaceIcon.LAYOUT, "切换横向 / 纵向布局", KeyEvent.VK_L, this::changeLayout);
        command(WorkspaceIcon.CONVERT, "Unicode 转换…", KeyEvent.VK_T, this::codeChangeAction);
        command(WorkspaceIcon.ABOUT, "关于 HiJson", 0, this::showAboutBox);
        command(WorkspaceIcon.EXIT, "退出 HiJson", 0, () -> MainApp.getApplication().exit());
    }

    private void command(WorkspaceIcon icon, String label, int key, Runnable run) {
        Action action = new AbstractAction(label, icon) {
            @Override public void actionPerformed(ActionEvent e) { run.run(); }
        };
        if (key != 0) action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK));
        commands.put(icon, action);
    }

    private void newDocument() {
        addTab("未命名 " + ++documentCount, true);
    }

    private void closeTab(int index) {
        if (index < 0) return;
        tabbedPane.removeTabAt(index);
        if (tabbedPane.getTabCount() == 0) newDocument();
    }

    private void rename(boolean window) {
        int index = getTabIndex();
        String current = window ? getFrame().getTitle() : tabbedPane.getTitleAt(index);
        String name = (String) JOptionPane.showInputDialog(getFrame(), window ? "窗口标题" : "标签页名称",
                window ? "修改窗口标题" : "重命名标签页", JOptionPane.PLAIN_MESSAGE, null, null, current);
        if (name == null || name.trim().isEmpty()) return;
        if (window) getFrame().setTitle(name.trim());
        else tabbedPane.setTitleAt(index, name.trim());
    }

    private JButton tool(WorkspaceIcon icon, String label) {
        JButton button = new JButton(commands.get(icon));
        button.setText(label);
        WorkspaceStyle.toolButton(button, (String) commands.get(icon).getValue(Action.NAME));
        return button;
    }

    private JPanel toolbarGroup(Component... buttons) {
        JPanel group = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 3));
        group.setOpaque(false);
        for (Component button : buttons) group.add(button);
        return group;
    }

    private JToolBar createToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBackground(WorkspaceStyle.PAPER);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, WorkspaceStyle.LINE),
                BorderFactory.createEmptyBorder(UIScale.scale(3), UIScale.scale(8), UIScale.scale(3), UIScale.scale(8))));
        JButton open = tool(WorkspaceIcon.OPEN, "打开");
        JButton save = tool(WorkspaceIcon.SAVE, "保存");
        JButton format = tool(WorkspaceIcon.FORMAT, "格式化");
        format.putClientProperty("JButton.buttonType", null);
        format.setBackground(WorkspaceStyle.ACCENT);
        format.setForeground(Color.WHITE);
        format.setMargin(new Insets(3, 10, 3, 10));
        JButton expand = tool(WorkspaceIcon.EXPAND, "展开嵌套 JSON");
        JButton search = tool(WorkspaceIcon.SEARCH, "查找");
        JButton more = new JButton(WorkspaceIcon.MORE);
        WorkspaceStyle.toolButton(more, "更多操作");
        more.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            for (WorkspaceIcon icon : new WorkspaceIcon[]{WorkspaceIcon.OPEN, WorkspaceIcon.SAVE, WorkspaceIcon.EXPAND,
                    WorkspaceIcon.SEARCH, WorkspaceIcon.FIND_NODE}) menu.add(commands.get(icon));
            menu.addSeparator();
            menu.add(commands.get(WorkspaceIcon.PASTE));
            menu.add(commands.get(WorkspaceIcon.CLEAR));
            menu.add(commands.get(WorkspaceIcon.LAYOUT));
            menu.add(commands.get(WorkspaceIcon.RENAME));
            menu.show(more, more.getWidth() - menu.getPreferredSize().width, more.getHeight());
        });
        bar.add(tool(WorkspaceIcon.NEW, "新建"));
        bar.add(open);
        bar.add(save);
        bar.addSeparator(UIScale.scale(new Dimension(12, 18)));
        bar.add(format);
        bar.add(expand);
        bar.add(Box.createHorizontalGlue());
        bar.add(search);
        bar.add(more);
        int fullWidth = bar.getPreferredSize().width;
        bar.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                boolean wide = bar.getWidth() >= fullWidth;
                for (JButton secondary : new JButton[]{open, save, expand, search}) secondary.setVisible(wide);
            }
        });
        return bar;
    }

    private int getTabIndex() { return tabbedPane.getSelectedIndex(); }

    private JMenu menu(String label, WorkspaceIcon icon, WorkspaceIcon... items) {
        JMenu menu = new JMenu(label);
        menu.setIcon(icon);
        for (WorkspaceIcon item : items) {
            if (item == null) menu.addSeparator();
            else menu.add(commands.get(item));
        }
        return menu;
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.add(menu("文件", WorkspaceIcon.DOCUMENT, WorkspaceIcon.NEW, WorkspaceIcon.OPEN, WorkspaceIcon.SAVE,
                null, WorkspaceIcon.RENAME, WorkspaceIcon.CLOSE, null, WorkspaceIcon.EXIT));
        bar.add(menu("编辑", WorkspaceIcon.RENAME, WorkspaceIcon.FORMAT, WorkspaceIcon.EXPAND, WorkspaceIcon.PASTE,
                null, WorkspaceIcon.SEARCH, WorkspaceIcon.FIND_NODE, null, WorkspaceIcon.NEWLINE,
                WorkspaceIcon.SLASH, WorkspaceIcon.CLEAR));
        bar.add(menu("工具", WorkspaceIcon.TOOLS, WorkspaceIcon.LAYOUT, WorkspaceIcon.WINDOW, WorkspaceIcon.CONVERT));
        bar.add(menu("帮助", WorkspaceIcon.HELP, WorkspaceIcon.ABOUT));
        return bar;
    }

    public void receiveExternalText(String text) {
        addTab("传入文本", true);
        JTextArea ta = getTextArea();
        if (ta != null) {
            ta.setText(text);
            formatJson();
        }
    }

    public void showAboutBox() {
        JFrame mainFrame = MainApp.getApplication().getMainFrame();
        if (aboutBox == null) {
            aboutBox = new MainAboutBox(mainFrame);
        }
        aboutBox.setLocationRelativeTo(mainFrame);
        MainApp.getApplication().show(aboutBox);
    }

    private static final class EditorTab extends JSplitPane {
        final RSyntaxTextArea source;
        final JTree tree;
        final JTable table;
        final JTabbedPane modes = new JTabbedPane();
        final JsonTextFile file = new JsonTextFile();
        TreeSourceLink link;

        EditorTab(RSyntaxTextArea source, JTree tree, JTable table) {
            this.source = source;
            this.tree = tree;
            this.table = table;
        }
    }

    private JPanel sourcePanel(RSyntaxTextArea source) {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(WorkspaceStyle.PAPER);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, WorkspaceStyle.LINE),
                BorderFactory.createEmptyBorder(0, UIScale.scale(12), 0, UIScale.scale(12))));
        header.setPreferredSize(UIScale.scale(new Dimension(0, 33)));
        JLabel title = new JLabel("源文本", WorkspaceIcon.CODE, JLabel.LEADING);
        title.setIconTextGap(UIScale.scale(7));
        title.setForeground(WorkspaceStyle.INK);
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        JLabel position = new JLabel("行 1  ·  列 1");
        position.setForeground(WorkspaceStyle.MUTED);
        position.setFont(position.getFont().deriveFont(position.getFont().getSize2D() - 1));
        source.addCaretListener(e -> {
            int offset = e.getDot();
            Element lines = source.getDocument().getDefaultRootElement();
            int row = lines.getElementIndex(offset);
            int column = offset - lines.getElement(row).getStartOffset();
            position.setText("行 " + (row + 1) + "  ·  列 " + (column + 1));
        });
        header.add(title, BorderLayout.WEST);
        header.add(position, BorderLayout.EAST);
        RTextScrollPane scroll = new RTextScrollPane(source);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setFoldIndicatorEnabled(true);
        scroll.getGutter().setBackground(WorkspaceStyle.PAPER);
        scroll.getGutter().setLineNumberColor(WorkspaceStyle.MUTED);
        scroll.getGutter().setBorderColor(WorkspaceStyle.LINE);
        scroll.getGutter().setFoldIndicatorForeground(WorkspaceStyle.MUTED);
        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.setMinimumSize(UIScale.scale(new Dimension(100, 75)));
        return panel;
    }

    private void initializeDivider(JSplitPane split, double ratio) {
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setContinuousLayout(true);
        split.setResizeWeight(ratio);
        split.addHierarchyListener(new HierarchyListener() {
            @Override public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !split.isShowing()) return;
                split.removeHierarchyListener(this);
                // Nested panes acquire their final bounds after the document tab is shown.
                SwingUtilities.invokeLater(() -> {
                    if (!split.isShowing()) {
                        split.addHierarchyListener(this);
                        return;
                    }
                    split.getTopLevelAncestor().validate();
                    if (split.getRightComponent() != null) split.setDividerLocation(ratio);
                    split.validate();
                });
            }
        });
    }

    private JSplitPane newTabSplitPane() {
        final EditorTab splitPane = new EditorTab(newTextArea(), newTree(), newTable());
        final RSyntaxTextArea textArea = splitPane.source;
        textArea.setMargin(UIScale.scale(new Insets(8, 7, 8, 7)));
        splitPane.setLeftComponent(sourcePanel(textArea));

        final JTree tree = splitPane.tree;
        final JTable table = splitPane.table;
        JScrollPane treeScroll = new JScrollPane(tree);
        JScrollPane fields = new JScrollPane(table);
        treeScroll.setMinimumSize(UIScale.scale(new Dimension(72, 30)));
        fields.setMinimumSize(UIScale.scale(new Dimension(60, 30)));
        fields.getViewport().setBackground(Color.WHITE);
        JSplitPane details = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, fields);
        initializeDivider(details, 0.62);
        JToggleButton inspector = new JToggleButton(WorkspaceIcon.INSPECTOR, true);
        WorkspaceStyle.toolButton(inspector, "显示字段表");
        int dividerSize = details.getDividerSize();
        inspector.addActionListener(new ActionListener() {
            private double proportion = 0.62;
            @Override public void actionPerformed(ActionEvent e) {
                if (inspector.isSelected()) {
                    details.setRightComponent(fields);
                    details.setDividerSize(dividerSize);
                    details.setDividerLocation(proportion);
                } else {
                    int width = details.getWidth() - dividerSize;
                    if (width > 0) proportion = Math.max(0, Math.min(1, details.getDividerLocation() / (double) width));
                    details.setRightComponent(null);
                    details.setDividerSize(0);
                }
            }
        });
        tree.addTreeSelectionListener(e -> treeSelection(tree, table));
        splitPane.link = new TreeSourceLink(textArea, tree, () -> {
            ((DefaultTableModel) table.getModel()).setRowCount(0);
            if (splitPane == selectedTab()) treePathLst.clear();
        });
        final JsonGraphPanel graph = new JsonGraphPanel(textArea);
        splitPane.modes.setBackground(WorkspaceStyle.PAPER);
        splitPane.modes.setMinimumSize(UIScale.scale(new Dimension(145, 80)));
        splitPane.modes.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        splitPane.modes.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_HEIGHT, 32);
        splitPane.modes.putClientProperty(FlatClientProperties.TABBED_PANE_TRAILING_COMPONENT, toolbarGroup(inspector));
        splitPane.modes.addTab("树状", WorkspaceIcon.TREE, details);
        splitPane.modes.addTab("图形", WorkspaceIcon.GRAPH, graph);
        splitPane.modes.addChangeListener(e -> {
            boolean graphMode = splitPane.modes.getSelectedComponent() == graph;
            inspector.setVisible(!graphMode);
            splitPane.link.setActive(!graphMode);
            graph.setActive(graphMode);
        });
        splitPane.setRightComponent(splitPane.modes);
        initializeDivider(splitPane, 0.42);

        // Ctrl/Cmd + 鼠标滚轮调节字体大小
        splitPane.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                if (e.isControlDown() || e.isMetaDown()) {
                    int delta = e.getWheelRotation() < 0 ? 1 : -1;
                    int newSize = currentFontSize + delta;
                    if (newSize < 8 || newSize > 28) return;
                    currentFontSize = newSize;
                    Font f = new Font("Monospaced", Font.PLAIN, newSize);
                    if (textArea != null) textArea.setFont(f);
                    if (tree != null) {
                        tree.setFont(f);
                        tree.setRowHeight(Math.max(18, newSize + 13));
                    }
                    if (table != null) {
                        table.setFont(f);
                        table.setRowHeight(Math.max(20, newSize + 8));
                        if (table.getTableHeader() != null)
                            table.getTableHeader().setFont(f);
                    }
                    e.consume();
                }
            }
        });

        return splitPane;
    }

    private RSyntaxTextArea newTextArea(){
        RSyntaxTextArea textArea = new RSyntaxTextArea() {
            @Override public Color getForegroundForToken(Token token) {
                if (token.type == Token.LITERAL_STRING_DOUBLE_QUOTE) {
                    Token next = token.getNextToken();
                    while (next != null && next.isWhitespace()) next = next.getNextToken();
                    if (next != null && next.isPaintable() && next.isSingleChar(':')) return WorkspaceStyle.INK;
                }
                return super.getForegroundForToken(token);
            }
        };
        textArea.setFont(new Font("Monospaced", Font.PLAIN, DEFAULT_FONT_SIZE));
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setAutoscrolls(true);

        SyntaxScheme scheme = textArea.getSyntaxScheme();
        scheme.getStyle(Token.LITERAL_STRING_DOUBLE_QUOTE).foreground = WorkspaceStyle.TEAL;
        scheme.getStyle(Token.LITERAL_NUMBER_DECIMAL_INT).foreground = WorkspaceStyle.AMBER;
        scheme.getStyle(Token.LITERAL_NUMBER_FLOAT).foreground = WorkspaceStyle.AMBER;
        scheme.getStyle(Token.LITERAL_BOOLEAN).foreground = WorkspaceStyle.VIOLET;
        scheme.getStyle(Token.RESERVED_WORD).foreground = WorkspaceStyle.VIOLET;
        scheme.getStyle(Token.OPERATOR).foreground = WorkspaceStyle.INK;
        scheme.getStyle(Token.SEPARATOR).foreground = WorkspaceStyle.MUTED;
        textArea.setForeground(WorkspaceStyle.INK);
        textArea.setBackground(Color.WHITE);
        textArea.setCurrentLineHighlightColor(WorkspaceStyle.PAPER);
        textArea.setSelectionColor(WorkspaceStyle.SELECTION);
        textArea.setCaretColor(WorkspaceStyle.ACCENT);
        configureTextMenu(textArea);
        return textArea;
    }

    private JTree newTree(){
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("o-JSON");
        DefaultTreeModel model = new DefaultTreeModel(root);
        JTree tree = new JTree(model);
        tree.setFont(new Font("Monospaced", Font.PLAIN, DEFAULT_FONT_SIZE));
        setNodeIcon(tree);
        tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 0));
        installTreeMenu(tree);
        return tree;
    }

    private JTable newTable(){
        String[] col = {"键", "值"};
        DefaultTableModel tm = new DefaultTableModel();
        tm.setColumnCount(2);
        tm.setColumnIdentifiers(col);
        JTable table = new JTable(tm) {
            @Override public boolean getScrollableTracksViewportWidth() {
                return getParent() instanceof JViewport && getPreferredSize().width <= getParent().getWidth();
            }
        };
        table.setFont(new Font("Monospaced", Font.PLAIN, DEFAULT_FONT_SIZE));
        table.setForeground(WorkspaceStyle.INK);
        table.setRowHeight(UIScale.scale(23));
        table.setShowHorizontalLines(true);
        table.setGridColor(WorkspaceStyle.CHROME);
        table.getTableHeader().setBackground(WorkspaceStyle.PAPER);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setAutoscrolls(true);
        table.setMinimumSize(new Dimension(160, 100));
        return table;
    }

    private void treeSelection(JTree tree,JTable table){
        DefaultMutableTreeNode selNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if(selNode==null){
            //System.out.println("jTree1ValueChanged:selNode is null");
            return;
        }
        int rows = selNode instanceof JsonSourceTree.Node ? Math.max(1, selNode.getChildCount()) : 0;
        Object[][] data = new Object[rows][2];
        for (int i = 0; i < rows; i++) {
            JsonSourceTree.Node node = (JsonSourceTree.Node) (selNode.isLeaf() ? selNode : selNode.getChildAt(i));
            data[i][0] = node.key;
            data[i][1] = node.summary;
        }
        ((DefaultTableModel) table.getModel()).setDataVector(data, new String[]{"键", "值"});
        TableColumn column0 = table.getColumnModel().getColumn(0);
        column0.setPreferredWidth(getPreferredWidthForColumn(table,column0));
        TableColumn column1 = table.getColumnModel().getColumn(1);
        column1.setPreferredWidth(getPreferredWidthForColumn(table,column1));
    }

    private int addTab(String tabName,boolean isSel){
        JSplitPane splitPane = newTabSplitPane();
        tabbedPane.addTab(tabName, WorkspaceIcon.DOCUMENT, splitPane);
        int newIndex = tabbedPane.getTabCount() - 1;
        if(isSel){
            tabbedPane.setSelectedIndex(newIndex);
        }
        return newIndex;
    }

    private EditorTab selectedTab() {
        return (EditorTab) tabbedPane.getSelectedComponent();
    }

    private JTextArea getTextArea() {
        EditorTab tab = selectedTab();
        return tab == null ? null : tab.source;
    }

    private JTree getTree(int index) {
        return index < 0 ? null : ((EditorTab) tabbedPane.getComponentAt(index)).tree;
    }

    private JTree getTree() {
        return getTree(getTabIndex());
    }

    private void expandAllJson() {
        formatSource(selectedTab(), true);
    }

    private JsonElement expandNestedJson(JsonElement ele) {
        if (ele.isJsonObject()) {
            JsonObject obj = ele.getAsJsonObject();
            JsonObject newObj = new JsonObject();
            for (java.util.Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                newObj.add(entry.getKey(), expandNestedJson(entry.getValue()));
            }
            return newObj;
        } else if (ele.isJsonArray()) {
            JsonArray arr = ele.getAsJsonArray();
            JsonArray newArr = new JsonArray();
            for (JsonElement child : arr) {
                newArr.add(expandNestedJson(child));
            }
            return newArr;
        } else if (ele.isJsonPrimitive()) {
            JsonPrimitive prim = ele.getAsJsonPrimitive();
            if (prim.isString()) {
                String val = prim.getAsString().trim();
                if ((val.startsWith("{") && val.endsWith("}")) || (val.startsWith("[") && val.endsWith("]"))) {
                    try {
                        JsonElement parsed = GraphNode.parse(val, Strictness.LENIENT);
                        if (parsed.isJsonObject() || parsed.isJsonArray()) {
                            return expandNestedJson(parsed);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return ele;
    }

    private void formatJson() {
        formatSource(selectedTab(), false);
    }

    private void formatSource(EditorTab tab, boolean expand) {
        if (tab == null) return;
        String text = tab.source.getText().trim();
        if (expand && text.isEmpty()) return;
        try {
            // Keep the formatter's support for relaxed object/array syntax.
            Strictness mode = text.startsWith("{") || text.startsWith("[")
                    ? Strictness.LENIENT : Strictness.STRICT;
            JsonElement value = GraphNode.parse(text, mode);
            if (expand) value = expandNestedJson(value);
            String formatted = GraphNode.JSON.toJson(value);
            tab.source.setText(formatted);
            tab.link.display(formatted, value);
        } catch (IllegalArgumentException ex) {
            showMessageDialog("非法JSON字符串！", ex.getMessage());
        }
    }

    private void setNodeIcon(JTree tree) {
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                    boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                String label = value.toString();
                if (label.startsWith(Kit.sObj)) setIcon(WorkspaceIcon.OBJECT);
                else if (label.startsWith(Kit.sArr)) setIcon(WorkspaceIcon.ARRAY);
                else if (label.startsWith(Kit.sNum)) setIcon(WorkspaceIcon.NUMBER);
                else if (label.startsWith(Kit.sNull)) setIcon(WorkspaceIcon.NULL);
                else if (label.startsWith(Kit.sBool)) setIcon(WorkspaceIcon.BOOLEAN);
                else setIcon(WorkspaceIcon.STRING);
                setIconTextGap(UIScale.scale(6));
                setText(label.substring(2));
                return this;
            }
        });
    }

    private void showMessageDialog(String title,String msg){
        if(msg==null) msg = "";
        String ex = "com.google.gson.stream.MalformedJsonException:";
        int index = msg.indexOf(ex);
        if(index>=0){
            msg = msg.substring(index+ex.length());
        }
        JDialog dlg = new JDialog(getFrame());
        dlg.setTitle(title);
        dlg.setMinimumSize(new Dimension(350, 160));
        BorderLayout layout = new BorderLayout();
        dlg.getContentPane().setLayout(layout);
        dlg.getContentPane().add(new JLabel("异常信息："),  BorderLayout.NORTH);
        JTextArea ta = new JTextArea();
        ta.setLineWrap(true);
        ta.setText(msg);
        ta.setWrapStyleWord(true);
        dlg.getContentPane().add(new JScrollPane(ta), BorderLayout.CENTER);
        MainApp.getApplication().show(dlg);
    }

    //[start]自动调列宽
    private int getPreferredWidthForColumn(JTable table,TableColumn col) {
        int hw = columnHeaderWidth(table,col);  // hw = header width
        int cw = widestCellInColumn(table,col);  // cw = column width
        return hw > cw ? hw : cw;

    }
    private int columnHeaderWidth(JTable table,TableColumn col) {
        TableCellRenderer renderer = table.getTableHeader().getDefaultRenderer();
        Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
        return comp.getPreferredSize().width;
    }
    private int widestCellInColumn(JTable table,TableColumn col) {
        int c = col.getModelIndex();
        int width = 0, maxw = 0;
        for (int r = 0; r < table.getRowCount(); r++) {
            TableCellRenderer renderer = table.getCellRenderer(r, c);
            Component comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, c), false, false, r, c);
            width = comp.getPreferredSize().width;
            maxw = width > maxw ? width : maxw;
        }
        if(maxw<90)maxw = 90;
        return maxw + 10;
    }
    //[end]自动调列宽

    private void modifyDialgTitle(JDialog dlg,boolean flag,int n){
        String[] tmp = dlg.getTitle().split("-");
        if(n==-1){
            dlg.setTitle(tmp[0] + "-" + "  ==");
            return;
        }
        if (flag) {
            dlg.setTitle(tmp[0] + "-" + "  找到了^_^");
        } else {
            dlg.setTitle(tmp[0] + "-" + "  没找到╮(╯_╰)╭");
        }
    }

    private TreePath expandTreeNode(JTree tree,TreeNode[] arr, Boolean expand) {
        TreePath[] tp = new TreePath[arr.length];
        tp[0] = new TreePath(arr[0]);
        int pos = 0;
        for (int i = 1; i < arr.length; i++) {
            tp[i] = tp[i - 1].pathByAddingChild(arr[i]);
        }
        for (int i = 0; i < arr.length; i++) {
            if (expand) {
                tree.expandPath(tp[i]);
            } else {
                tree.collapsePath(tp[i]);
            }
            pos = i;
        }
        return tp[pos];
    }

    private void findTreeChildValue(String findText,List<TreePath> treePathLst) {
        JTree tree = getTree();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getModel().getRoot();
        Enumeration e = root.depthFirstEnumeration();
        treePathLst.clear();
        curPos = 0;
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.isLeaf()) {
                String str = node.toString();
                if (str.substring(2).indexOf(findText) >= 0) {
                    tree.expandPath(new TreePath(node.getPath()));
                    TreePath tp = expandTreeNode(tree,node.getPath(), true);
                    treePathLst.add(tp);
                }
            }
        }
        if(!treePathLst.isEmpty()){
            tree.setSelectionPath(treePathLst.get(0));
            tree.scrollPathToVisible(treePathLst.get(0));
        }
//        return treePathLst;
    }

    /**
     * 打开查找对话框
     * @param type 查找类型（1：文本查找，2树节点查找）
     * @param title 打开的窗口标题名称
     */
    private void showFindDialog(final int type,String title){
        final  JDialog openDlg = new JDialog(getFrame());
        openDlg.setTitle(title);
        openDlg.setModal(false);
        openDlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        openDlg.setResizable(false);
        java.awt.Container pane =  openDlg.getContentPane();
        FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
        pane.setLayout(layout);
        JButton btnFind = new JButton("查找", WorkspaceIcon.SEARCH);
        JButton btnNext = new JButton("下一个");
        JButton btnPrev = new JButton("上一个");
        final JTextField textFieldFind = new JTextField(22);
        pane.add(textFieldFind);
        pane.add(btnFind);
        pane.add(btnPrev);
        pane.add(btnNext);
        openDlg.getRootPane().setDefaultButton(btnFind);
        openDlg.pack();
        openDlg.setLocationRelativeTo(getFrame());
        //从头开始查找
        btnFind.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean flag = false;
                modifyDialgTitle(openDlg,flag,-1);
                if(type==1){
                    flag =  startSegmentFindOrReplaceOperation(getTextArea(),textFieldFind.getText(), true, true,true);
                }else{
                    findTreeChildValue(textFieldFind.getText(),treePathLst);
                    if(!treePathLst.isEmpty()) flag = true;
                }
                modifyDialgTitle(openDlg,flag,1);
            }
        });
        //向下查找
        btnNext.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean flag = false;
                modifyDialgTitle(openDlg,flag,-1);
                JTree tree = getTree();
                if(type==1){
                    flag =  startSegmentFindOrReplaceOperation(getTextArea(),textFieldFind.getText(), true, true,false);
                }else{
                    curPos++;
                    if(curPos<treePathLst.size()){
                        tree.setSelectionPath(treePathLst.get(curPos));
                        tree.scrollPathToVisible(treePathLst.get(curPos));
                        flag = true;
                    }else{
                        curPos = treePathLst.size() - 1;
                    }
                }
                modifyDialgTitle(openDlg,flag,1);
            }
        });
        //向上查找
        btnPrev.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean flag = false;
                JTree tree = getTree();
                modifyDialgTitle(openDlg,flag,-1);
                 if(type==1){
                    flag = startSegmentFindOrReplaceOperation(getTextArea(),textFieldFind.getText(), true, false,false);
                }else{
                    curPos--;
                    if(curPos>=0){
                        tree.setSelectionPath(treePathLst.get(curPos));
                        tree.scrollPathToVisible(treePathLst.get(curPos));
                        flag = true;
                    }else{
                        curPos = 0;
                    }
                }
                modifyDialgTitle(openDlg,flag,1);
            }
        });

        openDlg.addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent e) {}
            public void windowClosing(WindowEvent e) {
                treePathLst.clear();
                if(type == 1){
                    isTxtFindDlgOpen = false;
                }else{
                    isTreeFinDlgdOpen = false;
                }
                System.gc();
            }
            public void windowClosed(WindowEvent e) { }
            public void windowIconified(WindowEvent e) {}
            public void windowDeiconified(WindowEvent e) {}
            public void windowActivated(WindowEvent e) {}
            public void windowDeactivated(WindowEvent e) {}
        });

        MainApp.getApplication().show(openDlg);

        if(type == 1){
            isTxtFindDlgOpen = true;
        }else{
            isTreeFinDlgdOpen = true;
        }
    }

    /**
     * 文本内容查找定位
     * @param key 要查找的字符串
     * @param ignoreCase 是否区分大小写
     * @param down  查找方向（向上false，向下true）
     * @param isFirst 是否从开头开始查找
     * @return
     */
    public boolean startSegmentFindOrReplaceOperation(JTextArea textArea, String key, boolean ignoreCase, boolean down,boolean isFirst) {
        int length = key.length();
        Document doc = textArea.getDocument();
        int offset = textArea.getCaretPosition();
        int charsLeft = doc.getLength() - offset;
        if(charsLeft <=0 ){
            offset = 0;
            charsLeft = doc.getLength() - offset;
        }
        if (!down) {
            offset -= length;
            offset--;
            charsLeft = offset;
        }
        if(isFirst){
            offset = 0;
            charsLeft = doc.getLength() - offset;
        }
        Segment text = new Segment();
        text.setPartialReturn(true);
        try {
            while (charsLeft > 0) {
                doc.getText(offset, length, text);
                if ((ignoreCase == true && text.toString().equalsIgnoreCase(key))
                        || (ignoreCase == false && text.toString().equals(key))) {
                    textArea.requestFocus();////焦点,才能能看到效果
                    textArea.setSelectionStart(offset);
                    textArea.setSelectionEnd(offset + length);
                    return true;
                }
                charsLeft--;
                if (down) {
                    offset++;
                } else {
                    offset--;
                }

            }
        } catch (Exception e) {

        }
        return false;
    }

    private void changeLayout() {
        int selIndex = getTabIndex();
        if(selIndex < 0){
            return;
        }
        JSplitPane splitPane = (JSplitPane) tabbedPane.getComponentAt(selIndex);
        if (splitPane.getOrientation() == JSplitPane.VERTICAL_SPLIT) {
            splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setDividerLocation(0.45);
        } else {
            splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
            splitPane.setDividerLocation(0.45);
        }
    }

    private void installTreeMenu(JTree tree) {
        tree.addMouseListener(new TreePopupMouseListener(tree, (path, point) ->
                showTreeMenu(tree, path, point.x, point.y)));
        tree.registerKeyboardAction(e -> {
            TreePath path = tree.getSelectionPath();
            Rectangle row = path == null ? null : tree.getPathBounds(path);
            if (row != null) showTreeMenu(tree, path, row.x, row.y + row.height);
        }, KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK), JTree.WHEN_FOCUSED);
    }

    private void showTreeMenu(JTree tree, TreePath path, int x, int y) {
        Object node = path.getLastPathComponent();
        if (!(node instanceof JsonSourceTree.Node)) return;
        tree.setSelectionPath(path);
        JPopupMenu menu = new JPopupMenu();
        copyItem(menu, "复制键名", WorkspaceIcon.KEY, 1, node);
        copyItem(menu, "复制值", WorkspaceIcon.VALUE, 2, node);
        copyItem(menu, "复制键值对", WorkspaceIcon.PAIR, 3, node);
        menu.addSeparator();
        copyItem(menu, "复制节点 JSON", WorkspaceIcon.CODE, 6, path);
        copyItem(menu, "复制格式化 JSON", WorkspaceIcon.FORMAT, 7, path);
        menu.addSeparator();
        copyItem(menu, "复制路径", WorkspaceIcon.PATH, 4, path);
        copyItem(menu, "复制同路径的值", WorkspaceIcon.RELATED, 5, node);
        copyItem(menu, "复制为 Map 条目", WorkspaceIcon.CONVERT, 8, node);
        menu.show(tree, x, y);
    }

    private void copyItem(JPopupMenu menu, String label, WorkspaceIcon icon, int operation, Object target) {
        JMenuItem item = new JMenuItem(label, icon);
        item.addActionListener(new TreeNodeMenuItemActionListener(operation, target));
        menu.add(item);
    }

    private class TreeNodeMenuItemActionListener implements ActionListener{
        private int optType;
        private Object obj;
        /**
         * optType 1:key;2:value;3:key value
         * @param optType
         * @param str
         */
        public TreeNodeMenuItemActionListener(int optType,Object obj){
            this.optType = optType;
            this.obj = obj;
        }
        /**
         * 复制节点路径.
         * @param treePath
         * @return 
         */
        public String copyTreeNodePath(TreePath treePath) {
            StringBuilder path = new StringBuilder();
            for (int i = 1; i < treePath.getPathCount(); i++) {
                JsonSourceTree.Node node = (JsonSourceTree.Node) treePath.getPathComponent(i);
                JsonSourceTree.Node parent = (JsonSourceTree.Node) node.getParent();
                if (parent.value.isJsonArray()) {
                    path.append(node.key);
                } else if (node.key.isEmpty() || StringUtils.containsAny(node.key, ".[]\"\\")) {
                    path.append('[').append(GraphNode.JSON.toJson(node.key)).append(']');
                } else {
                    if (path.length() > 0) path.append('.');
                    path.append(node.key);
                }
            }
            return path.toString();
        }
        /**
         * 复制相似路径节点键值对.
         * @param treeNode
         * @return 
         */
        public String copySimilarPathKeyValue(TreeNode treeNode){
            String str = "";
            String key = ((JsonSourceTree.Node) treeNode).key;
            TreeNode node = treeNode.getParent();
            if(node==null) return "";
            node = node.getParent();
            if(node == null) return "";
            int count = node.getChildCount();
            int size = 0;
            for(int i = 0; i < count; i++){
                TreeNode child = node.getChildAt(i);
                if(child==null) continue;
                size = child.getChildCount();
                for(int i2 = 0; i2 < size; i2++){
                    TreeNode tmp = child.getChildAt(i2);
                    if(tmp==null)continue;
                    JsonSourceTree.Node entry = (JsonSourceTree.Node) tmp;
                    if (key.equals(entry.key)) {
                        str += entry.summary + "\n";
                    }
                }
            }
            return str;
        }
        public void actionPerformed(ActionEvent e) {
            if(obj==null) return;
            StringSelection stringSelection = null;
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Object selected = obj instanceof TreePath ? ((TreePath) obj).getLastPathComponent() : obj;
            if (!(selected instanceof JsonSourceTree.Node)) return;
            JsonSourceTree.Node node = (JsonSourceTree.Node) selected;
            String str = node.toString();
            String[] arr = {str.substring(0, 1), node.key, node.summary};
            if(optType == 4){
                 String path = copyTreeNodePath((TreePath)obj);
                 stringSelection = new StringSelection(path);
                 clipboard.setContents(stringSelection, null);
                 return;
            }
            else if(optType == 5){
                 stringSelection = new StringSelection(copySimilarPathKeyValue((TreeNode)obj));
                 clipboard.setContents(stringSelection, null);
                 return;
            }
            else if(optType == 6 || optType == 7){
                String content = optType == 7 ? GraphNode.JSON.toJson(node.value) : node.value.toString();
                stringSelection = new StringSelection(content);
                clipboard.setContents(stringSelection, null);
                 return;
            }
            else{
                 if("<null>".equals(arr[2])){
                        arr[2] = "null";
                 }
                if (optType == 1 || optType == 2){
                    if (optType == 2 && (Kit.cObj.equals(arr[0]) || Kit.cArr.equals(arr[0]))) {
                        arr[2] = node.value.toString();
                    }
                    stringSelection = new StringSelection(arr[optType]);
                } else if (optType == 3) {
                    stringSelection = new StringSelection(str.substring(2));
                }else if(optType == 8){
                    String temp = "\"" + arr[1] + "\",\"" + arr[2] + "\"";
                    stringSelection = new StringSelection(temp);
                }
                clipboard.setContents(stringSelection, null);
            }
        }
    }//end TreeNodeCopyActionListener

    private void configureTextMenu(RSyntaxTextArea source) {
        // Keep RSyntaxTextArea's actions so undo, selection and folding still control enablement.
        JPopupMenu menu = source.getPopupMenu();
        for (Component component : menu.getComponents()) {
            if (component instanceof JMenu) {
                JMenu folds = (JMenu) component;
                folds.setText("代码折叠");
                folds.setIcon(WorkspaceIcon.FOLD);
                for (Component child : folds.getMenuComponents())
                    if (child instanceof JMenuItem) styleTextAction((JMenuItem) child);
            } else if (component instanceof JMenuItem) styleTextAction((JMenuItem) component);
        }
        // The editor expects its folding submenu to remain last.
        int beforeFolding = menu.getComponentCount() - 1;
        menu.insert(new JMenuItem(commands.get(WorkspaceIcon.PASTE)), beforeFolding);
        menu.insert(new JMenuItem(commands.get(WorkspaceIcon.CLEAR)), beforeFolding + 1);
    }

    private void styleTextAction(JMenuItem item) {
        Action action = item.getAction();
        if (action == null) return;
        String text;
        WorkspaceIcon icon;
        switch (action.getClass().getSimpleName()) {
            case "UndoAction": text = "撤销"; icon = WorkspaceIcon.UNDO; break;
            case "RedoAction": text = "重做"; icon = WorkspaceIcon.REDO; break;
            case "CutAction": text = "剪切"; icon = WorkspaceIcon.CUT; break;
            case "CopyAction": text = "复制"; icon = WorkspaceIcon.COPY; break;
            case "PasteAction": text = "粘贴"; icon = WorkspaceIcon.PASTE; break;
            case "DeleteNextCharAction": text = "删除"; icon = WorkspaceIcon.DELETE; break;
            case "SelectAllAction": text = "全选"; icon = WorkspaceIcon.SELECT_ALL; break;
            case "ToggleCurrentFoldAction": text = "切换当前折叠"; icon = WorkspaceIcon.FOLD; break;
            case "CollapseAllCommentFoldsAction": text = "折叠注释"; icon = WorkspaceIcon.FOLD; break;
            case "CollapseAllFoldsAction": text = "全部折叠"; icon = WorkspaceIcon.COLLAPSE_ALL; break;
            case "ExpandAllFoldsAction": text = "全部展开"; icon = WorkspaceIcon.EXPAND_ALL; break;
            default: return;
        }
        item.setText(text);
        item.setIcon(icon);
    }

    private void chooseFile(int mode) {
        EditorTab tab = selectedTab();
        if (tab == null) return;
        boolean opening = mode == FileDialog.LOAD;
        String title = resourceMap.getString(opening ? "openDlg.text" : "closeDlg.text");
        FileDialog dialog = new FileDialog(getFrame(), title, mode);
        File current = tab.file.location();
        if (current != null) {
            dialog.setDirectory(current.getParent());
            if (!opening) dialog.setFile(current.getName());
        }
        try {
            dialog.setVisible(true);
            if (dialog.getFile() == null) return;
            File selected = new File(dialog.getDirectory(), dialog.getFile());
            if (opening) {
                tab.source.setText(tab.file.read(selected));
                formatSource(tab, false);
            } else {
                tab.file.write(selected, tab.source.getText());
            }
            int index = tabbedPane.indexOfComponent(tab);
            if (index >= 0) {
                tabbedPane.setTitleAt(index, selected.getName());
                tabbedPane.setToolTipTextAt(index, selected.getAbsolutePath());
            }
        } catch (IOException ex) {
            showMessageDialog(opening ? "打开文件失败" : "保存文件失败", ex.getMessage());
        } finally {
            dialog.dispose();
        }
    }

    private void codeChangeAction(){
        javax.swing.JDialog dlg = new javax.swing.JDialog(getFrame());
        dlg.setTitle(resourceMap.getString("menuItemCode.text"));
        dlg.setSize(500, 350);
        dlg.setMinimumSize(new Dimension(500, 350));
        JSplitPane spiltPane2 = new JSplitPane();
        spiltPane2.setDividerLocation(150);
        spiltPane2.setOrientation(JSplitPane.VERTICAL_SPLIT);

        final JTextArea textAreaSrc = new JTextArea();
        final JTextArea textAreaDest = new JTextArea();
        textAreaSrc.setLineWrap(true);
        textAreaDest.setLineWrap(true);

        spiltPane2.setTopComponent(new JScrollPane(textAreaSrc));
        spiltPane2.setBottomComponent(new JScrollPane(textAreaDest));

        JButton btnOK = new JButton("转换");
        btnOK.setSize(50, 25);
        java.awt.Container pane = dlg.getContentPane();
        BorderLayout layout = new BorderLayout();
        //layout.addLayoutComponent(spiltPane, BorderLayout.CENTER);
       // layout.addLayoutComponent(btnOK, BorderLayout.SOUTH);
        pane.setLayout(layout);
        pane.add(spiltPane2,  BorderLayout.CENTER);
        pane.add(btnOK,  BorderLayout.SOUTH);

        btnOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String str = textAreaSrc.getText();
                str = StringEscapeUtils.unescapeJava(str);
                textAreaDest.setText(str);
            }
        });
        MainApp.getApplication().show(dlg);

    }

}
