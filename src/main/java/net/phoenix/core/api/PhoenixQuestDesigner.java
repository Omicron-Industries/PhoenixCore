package net.phoenix.core.api;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

public class PhoenixQuestDesigner extends JFrame {

    private final List<QuestNode> quests = new ArrayList<>();
    private QuestNode selectedNode = null;
    private Point dragStart = null;
    private final QuestCanvas canvas;

    public PhoenixQuestDesigner() {
        setTitle("Phoenix Core API - Quest Designer");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        canvas = new QuestCanvas();
        canvas.setBackground(new Color(30, 30, 30));

        JPanel toolbar = new JPanel();
        JButton btnExport = new JButton("Export SNBT to Console");
        btnExport.addActionListener(e -> System.out.println(exportToSNBT("new_chapter", quests)));

        JButton btnClear = new JButton("Clear All");
        btnClear.addActionListener(e -> {
            quests.clear();
            canvas.repaint();
        });

        toolbar.add(new JLabel("Left-Click: Create/Select | Drag: Move | Right-Click: Templates"));
        toolbar.add(btnExport);
        toolbar.add(btnClear);

        add(toolbar, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
    }

    public static class QuestNode {

        String id, title, icon, shape = "rsquare";
        double x, y;
        List<String> dependencies = new ArrayList<>();

        public QuestNode(String title, double x, double y) {
            this.id = generateHexID();
            this.title = title;
            this.x = x;
            this.y = y;
            this.icon = "minecraft:paper";
        }

        public Rectangle getBounds() {
            return new Rectangle((int) (x * 50) + 450, (int) (y * 50) + 300, 60, 60);
        }
    }

    private class QuestCanvas extends JPanel {

        public QuestCanvas() {
            addMouseListener(new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    selectedNode = null;
                    for (QuestNode q : quests) {
                        if (q.getBounds().contains(e.getPoint())) {
                            selectedNode = q;
                            break;
                        }
                    }

                    if (SwingUtilities.isRightMouseButton(e) && selectedNode != null) {
                        showTemplateMenu(e.getPoint());
                    } else if (selectedNode == null && SwingUtilities.isLeftMouseButton(e)) {
                        double qX = (e.getX() - 450) / 50.0;
                        double qY = (e.getY() - 300) / 50.0;
                        quests.add(new QuestNode("New Quest", qX, qY));
                    }
                    dragStart = e.getPoint();
                    repaint();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (selectedNode != null && dragStart != null) {
                        double deltaX = (e.getX() - dragStart.x) / 50.0;
                        double deltaY = (e.getY() - dragStart.y) / 50.0;
                        selectedNode.x += deltaX;
                        selectedNode.y += deltaY;
                        dragStart = e.getPoint();
                        repaint();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(50, 50, 50));
            for (int i = 0; i < getWidth(); i += 50) g2.drawLine(i, 0, i, getHeight());
            for (int i = 0; i < getHeight(); i += 50) g2.drawLine(0, i, getWidth(), i);

            for (QuestNode q : quests) {
                Rectangle r = q.getBounds();
                g2.setColor(q == selectedNode ? Color.CYAN : Color.LIGHT_GRAY);
                g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);

                g2.setColor(Color.BLACK);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2.drawString(q.title, r.x + 5, r.y + 35);
            }
        }
    }

    private void showTemplateMenu(Point p) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem gt = new JMenuItem("Apply GregTech Template");
        gt.addActionListener(e -> {
            selectedNode.title = "GT Machine";
            selectedNode.icon = "gtceu:basic_machine";
            selectedNode.shape = "gear";
            canvas.repaint();
        });

        JMenuItem bee = new JMenuItem("Apply Bee Template");
        bee.addActionListener(e -> {
            selectedNode.title = "New Bee Mutation";
            selectedNode.icon = "productivebees:spawn_egg";
            selectedNode.shape = "circle";
            canvas.repaint();
        });

        menu.add(gt);
        menu.add(bee);
        menu.show(canvas, p.x, p.y);
    }

    public String exportToSNBT(String filename, List<QuestNode> nodes) {
        StringBuilder sb = new StringBuilder("{\n\tfilename: \"" + filename + "\"\n\tquests: [\n");
        for (QuestNode q : nodes) {
            sb.append("\t\t{\n");
            sb.append("\t\t\tid: \"").append(q.id).append("\"\n");
            sb.append("\t\t\ttitle: \"").append(q.title).append("\"\n");
            sb.append("\t\t\tx: ").append(String.format("%.2fd", q.x)).append("\n");
            sb.append("\t\t\ty: ").append(String.format("%.2fd", q.y)).append("\n");
            sb.append("\t\t\tshape: \"").append(q.shape).append("\"\n");
            sb.append("\t\t}\n");
        }
        sb.append("\t]\n}");
        return sb.toString();
    }

    private static String generateHexID() {
        return Long.toHexString(Double.doubleToLongBits(Math.random())).substring(0, 16).toUpperCase();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new PhoenixQuestDesigner().setVisible(true);
        });
    }
}
