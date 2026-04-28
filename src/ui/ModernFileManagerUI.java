package ui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.swing.*;
import model.FileItem;
import model.FolderItem;
import service.FileExplorerService;
import service.SearchService;
import service.WatcherService;
import threading.ProgressPublisher;

public class ModernFileManagerUI {

  private enum SortMode {
    NAME, SIZE, DATE
  }

  private JProgressBar progressBar;
  private JPanel grid;
  private JPanel sidebarContainer;
  private JLabel titleLabel;
  private JButton upButton;
  private JButton undoButton;
  private JComboBox<SortMode> sortComboBox;
  private JTextField searchField;
  private JCheckBox recursiveSearchCheck;
  private Path currentPath;
  private Path clipboardPath;
  private boolean isCutOperation;
  private SortMode currentSortMode = SortMode.NAME;
  private final FileExplorerService service = FileExplorerService.getInstance();
  private final SearchService searchService = new SearchService();
  private Timer searchDebounceTimer;
  private SwingWorker<List<FileItem>, Void> searchWorker;
  private WatcherService currentWatcher;

  public static void main(String[] args) {

    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");
    SwingUtilities.invokeLater(() -> new ModernFileManagerUI().createUI());
  }

  private void triggerCopy(Path source, Path destination) {
    progressBar.setVisible(true);
    progressBar.setValue(0);

    service.copy(
        source,
        destination,
        new ProgressPublisher.ProgressListener() {
          @Override
          public void onProgress(int percent) {
            progressBar.setValue(percent);
          }

          @Override
          public void onDone() {
            progressBar.setVisible(false);
            JOptionPane.showMessageDialog(null, "Copy complete!");
            refreshUI();
          }
        });
  }

