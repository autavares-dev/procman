package procman;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

class Section {
	public record Content(String subtitle, String content) {
	};

	public static JPanel Create(String title, Content[] contents) {
		var section = new JPanel();
		var sectionGrid = new GridLayout(2, 1);
		sectionGrid.setVgap(2);
		section.setLayout(sectionGrid);

		var titleLabel = new JLabel(title);
		titleLabel.setHorizontalAlignment(JLabel.CENTER);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
		section.add(titleLabel);

		var contentPanel = new JPanel();
		var contentGrid = new GridLayout(contents.length, 2);
		contentGrid.setVgap(2);
		contentGrid.setHgap(8);
		contentPanel.setLayout(contentGrid);

		for (var content : contents) {
			var subtitleBox = Box.createHorizontalBox();
			subtitleBox.add(Box.createHorizontalGlue());
			var subtitle = new JLabel(content.subtitle + ":");
			subtitle.setFont(subtitle.getFont().deriveFont(Font.BOLD, 14));
			subtitleBox.add(subtitle);

			var contentBox = Box.createHorizontalBox();
			contentBox.add(new JLabel(content.content));
			contentBox.add(Box.createHorizontalGlue());

			contentPanel.add(subtitleBox);
			contentPanel.add(contentBox);
		}

		section.add(contentPanel);

		return section;
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
				new Section.Content("Architecture",
						System.getProperty("os.arch")) };
		content.add(Section.Create("Operational System", osSection));

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
