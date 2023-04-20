package procman;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.Arrays;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
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

	// TODO: add other relevant columns.
	static final String[] COLUMN_NAMES = { "PID", "Path", "Process", "User" };

	public ProcessList() {
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));

		// Filters.
		// TODO: improve appearance using smaller fields and left alignment.
		var filters = new JPanel();
		filters.setBorder(new EmptyBorder(16, 16, 16, 16));
		add(filters, BorderLayout.NORTH);
		var grid = new GridLayout(4, 2);
		grid.setVgap(4);
		filters.setLayout(grid);

		filters.add(new JLabel("PID:"));
		pidFilter = new JTextField();
		filters.add(pidFilter);

		filters.add(new JLabel("Path:"));
		pathFilter = new JTextField();
		filters.add(pathFilter);

		filters.add(new JLabel("Process:"));
		processFilter = new JTextField();
		filters.add(processFilter);

		filters.add(new JLabel("User:"));
		userFilter = new JTextField();
		filters.add(userFilter);

		// Creates JTable passing a TableModel to allow data updating and
		// disables 'setAutoCreateColumnsFromModel' to keep columns sizes after
		// updating the data.
		var model = new DefaultTableModel(COLUMN_NAMES, 0);
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
	}

	public void updateTable() {
		final var filteredPid = pidFilter.getText();
		final var filteredPath = pathFilter.getText();
		final var filteredProcess = processFilter.getText();
		final var filteredUser = userFilter.getText();

		var processList = ProcessHandle
				.allProcesses()
				.map((p) -> {
					final var pid = Long.valueOf(p.pid());
					final var info = p.info();
					final var fullPath = info.command().orElse("");
					final var paths = fullPath.split(File.separator);
					final var path = String.join(File.separator,
							Arrays.copyOf(paths, paths.length - 1));
					final var process = paths[paths.length - 1];
					final var user = info.user().orElse("");

					return new Object[] { pid, path, process, user };
				})
				.filter(p ->
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

		// TODO: add right-click menu with actions to selected process.
		var model = (DefaultTableModel) table.getModel();

		// Stores current sorting keys.
		// TODO: could be done using a sorting change event in the table?
		final var sortKeys = table.getRowSorter().getSortKeys();

		// Stores current selected PID.
		// TODO: could be done using a click event in the row?
		final var selectedRow = table.getSelectedRow();
		final var selectedPid = selectedRow != -1
				? model.getValueAt(selectedRow, 0).toString()
				: null;

		// Updates the table content.
		model.setDataVector(processList, COLUMN_NAMES);
		model.fireTableDataChanged();

		// Sorts again with same keys.
		table.getRowSorter().setSortKeys(sortKeys);

		// Reselects row of the selected PID if any.
		if (selectedPid != null) {
			for (var i = 0; i < model.getRowCount(); i++) {
				if (model.getValueAt(i, 0).toString().equals(selectedPid)) {
					table.setRowSelectionInterval(i, i);
					break;
				}
			}
		}
	}

	/**
	 * Resizes each columns to have 2x the width of the largest cell, based
	 * on the current table data.
	 */
	public void resizeColumns() {
		var columnModel = table.getColumnModel();
		for (var j = 0; j < COLUMN_NAMES.length; j++) {
			var col = columnModel.getColumn(j);
			var renderer = col.getHeaderRenderer();
			var width = 0;

			for (var i = 0; i < table.getRowCount(); i++) {
				renderer = table.getCellRenderer(i, j);
				var comp = renderer.getTableCellRendererComponent(
						table,
						table.getValueAt(i, j),
						false,
						false,
						i, j);
				width = Math.max(width, comp.getPreferredSize().width);
			}

			col.setPreferredWidth(width * 2);
		}
	}
}
