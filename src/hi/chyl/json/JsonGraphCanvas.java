package hi.chyl.json;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

final class JsonGraphCanvas extends JPanel {
    private static final int WIDTH = 222, HEADER = 34, ROW = 27, GAP_X = 68, GAP_Y = 24, PAD = 24;
    private static final int FOOTER = 16, MIN_WIDTH = 150;
    private static final Color PAPER = new Color(246, 248, 251), INK = new Color(39, 49, 67),
            BORDER = new Color(201, 212, 225), BLUE = new Color(49, 89, 162),
            TEAL = new Color(27, 114, 104), SELECTED = new Color(221, 235, 255);

    private static final class Card {
        final GraphNode node;
        final Card parent;
        final String path;
        final List<GraphNode> rows;
        final Rectangle bounds;
        int firstRow;

        Card(GraphNode node, Card parent, int x, int y) {
            this.node = node;
            this.parent = parent;
            String segment = node.key == null ? String.valueOf(node.index) : node.key;
            path = parent == null ? "" : parent.path + "/" + segment.replace("~", "~0").replace("/", "~1");
            rows = node.children();
            bounds = new Rectangle(x, y, WIDTH, HEADER + ROW * rowCount() + FOOTER);
        }

        int rowCount() { return node.isContainer() ? rows.size() : 1; }

        int visibleRows() {
            return Math.min(rowCount(), (bounds.height - HEADER - FOOTER) / ROW);
        }

        int minHeight() { return HEADER + FOOTER + (rowCount() == 0 ? 0 : ROW); }

        int bottom() { return bounds.y + bounds.height; }

        int right() { return bounds.x + bounds.width; }

        boolean inGrip(Point p) { return p.x >= right() - FOOTER && p.y >= bottom() - FOOTER; }

        boolean inPager(Point p) {
            return visibleRows() < rowCount() && p.y >= bottom() - FOOTER && p.x < bounds.x + 48;
        }

        GraphNode nodeAt(Point p) {
            int offset = p.y - bounds.y - HEADER;
            if (offset < 0 || rows.isEmpty() || offset >= visibleRows() * ROW) return node;
            return rows.get(firstRow + offset / ROW);
        }
    }

    private static final class Layout {
        Point location;
        Dimension size;
        long layer;
    }

    private final Map<String, Layout> layouts = new HashMap<>();
    private final List<Card> cards = new ArrayList<>();
    private GraphNode root;
    private long topLayer;
    private Card movingCard;
    private Rectangle initialBounds;
    private boolean resizing;
    private final Consumer<GraphNode> edit;
    private final BiConsumer<GraphNode, GraphNode> move;
    private Dimension diagram = new Dimension();
    private double zoom = 1;
    private GraphNode selected, dragged, target;
    private Point pressed, dragPoint, panStart, viewStart;
    private String message;