  void createUI() {
    currentPath = Paths.get(System.getProperty("user.home"));

    JFrame frame = new JFrame("File Manager");
    frame.setSize(1000, 650);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    sidebarContainer = new JPanel();
    sidebarContainer.setLayout(new BoxLayout(sidebarContainer, BoxLayout.Y_AXIS));
    sidebarContainer.setBackground(new Color(30, 30, 30));
    sidebarContainer.setPreferredSize(new Dimension(150, 650));

    refreshSidebar();

    JPanel topBar = new JPanel(new BorderLayout(15, 0));
    topBar.setBackground(new Color(20, 20, 20));
    topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

    JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    leftTop.setOpaque(false);

    upButton = new RoundedButton("↑", 12);
    upButton.setPreferredSize(new Dimension(40, 35));
    upButton.setBackground(new Color(45, 45, 45));
    upButton.setForeground(Color.WHITE);
    upButton.setFocusPainted(false);
    upButton.setBorder(BorderFactory.createEmptyBorder());
    upButton.addActionListener(
        e -> {
          if (currentPath.getParent() != null) {
            navigateTo(currentPath.getParent());
          }
        });

    titleLabel = new JLabel(currentPath.getFileName().toString());
    titleLabel.setForeground(Color.WHITE);
    titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
    titleLabel.setHorizontalAlignment(SwingConstants.LEFT);

    leftTop.add(upButton);

    JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
    rightTop.setOpaque(false);

    undoButton = new RoundedButton("↶ Undo", 12);
    undoButton.setPreferredSize(new Dimension(80, 35));
    undoButton.setBackground(new Color(45, 45, 45));
    undoButton.setForeground(Color.WHITE);
    undoButton.setFocusPainted(false);
    undoButton.setBorder(BorderFactory.createEmptyBorder());
    undoButton.addActionListener(
        e -> {
          service.undo();
          refreshUI();
        });

    sortComboBox = new JComboBox<>(SortMode.values());
    sortComboBox.setPreferredSize(new Dimension(80, 35));
    sortComboBox.setBackground(new Color(45, 45, 45));
    sortComboBox.setForeground(Color.WHITE);
    sortComboBox.setFocusable(false);
    sortComboBox.addActionListener(e -> {
        currentSortMode = (SortMode) sortComboBox.getSelectedItem();
        refreshUI();
    });

    searchField = new RoundedTextField(15);
    searchField.setText(" Search files...");
    searchField.setPreferredSize(new Dimension(220, 35));
    searchField.setBackground(new Color(45, 45, 45));
    searchField.setForeground(Color.WHITE);
    searchField.setCaretColor(Color.WHITE);
    searchField.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

    searchField.addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyReleased(KeyEvent e) {
            String query = searchField.getText().trim();
            if (query.isEmpty() || query.equals("Search files...")) {
              if (searchDebounceTimer != null) searchDebounceTimer.stop();
              refreshUI();
            } else {
              debounceSearch(query);
            }
          }
        });

    searchField.addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(FocusEvent e) {
            if (searchField.getText().equals(" Search files...")) {
              searchField.setText("");
            }
          }

          @Override
          public void focusLost(FocusEvent e) {
            if (searchField.getText().isEmpty()) {
              searchField.setText(" Search files...");
            }
          }
        });

    recursiveSearchCheck = new JCheckBox("Recursive");
    recursiveSearchCheck.setOpaque(false);
    recursiveSearchCheck.setForeground(Color.WHITE);
    recursiveSearchCheck.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    recursiveSearchCheck.addActionListener(e -> {
      String query = searchField.getText().trim();
      if (!query.isEmpty() && !query.equals("Search files...")) {
        performSearch(query);
      }
    });

    rightTop.add(recursiveSearchCheck);
    rightTop.add(sortComboBox);
    rightTop.add(undoButton);
    rightTop.add(searchField);

    topBar.add(leftTop, BorderLayout.WEST);
    topBar.add(titleLabel, BorderLayout.CENTER);
    topBar.add(rightTop, BorderLayout.EAST);

    grid = new JPanel(new WrapLayout());
    grid.setBackground(new Color(18, 18, 18));
    grid.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 45));

    JPanel gridWrapper = new JPanel(new BorderLayout());
    gridWrapper.setBackground(new Color(18, 18, 18));
    gridWrapper.add(grid, BorderLayout.NORTH);

    JPopupMenu bgMenu = new JPopupMenu();
    JMenuItem newItem = new JMenuItem("New Folder");
    newItem.addActionListener(e -> createNewFolder());
    bgMenu.add(newItem);

    JMenuItem pasteItem = new JMenuItem("Paste");
    pasteItem.addActionListener(e -> triggerPaste());
    bgMenu.add(pasteItem);

    grid.setComponentPopupMenu(bgMenu);
    gridWrapper.setComponentPopupMenu(bgMenu);

    navigateTo(currentPath);

    JScrollPane scrollPane = new JScrollPane(gridWrapper);
    scrollPane.setBorder(null);
    scrollPane.getVerticalScrollBar().setUnitIncrement(20);
    scrollPane.getViewport().setBackground(new Color(18, 18, 18));
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    progressBar = new JProgressBar(0, 100);
    progressBar.setStringPainted(true);
    progressBar.setVisible(false);

    frame.add(sidebarContainer, BorderLayout.WEST);
    frame.add(topBar, BorderLayout.NORTH);
    frame.add(scrollPane, BorderLayout.CENTER);
    frame.add(progressBar, BorderLayout.SOUTH);

    frame.setVisible(true);
  }

  private void refreshUI() {
    grid.removeAll();

    String pathString = currentPath.toString();
    if (pathString.length() > 30) {
      String start = pathString.substring(0, 10);
      String end = pathString.substring(pathString.length() - 17);
      titleLabel.setText(start + "..." + end);
    } else {
      titleLabel.setText(pathString);
    }

    titleLabel.setToolTipText(pathString);
    upButton.setEnabled(currentPath.getParent() != null);

    List<FileItem> items = service.listContents(currentPath, this::navigateTo);

    for (FileItem item : items) {
      grid.add(createFileTile(item));
    }

    grid.revalidate();
    grid.repaint();
  }

  private void refreshSidebar() {
    sidebarContainer.removeAll();

    sidebarContainer.add(createSectionLabel("SYSTEM"));
    sidebarContainer.add(createSidebarButton("Home", System.getProperty("user.home"), false));
    sidebarContainer.add(createSidebarButton("Documents", System.getProperty("user.home") + "/Documents", false));
    sidebarContainer.add(createSidebarButton("Downloads", System.getProperty("user.home") + "/Downloads", false));

    List<String[]> favorites = service.getFavorites();
    if (!favorites.isEmpty()) {
        sidebarContainer.add(Box.createVerticalStrut(20));
        sidebarContainer.add(createSectionLabel("FAVORITES"));
        for (String[] fav : favorites) {
            sidebarContainer.add(createSidebarButton(fav[1], fav[0], true));
        }
    }

    List<String> recent = service.getRecentFiles();
    if (!recent.isEmpty()) {
        sidebarContainer.add(Box.createVerticalStrut(20));
        sidebarContainer.add(createSectionLabel("RECENT"));
        int count = 0;
        for (String path : recent) {
            if (count++ >= 5) break;
            Path p = Paths.get(path);
            sidebarContainer.add(createSidebarButton(p.getFileName().toString(), path, false));
        }
    }

    sidebarContainer.revalidate();
    sidebarContainer.repaint();
  }

  private JLabel createSectionLabel(String text) {
    JLabel label = new JLabel(text);
    label.setForeground(new Color(100, 100, 100));
    label.setFont(new Font("Segoe UI", Font.BOLD, 10));
    label.setBorder(BorderFactory.createEmptyBorder(10, 20, 5, 10));
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    return label;
  }

  private void debounceSearch(String query) {
    if (searchDebounceTimer != null && searchDebounceTimer.isRunning()) {
      searchDebounceTimer.restart();
    } else {
      searchDebounceTimer = new Timer(300, e -> performSearch(query));
      searchDebounceTimer.setRepeats(false);
      searchDebounceTimer.start();
    }
  }

  private void performSearch(String query) {
    if (searchWorker != null && !searchWorker.isDone()) {
      searchWorker.cancel(true);
    }

    titleLabel.setText("Searching for: " + query + "...");
    grid.removeAll();
    grid.revalidate();
    grid.repaint();

    searchWorker = new SwingWorker<>() {
      @Override
      protected List<FileItem> doInBackground() {
        return searchService.search(currentPath, query, recursiveSearchCheck.isSelected(), ModernFileManagerUI.this::navigateTo);
      }

      @Override
      protected void done() {
        if (isCancelled()) return;
        try {
          List<FileItem> results = get();
          grid.removeAll();
          titleLabel.setText("Search: " + query + " (" + results.size() + " found)");
          for (FileItem item : results) {
            grid.add(createFileTile(item));
          }
          grid.revalidate();
          grid.repaint();
        } catch (Exception e) {
          if (!(e instanceof java.util.concurrent.CancellationException)) {
            titleLabel.setText("Search failed: " + e.getMessage());
          }
        }
      }
    };
    searchWorker.execute();
  }

  private void createNewFolder() {
    String name = JOptionPane.showInputDialog(null, "Folder name:", "New Folder");
    if (name != null && !name.trim().isEmpty()) {
      try {
        Files.createDirectory(currentPath.resolve(name));
        refreshUI();
      } catch (IOException e) {
        JOptionPane.showMessageDialog(null, "Could not create folder: " + e.getMessage());
      }
    }
  }

  private void triggerPaste() {
    if (clipboardPath == null || !Files.exists(clipboardPath)) {
      JOptionPane.showMessageDialog(null, "Clipboard is empty or file missing.");
      return;
    }

    Path destination = currentPath.resolve(clipboardPath.getFileName());

    if (Files.exists(destination)) {
      String newName = "Copy_of_" + clipboardPath.getFileName().toString();
      destination = currentPath.resolve(newName);
    }

    if (isCutOperation) {
      service.move(clipboardPath, destination);
      clipboardPath = null;
      refreshUI();
    } else {
      triggerCopy(clipboardPath, destination);
    }
  }

  private void handleFuture(java.util.concurrent.Future<Void> future, String successMsg) {
    new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() throws Exception {
        future.get();
        return null;
      }

      @Override
      protected void done() {
        try {
          get();
          refreshUI();
          if (successMsg != null) {
            JOptionPane.showMessageDialog(null, successMsg);
          }
        } catch (Exception e) {
          JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
      }
    }.execute();
  }

  private void navigateTo(Path path) {
    if (currentWatcher != null) {
      currentWatcher.stop();
    }

    this.currentPath = path;
    refreshUI();

    currentWatcher = new WatcherService(currentPath, (eventType, affectedPath) -> {

      SwingUtilities.invokeLater(this::refreshUI);
    });
    currentWatcher.start();
  }

  JButton createSidebarButton(String text, String path, boolean isFavorite) {
    JButton btn = new RoundedButton(text, 15);
    btn.setMaximumSize(new Dimension(140, 40));
    btn.setPreferredSize(new Dimension(140, 40));
    btn.setAlignmentX(Component.LEFT_ALIGNMENT);
    btn.setFocusPainted(false);
    btn.setBackground(new Color(30, 30, 30));
    btn.setForeground(Color.LIGHT_GRAY);
    btn.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 10));
    btn.setHorizontalAlignment(SwingConstants.LEFT);

    btn.addActionListener(e -> {
      Path p = Paths.get(path);
      if (Files.isDirectory(p)) {
        navigateTo(p);
      } else if (Files.exists(p)) {
        openFile(service.toFileItem(p, this::navigateTo));
      }
    });

    if (isFavorite) {
      JPopupMenu sidebarMenu = new JPopupMenu();
      JMenuItem removeFav = new JMenuItem("Remove from Favorites");
      removeFav.addActionListener(e -> {
        service.removeFavorite(Paths.get(path));
        refreshSidebar();
      });
      sidebarMenu.add(removeFav);
      btn.setComponentPopupMenu(sidebarMenu);
    }

    btn.addMouseListener(
        new MouseAdapter() {
          public void mouseEntered(MouseEvent e) {
            btn.setBackground(new Color(45, 45, 45));
            btn.setForeground(Color.WHITE);
          }

          public void mouseExited(MouseEvent e) {
            btn.setBackground(new Color(30, 30, 30));
            btn.setForeground(Color.LIGHT_GRAY);
          }
        });

    return btn;
  }

  JPanel createFileTile(FileItem item) {
    JPanel tile = new RoundedPanel(22);
    tile.setLayout(new BorderLayout(0, 10));
    tile.setBackground(new Color(35, 35, 35));
    tile.setPreferredSize(new Dimension(90, 100));
    tile.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

    JLabel icon = new JLabel(item.getIcon(), SwingConstants.CENTER);

    String displayName = item.getName();
    if (displayName.length() > 10) {
      displayName = displayName.substring(0, 8) + "..";
    }
    JLabel label = new JLabel(displayName, SwingConstants.CENTER);
    label.setForeground(Color.WHITE);
    label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
    label.setToolTipText(item.getName());

    label.putClientProperty(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    tile.add(icon, BorderLayout.CENTER);
    tile.add(label, BorderLayout.SOUTH);

    JPopupMenu menu = new JPopupMenu();

    JMenuItem openItem = new JMenuItem("Open");
    openItem.addActionListener(e -> openFile(item));
    menu.add(openItem);

    menu.addSeparator();

    JMenuItem cutItem = new JMenuItem("Cut");
    cutItem.addActionListener(
        e -> {
          clipboardPath = item.getPath();
          isCutOperation = true;
        });
    menu.add(cutItem);

    JMenuItem copyItem = new JMenuItem("Copy");
    copyItem.addActionListener(
        e -> {
          clipboardPath = item.getPath();
          isCutOperation = false;
        });
    menu.add(copyItem);

    JMenuItem renameItem = new JMenuItem("Rename");
    renameItem.addActionListener(
        e -> {
          String newName = JOptionPane.showInputDialog(null, "New name:", item.getName());
          if (newName != null && !newName.trim().isEmpty()) {
            handleFuture(
                service.rename(item.getPath(), item.getPath().resolveSibling(newName)), null);
          }
        });
    menu.add(renameItem);

    JMenuItem deleteItem = new JMenuItem("Delete");
    deleteItem.addActionListener(
        e -> {
          int confirm = JOptionPane.showConfirmDialog(null, "Delete " + item.getName() + "?");
          if (confirm == JOptionPane.YES_OPTION) {
            handleFuture(service.delete(item.getPath()), null);
          }
        });
    menu.add(deleteItem);

    menu.addSeparator();

    if (item.isEncrypted()) {
      JMenuItem decryptItem = new JMenuItem("Decrypt");
      decryptItem.addActionListener(
          e -> handleFuture(service.decrypt(item.getPath()), "File Decrypted"));
      menu.add(decryptItem);
    } else if (!(item instanceof FolderItem)) {
      JMenuItem encryptItem = new JMenuItem("Encrypt");
      encryptItem.addActionListener(
          e -> handleFuture(service.encrypt(item.getPath()), "File Encrypted"));
      menu.add(encryptItem);
    }

    JMenuItem zipItem = new JMenuItem("Zip");
    zipItem.addActionListener(
        e -> {
          Path zipPath = item.getPath().resolveSibling(item.getName() + ".zip");
          handleFuture(service.zip(item.getPath(), zipPath), "Zip created");
          });
          menu.add(zipItem);

          menu.addSeparator();

          if (service.isFavorite(item.getPath())) {
          JMenuItem removeFav = new JMenuItem("Remove from Favorites");
          removeFav.addActionListener(e -> {
          service.removeFavorite(item.getPath());
          refreshSidebar();
          });
          menu.add(removeFav);
          } else {
          JMenuItem addFav = new JMenuItem("Add to Favorites");
          addFav.addActionListener(e -> {
          service.addFavorite(item.getPath());
          refreshSidebar();
          });
          menu.add(addFav);
          }

          tile.setComponentPopupMenu(menu);

    tile.addMouseListener(
        new MouseAdapter() {
          public void mouseEntered(MouseEvent e) {
            tile.setBackground(new Color(50, 50, 50));
            tile.setCursor(new Cursor(Cursor.HAND_CURSOR));
          }

          public void mouseExited(MouseEvent e) {
            tile.setBackground(new Color(35, 35, 35));
            tile.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
          }

          public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
              openFile(item);
            }
          }
        });

    return tile;
  }

  private void openFile(FileItem item) {
    if (!(item instanceof FolderItem)) {
      service.trackRecent(item.getPath());
      refreshSidebar();
    }
    item.open();
  }

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
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2.setColor(getBackground());
      g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
    }
  }

  class RoundedButton extends JButton {
    private int cornerRadius;

    RoundedButton(String label, int radius) {
      super(label);
      this.cornerRadius = radius;
      setOpaque(false);
      setContentAreaFilled(false);
      setFocusPainted(false);
      setBorderPainted(false);
    }

    protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(getBackground());
      g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
      super.paintComponent(g);
    }
  }

  class RoundedTextField extends JTextField {
    private int cornerRadius;

    RoundedTextField(int radius) {
      this.cornerRadius = radius;
      setOpaque(false);
    }

    protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(getBackground());
      g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
      super.paintComponent(g);
    }

    protected void paintBorder(Graphics g) {

    }
  }

