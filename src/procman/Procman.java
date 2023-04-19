package procman;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/**
 * Main class of the application. Renders the Swing GUI.
 */
public final class Procman {
	public Procman() {
		// Main window, starts maximized.
		var mainFrame = new JFrame("procman");
		mainFrame.setSize(256, 256);
		mainFrame.setExtendedState(
				mainFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Tabs navigation for each screen.
		var tabbed = new JTabbedPane();
		tabbed.addTab("Processes", new ProcessList());
		tabbed.addTab("System  info.", new SystemInfo());

		// Outer panel to contain the whole application.
		var outterPanel = new JPanel(new BorderLayout());
		outterPanel.add(tabbed, BorderLayout.CENTER);

		// Scroll pane for the whole application.
		var scrollPane = new JScrollPane(outterPanel);
		mainFrame.add(scrollPane, BorderLayout.CENTER);

		mainFrame.setVisible(true);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new Procman();
			}
		});
	}
}
