package procman;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

@SuppressWarnings("serial")
public final class ProcessList extends JPanel {
  // Stores the OS file path separator character. Needs to escape to use as string splitter.
  private final String filePathSplitter = File.separator.replace("\\", "\\\\");

  // TODO: add other relevant columns.
  static final String[] COLUMN_NAMES = {"PID", "Path", "Process", "User"};

  private JTable table;
  private JScrollPane scrollPane;
  private Timer updateTimer;

  // Filters and task kill buttons.
  // TODO: allow only numeric values in the PID filter.
  // TODO: use a check-box drop down list for users.
  private JTextField pidFilter;
  private JTextField pathFilter;
  private JTextField processFilter;
  private JTextField userFilter;
  private JLabel pidLabel;
  private JButton killButton;
  private JButton forceKillButton;

  public ProcessList() {
    setLayout(new BorderLayout());
    setBorder(new EmptyBorder(8, 8, 8, 8));

    createTopMenu();
    createTable();
    createPopupMenu();
  }

  /**
   * Creates the top menu with filters and kill buttons.
   */
  private void createTopMenu() {
    // TODO: improve appearance using smaller fields and left alignment.

    // Filters.
    var topMenu = new JPanel();
    topMenu.setBorder(new EmptyBorder(16, 16, 16, 16));
    add(topMenu, BorderLayout.NORTH);

    var grid = new GridLayout(5, 2);
    grid.setVgap(4);
    topMenu.setLayout(grid);

    pidFilter = new JTextField();
    topMenu.add(new JLabel("PID:"));
    topMenu.add(pidFilter);

    pathFilter = new JTextField();
    topMenu.add(new JLabel("Path:"));
    topMenu.add(pathFilter);

    processFilter = new JTextField();
    topMenu.add(new JLabel("Process:"));
    topMenu.add(processFilter);

    userFilter = new JTextField();
    topMenu.add(new JLabel("User:"));
    topMenu.add(userFilter);

    // Task kill buttons.

    var buttonBox = Box.createHorizontalBox();
    topMenu.add(new JLabel("Selected PID: "));
    topMenu.add(buttonBox);

    pidLabel = new JLabel("-");
    buttonBox.add(pidLabel);
    buttonBox.add(Box.createHorizontalGlue());

    killButton = new JButton("Kill process");
    killButton.setEnabled(false);
    killButton.addActionListener((_ae) -> {
      kill(false);
    });

    forceKillButton = new JButton("Kill process (focibly)");
    forceKillButton.setEnabled(false);
    forceKillButton.addActionListener((_ae) -> {
      kill(true);
    });

    buttonBox.add(killButton);
    buttonBox.add(forceKillButton);
  }

