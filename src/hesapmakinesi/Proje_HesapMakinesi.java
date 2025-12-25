package hesapmakinesi;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat; // Yeni Formatlayıcı

public class Proje_HesapMakinesi extends JFrame {

    // --- RENK PALETİ ---
    private Color colBackground, colPanel, colButton, colText, colAccent, colBorder;
    private boolean isDarkTheme = true; 

    // --- ARAYÜZ BİLEŞENLERİ ---
    private JPanel mainContainer;
    private JLayeredPane layeredPane;
    private JPanel sidebarPanel;
    private JPanel historyOverlayPanel;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JButton menuToggleBtn;
    private JButton historyToggleBtn;
    private JLabel titleLabel;
    
    // YÖNETİM LİSTELERİ
    private ArrayList<Component> themeComponents = new ArrayList<>();
    private ArrayList<JTextField> screenList = new ArrayList<>(); 
    private LinkedList<String> historyList = new LinkedList<>(); 
    private JList<String> historyJList; 
    
    // --- AKILLI FORMATLAYICI ---
    // # işareti: Varsa gösterir, yoksa (sıfırsa) göstermez.
    private DecimalFormat smartFormat = new DecimalFormat("#.##########");

    public Proje_HesapMakinesi() {
        temaRenkleriniAyarla(true);

        setTitle("Hesap Makinesi");
        setSize(420, 750);
        setMinimumSize(new Dimension(380, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        mainContainer = new JPanel(new BorderLayout());
        setContentPane(mainContainer);

        // 1. ÜST BAR
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        headerPanel.setPreferredSize(new Dimension(0, 60));
        
        menuToggleBtn = createHamburgerButton();
        menuToggleBtn.setPreferredSize(new Dimension(50, 40));
        
        historyToggleBtn = createHistoryButton();
        historyToggleBtn.setPreferredSize(new Dimension(50, 40));
        
        titleLabel = new JLabel("Standart", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        
        headerPanel.add(menuToggleBtn, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(historyToggleBtn, BorderLayout.EAST);
        
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        themeComponents.add(headerPanel);
        themeComponents.add(titleLabel);

        // 2. KATMANLI ALAN
        layeredPane = new JLayeredPane();
        mainContainer.add(layeredPane, BorderLayout.CENTER);

        // --- KATMAN 1: İÇERİK ---
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        layeredPane.add(contentPanel, JLayeredPane.DEFAULT_LAYER);
        themeComponents.add(contentPanel);

        // --- KATMAN 2: YAN MENÜ ---
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBorder(new EmptyBorder(15, 10, 10, 10));
        sidebarPanel.setOpaque(true);
        sidebarPanel.setVisible(false);
        layeredPane.add(sidebarPanel, JLayeredPane.PALETTE_LAYER);
        themeComponents.add(sidebarPanel);

        // --- KATMAN 3: GEÇMİŞ ---
        historyOverlayPanel = new JPanel(new BorderLayout());
        historyOverlayPanel.setBorder(new EmptyBorder(15, 10, 10, 10));
        historyOverlayPanel.setOpaque(true);
        historyOverlayPanel.setVisible(false);
        
        JLabel histTitle = new JLabel("Geçmiş", SwingConstants.CENTER);
        histTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        histTitle.setBorder(new EmptyBorder(0,0,10,0));
        historyOverlayPanel.add(histTitle, BorderLayout.NORTH);
        historyOverlayPanel.add(gecmisIcerigiOlustur(), BorderLayout.CENTER);

        layeredPane.add(historyOverlayPanel, JLayeredPane.POPUP_LAYER);
        themeComponents.add(historyOverlayPanel);
        themeComponents.add(histTitle);

        // --- BOYUTLANDIRMA ---
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = layeredPane.getWidth();
                int h = layeredPane.getHeight();
                contentPanel.setBounds(0, 0, w, h);
                sidebarPanel.setBounds(0, 0, 240, h);
                int histH = (int)(h * 0.6);
                historyOverlayPanel.setBounds(0, h - histH, w, histH);
                layeredPane.revalidate(); layeredPane.repaint();
            }
        });

        initModules();
        setupActions();
        
        // GLOBAL KLAVYE KONTROLÜ
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && isFocused()) {
                // Menüler açıksa yazmayı engelle
                if (sidebarPanel.isVisible() || historyOverlayPanel.isVisible()) return false;

                char k = e.getKeyChar();
                JTextField active = null;
                for(JTextField tf : screenList) if(tf.isShowing()) { active = tf; break; }
                if(active != null) {
                    if(Character.isDigit(k) || "+-*/.()^%".indexOf(k) != -1) cmdIsle(String.valueOf(k), active);
                    else if(e.getKeyCode() == KeyEvent.VK_ENTER) cmdIsle("=", active);
                    else if(e.getKeyCode() == KeyEvent.VK_BACK_SPACE) cmdIsle("Del", active);
                }
            }
            return false;
        });

        applyTheme(); 
        try {
            java.net.URL url = getClass().getResource("/icon.png");
            if (url != null) this.setIconImage(Toolkit.getDefaultToolkit().getImage(url));
        } catch (Exception ex) {}

        setVisible(true);
    }

    private void setupActions() {
        menuToggleBtn.addActionListener(e -> {
            historyOverlayPanel.setVisible(false);
            sidebarPanel.setVisible(!sidebarPanel.isVisible());
            if(sidebarPanel.isVisible()) layeredPane.moveToFront(sidebarPanel);
        });

        historyToggleBtn.addActionListener(e -> {
            sidebarPanel.setVisible(false);
            historyOverlayPanel.setVisible(!historyOverlayPanel.isVisible());
            if(historyOverlayPanel.isVisible()) layeredPane.moveToFront(historyOverlayPanel);
        });
    }

    private void initModules() {
        JPanel standartPanel = hesapMakinesiPaneliOlustur(false);
        JPanel bilimselPanel = hesapMakinesiPaneliOlustur(true);

        addSidebarItem("Standart", "STANDART", standartPanel);
        addSidebarItem("Bilimsel", "BILIMSEL", bilimselPanel);
        
        sidebarPanel.add(Box.createVerticalStrut(15));
        sidebarPanel.add(new JSeparator());
        sidebarPanel.add(Box.createVerticalStrut(15));
        
        addSidebarItem("Uzunluk", "UZUNLUK", donusturucuPaneliOlustur("Uzunluk"));
        addSidebarItem("Ağırlık", "AGIRLIK", donusturucuPaneliOlustur("Ağırlık"));
        addSidebarItem("Hacim", "HACIM", donusturucuPaneliOlustur("Hacim"));
        addSidebarItem("Veri", "VERI", donusturucuPaneliOlustur("Veri"));
        addSidebarItem("Zaman", "ZAMAN", donusturucuPaneliOlustur("Zaman"));
        addSidebarItem("Sıcaklık", "SICAKLIK", sicaklikPaneliOlustur());
        addSidebarItem("Tarih", "TARIH", tarihHesaplamaPaneliOlustur());
        
        sidebarPanel.add(Box.createVerticalGlue());
        addSidebarItem("Ayarlar", "AYARLAR", ayarlarPaneliOlustur());
    }

    private void addSidebarItem(String name, String cardName, JPanel panel) {
        contentPanel.add(panel, cardName);
        JButton btn = createRoundedButton(name);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        btn.addActionListener(e -> {
            for(JTextField tf : screenList) tf.setText(""); 
            cardLayout.show(contentPanel, cardName);
            titleLabel.setText(name);
            sidebarPanel.setVisible(false);
            
            if (cardName.equals("BILIMSEL")) setSize(520, 750); 
            else setSize(420, 750);
            setLocationRelativeTo(null);
        });
        
        sidebarPanel.add(btn);
        sidebarPanel.add(Box.createVerticalStrut(8));
    }

    // --- GEÇMİŞ YÖNETİMİ ---
    private void gecmiseEkle(String islem, String sonuc) {
        String kayit = islem + " = " + sonuc;
        historyList.addFirst(kayit);
        if (historyList.size() > 5) historyList.removeLast(); 
        
        DefaultListModel<String> model = (DefaultListModel<String>) historyJList.getModel();
        model.clear();
        for(String s : historyList) model.addElement(s);
    }

    private JScrollPane gecmisIcerigiOlustur() {
        DefaultListModel<String> model = new DefaultListModel<>();
        historyJList = new JList<>(model);
        historyJList.setFont(new Font("Consolas", Font.PLAIN, 18));
        historyJList.setFixedCellHeight(40);
        historyJList.setBackground(colPanel);
        historyJList.setForeground(colText);
        JScrollPane scroll = new JScrollPane(historyJList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(colPanel);
        return scroll;
    }

    // --- TEMA ---
    private void temaRenkleriniAyarla(boolean dark) {
        if (dark) {
            colBackground = new Color(32, 32, 32);
            colPanel = new Color(45, 45, 45);
            colButton = new Color(60, 60, 60);
            colText = new Color(240, 240, 240);
            colAccent = new Color(255, 140, 0);
            colBorder = new Color(80, 80, 80);
        } else {
            colBackground = new Color(245, 245, 245);
            colPanel = new Color(255, 255, 255);
            colButton = new Color(230, 230, 230);
            colText = new Color(30, 30, 30);
            colAccent = new Color(0, 120, 215);
            colBorder = new Color(200, 200, 200);
        }
    }

    private void applyTheme() {
        mainContainer.setBackground(colBackground);
        sidebarPanel.setBackground(colPanel);
        historyOverlayPanel.setBackground(colPanel);
        contentPanel.setBackground(colBackground);
        if(menuToggleBtn instanceof IconButton) ((IconButton)menuToggleBtn).updateThemeColors();
        if(historyToggleBtn instanceof IconButton) ((IconButton)historyToggleBtn).updateThemeColors();
        
        for (Component comp : themeComponents) {
            updateComponentTheme(comp);
        }
        if(historyJList != null) {
             historyJList.setBackground(colPanel); historyJList.setForeground(colText);
             if(historyJList.getParent() instanceof JViewport) 
                 historyJList.getParent().setBackground(colPanel);
        }
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void updateComponentTheme(Component comp) {
        if (comp instanceof JPanel) {
            comp.setBackground(colBackground);
            if (comp == sidebarPanel || comp == historyOverlayPanel) comp.setBackground(colPanel);
        } else if (comp instanceof RoundedButton) {
            ((RoundedButton) comp).updateThemeColors();
        } else if (comp instanceof JLabel || comp instanceof JRadioButton) {
            comp.setForeground(colText);
        } else if (comp instanceof JTextField) {
            JTextField txt = (JTextField) comp;
            txt.setBackground(colPanel); txt.setForeground(colText); txt.setCaretColor(colText);
            txt.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(15), new EmptyBorder(5,10,5,10)));
        } else if (comp instanceof ModernComboBoxButton) {
             ((ModernComboBoxButton) comp).updateThemeColors();
        }
    }

    // --- HESAP MAKİNESİ PANELİ ---
    private JPanel hesapMakinesiPaneliOlustur(boolean bilimsel) {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setFocusable(false);
        themeComponents.add(p);
        
        JTextField ekran = new JTextField();
        ekran.setFont(new Font("Consolas", Font.BOLD, 45));
        ekran.setHorizontalAlignment(JTextField.RIGHT);
        ekran.setEditable(false); ekran.setFocusable(false);
        themeComponents.add(ekran); screenList.add(ekran); 
        p.add(ekran, BorderLayout.NORTH);

        int cols = bilimsel ? 5 : 4; 
        JPanel tusPaneli = new JPanel(new GridLayout(0, cols, 8, 8));
        themeComponents.add(tusPaneli);

        String[] tuslar;
        if (bilimsel) {
            tuslar = new String[]{
                "sin", "cos", "tan", "Del", "C",
                "cot", "sec", "csc", "(", ")",
                "x^y", "sqrt", "%", "n!", "/",  
                "7", "8", "9", "*", "log",
                "4", "5", "6", "-", "ln",
                "1", "2", "3", "+", "pi",
                "+/-", "0", ".", "=", "e"
            };
        } else {
            tuslar = new String[]{
                "%", "CE", "C", "Del",  
                "1/x", "x^y", "sqrt", "/", 
                "7", "8", "9", "*",      
                "4", "5", "6", "-",      
                "1", "2", "3", "+",      
                "+/-", "0", ".", "="     
            };
        }

        for (String t : tuslar) {
            JButton b = createRoundedButton(t);
            b.setFont(new Font("Segoe UI", Font.BOLD, 20));
            b.addActionListener(e -> cmdIsle(e.getActionCommand(), ekran));
            tusPaneli.add(b);
        }
        
        p.add(tusPaneli, BorderLayout.CENTER);
        return p;
    }

    private void cmdIsle(String cmd, JTextField txt) {
        if (cmd.equals("=")) {
            try {
                if(txt.getText().isEmpty()) return;
                String islem = txt.getText(); 
                double sonuc = eval(islem);
                // Sonucu temiz formatla göster
                String s = smartFormat.format(sonuc);
                txt.setText(s);
                gecmiseEkle(islem, s); 
            } catch (Exception e) { txt.setText("Hata"); }
        } else if (cmd.equals("C")) { txt.setText(""); }
        else if (cmd.equals("CE")) { 
            for(JTextField tf : screenList) tf.setText("");
            historyList.clear();
            ((DefaultListModel)historyJList.getModel()).clear();
        } 
        else if (cmd.equals("Del")) {
            String s = txt.getText(); if (s.length() > 0) txt.setText(s.substring(0, s.length() - 1));
        } else if (cmd.equals("x^y")) txt.setText(txt.getText() + "^");
          else if (cmd.equals("%")) txt.setText(txt.getText() + "%"); 
          else if (cmd.equals("sqrt")) txt.setText(txt.getText() + "sqrt(");
          else if (cmd.equals("1/x")) txt.setText("1/(" + txt.getText() + ")");
          else if (cmd.equals("n!")) {
             try { txt.setText(String.valueOf(faktoriyel(Double.parseDouble(txt.getText())))); }
             catch(Exception ex) { txt.setText("Hata"); }
          } else if (isFonksiyon(cmd)) txt.setText(txt.getText() + cmd + "(");
            else if (cmd.equals("pi")) txt.setText(txt.getText() + Math.PI);
            else if (cmd.equals("e")) txt.setText(txt.getText() + Math.E);
            else if (cmd.equals("+/-")) {
                 if(!txt.getText().isEmpty()) txt.setText("-" + txt.getText());
                 else txt.setText("-");
            } else txt.setText(txt.getText() + cmd);
        
        txt.setCaretPosition(txt.getText().length());
    }

    // --- YARDIMCI SINIFLAR ---
    private abstract class IconButton extends JButton {
        protected Color hoverColor = colAccent;
        public IconButton() {
            setFocusPainted(false); setContentAreaFilled(false); setBorderPainted(false);
            updateThemeColors();
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { setBackground(hoverColor); }
                public void mouseExited(MouseEvent e) { updateThemeColors(); }
            });
        }
        public void updateThemeColors() { setBackground(colBackground); setForeground(colText); hoverColor = colButton; repaint(); }
    }

    private class HamburgerButton extends IconButton {
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground()); g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),15,15));
            g2.setColor(getForeground()); g2.setStroke(new BasicStroke(3));
            int w=getWidth(), h=getHeight(), x=(w-24)/2, y=(h-21)/2+1;
            g2.drawLine(x,y,x+24,y); g2.drawLine(x,y+9,x+24,y+9); g2.drawLine(x,y+18,x+24,y+18);
            g2.dispose();
        }
    }

    private class HistoryButton extends IconButton {
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground()); g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),15,15));
            g2.setColor(getForeground()); g2.setStroke(new BasicStroke(2.5f));
            int s=24, x=(getWidth()-s)/2, y=(getHeight()-s)/2;
            g2.draw(new Ellipse2D.Double(x,y,s,s));
            g2.drawLine(x+s/2, y+s/2, x+s/2, y+s/2-7);
            g2.drawLine(x+s/2, y+s/2, x+s/2+6, y+s/2);
            g2.dispose();
        }
    }

    private JButton createHamburgerButton() { HamburgerButton b = new HamburgerButton(); themeComponents.add(b); return b; }
    private JButton createHistoryButton() { HistoryButton b = new HistoryButton(); themeComponents.add(b); return b; }

    private class RoundedButton extends JButton {
        private Color hoverColor = colAccent;
        public RoundedButton(String text) {
            super(text); setFocusPainted(false); setContentAreaFilled(false); setBorderPainted(false);
            setFont(new Font("Segoe UI", Font.PLAIN, 18)); updateThemeColors();
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { setBackground(hoverColor); setForeground(Color.WHITE); }
                public void mouseExited(MouseEvent e) { updateThemeColors(); }
            });
        }
        public void updateThemeColors() {
            if(getText().equals("=")) { setBackground(colAccent); setForeground(Color.WHITE); hoverColor = colAccent.darker(); }
            else { setBackground(colButton); setForeground(colText); hoverColor = colAccent; } repaint();
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground()); g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),20,20));
            super.paintComponent(g); g2.dispose();
        }
    }

    private class ModernComboBoxButton extends JButton {
        private JPopupMenu popup;
        public ModernComboBoxButton(String[] items) {
            super(items.length>0 ? items[0]+" ▼" : "Seçiniz ▼");
            setFont(new Font("Segoe UI", Font.PLAIN, 18));
            setFocusPainted(false); setContentAreaFilled(false); setBorder(new RoundedBorder(15));
            setHorizontalAlignment(SwingConstants.LEFT);
            popup = new JPopupMenu();
            for(String s : items) {
                JMenuItem i = new JMenuItem(s); i.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                i.addActionListener(e -> setText(s+" ▼")); popup.add(i);
            }
            addActionListener(e -> popup.show(ModernComboBoxButton.this, 0, getHeight()));
            updateThemeColors();
        }
        public String getSelectedItem() { return getText().replace(" ▼",""); }
        public void updateThemeColors() {
            setBackground(colButton); setForeground(colText);
            if(popup!=null) { popup.setBackground(colPanel); for(Component c:popup.getComponents()) { c.setBackground(colPanel); c.setForeground(colText); } }
        }
        protected void paintComponent(Graphics g) {
             Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground()); g2.fill(new RoundRectangle2D.Double(0,0,getWidth()-1,getHeight()-1,15,15));
            super.paintComponent(g); g2.dispose();
        }
    }

    private class RoundedBorder extends AbstractBorder {
        private int r; RoundedBorder(int r) { this.r=r; }
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(colBorder); g2.draw(new RoundRectangle2D.Double(x,y,w-1,h-1,r,r)); g2.dispose();
        }
        public Insets getBorderInsets(Component c) { return new Insets(r/2,r/2,r/2,r/2); }
    }

    private JButton createRoundedButton(String text) { RoundedButton b = new RoundedButton(text); if(!text.equals("=")) themeComponents.add(b); return b; }

    // --- DİĞER PANELLER ---
    private JPanel ayarlarPaneliOlustur() {
        JPanel p = new JPanel(new GridLayout(4,1,15,15)); p.setBorder(new EmptyBorder(30,30,30,30)); themeComponents.add(p);
        JLabel l = new JLabel("Tema", SwingConstants.CENTER); l.setFont(new Font("Segoe UI",Font.BOLD,24)); themeComponents.add(l); p.add(l);
        JRadioButton d = new JRadioButton("Koyu"), lg = new JRadioButton("Açık");
        d.setOpaque(false); lg.setOpaque(false); d.setFont(new Font("Segoe UI",0,18)); lg.setFont(new Font("Segoe UI",0,18));
        ButtonGroup g = new ButtonGroup(); g.add(d); g.add(lg); d.setSelected(true);
        ActionListener al = e -> { isDarkTheme = d.isSelected(); temaRenkleriniAyarla(isDarkTheme); applyTheme(); };
        d.addActionListener(al); lg.addActionListener(al);
        p.add(d); p.add(lg); themeComponents.add(d); themeComponents.add(lg); return p;
    }

    private JPanel donusturucuPaneliOlustur(String tip) {
        JPanel p = new JPanel(new GridLayout(6,1,12,12)); p.setBorder(new EmptyBorder(20,30,20,30)); themeComponents.add(p);
        JLabel l = new JLabel(tip,0); l.setFont(new Font("Segoe UI",1,28)); themeComponents.add(l); p.add(l);
        JTextField t = new JTextField(); t.setFont(new Font("Arial",0,22)); themeComponents.add(t); p.add(t);
        String[] b = birimListesi(tip); ModernComboBoxButton c1 = new ModernComboBoxButton(b), c2 = new ModernComboBoxButton(b);
        p.add(c1); p.add(c2); themeComponents.add(c1); themeComponents.add(c2);
        JButton btn = createRoundedButton("Çevir"); JLabel res = new JLabel("Sonuç: ",0); res.setFont(new Font("Segoe UI",1,20)); themeComponents.add(res);
        btn.addActionListener(e -> { 
            try { 
                double v = Double.parseDouble(t.getText()); 
                double r = birimCevirTam(tip, v, c1.getSelectedItem(), c2.getSelectedItem()); 
                // Format düzeltildi: Gereksiz sıfırlar yok
                res.setText("Sonuç: " + smartFormat.format(r)); 
            } catch(Exception ex) { res.setText("Sayı giriniz"); } 
        });
        p.add(btn); p.add(res); return p;
    }

    private JPanel sicaklikPaneliOlustur() {
        JPanel p = new JPanel(new GridLayout(6,1,12,12)); p.setBorder(new EmptyBorder(20,30,20,30)); themeComponents.add(p);
        JLabel l = new JLabel("Sıcaklık",0); l.setFont(new Font("Segoe UI",1,28)); themeComponents.add(l); p.add(l);
        JTextField t = new JTextField(); t.setFont(new Font("Arial",0,22)); themeComponents.add(t); p.add(t);
        String[] b = {"Celsius","Fahrenheit","Kelvin"}; ModernComboBoxButton c1 = new ModernComboBoxButton(b), c2 = new ModernComboBoxButton(b);
        p.add(c1); p.add(c2); themeComponents.add(c1); themeComponents.add(c2);
        JButton btn = createRoundedButton("Çevir"); JLabel res = new JLabel("Sonuç: ",0); res.setFont(new Font("Segoe UI",1,20)); themeComponents.add(res);
        btn.addActionListener(e -> { 
            try { double v=Double.parseDouble(t.getText()), c=v, r=v; String f=c1.getSelectedItem(), to=c2.getSelectedItem();
            if(f.equals("Fahrenheit")) c=(v-32)*5.0/9.0; if(f.equals("Kelvin")) c=v-273.15;
            if(to.equals("Fahrenheit")) r=(c*9.0/5.0)+32; if(to.equals("Kelvin")) r=c+273.15; else r=c;
            res.setText("Sonuç: " + smartFormat.format(r)); } catch(Exception ex){res.setText("Hata");} 
        });
        p.add(btn); p.add(res); return p;
    }

    private JPanel tarihHesaplamaPaneliOlustur() {
        JPanel p = new JPanel(new GridLayout(6,1,12,12)); p.setBorder(new EmptyBorder(20,30,20,30)); themeComponents.add(p);
        JLabel l = new JLabel("Tarih",0); l.setFont(new Font("Segoe UI",1,28)); themeComponents.add(l); p.add(l);
        JTextField t1 = new JTextField(LocalDate.now().toString()), t2 = new JTextField(LocalDate.now().plusDays(1).toString());
        t1.setBorder(BorderFactory.createTitledBorder(new RoundedBorder(10),"Başlangıç",0,0,null,colText));
        t2.setBorder(BorderFactory.createTitledBorder(new RoundedBorder(10),"Bitiş",0,0,null,colText));
        themeComponents.add(t1); themeComponents.add(t2); p.add(t1); p.add(t2);
        JButton btn = createRoundedButton("Hesapla"); JLabel res = new JLabel("Fark: ",0); res.setFont(new Font("Segoe UI",1,20)); themeComponents.add(res);
        btn.addActionListener(e -> { try { long d = ChronoUnit.DAYS.between(LocalDate.parse(t1.getText()), LocalDate.parse(t2.getText())); res.setText(Math.abs(d) + " Gün"); } catch(Exception ex){res.setText("Format: YYYY-AA-GG");} });
        p.add(btn); p.add(res); return p;
    }

    // --- MATEMATİK VE ÇEVİRİ MANTIĞI ---
    private boolean isFonksiyon(String s) { return s.equals("sin")||s.equals("cos")||s.equals("tan")||s.equals("cot")||s.equals("sec")||s.equals("csc")||s.equals("log")||s.equals("ln"); }
    private double faktoriyel(double d) { int n=(int)d; if(n<0)return 0; if(n==0)return 1; double f=1; for(int i=2;i<=n;i++)f*=i; return f; }
    
    private String[] birimListesi(String t) {
        if(t.equals("Uzunluk")) return new String[]{"Metre","Kilometre","Mil","Inch","Yard","Foot","Santimetre"};
        if(t.equals("Ağırlık")) return new String[]{"Kilogram","Gram","Pound","Ons","Ton"};
        if(t.equals("Hacim")) return new String[]{"Litre","Mililitre","Galon","Cup"};
        if(t.equals("Veri")) return new String[]{"Byte","KB","MB","GB","TB"};
        if(t.equals("Zaman")) return new String[]{"Saniye","Dakika","Saat","Gün","Yıl"};
        return new String[]{};
    }
    
    // TAM VE HATASIZ ÇEVİRİ MANTIĞI
    private double birimCevirTam(String t, double v, String f, String to) {
        double b=0;
        if(t.equals("Uzunluk")){ 
            if(f.equals("Metre"))b=v; else if(f.equals("Kilometre"))b=v*1000; else if(f.equals("Mil"))b=v*1609.34; else if(f.equals("Inch"))b=v*0.0254; else if(f.equals("Yard"))b=v*0.9144; else if(f.equals("Foot"))b=v*0.3048; else if(f.equals("Santimetre"))b=v*0.01;
            if(to.equals("Metre"))return b; if(to.equals("Kilometre"))return b/1000; if(to.equals("Mil"))return b/1609.34; if(to.equals("Inch"))return b/0.0254; if(to.equals("Yard"))return b/0.9144; if(to.equals("Foot"))return b/0.3048; if(to.equals("Santimetre"))return b/0.01;
        }
        else if(t.equals("Ağırlık")){ 
            if(f.equals("Gram"))b=v; else if(f.equals("Kilogram"))b=v*1000; else if(f.equals("Ton"))b=v*1000000; else if(f.equals("Pound"))b=v*453.592; else if(f.equals("Ons"))b=v*28.3495;
            if(to.equals("Gram"))return b; if(to.equals("Kilogram"))return b/1000; if(to.equals("Ton"))return b/1000000; if(to.equals("Pound"))return b/453.592; if(to.equals("Ons"))return b/28.3495;
        }
        else if(t.equals("Hacim")){ 
            if(f.equals("Litre"))b=v; else if(f.equals("Mililitre"))b=v*0.001; else if(f.equals("Galon"))b=v*3.785; else if(f.equals("Cup"))b=v*0.236;
            if(to.equals("Litre"))return b; if(to.equals("Mililitre"))return b/0.001; if(to.equals("Galon"))return b/3.785; if(to.equals("Cup"))return b/0.236;
        }
        else if(t.equals("Veri")){ 
            double k=1024; if(f.equals("Byte"))b=v; else if(f.equals("KB"))b=v*k; else if(f.equals("MB"))b=v*k*k; else if(f.equals("GB"))b=v*k*k*k; else if(f.equals("TB"))b=v*k*k*k*k;
            if(to.equals("Byte"))return b; if(to.equals("KB"))return b/k; if(to.equals("MB"))return b/(k*k); if(to.equals("GB"))return b/(k*k*k); if(to.equals("TB"))return b/(k*k*k*k);
        }
        else if(t.equals("Zaman")){ 
            if(f.equals("Saniye"))b=v; else if(f.equals("Dakika"))b=v*60; else if(f.equals("Saat"))b=v*3600; else if(f.equals("Gün"))b=v*86400; else if(f.equals("Yıl"))b=v*31536000;
            if(to.equals("Saniye"))return b; if(to.equals("Dakika"))return b/60; if(to.equals("Saat"))return b/3600; if(to.equals("Gün"))return b/86400; if(to.equals("Yıl"))return b/31536000;
        }
        return v; 
    }

    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;
            void nextChar() { ch = (++pos < str.length()) ? str.charAt(pos) : -1; }
            boolean eat(int charToEat) { while (ch == ' ') nextChar(); if (ch == charToEat) { nextChar(); return true; } return false; }
            double parse() { nextChar(); double x = parseExpression(); if (pos < str.length()) throw new RuntimeException("Error"); return x; }
            double parseExpression() { double x = parseTerm(); for (;;) { if (eat('+')) x += parseTerm(); else if (eat('-')) x -= parseTerm(); else return x; } }
            double parseTerm() { double x = parseFactor(); for (;;) { if (eat('*')) x *= parseFactor(); else if (eat('/')) x /= parseFactor(); else if (eat('%')) x %= parseFactor(); else return x; } }
            double parseFactor() {
                if (eat('+')) return parseFactor(); if (eat('-')) return -parseFactor();
                double x = 0; int startPos = this.pos;
                if (eat('(')) { x = parseExpression(); eat(')'); }
                else if ((ch >= '0' && ch <= '9') || ch == '.') { while ((ch >= '0' && ch <= '9') || ch == '.') nextChar(); x = Double.parseDouble(str.substring(startPos, this.pos)); }
                else if (ch >= 'a' && ch <= 'z') { while (ch >= 'a' && ch <= 'z') nextChar(); String func = str.substring(startPos, this.pos); x = parseFactor(); double r = Math.toRadians(x);
                    if (func.equals("sqrt")) x = Math.sqrt(x); else if (func.equals("sin")) x = Math.sin(r); else if (func.equals("cos")) x = Math.cos(r); else if (func.equals("tan")) x = Math.tan(r); else if (func.equals("cot")) x = 1.0/Math.tan(r); else if (func.equals("sec")) x = 1.0/Math.cos(r); else if (func.equals("csc")) x = 1.0/Math.sin(r); else if (func.equals("log")) x = Math.log10(x); else if (func.equals("ln")) x = Math.log(x);
                }
                if (eat('^')) x = Math.pow(x, parseFactor()); return x;
            }
        }.parse();
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new Proje_HesapMakinesi()); }
}