import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ModernFileManagerUI {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ModernFileManagerUI().createUI());
    }

    void createUI() {
        JFrame frame = new JFrame("File Manager");
        frame.setSize(1000, 650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // ===== Sidebar =====
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(30, 30, 30));
        sidebar.setPreferredSize(new Dimension(200, 650));

        String[] items = {"Home", "Documents", "Downloads", "Music", "Pictures", "Videos", "Trash"};

        for (String item : items) {
            sidebar.add(createSidebarButton(item));
        }

        // ===== Top Bar =====
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(20, 20, 20));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel title = new JLabel("Home");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JTextField search = new JTextField(" Search files...");
        search.setPreferredSize(new Dimension(220, 35));
        search.setBackground(new Color(45, 45, 45));
        search.setForeground(Color.WHITE);
        search.setCaretColor(Color.WHITE);
        search.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        topBar.add(title, BorderLayout.WEST);
        topBar.add(search, BorderLayout.EAST);

        // ===== Main Grid =====
        JPanel grid = new JPanel(new GridLayout(2, 4, 25, 25));
        grid.setBackground(new Color(18, 18, 18));
        grid.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        String[] folders = {"Desktop", "Documents", "Downloads", "Music", "Pictures", "Public", "Templates", "Videos"};

        for (String f : folders) {
            grid.add(createFolderCard(f));
        }

        JScrollPane scrollPane = new JScrollPane(grid);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);

        frame.add(sidebar, BorderLayout.WEST);
        frame.add(topBar, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    // ===== Sidebar Button with selection =====
    JButton createSidebarButton(String text) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(200, 45));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setBackground(new Color(30, 30, 30));
        btn.setForeground(Color.LIGHT_GRAY);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));
        btn.setHorizontalAlignment(SwingConstants.LEFT);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(55, 55, 55));
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(30, 30, 30));
            }

            public void mouseClicked(MouseEvent e) {
                btn.setBackground(new Color(70, 70, 70)); // selected effect
            }
        });

        return btn;
    }

    // ===== Folder Card with hover + shadow feel =====
    JPanel createFolderCard(String name) {
        JPanel card = new RoundedPanel(20);
        card.setLayout(new BorderLayout());
        card.setBackground(new Color(40, 40, 40));
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel icon = new JLabel("\uD83D\uDCC1", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 50));

        JLabel label = new JLabel(name, SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        card.add(icon, BorderLayout.CENTER);
        card.add(label, BorderLayout.SOUTH);

        // hover effect
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(65, 65, 65));
                card.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            public void mouseExited(MouseEvent e) {
                card.setBackground(new Color(40, 40, 40));
                card.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        return card;
    }

    // ===== Custom Rounded Panel =====
    class RoundedPanel extends JPanel {
        private int cornerRadius;

        RoundedPanel(int radius) {
            this.cornerRadius = radius;
            setOpaque(false);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        }
    }
}