  /**
   * Creates the process table.
   */
  private void createTable() {
    // Creates JTable passing a TableModel to allow data updating and
    // disables 'setAutoCreateColumnsFromModel' to keep columns sizes after
    // updating the data.
    // Overrides getColumnClass so Long column sorts properly.
    var model = new DefaultTableModel(COLUMN_NAMES, 0) {
      @Override
      public Class<?> getColumnClass(int column) {
        return switch (column) {
          case 0 -> Long.class;
          default -> String.class;
        };
      }
    };

    table = new JTable(model);
    table.setDefaultEditor(Object.class, null);
    table.setAutoCreateColumnsFromModel(false);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setAutoCreateRowSorter(true);
    table.setColumnSelectionAllowed(false);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    // Initializes empty table inside scroll table, not editable.
    scrollPane = new JScrollPane();
    add(scrollPane, BorderLayout.CENTER);
    scrollPane.setViewportView(table);

    // First update is invoked manually, resizes columns and sorts by PID.
    updateTable();
    resizeColumns();
    table.getRowSorter().setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));

    // Updates the process table each 1 seconds (1000ms).
    updateTimer = new Timer(1000, (_ev) -> {
      updateTable();
    });
    updateTimer.setRepeats(true);
    updateTimer.start();

    // Row selection event.
    table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent event) {
        updateSelectedRow();
      }
    });
  }

  /**
   * Creates the right-click pop-up menu.
   */
  private void createPopupMenu() {
    var popupMenu = new JPopupMenu();

    var killItem = new JMenuItem("Kill");
    var killAllItem = new JMenuItem("Kill all");

    var forceKillItem = new JMenuItem("Kill (forcibly)");
    var forceKillAllItem = new JMenuItem("Kill all (forcibly)");

    var openFolderItem = new JMenuItem("Open folder");

    killItem.addActionListener((_ae) -> {
      kill(false);
    });

    killAllItem.addActionListener((_ae) -> {
      killAll(false);
    });

    forceKillItem.addActionListener((_ae) -> {
      kill(true);
    });

    forceKillAllItem.addActionListener((_ae) -> {
      killAll(true);
    });

    openFolderItem.addActionListener((_ae) -> {
      try {
        Desktop.getDesktop().open(new File(getSelectedPath()));
      } catch (IOException e) {
        // TODO: replace auto-generated try-catch block.
        e.printStackTrace();
      }
    });

    popupMenu.add(killItem);
    popupMenu.add(killAllItem);

    popupMenu.addSeparator();

    popupMenu.add(forceKillItem);
    popupMenu.add(forceKillAllItem);

    popupMenu.addSeparator();

    popupMenu.add(openFolderItem);

    // Mouse listener for the pop-up menu. Right-click also selects the row.
    table.addMouseListener(new MouseAdapter() {
      private void processMouse(MouseEvent me) {
        if (me.isPopupTrigger()) {
          int r = table.rowAtPoint(me.getPoint());
          if (r >= 0 && r < table.getRowCount()) {
            table.setRowSelectionInterval(r, r);
          } else {
            table.clearSelection();
          }

          popupMenu.show(me.getComponent(), me.getX(), me.getY());
        }
      }

      @Override
      public void mousePressed(MouseEvent me) {
        processMouse(me);
      }

      @Override
      public void mouseReleased(MouseEvent me) {
        processMouse(me);
      }
    });
  }

  /**
   * Kills a single process given the process handle.
   */
  private static void kill(ProcessHandle process, Boolean forcibly) {
    if (process != null) {
      if (forcibly) {
        process.destroyForcibly();
      } else {
        process.destroy();
      }
    }
  }

  /**
   * Kills the current selected process in the table.
   */
  private void kill(Boolean forcibly) {
    final var pid = getSelectedPid();
    kill(ProcessHandle.of(Long.valueOf(pid)).orElse(null), forcibly);
  }

  /**
   * Kills the current selected process in the table and all child processes.
   */
  private void killAll(Boolean forcibly) {
    final var pid = getSelectedPid();
    var process = ProcessHandle.of(Long.valueOf(pid)).orElse(null);
    if (process != null) {
      process.descendants().forEach(child -> kill(child, forcibly));
      kill(process, forcibly);
    }
  }

  /**
   * @return Column value of the current selected process (table row).
   */
  private String getSelectedRowColumn(int column) {
    final var i = table.getSelectedRow();
    // Converts table index (filtered and sorted view) to model index (data)
    return i != -1
        ? table.getModel().getValueAt(table.getRowSorter().convertRowIndexToModel(i), column)
            .toString()
        : null;
  }

  /**
   * @return PID of the current selected row.
   */
  public String getSelectedPid() {
    return getSelectedRowColumn(0);
  }

  /**
   * @return Folder path of the process of the current selected row.
   */
  public String getSelectedPath() {
    return getSelectedRowColumn(1);
  }

  /**
   * Updates the UI button and label related to the current selected process (table row).
   */
  private void updateSelectedRow() {
    final var selectedPid = getSelectedPid();
    pidLabel.setText(selectedPid != null ? selectedPid : "-");
    killButton.setEnabled(selectedPid != null);
    forceKillButton.setEnabled(selectedPid != null);
  }

  /**
   * Update table contents by checking running processes and applying filters.
   */
  public void updateTable() {

    final var processList = ProcessHandle.allProcesses().map((p) -> {

      final var info = p.info();
      final var paths = info.command().orElse("").split(filePathSplitter);

      final var pid = Long.valueOf(p.pid());
      final var path = String.join(File.separator, Arrays.copyOf(paths, paths.length - 1));
      final var process = paths[paths.length - 1];
      final var user = info.user().orElse("");

      return new Object[] {pid, path, process, user};
    }).filter(p -> (!((String) p[1]).isEmpty()) // If user has no privilege path is empty.
        && p[0].toString().toLowerCase().contains(pidFilter.getText().toLowerCase())
        && p[1].toString().toLowerCase().contains(pathFilter.getText().toLowerCase())
        && p[2].toString().toLowerCase().contains(processFilter.getText().toLowerCase())
        && p[3].toString().toLowerCase().contains(userFilter.getText().toLowerCase()))
        .toArray(Object[][]::new);

    // Stores current sorting keys and selected PID.
    final var sortKeys = table.getRowSorter().getSortKeys();
    final var selectedPid = getSelectedPid();

    // Updates the table content.
    var model = (DefaultTableModel) table.getModel();
    model.setDataVector(processList, COLUMN_NAMES);
    model.fireTableDataChanged();

    // Sorts again with same keys.
    table.getRowSorter().setSortKeys(sortKeys);

    // Reselects row of the selected PID if any.
    if (selectedPid != null) {
      for (var i = 0; i < model.getRowCount(); i++) {
        // Converts to model index to compare the values.
        var row = table.convertRowIndexToModel(i);
        if (model.getValueAt(row, 0).toString().equals(selectedPid)) {
          table.setRowSelectionInterval(i, i);
          break;
        }
      }
    }
  }

  /**
   * Resizes each columns to have 2x the width of the largest cell, based on the current table data.
   */
  public void resizeColumns() {
    var columnModel = table.getColumnModel();
    for (var j = 0; j < COLUMN_NAMES.length; j++) {
      var col = columnModel.getColumn(j);
      var renderer = col.getHeaderRenderer();
      var width = 0;

      for (var i = 0; i < table.getRowCount(); i++) {
        renderer = table.getCellRenderer(i, j);
        var comp = renderer.getTableCellRendererComponent(table, table.getValueAt(i, j), false,
            false, i, j);
        width = Math.max(width, comp.getPreferredSize().width);
      }

      col.setPreferredWidth(width * 2);
    }
  }
}
