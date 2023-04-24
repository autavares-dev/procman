package procman;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
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
	private JTable table;
	private JScrollPane scrollPane;
	private Timer updateTimer;

	// Filters.
	// TODO: allow only numeric values in the PID filter.
	// TODO: use a check-box drop down list for users.
	private JTextField pidFilter;
	private JTextField pathFilter;
	private JTextField processFilter;
	private JTextField userFilter;

	private JLabel pidLabel;
	private JButton killButton;
	private JButton forceKillButton;

	// TODO: add other relevant columns.
	static final String[] COLUMN_NAMES = { "PID", "Path", "Process", "User" };

	public ProcessList() {
		// TODO: add tree view toggle.

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));

		// Filters.
		// TODO: improve appearance using smaller fields and left alignment.
		var topMenu = new JPanel();
		topMenu.setBorder(new EmptyBorder(16, 16, 16, 16));
		add(topMenu, BorderLayout.NORTH);

		var grid = new GridLayout(5, 2);
		grid.setVgap(4);
		topMenu.setLayout(grid);

		topMenu.add(new JLabel("PID:"));
		pidFilter = new JTextField();
		topMenu.add(pidFilter);

		topMenu.add(new JLabel("Path:"));
		pathFilter = new JTextField();
		topMenu.add(pathFilter);

		topMenu.add(new JLabel("Process:"));
		processFilter = new JTextField();
		topMenu.add(processFilter);

		topMenu.add(new JLabel("User:"));
		userFilter = new JTextField();
		topMenu.add(userFilter);

		// Task kill buttons.
		// TODO: add kill children, kill with children.

		var selectedProcess = new JLabel("Selected PID: ");
		var buttonBox = Box.createHorizontalBox();
		topMenu.add(selectedProcess);
		topMenu.add(buttonBox);

		pidLabel = new JLabel("-");
		buttonBox.add(pidLabel);
		buttonBox.add(Box.createHorizontalGlue());

		killButton = new JButton("Kill process");
		killButton.setEnabled(false);
		killButton.addActionListener((ae) -> {
			kill(false);
		});

		forceKillButton = new JButton("Force kill process");
		forceKillButton.setEnabled(false);
		forceKillButton.addActionListener((ae) -> {
			kill(true);
		});

		buttonBox.add(killButton);
		buttonBox.add(forceKillButton);

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
		table.getRowSorter().setSortKeys(
				Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));

		// Updates the process table each 1 seconds (1000ms).
		updateTimer = new Timer(1000, (_ev) -> {
			updateTable();
		});
		updateTimer.setRepeats(true);
		updateTimer.start();

		// Row selection event.
		table.getSelectionModel()
				.addListSelectionListener(new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent event) {
						updateSelectedRow();
					}
				});

		// Right-click event with pop-up.
		var popup = new JPopupMenu();
		var kill = new JMenuItem("Kill");
		var forceKill = new JMenuItem("Force kill");
		kill.addActionListener((ae) -> {
			kill(false);
		});
		forceKill.addActionListener((ae) -> {
			kill(true);
		});
		popup.add(kill);
		popup.add(forceKill);

		table.addMouseListener(new MouseAdapter() {
			private void processMouse(MouseEvent me) {
				if (me.isPopupTrigger()) {
					int r = table.rowAtPoint(me.getPoint());
					if (r >= 0 && r < table.getRowCount()) {
						table.setRowSelectionInterval(r, r);
					} else {
						table.clearSelection();
					}

					popup.show(me.getComponent(), me.getX(), me.getY());
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

	private void kill(Boolean forcibly) {
		var pid = getSelectedPid();
		if (pid != null) {
			var process = ProcessHandle.of(Long.valueOf(pid)).orElse(null);
			if (process != null) {
				if (forcibly) {
					process.destroyForcibly();
				} else {
					process.destroy();
				}
			}
		}
	}

	/**
	 * @return Current PID of selected process (table row), null if none.
	 */
	public String getSelectedPid() {
		final var i = table.getSelectedRow();
		// Converts table index (filtered and sorted view) to model index (data)
		return i != -1
				? table.getModel()
						.getValueAt(
								table.getRowSorter().convertRowIndexToModel(i),
								0)
						.toString()
				: null;
	}

	private void updateSelectedRow() {
		final var selectedPid = getSelectedPid();
		pidLabel.setText(selectedPid != null ? selectedPid : "-");
		killButton.setEnabled(selectedPid != null);
		forceKillButton.setEnabled(selectedPid != null);
	}

	public void updateTable() {
		final var filteredPid = pidFilter.getText();
		final var filteredPath = pathFilter.getText();
		final var filteredProcess = processFilter.getText();
		final var filteredUser = userFilter.getText();

		var processList = ProcessHandle.allProcesses().map((p) -> {
			final var pid = Long.valueOf(p.pid());
			final var info = p.info();
			final var fullPath = info.command().orElse("");
			final var paths = fullPath.split(File.separator);
			final var path = String.join(File.separator,
					Arrays.copyOf(paths, paths.length - 1));
			final var process = paths[paths.length - 1];
			final var user = info.user().orElse("");

			return new Object[] { pid, path, process, user };
		}).filter(p ->
		// Filters out system process if the user has no privilege.
		(!((String) p[1]).isEmpty())
				&& (filteredPid.isEmpty()
						|| p[0].toString().contains(filteredPid))
				&& (filteredPath.isEmpty()
						|| p[1].toString().contains(filteredPath))
				&& (filteredProcess.isEmpty()
						|| p[2].toString().contains(filteredProcess))
				&& (filteredUser.isEmpty()
						|| p[3].toString().contains(filteredUser)))
				.toArray(Object[][]::new);

		var model = (DefaultTableModel) table.getModel();

		// Stores current sorting keys.
		final var sortKeys = table.getRowSorter().getSortKeys();

		// Stores current selected PID.
		var selectedPid = getSelectedPid();

		// Updates the table content.
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
	 * Resizes each columns to have 2x the width of the largest cell, based on
	 * the current table data.
	 */
	public void resizeColumns() {
		var columnModel = table.getColumnModel();
		for (var j = 0; j < COLUMN_NAMES.length; j++) {
			var col = columnModel.getColumn(j);
			var renderer = col.getHeaderRenderer();
			var width = 0;

			for (var i = 0; i < table.getRowCount(); i++) {
				renderer = table.getCellRenderer(i, j);
				var comp = renderer.getTableCellRendererComponent(table,
						table.getValueAt(i, j), false, false, i, j);
				width = Math.max(width, comp.getPreferredSize().width);
			}

			col.setPreferredWidth(width * 2);
		}
	}
}