    JsonGraphCanvas(Consumer<GraphNode> edit, BiConsumer<GraphNode, GraphNode> move) {
        this.edit = edit;
        this.move = move;
        setFont(UIManager.getFont("Label.font").deriveFont(13f));
        setOpaque(true);
        setBackground(PAPER);
        setFocusable(true);
        setAutoscrolls(true);
        getAccessibleContext().setAccessibleName("JSON 节点图");
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                requestFocusInWindow();
                pressed = e.getPoint();
                Point p = diagramPoint(pressed);
                Card card = cardAt(p);
                selected = card == null ? null : card.nodeAt(p);
                if (card == null) {
                    panStart = SwingUtilities.convertPoint(JsonGraphCanvas.this, pressed, viewport());
                    viewStart = viewport().getViewPosition();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else if (card.inGrip(p) || p.y < card.bounds.y + HEADER) {
                    movingCard = card;
                    initialBounds = new Rectangle(card.bounds);
                    resizing = card.inGrip(p);
                    layouts.computeIfAbsent(card.path, key -> new Layout()).layer = ++topLayer;
                    cards.remove(card);
                    cards.add(card);
                } else if (card.inPager(p)) {
                    int step = p.x < card.bounds.x + 24 ? -card.visibleRows() : card.visibleRows();
                    card.firstRow = Math.max(0, Math.min(card.rowCount() - card.visibleRows(), card.firstRow + step));
                    pressed = null;
                } else if (p.y >= card.bounds.y + HEADER + card.visibleRows() * ROW) {
                    pressed = null;
                }
                repaint();
            }

            @Override public void mouseDragged(MouseEvent e) {
                if (pressed == null) return;
                if (panStart != null) {
                    Point p = SwingUtilities.convertPoint(JsonGraphCanvas.this, e.getPoint(), viewport());
                    setViewPosition(viewStart.x + panStart.x - p.x, viewStart.y + panStart.y - p.y);
                } else if (movingCard != null) {
                    if (pressed.distance(e.getPoint()) < 3) return;
                    int dx = (int) Math.round((e.getX() - pressed.x) / zoom);
                    int dy = (int) Math.round((e.getY() - pressed.y) / zoom);
                    Layout saved = layouts.get(movingCard.path);
                    if (resizing) {
                        movingCard.bounds.setSize(Math.max(MIN_WIDTH, initialBounds.width + dx),
                                Math.max(movingCard.minHeight(), initialBounds.height + dy));
                        movingCard.firstRow = Math.min(movingCard.firstRow, movingCard.rowCount() - movingCard.visibleRows());
                        saved.size = movingCard.bounds.getSize();
                    } else {
                        movingCard.bounds.setLocation(Math.max(0, initialBounds.x + dx), Math.max(0, initialBounds.y + dy));
                        saved.location = movingCard.bounds.getLocation();
                    }
                    updateExtent();
                    scrollRectToVisible(new Rectangle(e.getX() - 6, e.getY() - 6, 12, 12));
                    repaint();
                } else if (selected != null && selected.parent != null && pressed.distance(e.getPoint()) >= 4) {
                    dragged = selected;
                    dragPoint = e.getPoint();
                    target = containerAt(dragPoint);
                    if (!dragged.canMoveInto(target)) target = null;
                    scrollRectToVisible(new Rectangle(e.getX() - 18, e.getY() - 18, 36, 36));
                    repaint();
                }
            }

            @Override public void mouseMoved(MouseEvent e) {
                Point p = diagramPoint(e.getPoint());
                Card card = cardAt(p);
                int cursor = Cursor.DEFAULT_CURSOR;
                if (card != null) {
                    if (card.inGrip(p)) cursor = Cursor.SE_RESIZE_CURSOR;
                    else if (p.y < card.bounds.y + HEADER) cursor = Cursor.MOVE_CURSOR;
                    else if (card.inPager(p)) cursor = Cursor.HAND_CURSOR;
                }
                setCursor(Cursor.getPredefinedCursor(cursor));
            }

            @Override public void mouseReleased(MouseEvent e) {
                GraphNode from = dragged;
                GraphNode to = containerAt(e.getPoint());
                clearGesture();
                repaint();
                if (from != null && from.canMoveInto(to)) move.accept(from, to);
            }

            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    Point p = diagramPoint(e.getPoint());
                    Card card = cardAt(p);
                    if (card != null && !card.inGrip(p) && !card.inPager(p)) edit.accept(card.nodeAt(p));
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        registerKeyboardAction(e -> { if (selected != null) edit.accept(selected); },
                javax.swing.KeyStroke.getKeyStroke("ENTER"), WHEN_FOCUSED);
    }

    void display(GraphNode root, String message) {
        this.root = root;
        this.message = message;
        cards.clear();
        selected = null;
        clearGesture();
        if (root != null) {
            place(root, null, PAD, PAD);
            Set<String> paths = new HashSet<>();
            for (Card card : cards) {
                paths.add(card.path);
                Layout saved = layouts.get(card.path);
                if (saved == null) continue;
                if (saved.location != null) card.bounds.setLocation(saved.location);
                if (saved.size != null) card.bounds.setSize(saved.size.width, Math.max(card.minHeight(), saved.size.height));
            }
            layouts.keySet().retainAll(paths);
            if (!layouts.isEmpty()) {
                cards.sort(Comparator.comparingLong(card -> {
                    Layout saved = layouts.get(card.path);
                    return saved == null ? 0 : saved.layer;
                }));
            }
        }
        updateExtent();
        repaint();
    }

