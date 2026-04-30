package net.phoenix.core.integration.ponder.gui;

import net.phoenix.core.integration.ponder.PonderSceneGenerator;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class PonderGuiMain {

    private JFrame frame;
    private JTextField modIdField, sceneIdField, titleField, structureIdField;
    private JTextField blockX, blockY, blockZ, blockId;
    private JTextArea scriptDisplay;
    private PonderSceneGenerator generator;

    public PonderGuiMain() {
        // Initialize Generator
        generator = new PonderSceneGenerator("phoenix", "example_scene", "Title", "phoenix:example");

        // Setup Window
        frame = new JFrame("Phoenix Ponder GUI (Swing Edition)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 700);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Metadata Section ---
        JPanel metaPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        modIdField = addField(metaPanel, "Mod ID:", "phoenix");
        sceneIdField = addField(metaPanel, "Scene ID:", "example_scene");
        titleField = addField(metaPanel, "Title:", "My Example Scene");
        structureIdField = addField(metaPanel, "Structure ID:", "phoenix:example");
        mainPanel.add(metaPanel);

        // --- Action Section ---
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        blockX = new JTextField("3", 3);
        blockY = new JTextField("1", 3);
        blockZ = new JTextField("3", 3);
        blockId = new JTextField("minecraft:stone", 15);
        JButton addBtn = new JButton("Add Block");

        addBtn.addActionListener(e -> {
            generator.addBlock(
                    Integer.parseInt(blockX.getText()),
                    Integer.parseInt(blockY.getText()),
                    Integer.parseInt(blockZ.getText()),
                    blockId.getText());
            updatePreview();
        });

        actionPanel.add(new JLabel("X/Y/Z:"));
        actionPanel.add(blockX);
        actionPanel.add(blockY);
        actionPanel.add(blockZ);
        actionPanel.add(new JLabel("ID:"));
        actionPanel.add(blockId);
        actionPanel.add(addBtn);
        mainPanel.add(actionPanel);

        // --- Preview Section ---
        scriptDisplay = new JTextArea(15, 50);
        scriptDisplay.setEditable(false);
        mainPanel.add(new JScrollPane(scriptDisplay));

        JButton saveBtn = new JButton("Save Script");
        saveBtn.addActionListener(e -> saveScript());
        mainPanel.add(saveBtn);

        frame.add(mainPanel);
        updatePreview();
    }

    private JTextField addField(JPanel panel, String label, String def) {
        panel.add(new JLabel(label));
        JTextField tf = new JTextField(def);
        panel.add(tf);
        return tf;
    }

    private void updatePreview() {
        // Sync metadata from fields to generator before generating
        generator.setMetadata(modIdField.getText(), sceneIdField.getText(), titleField.getText(),
                structureIdField.getText());
        scriptDisplay.setText(generator.generateScript());
    }

    private void saveScript() {
        try {
            Path path = Paths.get("src/main/resources/kubejs_export/client_scripts/" + sceneIdField.getText() + ".js");
            Files.createDirectories(path.getParent());
            Files.writeString(path, scriptDisplay.getText());
            JOptionPane.showMessageDialog(frame, "Saved to: " + path.toAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage(), "Save Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void show() {
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        // Swing likes to run on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new PonderGuiMain().show());
    }
}
