package me.kitakeyos.urimapper.ui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.util.*;

/**
 * Manager class for handling URI mapping data persistence
 * Supports JSON and Properties file formats
 */
public class URIDataManager {
    private static final String DEFAULT_CONFIG_FILE = "uri_mappings.properties";
    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".micoemu";

    /**
     * Save URI mappings to default configuration file
     * @param uriMap the URI mapping data to save
     * @return true if saved successfully, false otherwise
     */
    public static boolean saveToConfig(Map<String, String> uriMap) {
        try {
            File configDir = new File(CONFIG_DIR);
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            File configFile = new File(configDir, DEFAULT_CONFIG_FILE);
            return saveToPropertiesFile(uriMap, configFile);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load URI mappings from default configuration file
     * @return loaded URI mapping data, empty map if file doesn't exist or error occurs
     */
    public static Map<String, String> loadFromConfig() {
        try {
            File configFile = new File(CONFIG_DIR, DEFAULT_CONFIG_FILE);
            if (configFile.exists()) {
                return loadFromPropertiesFile(configFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    /**
     * Export URI mappings to a user-selected file
     * @param parent parent component for dialogs
     * @param uriMap the URI mapping data to export
     * @return true if exported successfully, false otherwise
     */
    public static boolean exportToFile(JFrame parent, Map<String, String> uriMap) {
        if (uriMap.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "No data to export!",
                    "Export Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export URI Mappings");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Properties Files (*.properties)", "properties"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files (*.json)", "json"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));

        // Set default filename
        fileChooser.setSelectedFile(new File("uri_mappings_" +
                new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".properties"));

        int result = fileChooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getName().toLowerCase();

            try {
                if (fileName.endsWith(".json")) {
                    return saveToJsonFile(uriMap, selectedFile);
                } else if (fileName.endsWith(".txt")) {
                    return saveToTextFile(uriMap, selectedFile);
                } else {
                    // Default to properties format
                    return saveToPropertiesFile(uriMap, selectedFile);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(parent,
                        "Failed to export data: " + e.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return false;
    }

    /**
     * Import URI mappings from a user-selected file
     * @param parent parent component for dialogs
     * @return loaded URI mapping data, null if cancelled or error
     */
    public static Map<String, String> importFromFile(JFrame parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import URI Mappings");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Properties Files (*.properties)", "properties"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files (*.json)", "json"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));

        int result = fileChooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getName().toLowerCase();

            try {
                if (fileName.endsWith(".json")) {
                    return loadFromJsonFile(selectedFile);
                } else if (fileName.endsWith(".txt")) {
                    return loadFromTextFile(selectedFile);
                } else {
                    // Default to properties format
                    return loadFromPropertiesFile(selectedFile);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(parent,
                        "Failed to import data: " + e.getMessage(),
                        "Import Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        return null;
    }

    /**
     * Save to Properties file format
     */
    private static boolean saveToPropertiesFile(Map<String, String> uriMap, File file) throws IOException {
        Properties props = new Properties();
        for (Map.Entry<String, String> entry : uriMap.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "URI Mappings - Generated on " + new Date());
            return true;
        }
    }

    /**
     * Load from Properties file format
     */
    private static Map<String, String> loadFromPropertiesFile(File file) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        }

        Map<String, String> uriMap = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            uriMap.put(key, props.getProperty(key));
        }
        return uriMap;
    }

    /**
     * Save to JSON file format (simple implementation without external library)
     */
    private static boolean saveToJsonFile(Map<String, String> uriMap, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("{\n");
            writer.write("  \"uri_mappings\": {\n");

            String[] keys = uriMap.keySet().toArray(new String[0]);
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                String value = uriMap.get(key);
                writer.write("    \"" + escapeJson(key) + "\": \"" + escapeJson(value) + "\"");
                if (i < keys.length - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }

            writer.write("  },\n");
            writer.write("  \"exported_at\": \"" + new Date().toString() + "\"\n");
            writer.write("}\n");
            return true;
        }
    }

    /**
     * Load from JSON file format (simple implementation)
     */
    private static Map<String, String> loadFromJsonFile(File file) throws IOException {
        Map<String, String> uriMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean inMappings = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.contains("\"uri_mappings\"")) {
                    inMappings = true;
                    continue;
                }

                if (inMappings && line.startsWith("\"") && line.contains("\":")) {
                    // Simple JSON parsing for key-value pairs
                    int colonIndex = line.indexOf("\":");
                    if (colonIndex > 0) {
                        String key = line.substring(1, colonIndex);
                        String remaining = line.substring(colonIndex + 2).trim();

                        if (remaining.startsWith("\"")) {
                            int endQuote = remaining.lastIndexOf("\"");
                            if (endQuote > 0) {
                                String value = remaining.substring(1, endQuote);
                                uriMap.put(unescapeJson(key), unescapeJson(value));
                            }
                        }
                    }
                }

                if (inMappings && line.equals("},")) {
                    break;
                }
            }
        }

        return uriMap;
    }

    /**
     * Save to plain text file format
     */
    private static boolean saveToTextFile(Map<String, String> uriMap, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("URI Mapping Export\n");
            writer.write("Generated: " + new Date() + "\n");
            writer.write("Format: OLD_URI -> NEW_URI\n");

            // Java 8 compatible way to repeat characters
            StringBuilder separator = new StringBuilder("=");
            for (int i = 0; i < 50; i++) {
                separator.append("=");
            }
            writer.write(separator.toString() + "\n\n");

            for (Map.Entry<String, String> entry : uriMap.entrySet()) {
                writer.write(entry.getKey() + " -> " + entry.getValue() + "\n");
            }
            return true;
        }
    }

    /**
     * Load from plain text file format
     */
    private static Map<String, String> loadFromTextFile(File file) throws IOException {
        Map<String, String> uriMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("URI Mapping") ||
                        line.startsWith("Generated:") || line.startsWith("Format:") || line.startsWith("=")) {
                    continue;
                }

                // Look for pattern: "oldUri -> newUri"
                int arrowIndex = line.indexOf(" -> ");
                if (arrowIndex > 0) {
                    String oldUri = line.substring(0, arrowIndex).trim();
                    String newUri = line.substring(arrowIndex + 4).trim();
                    if (!oldUri.isEmpty() && !newUri.isEmpty()) {
                        uriMap.put(oldUri, newUri);
                    }
                }
            }
        }

        return uriMap;
    }

    /**
     * Escape special characters for JSON
     */
    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Unescape JSON characters
     */
    private static String unescapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    /**
     * Check if auto-save configuration exists
     */
    public static boolean hasAutoSavedConfig() {
        File configFile = new File(CONFIG_DIR, DEFAULT_CONFIG_FILE);
        return configFile.exists();
    }

    /**
     * Delete auto-saved configuration
     */
    public static boolean deleteAutoSavedConfig() {
        File configFile = new File(CONFIG_DIR, DEFAULT_CONFIG_FILE);
        return configFile.delete();
    }
}