    void resetLayout() {
        layouts.clear();
        topLayer = 0;
        display(root, message);
        setViewPosition(0, 0);
    }

    private void updateExtent() {
        diagram = new Dimension();
        for (Card card : cards) {
            diagram.width = Math.max(diagram.width, card.right() + PAD);
            diagram.height = Math.max(diagram.height, card.bottom() + PAD);
        }
        revalidate();
    }

    private void clearGesture() {
        dragged = target = null;
        movingCard = null;
        pressed = dragPoint = panStart = viewStart = null;
        setCursor(Cursor.getDefaultCursor());
    }

    private int place(GraphNode node, Card parent, int x, int y) {
        Card card = new Card(node, parent, x, y);
        cards.add(card);
        int childY = y;
        for (GraphNode row : card.rows) {
            if (row.isContainer()) childY = place(row, card, x + WIDTH + GAP_X, childY) + GAP_Y;
        }
        return Math.max(card.bottom(), childY - GAP_Y);
    }

    double zoom() { return zoom; }

    void zoomTo(double requested) {
        double next = Math.max(0.25, Math.min(2, requested));
        JViewport vp = viewport();
        Point p = vp.getViewPosition();
        Dimension size = vp.getExtentSize();
        double ratio = next / zoom;
        zoom = next;
        revalidate();
        vp.setViewSize(getPreferredSize());
        setViewPosition((int) ((p.x + size.width / 2.0) * ratio - size.width / 2.0),
                (int) ((p.y + size.height / 2.0) * ratio - size.height / 2.0));
        repaint();
    }

    private JViewport viewport() { return (JViewport) getParent(); }

    private void setViewPosition(int x, int y) {
        JViewport vp = viewport();
        vp.setViewPosition(new Point(Math.max(0, Math.min(x, getWidth() - vp.getWidth())),
                Math.max(0, Math.min(y, getHeight() - vp.getHeight()))));
    }

    @Override public Dimension getPreferredSize() {
        return new Dimension((int) Math.ceil(diagram.width * zoom), (int) Math.ceil(diagram.height * zoom));
    }

    private Point diagramPoint(Point p) {
        return new Point((int) Math.floor(p.x / zoom), (int) Math.floor(p.y / zoom));
    }

    private Card cardAt(Point p) {
        for (int i = cards.size() - 1; i >= 0; i--) {
            Card card = cards.get(i);
            if (card.bounds.contains(p)) return card;
        }
        return null;
    }

