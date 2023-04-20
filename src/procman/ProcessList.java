package procman;

import java.awt.BorderLayout;
import java.io.File;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

@SuppressWarnings("serial")
public final class ProcessList extends JPanel {
	private JTable processesTable;
	private JScrollPane scrollPane;
	private Timer updateTimer;

	// TODO: add other relevant columns.
	static final String[] COLUMN_NAMES = { "PID", "Path", "Process", "User" };

	private class Updater extends TimerTask {
		@Override
		public void run() {
			updateTable();
		}
	}

	public ProcessList() {
		setLayout(new BorderLayout());

		// Creates JTable passing a TableModel to allow data updating and
		// disables 'setAutoCreateColumnsFromModel' to keep columns sizes after
		// updating the data.
		// TODO: add automatic column resizing on creation.
		processesTable = new JTable(new DefaultTableModel(COLUMN_NAMES, 0));
		processesTable.setDefaultEditor(Object.class, null);
		processesTable.setAutoCreateColumnsFromModel(false);
		processesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Initializes empty table inside scroll table, not editable.
		scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);
		scrollPane.setViewportView(processesTable);

		// Updates the process table each 2 seconds (2 * 1000ms).
		updateTimer = new Timer();
		updateTimer.schedule(new Updater(), 0, 2000);
	}

	public void updateTable() {
		var processList = ProcessHandle
				.allProcesses()
				.map((p) -> {
					final var pid = Long.valueOf(p.pid()).toString();
					final var info = p.info();
					final var fullPath = info.command().orElse("");
					final var paths = fullPath.split(File.separator);
					final var path = String.join(
							File.separator,
							Arrays.copyOf(paths, paths.length - 1));
					final var process = paths[paths.length - 1];
					final var user = info.user().orElse("");

					return new String[] { pid, path, process, user };
				})
				// Filters out system process if the user has no privilege.
				.filter(p -> !p[1].isEmpty())
				// TODO: add filtering by other columns.
				// TODO: add sorting by any column.
				.sorted((a, b) -> Integer.valueOf(a[0]) - Integer.valueOf(b[0]))
				.collect(Collectors.toList())
				.toArray(new String[0][0]);


		// TODO: add right-click menu with actions to selected process.
		var model = (DefaultTableModel) processesTable.getModel();

		// Stores current selected PID.
		// TODO: could be done using a click event in the row?
		final var selectedRow = processesTable.getSelectedRow();
		final var selectedPid = selectedRow != -1 ?
				model.getValueAt(selectedRow, 0).toString() : null;

		// Updates the table content.
		model.setDataVector(processList, COLUMN_NAMES);
		model.fireTableDataChanged();

		// Reselects row of the selected PID if any.
		if (selectedPid != null) {
			for(var i = 0; i < model.getRowCount(); i++) {
				if (model.getValueAt(i, 0).toString().equals(selectedPid)) {
					processesTable.setRowSelectionInterval(i, i);
				}
			}
		}
	}
}
