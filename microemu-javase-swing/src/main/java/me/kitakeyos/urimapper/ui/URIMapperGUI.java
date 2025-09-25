package me.kitakeyos.urimapper.ui;

import me.kitakeyos.urimapper.ConnectorImpl;
import org.microemu.microedition.ImplFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

public class URIMapperGUI extends JFrame {
    private Map<String, String> uriMap;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField oldUriField;
    private JTextField newUriField;

    // Components for captured URIs
    private DefaultListModel<String> capturedUriListModel;
    private JList<String> capturedUriList;

    // Auto-save flag
    private boolean autoSaveEnabled = true;

    public URIMapperGUI(JCheckBoxMenuItem parent) {
        uriMap = new HashMap<>();
        capturedUriListModel = new DefaultListModel<>();
        ImplFactory.registerGCF(ImplFactory.DEFAULT, new ConnectorImpl());
        initializeGUI();
        setupWindowListener();
        loadAutoSavedData();
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                parent.setSelected(false);
            }
        });
        setLocationRelativeTo(null);
    }

    private void initializeGUI() {
        setTitle("URI Mapper - Manage URI Replacement");
        setSize(1200, 650);
        setLayout(new BorderLayout());

        // Create menu bar
        setJMenuBar(createMenuBar());

        // Create main panel with JSplitPane
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(350);

        // Left panel - Captured URIs
        JPanel leftPanel = createCapturedUriPanel();
        mainSplitPane.setLeftComponent(leftPanel);

        // Right panel - URI Mapping
        JPanel rightPanel = createMappingPanel();
        mainSplitPane.setRightComponent(rightPanel);

        add(mainSplitPane, BorderLayout.CENTER);

        // Status bar
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");

        JMenuItem saveItem = new JMenuItem("Save Configuration");
        saveItem.setAccelerator(KeyStroke.getKeyStroke("ctrl S"));
        saveItem.addActionListener(e -> saveConfiguration());

        JMenuItem loadItem = new JMenuItem("Load Configuration");
        loadItem.setAccelerator(KeyStroke.getKeyStroke("ctrl O"));
        loadItem.addActionListener(e -> loadConfiguration());

        JMenuItem exportItem = new JMenuItem("Export to File...");
        exportItem.setAccelerator(KeyStroke.getKeyStroke("ctrl E"));
        exportItem.addActionListener(e -> exportToFile());

        JMenuItem importItem = new JMenuItem("Import from File...");
        importItem.setAccelerator(KeyStroke.getKeyStroke("ctrl I"));
        importItem.addActionListener(e -> importFromFile());

        fileMenu.add(saveItem);
        fileMenu.add(loadItem);
        fileMenu.addSeparator();
        fileMenu.add(exportItem);
        fileMenu.add(importItem);

        // Settings Menu
        JMenu settingsMenu = new JMenu("Settings");

        JCheckBoxMenuItem autoSaveItem = new JCheckBoxMenuItem("Auto-save on Exit", autoSaveEnabled);
        autoSaveItem.addActionListener(e -> autoSaveEnabled = autoSaveItem.isSelected());

        JMenuItem clearConfigItem = new JMenuItem("Clear Auto-saved Configuration");
        clearConfigItem.addActionListener(e -> clearAutoSavedConfig());

        settingsMenu.add(autoSaveItem);
        settingsMenu.addSeparator();
        settingsMenu.add(clearConfigItem);

        menuBar.add(fileMenu);
        menuBar.add(settingsMenu);

        return menuBar;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());

        JLabel statusLabel = new JLabel("Ready");
        if (URIDataManager.hasAutoSavedConfig()) {
            statusLabel.setText("Auto-saved configuration loaded");
        }

        statusBar.add(statusLabel);
        return statusBar;
    }

    private JPanel createCapturedUriPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Captured URIs from Application"));

        // Create list to display captured URIs
        capturedUriList = new JList<>(capturedUriListModel);
        capturedUriList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        capturedUriList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane capturedScrollPane = new JScrollPane(capturedUriList);
        capturedScrollPane.setPreferredSize(new Dimension(330, 400));

        panel.add(capturedScrollPane, BorderLayout.CENTER);

        // Panel with control buttons
        JPanel capturedButtonPanel = new JPanel(new FlowLayout());

        JButton useUriButton = new JButton("Use This URI");
        useUriButton.addActionListener(e -> {
            String selectedUri = capturedUriList.getSelectedValue();
            if (selectedUri != null) {
                oldUriField.setText(selectedUri);
                newUriField.requestFocus();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Please select a URI from the list!",
                        "Information", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JButton clearCapturedButton = new JButton("Clear List");
        clearCapturedButton.addActionListener(e -> {
            if (!capturedUriListModel.isEmpty()) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to clear all captured URIs?",
                        "Confirm Clear", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    capturedUriListModel.clear();
                }
            }
        });

        capturedButtonPanel.add(useUriButton);
        capturedButtonPanel.add(clearCapturedButton);

        panel.add(capturedButtonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMappingPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Input panel
        JPanel inputPanel = createInputPanel();
        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // Table for displaying data
        JScrollPane tableScrollPane = createTable();
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Add URI Mapping"));
        GridBagConstraints gbc = new GridBagConstraints();

        // Label and field for Old URI
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(new JLabel("Old URI:"), gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        oldUriField = new JTextField(30);
        panel.add(oldUriField, gbc);

        // Label and field for New URI
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("New URI:"), gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        newUriField = new JTextField(30);
        panel.add(newUriField, gbc);

        // Add Button
        gbc.gridx = 2; gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weightx = 0;
        JButton addButton = new JButton("Add");
        addButton.setPreferredSize(new Dimension(80, 50));
        addButton.addActionListener(new AddButtonListener());
        panel.add(addButton, gbc);

        return panel;
    }

    private JScrollPane createTable() {
        // Create model for table
        String[] columnNames = {"Old URI", "New URI"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Disable direct editing
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // Set column width
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("URI Mapping List"));

        return scrollPane;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(new DeleteButtonListener());

        JButton clearAllButton = new JButton("Clear All");
        clearAllButton.addActionListener(new ClearAllButtonListener());

        panel.add(deleteButton);
        panel.add(clearAllButton);

        return panel;
    }

    // Menu action methods
    private void saveConfiguration() {
        if (URIDataManager.saveToConfig(uriMap)) {
            JOptionPane.showMessageDialog(this,
                    "Configuration saved successfully!",
                    "Save Complete", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Failed to save configuration!",
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadConfiguration() {
        Map<String, String> loadedData = URIDataManager.loadFromConfig();
        if (!loadedData.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "This will replace current mappings. Continue?",
                    "Confirm Load", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                uriMap.clear();
                uriMap.putAll(loadedData);
                updateTable();
                JOptionPane.showMessageDialog(this,
                        "Configuration loaded successfully! (" + loadedData.size() + " mappings)",
                        "Load Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "No saved configuration found!",
                    "Load Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void exportToFile() {
        URIDataManager.exportToFile(this, uriMap);
    }

    private void importFromFile() {
        Map<String, String> importedData = URIDataManager.importFromFile(this);
        if (importedData != null && !importedData.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Import " + importedData.size() + " mappings?\nThis will merge with existing data.",
                    "Confirm Import", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                uriMap.putAll(importedData);
                updateTable();
                JOptionPane.showMessageDialog(this,
                        "Successfully imported " + importedData.size() + " mappings!",
                        "Import Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void clearAutoSavedConfig() {
        if (URIDataManager.hasAutoSavedConfig()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete the auto-saved configuration?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                if (URIDataManager.deleteAutoSavedConfig()) {
                    JOptionPane.showMessageDialog(this,
                            "Auto-saved configuration deleted!",
                            "Delete Complete", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to delete auto-saved configuration!",
                            "Delete Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "No auto-saved configuration found!",
                    "Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void setupWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (autoSaveEnabled && !uriMap.isEmpty()) {
                    URIDataManager.saveToConfig(uriMap);
                }
            }
        });
    }

    private void loadAutoSavedData() {
        if (URIDataManager.hasAutoSavedConfig()) {
            Map<String, String> autoSavedData = URIDataManager.loadFromConfig();
            if (!autoSavedData.isEmpty()) {
                uriMap.putAll(autoSavedData);
                updateTable();
            }
        }
    }

    // Listener for Add button
    private class AddButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String oldUri = oldUriField.getText().trim();
            String newUri = newUriField.getText().trim();

            if (oldUri.isEmpty() || newUri.isEmpty()) {
                JOptionPane.showMessageDialog(URIMapperGUI.this,
                        "Please enter both old URI and new URI!",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (uriMap.containsKey(oldUri)) {
                int choice = JOptionPane.showConfirmDialog(URIMapperGUI.this,
                        "Old URI already exists. Do you want to overwrite?",
                        "Confirm", JOptionPane.YES_NO_OPTION);
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // Add to map and table
            uriMap.put(oldUri, newUri);
            updateTable();

            // Clear field contents
            oldUriField.setText("");
            newUriField.setText("");
            oldUriField.requestFocus();
        }
    }

    // Listener for Delete button
    private class DeleteButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(URIMapperGUI.this,
                        "Please select a row to delete!",
                        "Information", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String oldUri = (String) tableModel.getValueAt(selectedRow, 0);

            int choice = JOptionPane.showConfirmDialog(URIMapperGUI.this,
                    "Are you sure you want to delete this mapping?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                uriMap.remove(oldUri);
                updateTable();
            }
        }
    }

    // Listener for Clear All button
    private class ClearAllButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (uriMap.isEmpty()) {
                JOptionPane.showMessageDialog(URIMapperGUI.this,
                        "List is already empty!",
                        "Information", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int choice = JOptionPane.showConfirmDialog(URIMapperGUI.this,
                    "Are you sure you want to delete all mappings?",
                    "Confirm Clear All", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                uriMap.clear();
                updateTable();
            }
        }
    }

    private void updateTable() {
        // Clear all current rows
        tableModel.setRowCount(0);

        // Add back from Map
        for (Map.Entry<String, String> entry : uriMap.entrySet()) {
            Object[] row = {entry.getKey(), entry.getValue()};
            tableModel.addRow(row);
        }
    }

    /**
     * Get mapped URI for the given old URI with null safety
     * @param oldUri the original URI to look up
     * @return the new URI if mapping exists, otherwise returns the original URI
     */
    public String getMappedURI(String oldUri) {
        if (oldUri == null || oldUri.trim().isEmpty()) {
            return oldUri;
        }
        String trimmedUri = oldUri.trim();
        return uriMap.getOrDefault(trimmedUri, trimmedUri);
    }

    // Public method to add captured URI from application
    public void addCapturedURI(String uri) {
        if (uri != null && !uri.trim().isEmpty()) {
            String trimmedUri = uri.trim();
            // Check if URI already exists to avoid duplicates
            if (!capturedUriListModel.contains(trimmedUri)) {
                capturedUriListModel.addElement(trimmedUri);
                // Auto scroll to bottom
                SwingUtilities.invokeLater(() -> {
                    int index = capturedUriListModel.getSize() - 1;
                    if (index >= 0) {
                        capturedUriList.ensureIndexIsVisible(index);
                    }
                });
            }
        }
    }
}