    private GraphNode containerAt(Point point) {
        Point p = diagramPoint(point);
        Card card = cardAt(p);
        if (card == null) return null;
        GraphNode node = card.nodeAt(p);
        return node.isContainer() ? node : node.parent;
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(PAPER);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (message != null) {
                g.setColor(INK);
                g.drawString(message, PAD, PAD + 12);
                return;
            }
            g.scale(zoom, zoom);
            Rectangle clip = g.getClipBounds();
            g.setColor(new Color(146, 169, 199));
            g.setStroke(new BasicStroke(1.5f));
            for (Card card : cards) {
                if (card.parent == null) continue;
                Card parent = card.parent;
                boolean rightwards = card.bounds.getCenterX() >= parent.bounds.getCenterX();
                int sx = rightwards ? parent.right() : parent.bounds.x;
                int row = card.node.index - parent.firstRow;
                int sy = parent.bounds.y + HEADER + row * ROW + ROW / 2;
                if (row < 0) sy = parent.bounds.y + HEADER;
                if (row >= parent.visibleRows()) sy = parent.bottom() - FOOTER / 2;
                int tx = rightwards ? card.bounds.x : card.right();
                int ty = card.bounds.y + HEADER / 2;
                int bend = (rightwards ? 1 : -1) * Math.max(25, Math.abs(sx - tx) / 2);
                CubicCurve2D edge = new CubicCurve2D.Double(sx, sy, sx + bend, sy, tx - bend, ty, tx, ty);
                if (clip.intersectsLine(sx, sy, tx, ty) || edge.getBounds2D().intersects(clip)) g.draw(edge);
            }
            for (Card card : cards) {
                if (clip.intersects(card.bounds)) drawCard(g, card, clip);
            }
            if (dragged != null) {
                g.setColor(BLUE);
                g.drawLine((int) (pressed.x / zoom), (int) (pressed.y / zoom),
                        (int) (dragPoint.x / zoom), (int) (dragPoint.y / zoom));
            }
        } finally {
            g.dispose();
        }
    }

    private void drawCard(Graphics2D g, Card card, Rectangle clip) {
        int x = card.bounds.x, y = card.bounds.y, width = card.bounds.width;
        GraphNode node = card.node;
        Color accent = node.value.isJsonArray() ? TEAL : BLUE;
        RoundRectangle2D shape = new RoundRectangle2D.Float(x, y, width, card.bounds.height, 12, 12);
        g.setColor(Color.WHITE);
        g.fill(shape);
        g.setColor(node == selected ? SELECTED : new Color(232, 238, 247));
        g.fillRoundRect(x, y, width, HEADER, 12, 12);
        g.fillRect(x, y + HEADER / 2, width, HEADER / 2);
        g.setFont(getFont().deriveFont(Font.BOLD));
        drawText(g, node.label(), x + 12, y + 22, width - 84, INK);
        String type = node.value.isJsonObject() ? "对象 {}" : node.value.isJsonArray() ? "数组 []" : "值";
        drawText(g, type, x + width - 62, y + 22, 55, accent);
        g.setFont(getFont());
        if (!node.isContainer()) drawText(g, node.summary(), x + 12, y + HEADER + 18, width - 24, TEAL);
        int first = Math.max(0, (clip.y - y - HEADER) / ROW);
        int end = Math.min(card.visibleRows(), (clip.y + clip.height - y - HEADER) / ROW + 1);
        int divider = width * 47 / 100;
        for (int i = first; i < end && !card.rows.isEmpty(); i++) {
            GraphNode row = card.rows.get(card.firstRow + i);
            int rowY = y + HEADER + i * ROW;
            if (row == selected || row == target) {
                g.setColor(SELECTED);
                g.fillRect(x + 1, rowY, width - 2, ROW);
            }
            g.setColor(BORDER);
            g.drawLine(x, rowY, x + width, rowY);
            drawText(g, row.label(), x + 12, rowY + 18, divider - 20, INK);
            drawText(g, row.summary(), x + divider + 8, rowY + 18, width - divider - 24, row.isContainer() ? BLUE : TEAL);
            if (row.isContainer()) {
                g.setColor(BLUE);
                g.fillOval(x + width - 4, rowY + ROW / 2 - 3, 6, 6);
            }
        }
        g.setColor(node == target || node == selected ? BLUE : BORDER);
        g.setStroke(new BasicStroke(node == target ? 3 : 1));
        g.draw(shape);
        g.setStroke(new BasicStroke(1));
        g.setColor(BLUE);
        int right = card.right(), bottom = card.bottom();
        g.drawLine(right - 12, bottom - 4, right - 4, bottom - 12);
        g.drawLine(right - 8, bottom - 4, right - 4, bottom - 8);
        if (card.visibleRows() < card.rowCount()) {
            g.setFont(getFont().deriveFont(11f));
            g.drawString("▲", x + 7, bottom - 3);
            g.drawString("▼", x + 29, bottom - 3);
            g.drawString((card.firstRow + 1) + "–" + (card.firstRow + card.visibleRows())
                    + " / " + card.rowCount(), x + 53, bottom - 3);
        }
    }

    private void drawText(Graphics2D g, String text, int x, int baseline, int width, Color color) {
        String s = text.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        FontMetrics metrics = g.getFontMetrics();
        if (metrics.stringWidth(s) > width) {
            int end = 0, used = metrics.stringWidth("…");
            while (end < s.length()) {
                int cp = s.codePointAt(end);
                used += metrics.charWidth(cp);
                if (used > width) break;
                end += Character.charCount(cp);
            }
            s = s.substring(0, end) + "…";
        }
        g.setColor(color);
        g.drawString(s, x, baseline);
    }
}
