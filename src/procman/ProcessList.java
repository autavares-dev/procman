package procman;

import java.awt.BorderLayout;
import java.io.File;
import java.util.Arrays;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

@SuppressWarnings("serial")
public final class ProcessList extends JPanel {
	private JTable processesTable;
	private JScrollPane scrollPane;
	private Timer updateTimer;

	// TODO: add other relevant columns.
	static final String[] COLUMN_NAMES = { "PID", "Path", "Process", "User" };

	public ProcessList() {
		setLayout(new BorderLayout());

		// Creates JTable passing a TableModel to allow data updating and
		// disables 'setAutoCreateColumnsFromModel' to keep columns sizes after
		// updating the data.
		// TODO: add automatic column resizing on creation.
		var model = new DefaultTableModel(COLUMN_NAMES, 0);
		processesTable = new JTable(model);
		processesTable.setDefaultEditor(Object.class, null);
		processesTable.setAutoCreateColumnsFromModel(false);
		processesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		processesTable.setAutoCreateRowSorter(true);

		// Initializes empty table inside scroll table, not editable.
		scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);
		scrollPane.setViewportView(processesTable);

		// Updates the process table each 2 seconds (2 * 1000ms).
		// First update is manually invoked.
		updateTable();
		updateTimer = new Timer(2000, (ev) -> {
			updateTable();
		});
		updateTimer.setRepeats(true);
		updateTimer.start();
	}

	public void updateTable() {
		var processList = ProcessHandle
				.allProcesses().map((p) -> {
					final var pid = Long.valueOf(p.pid());
					final var info = p.info();
					final var fullPath = info.command().orElse("");
					final var paths = fullPath.split(File.separator);
					final var path = String.join(
							File.separator,
							Arrays.copyOf(paths, paths.length - 1));
					final var process = paths[paths.length - 1];
					final var user = info.user().orElse("");

					return new Object[] { pid, path, process, user };
				})
				// Filters out system process if the user has no privilege.
				// TODO: add filtering by other columns.
				.filter(p -> !((String) p[1]).isEmpty())
				.toArray(Object[][]::new);

		// TODO: add right-click menu with actions to selected process.
		var model = (DefaultTableModel) processesTable.getModel();

		// Stores current sorting keys.
		// TODO: could be done using a sorting change event in the table?
		final var sortKeys = processesTable.getRowSorter().getSortKeys();

		// Stores current selected PID.
		// TODO: could be done using a click event in the row?
		final var selectedRow = processesTable.getSelectedRow();
		final var selectedPid = selectedRow != -1
				? model.getValueAt(selectedRow, 0).toString()
				: null;

		// Updates the table content.
		model.setDataVector(processList, COLUMN_NAMES);
		model.fireTableDataChanged();

		// Sorts again with same keys.
		processesTable.getRowSorter().setSortKeys(sortKeys);

		// Reselects row of the selected PID if any.
		if (selectedPid != null) {
			for (var i = 0; i < model.getRowCount(); i++) {
				if (model.getValueAt(i, 0).toString().equals(selectedPid)) {
					processesTable.setRowSelectionInterval(i, i);
				}
			}
		}
	}
}
