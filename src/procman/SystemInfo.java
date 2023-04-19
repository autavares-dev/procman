package procman;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public final class SystemInfo extends JPanel {
	public SystemInfo() {
		setLayout(new BorderLayout());

		// Vertical stack on the top of the screen.
		var content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		add(content, BorderLayout.NORTH);

		// TODO: align labels to the left of the 'content' panel.

		content.add(createTitle("Operational System"));
		content.add(createInfo("O.S.", System.getProperty("os.name")));
		content.add(createInfo("Version", System.getProperty("os.version")));
		content.add(createInfo("Arch.", System.getProperty("os.arch")));

		content.add(createTitle("Java"));
		content.add(createInfo("Version", System.getProperty("java.version")));
		content.add(createInfo("Vendor", System.getProperty("java.vendor")));
		content.add(createInfo("URL", System.getProperty("java.vendor.url")));
	}

	static JPanel createTitle(String title) {
		return createRow(16, title, "");
	}

	static JPanel createInfo(String title, String content) {
		return createRow(14, title, content);
	}

	static JPanel createRow(int titleSize, String title, String content) {
		var row = new JPanel();
		row.setLayout(new FlowLayout());

		var titleLabel = new JLabel(title + ": ");
		titleLabel.setFont(row.getFont().deriveFont(Font.BOLD, titleSize));
		var contentLabel = new JLabel(content);

		row.add(titleLabel);
		row.add(contentLabel);

		return row;
	}
}
