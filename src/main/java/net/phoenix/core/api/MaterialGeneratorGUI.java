package net.phoenix.core.api;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.swing.*;

public class MaterialGeneratorGUI extends JFrame {

    private static final Random RANDOM = new Random();
    private final JLabel nameLabel;
    private final JPanel primaryPreview;
    private final JPanel secondaryPreview;
    private final JLabel primaryText;
    private final JLabel secondaryText;

    private static final List<String> MODIFIERS = List.of(
            "Soggy", "Judgmental", "Caffeinated", "Middle-Aged", "Tax-Exempt", "Confused",
            "Greasy", "Passive-Aggressive", "Gluten-Free", "Low-Resolution", "Slappy",
            "Tactical", "Deep-Fried", "Organic", "Disappointed", "Non-Euclidean",
            "Sentient", "Anxious", "Lactose-Intolerant", "Over-Engineered", "Under-Cooked",
            "Aggressively-Average", "Highly-Suspect", "Moist", "Bureaucratic",
            "Existential", "Ironic", "Spicy", "Lukewarm", "Slightly-Sticky", "Omega",
            "Giga", "Ultra-Violent", "Cataclysmic", "World-Ending", "Hyper-Dense",
            "Infinite", "Nuclear", "God-Slaying", "Thundering", "Apex", "Super-Critical",
            "Absolute", "Desolation", "Titanic", "Eraser", "Primordial", "Quantum",
            "Singularity", "Apocalyptic", "Omnipotent", "Relentless", "Unstoppable",
            "Shattering", "Dominion", "Overlord", "Extinction-Level", "Raging",
            "Invincible", "Universal", "Ethereal", "Void-Touched", "Runic", "Astral",
            "Enchanted", "Arcane", "Mana-Soaked", "Chronos", "Alchemical", "Ley-Line",
            "Celestial", "Necrotic", "Soul-Bound", "Warped", "Mythic", "Forbidden",
            "Hallowed", "Infernal", "Planar", "Spectral", "Mystic", "Occult",
            "Otherworldly", "Shadow-Woven", "Dream-Stealing", "Prophetic", "Runebound",
            "Cosmic", "Sigil-Etched", "Blessed", "Bio-Hazardous", "Radioactive",
            "Fragile", "Refined", "Corrupted", "Unstable", "Polished", "Translucent",
            "Anodized", "Galvanized", "Crystalline", "Vibrating", "Screaming");

    private static final List<String> BASES = List.of(
            "Cardboard", "Fiberglass", "Oatmeal", "Sludge", "Tofu", "Concrete", "Dust",
            "Plywood", "Laminate", "Gelatin", "Goop", "Gravel", "Asbestos", "Lint",
            "Mayonnaise", "Styrofoam", "Plastic-Wrap", "Duct-Tape", "Sponge", "Breadcrumb",
            "Sawdust", "Plaster", "Yarn", "Rubber", "Mulch", "Hummus", "Clay", "Tar",
            "Velvet", "Granite", "Pudding", "Wet-Napkin", "Zucchini", "Particle-Board",
            "Old-Cheese", "Dish-Water", "Pet-Hair", "Loose-Change", "Packing-Peanut");

    public MaterialGeneratorGUI() {
        setTitle("Phoenix Core: Random Material Forge");
        setSize(500, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(45, 45, 45));

        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.Y_AXIS));
        displayPanel.setOpaque(false);
        displayPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        nameLabel = new JLabel("Click Forge to Begin", SwingConstants.CENTER);
        nameLabel.setFont(new Font("Serif", Font.BOLD, 22));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        primaryPreview = createColorCircle();
        secondaryPreview = createColorCircle();
        primaryText = createColorLabel();
        secondaryText = createColorLabel();

        JPanel colorContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 20));
        colorContainer.setOpaque(false);
        colorContainer.add(createLabeledPreview("Primary", primaryPreview, primaryText));
        colorContainer.add(createLabeledPreview("Secondary", secondaryPreview, secondaryText));

        displayPanel.add(nameLabel);
        displayPanel.add(Box.createVerticalStrut(20));
        displayPanel.add(colorContainer);

        JButton forgeButton = new JButton("FORGE MATERIAL");
        forgeButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        forgeButton.setBackground(new Color(100, 60, 150));
        forgeButton.setForeground(Color.WHITE);
        forgeButton.setFocusPainted(false);
        forgeButton.addActionListener(e -> generate());

        add(displayPanel, BorderLayout.CENTER);
        add(forgeButton, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private void generate() {
        List<String> shuffled = new ArrayList<>(MODIFIERS);
        Collections.shuffle(shuffled);

        String result = String.format("%s %s %s %s",
                shuffled.get(0), shuffled.get(1), shuffled.get(2),
                BASES.get(RANDOM.nextInt(BASES.size())));

        int pColor = RANDOM.nextInt(0xFFFFFF + 1);
        int sColor = RANDOM.nextInt(0xFFFFFF + 1);

        nameLabel.setText("<html><center>" + result + "</center></html>");
        primaryPreview.setBackground(new Color(pColor));
        secondaryPreview.setBackground(new Color(sColor));
        primaryText.setText(String.format("#%06X", pColor));
        secondaryText.setText(String.format("#%06X", sColor));
    }

    private JPanel createColorCircle() {
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(60, 60));
        p.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        p.setBackground(new Color(60, 60, 60));
        return p;
    }

    private JLabel createColorLabel() {
        JLabel l = new JLabel("#------");
        l.setForeground(Color.LIGHT_GRAY);
        l.setFont(new Font("Monospaced", Font.PLAIN, 14));
        return l;
    }

    private JPanel createLabeledPreview(String title, JPanel preview, JLabel text) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setOpaque(false);
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setForeground(Color.GRAY);
        p.add(t, BorderLayout.NORTH);
        p.add(preview, BorderLayout.CENTER);
        p.add(text, BorderLayout.SOUTH);
        return p;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MaterialGeneratorGUI().setVisible(true));
    }
}
