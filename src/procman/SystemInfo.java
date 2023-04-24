package procman;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.EmptyBorder;

class Section {
	public record Content(String title, String content) {
	};

	public static JPanel Create(String title, Content[] contents) {
		var section = new JPanel();
		var grid = new GridLayout(1 + contents.length, 1);
		grid.setVgap(2);
		section.setLayout(grid);

		section.add(createTitle(title));
		for (var content : contents) {
			section.add(createInfo(content.title, content.content));
		}

		return section;
	}

	private static Box createTitle(String title) {
		return createRow(16, title, "");
	}

	private static Box createInfo(String title, String content) {
		return createRow(14, title, content);
	}

	private static Box createRow(int titleSize, String title, String content) {
		var row = Box.createHorizontalBox();

		var titleLabel = new JLabel(title + ": ");
		titleLabel
				.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleSize));
		var contentLabel = new JLabel(content);

		row.add(titleLabel);
		row.add(contentLabel);
		row.add(Box.createGlue());

		return row;
	}
}

@SuppressWarnings("serial")
public final class SystemInfo extends JPanel {
	public SystemInfo() {
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));
		// Vertical stack on the top of the screen.
		var content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		add(content, BorderLayout.NORTH);

		Section.Content[] osSection = {
				new Section.Content("O.S.", System.getProperty("os.name")),
				new Section.Content("Version",
						System.getProperty("os.version")),
				new Section.Content("Arch.", System.getProperty("os.arch")) };
		content.add(Section.Create("Operational System", osSection));

		content.add(new JSeparator());

		Section.Content[] javaSection = {
				new Section.Content("Version",
						System.getProperty("java.version")),
				new Section.Content("Vendor",
						System.getProperty("java.vendor")),
				new Section.Content("URL",
						System.getProperty("java.vendor.url")) };
		content.add(Section.Create("Java", javaSection));
	}
}