class WrapLayout extends FlowLayout {
    public WrapLayout() {
      super(FlowLayout.LEFT, 15, 15);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
      return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
      Dimension minimum = layoutSize(target, false);
      minimum.width -= (getHgap() + 1);
      return minimum;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
      synchronized (target.getTreeLock()) {
        int targetWidth = target.getSize().width;
        if (targetWidth == 0) {
          targetWidth = target.getParent() != null ? target.getParent().getSize().width : 1000;
        }
        int hgap = getHgap(), vgap = getVgap();
        Insets insets = target.getInsets();
        int maxWidth = targetWidth - (insets.left + insets.right + (hgap * 2));
        Dimension dim = new Dimension(0, 0);
        int rowWidth = 0, rowHeight = 0;
        for (int i = 0, n = target.getComponentCount(); i < n; i++) {
          Component m = target.getComponent(i);
          if (m.isVisible()) {
            Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
            if (rowWidth + d.width > maxWidth && rowWidth > 0) {
              dim.width = Math.max(dim.width, rowWidth);
              dim.height += rowHeight + vgap;
              rowWidth = 0; rowHeight = 0;
            }
            rowWidth += d.width + hgap;
            rowHeight = Math.max(rowHeight, d.height);
          }
        }
        dim.width = Math.max(dim.width, rowWidth);
        dim.height += rowHeight + vgap + insets.top + insets.bottom;
        return dim;
      }
    }
  }
}
