/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author iNVANSION
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class KalkulatorUAS extends javax.swing.JFrame {

    private String currentInput = "";
    private String expression = "";
    private boolean resetOnNextInput = false;
    private String lastOperator = "";
    private String lastNumber = "";
    private String committedHistory = "";
    private static final String HISTORY_FILE = "history_db.txt";

    public KalkulatorUAS() {
        initComponents();

        history.setEditable(false);
        history.setFocusable(false); // cursor tidak bisa masuk
        layar.requestFocusInWindow(); // cursor langsung ke layar
        
        loadHistoryFromFile();
    }

    // Parsing input Indonesia → Double
    private double parseInput(String input) {
        String clean = input.replace(".", "").replace(",", ".");
        return Double.parseDouble(clean);
    }

    // Format hasil ke Indonesia
    private String formatResult(double value) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols();
        sym.setGroupingSeparator('.');
        sym.setDecimalSeparator(',');

        DecimalFormat df = new DecimalFormat("#,##0.########", sym);
        return df.format(value);
    }

    private String formatLiveInput(String input) {
        if (input.isEmpty()) {
            return "";
        }

        // Pisahkan desimal (koma)
        String[] parts = input.split(",", 2);
        String integerPart = parts[0].replace(".", "");

        DecimalFormatSymbols sym = new DecimalFormatSymbols();
        sym.setGroupingSeparator('.');

        DecimalFormat df = new DecimalFormat("#,###", sym);
        String formatted = df.format(Long.parseLong(integerPart));

        if (parts.length == 2) {
            return formatted + "," + parts[1];
        }
        return formatted;
    }

    private void loadHistoryFromFile() {
        File file = new File(HISTORY_FILE);
        if (!file.exists()) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            System.out.println("Gagal membaca history");
        }

        committedHistory = sb.toString().trim();
        history.setText(committedHistory);
        history.setCaretPosition(history.getText().length());
    }
    
    private void saveHistoryToFile(String line) {
        try (BufferedWriter bw = new BufferedWriter(
                new FileWriter(HISTORY_FILE, true))) {

            bw.write(line);
            bw.newLine();

        } catch (IOException e) {
            System.out.println("Gagal menyimpan history");
        }
    }

    private void clearHistoryFile() {
        try (BufferedWriter bw = new BufferedWriter(
                new FileWriter(HISTORY_FILE))) {
            bw.write("");
        } catch (IOException e) {
            System.out.println("Gagal menghapus history file");
        }
    }

    
    public void tampilAngka(String angka) {
        if (resetOnNextInput) {
            currentInput = "";
            resetOnNextInput = false;
        }

        // Ambil angka polos (tanpa titik)
        String raw = currentInput.replace(".", "");

        // Cegah nol berlebihan
        if (raw.equals("0")) {
            raw = "";
        }

        raw += angka;

        // Simpan balik ke currentInput
        currentInput = formatLiveInput(raw);

        layar.setText(currentInput);
        updateHistoryLive();
    }

    public void setOperator(String op) {
        if (!currentInput.isEmpty()) {
            expression += currentInput + " " + op + " ";
            currentInput = "";
            layar.setText("0");
            updateHistoryLive();
        }
    }

    public void hitung() {
        if (!expression.isEmpty() && !currentInput.isEmpty()) {
            expression += currentInput;

            try {
                double hasil = evaluateExpression(expression);
                String hasilText = formatResult(hasil);

                String finalLine = expression + " = " + hasilText;
                // simpan ke database (txt)
                saveHistoryToFile(finalLine);
                committedHistory += committedHistory.isEmpty()
                        ? finalLine
                        : "\n" + finalLine;

                history.setText(committedHistory);
                history.setCaretPosition(history.getText().length());

                layar.setText(hasilText);

                expression = "";
                currentInput = hasilText;
                resetOnNextInput = true;

            } catch (Exception e) {
                layar.setText("Error");
                resetKalkulator();
            }
        }
    }

    private void addToHistory(String text) {
        if (history.getText().isEmpty()) {
            history.setText(text);
        } else {
            history.append("\n" + text);
        }

        // auto scroll ke bawah
        history.setCaretPosition(history.getText().length());
    }

    private void updateLastHistory(String newOperator) {
        String currentHistory = history.getText();
        if (currentHistory.isEmpty()) {
            return;
        }

        String[] lines = currentHistory.split("\n");

        String lastLine = lines[lines.length - 1];

        // validasi format: "angka operator"
        if (!lastLine.matches("\\d+(\\.\\d+)?\\s[+\\-x/]")) {
            return;
        }

        String[] parts = lastLine.split(" ");
        parts[1] = newOperator;

        lines[lines.length - 1] = parts[0] + " " + parts[1];

        history.setText(String.join("\n", lines));

        // auto scroll
        history.setCaretPosition(history.getText().length());
    }

    private double evaluateExpression(String expr) {
        String[] tokens = expr.replace(" ", "")
                .split("(?<=[+\\-x/])|(?=[+\\-x/])");

        java.util.List<Double> numbers = new java.util.ArrayList<>();
        java.util.List<Character> operators = new java.util.ArrayList<>();

        for (int i = 0; i < tokens.length; i++) {
            if (i % 2 == 0) {
                numbers.add(parseInput(tokens[i]));
            } else {
                operators.add(tokens[i].charAt(0));
            }
        }

        // x dan /
        for (int i = 0; i < operators.size(); i++) {
            char op = operators.get(i);
            if (op == 'x' || op == '/') {
                double a = numbers.get(i);
                double b = numbers.get(i + 1);

                double r = (op == 'x') ? a * b : a / b;

                numbers.set(i, r);
                numbers.remove(i + 1);
                operators.remove(i);
                i--;
            }
        }

        double result = numbers.get(0);
        for (int i = 0; i < operators.size(); i++) {
            char op = operators.get(i);
            double b = numbers.get(i + 1);

            if (op == '+') {
                result += b;
            } else {
                result -= b;
            }
        }

        return result;
    }

    public void resetKalkulator() {
        currentInput = "";
        expression = "";
        committedHistory = "";
        resetOnNextInput = false;

        layar.setText("0");
        history.setText("");
        clearHistoryFile(); // 🔥 RESET DATABASE
        layar.requestFocusInWindow();
    }


    public void hapusSatuAngka() {
        if (currentInput.isEmpty()) {
            return;
        }

        String raw = currentInput.replace(".", "");

        if (raw.length() <= 1) {
            currentInput = "";
            layar.setText("0");
        } else {
            raw = raw.substring(0, raw.length() - 1);
            currentInput = formatLiveInput(raw);
            layar.setText(currentInput);
        }

        updateHistoryLive();
    }

    private void updateHistoryLive() {
        String liveLine = expression + currentInput;

        if (committedHistory.isEmpty()) {
            history.setText(liveLine);
        } else {
            history.setText(committedHistory + "\n" + liveLine);
        }

        history.setCaretPosition(history.getText().length());
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        layar = new javax.swing.JTextField();
        button_7 = new javax.swing.JButton();
        button_9 = new javax.swing.JButton();
        button_8 = new javax.swing.JButton();
        button_4 = new javax.swing.JButton();
        button_5 = new javax.swing.JButton();
        button_6 = new javax.swing.JButton();
        button_1 = new javax.swing.JButton();
        button_3 = new javax.swing.JButton();
        button_2 = new javax.swing.JButton();
        button_0 = new javax.swing.JButton();
        button_plus = new javax.swing.JButton();
        button_clear = new javax.swing.JButton();
        button_delete = new javax.swing.JButton();
        button_divide1 = new javax.swing.JButton();
        button_multiply1 = new javax.swing.JButton();
        button_subtract1 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        history = new javax.swing.JTextArea();
        text1 = new javax.swing.JLabel();
        button_equals1 = new javax.swing.JButton();
        button_coma = new javax.swing.JButton();
        text2 = new javax.swing.JLabel();
        text3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(255, 204, 204));

        layar.setBackground(new java.awt.Color(255, 204, 204));
        layar.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        layar.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        layar.setText("0");
        layar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                layarActionPerformed(evt);
            }
        });

        button_7.setBackground(new java.awt.Color(255, 153, 153));
        button_7.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_7.setText("7");
        button_7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_7ActionPerformed(evt);
            }
        });

        button_9.setBackground(new java.awt.Color(255, 153, 153));
        button_9.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_9.setText("9");
        button_9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_9ActionPerformed(evt);
            }
        });

        button_8.setBackground(new java.awt.Color(255, 153, 153));
        button_8.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_8.setText("8");
        button_8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_8ActionPerformed(evt);
            }
        });

        button_4.setBackground(new java.awt.Color(255, 153, 153));
        button_4.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_4.setText("4");
        button_4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_4ActionPerformed(evt);
            }
        });

        button_5.setBackground(new java.awt.Color(255, 153, 153));
        button_5.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_5.setText("5");
        button_5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_5ActionPerformed(evt);
            }
        });

        button_6.setBackground(new java.awt.Color(255, 153, 153));
        button_6.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_6.setText("6");
        button_6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_6ActionPerformed(evt);
            }
        });

        button_1.setBackground(new java.awt.Color(255, 153, 153));
        button_1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_1.setText("1");
        button_1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_1ActionPerformed(evt);
            }
        });

        button_3.setBackground(new java.awt.Color(255, 153, 153));
        button_3.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_3.setText("3");
        button_3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_3ActionPerformed(evt);
            }
        });

        button_2.setBackground(new java.awt.Color(255, 153, 153));
        button_2.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_2.setText("2");
        button_2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_2ActionPerformed(evt);
            }
        });

        button_0.setBackground(new java.awt.Color(255, 153, 153));
        button_0.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_0.setText("0");
        button_0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_0ActionPerformed(evt);
            }
        });

        button_plus.setBackground(new java.awt.Color(255, 102, 153));
        button_plus.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_plus.setText("+");
        button_plus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_plusActionPerformed(evt);
            }
        });

        button_clear.setBackground(new java.awt.Color(204, 0, 204));
        button_clear.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_clear.setText("C");
        button_clear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_clearActionPerformed(evt);
            }
        });

        button_delete.setBackground(new java.awt.Color(255, 0, 153));
        button_delete.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_delete.setText("del");
        button_delete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_deleteActionPerformed(evt);
            }
        });

        button_divide1.setBackground(new java.awt.Color(255, 102, 153));
        button_divide1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_divide1.setText("/");
        button_divide1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_divide1ActionPerformed(evt);
            }
        });

        button_multiply1.setBackground(new java.awt.Color(255, 102, 153));
        button_multiply1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_multiply1.setText("x");
        button_multiply1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_multiply1ActionPerformed(evt);
            }
        });

        button_subtract1.setBackground(new java.awt.Color(255, 102, 153));
        button_subtract1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_subtract1.setText("-");
        button_subtract1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_subtract1ActionPerformed(evt);
            }
        });

        history.setColumns(20);
        history.setRows(5);
        jScrollPane1.setViewportView(history);

        text1.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        text1.setText("KALKULATOR SEDERHANA");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 440, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(text1, javax.swing.GroupLayout.PREFERRED_SIZE, 264, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(73, 73, 73))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(text1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        button_equals1.setBackground(new java.awt.Color(255, 102, 153));
        button_equals1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_equals1.setText("=");
        button_equals1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_equals1ActionPerformed(evt);
            }
        });

        button_coma.setBackground(new java.awt.Color(255, 102, 153));
        button_coma.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        button_coma.setText(",");
        button_coma.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_comaActionPerformed(evt);
            }
        });

        text2.setFont(new java.awt.Font("Segoe UI", 2, 18)); // NOI18N
        text2.setText("credit by Maisya Fitrya");

        text3.setFont(new java.awt.Font("Segoe UI", 2, 18)); // NOI18N
        text3.setText("NPM 23183207017");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                            .addComponent(button_plus, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(button_0, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(button_subtract1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addComponent(layar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 442, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                            .addComponent(button_7, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(button_8, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(button_9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(button_1, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(button_2, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12)
                                .addComponent(button_3, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(button_4, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(button_5, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12)
                                .addComponent(button_6, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(text2)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(button_divide1, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE)
                                .addComponent(button_clear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(button_delete, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(button_multiply1, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(button_coma, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addGap(12, 12, 12)
                                    .addComponent(button_equals1, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addComponent(text3)))
                .addContainerGap(29, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(layar, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(button_7, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_9, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_8, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(button_6, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_4, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_5, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(button_1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_3, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_2, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(button_0, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_plus, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_subtract1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(button_divide1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_multiply1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_coma, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(button_equals1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_clear, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(button_delete, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(text2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(text3)
                .addContainerGap(20, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void layarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_layarActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_layarActionPerformed

    private void button_7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_7ActionPerformed
        tampilAngka("7");
    }//GEN-LAST:event_button_7ActionPerformed

    private void button_9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_9ActionPerformed
        tampilAngka("9");
    }//GEN-LAST:event_button_9ActionPerformed

    private void button_8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_8ActionPerformed
        tampilAngka("8");
    }//GEN-LAST:event_button_8ActionPerformed

    private void button_4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_4ActionPerformed
        tampilAngka("4");
    }//GEN-LAST:event_button_4ActionPerformed

    private void button_5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_5ActionPerformed
        tampilAngka("5");
    }//GEN-LAST:event_button_5ActionPerformed

    private void button_6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_6ActionPerformed
        tampilAngka("6");
    }//GEN-LAST:event_button_6ActionPerformed

    private void button_1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_1ActionPerformed
        tampilAngka("1");
    }//GEN-LAST:event_button_1ActionPerformed

    private void button_3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_3ActionPerformed
        tampilAngka("3");
    }//GEN-LAST:event_button_3ActionPerformed

    private void button_2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_2ActionPerformed
        tampilAngka("2");
    }//GEN-LAST:event_button_2ActionPerformed

    private void button_0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_0ActionPerformed
        tampilAngka("0");
    }//GEN-LAST:event_button_0ActionPerformed

    private void button_plusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_plusActionPerformed
        setOperator("+");
    }//GEN-LAST:event_button_plusActionPerformed

    private void button_clearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_clearActionPerformed
        resetKalkulator();
    }//GEN-LAST:event_button_clearActionPerformed

    private void button_deleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_deleteActionPerformed
        hapusSatuAngka();
    }//GEN-LAST:event_button_deleteActionPerformed

    private void button_divide1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_divide1ActionPerformed
        setOperator("/");
    }//GEN-LAST:event_button_divide1ActionPerformed

    private void button_multiply1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_multiply1ActionPerformed
        setOperator("x");
    }//GEN-LAST:event_button_multiply1ActionPerformed

    private void button_subtract1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_subtract1ActionPerformed
        setOperator("-");
    }//GEN-LAST:event_button_subtract1ActionPerformed

    private void button_equals1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_equals1ActionPerformed
        hitung();
    }//GEN-LAST:event_button_equals1ActionPerformed

    private void button_comaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_comaActionPerformed
        if (currentInput.contains(",")) {
            return;
        }

        if (currentInput.isEmpty()) {
            currentInput = "0,";
        } else {
            currentInput += ",";
        }

        layar.setText(currentInput);
        updateHistoryLive();
    }//GEN-LAST:event_button_comaActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Calculator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Calculator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Calculator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Calculator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new KalkulatorUAS().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton button_0;
    private javax.swing.JButton button_1;
    private javax.swing.JButton button_2;
    private javax.swing.JButton button_3;
    private javax.swing.JButton button_4;
    private javax.swing.JButton button_5;
    private javax.swing.JButton button_6;
    private javax.swing.JButton button_7;
    private javax.swing.JButton button_8;
    private javax.swing.JButton button_9;
    private javax.swing.JButton button_clear;
    private javax.swing.JButton button_coma;
    private javax.swing.JButton button_delete;
    private javax.swing.JButton button_divide1;
    private javax.swing.JButton button_equals1;
    private javax.swing.JButton button_multiply1;
    private javax.swing.JButton button_plus;
    private javax.swing.JButton button_subtract1;
    private javax.swing.JTextArea history;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField layar;
    private javax.swing.JLabel text1;
    private javax.swing.JLabel text2;
    private javax.swing.JLabel text3;
    // End of variables declaration//GEN-END:variables
}
