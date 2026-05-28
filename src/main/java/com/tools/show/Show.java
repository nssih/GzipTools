package com.tools.show;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.tools.constant.Constants;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

public class Show {

    public static void showResult(String text, long mtime) {

        JDialog dialog = new JDialog();
        dialog.setTitle(Constants.CONTENT);
        dialog.setModal(true);

        final String originalText = text;
        final boolean[] formatted = {false};

        JTextArea textArea = new JTextArea(text);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JTextArea lineNumbers = new JTextArea();
        lineNumbers.setEditable(false);
        lineNumbers.setFocusable(false);
        lineNumbers.setBackground(new Color(60, 63, 65));
        lineNumbers.setForeground(new Color(128, 128, 128));
        lineNumbers.setFont(textArea.getFont());
        lineNumbers.setMargin(new Insets(0, 4, 0, 4));

        Runnable updateLineNumbers = () -> {
            String t = textArea.getText();
            int lines = textArea.getLineCount();
            if (lines > 1 && t.endsWith("\n")) lines--;
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= lines; i++) {
                if (i > 1) sb.append("\n");
                sb.append(i);
            }
            lineNumbers.setText(sb.toString());
        };
        updateLineNumbers.run();

        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateLineNumbers.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateLineNumbers.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateLineNumbers.run(); }
        });

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setRowHeaderView(lineNumbers);
        scrollPane.setPreferredSize(new Dimension(700, 500));

        // size标签（单独一行）
        JLabel sizeLabel = new JLabel();
        sizeLabel.setForeground(new Color(140, 140, 140)); // 视觉更柔和
        updateSizeLabel(sizeLabel, text);

        JButton closeBtn = new JButton(Constants.CLOSE);
        JButton formatBtn = new JButton(Constants.FORMAT);
        JButton copyBtn = new JButton(Constants.COPY);

        styleButton(closeBtn);
        styleButton(formatBtn);
        styleButton(copyBtn);

        Color green = new Color(76, 160, 90);
        Color blue = new Color(66, 133, 244);

        Runnable resetCopy = () -> resetButton(copyBtn);

        // Format逻辑
        formatBtn.addActionListener(e -> {

            if (!formatted[0]) {

                String result = smartProcess(textArea.getText());
                if (result.equals(textArea.getText())) return;

                textArea.setText(result);

                // 更新size
                updateSizeLabel(sizeLabel, result);

                activateButton(formatBtn, green);
                formatBtn.setText(Constants.RESTORE);

                formatted[0] = true;
                resetCopy.run();

            } else {

                textArea.setText(originalText);
                updateSizeLabel(sizeLabel, originalText);

                resetButton(formatBtn);
                formatBtn.setText(Constants.FORMAT);

                formatted[0] = false;
                resetCopy.run();
            }
        });

        // Copy
        copyBtn.addActionListener(e -> {
            StringSelection sel = new StringSelection(textArea.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);

            activateButton(copyBtn, blue);
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        // 第一层：按钮行
        JPanel buttonPanel = new JPanel(new BorderLayout());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.setOpaque(false);
        left.add(closeBtn);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER));
        center.setOpaque(false);
        center.add(formatBtn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setOpaque(false);
        right.add(copyBtn);

        buttonPanel.add(left, BorderLayout.WEST);
        buttonPanel.add(center, BorderLayout.CENTER);
        buttonPanel.add(right, BorderLayout.EAST);

        // 第二层：size行
        JPanel sizePanel = new JPanel(new BorderLayout());
        sizePanel.setOpaque(false);
        sizePanel.add(sizeLabel, BorderLayout.WEST);

        if (mtime > 0) {
            String timeStr = DateTimeFormatter.ofPattern(Constants.TIME_FORMAT)
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochSecond(mtime));
            JLabel timeLabel = new JLabel("Created: " + timeStr);
            timeLabel.setForeground(new Color(140, 140, 140));
            sizePanel.add(timeLabel, BorderLayout.EAST);
        }

        // 底部容器（size在上，按钮在下）
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(sizePanel, BorderLayout.NORTH);   // size
        bottom.add(buttonPanel, BorderLayout.SOUTH); // 按钮

        JPanel main = new JPanel(new BorderLayout());
        main.add(scrollPane, BorderLayout.CENTER);
        main.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(main);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private static void updateSizeLabel(JLabel label, String text) {

        if (text == null) {
            label.setText("Size: 0 B");
            return;
        }

        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        int size = bytes.length;

        label.setText("Size: " + formatSize(size) + " (" + size + " bytes)");
    }

    private static String formatSize(int size) {

        if (size < 1024) return size + " B";

        double kb = size / 1024.0;
        if (kb < 1024) return String.format("%.2f KB", kb);

        double mb = kb / 1024.0;
        return String.format("%.2f MB", mb);
    }

    /* ================= UI ================= */

    private static void styleButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(120, 120, 120), 1));
        btn.setForeground(new Color(200, 200, 200));
    }

    private static void activateButton(JButton btn, Color color) {
        btn.setBorder(BorderFactory.createLineBorder(color, 1));
        btn.setForeground(color);
    }

    private static void resetButton(JButton btn) {
        btn.setBorder(BorderFactory.createLineBorder(new Color(120, 120, 120), 1));
        btn.setForeground(new Color(200, 200, 200));
    }

    /* ================= SMART CORE ================= */

    private static String smartProcess(String input) {

        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        String text = input.trim();

        // 尝试 Base64
        try {
            byte[] decoded = Base64.getDecoder().decode(text);

            // 判断 GZIP
            if (decoded.length >= 2 &&
                    decoded[0] == (byte) 0x1f &&
                    decoded[1] == (byte) 0x8b) {

                GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(decoded));

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(gis, StandardCharsets.UTF_8)
                );

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                return smartFormat(sb.toString());
            }

            // 普通 Base64
            String decodedText = new String(decoded, StandardCharsets.UTF_8);
            return smartFormat(decodedText);

        } catch (Exception ignored) {
        }

        return smartFormat(text);
    }

    private static String smartFormat(String input) {

        String trimmed = input.trim();

        // JSON
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return formatJson(input).stripTrailing();
        }

        // XML（注意这里必须是 < 不是 &lt;）
        if (trimmed.startsWith("<")) {
            return formatXml(input).stripTrailing();
        }

        return input;
    }

    /* ================= FORMAT ================= */

    private static String formatJson(String input) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();

            Object obj = mapper.readValue(input, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);

        } catch (Exception e) {
            return input;
        }
    }

    private static String formatXml(String input) {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);

            Document doc = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(input)));

            Transformer transformer = TransformerFactory.newInstance().newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            // 关键：去掉 XML 头
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            // 缩进4（可选）
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            return writer.toString();

        } catch (Exception e) {
            return input;
        }
    }

    /* ================= Notification ================= */

    public static void notifyError(String msg) {
        Notification notification = new Notification(
                Constants.GROUP_ID,
                Constants.TITLE,
                msg,
                NotificationType.ERROR
        );

        Notifications.Bus.notify(notification);
    }
}
