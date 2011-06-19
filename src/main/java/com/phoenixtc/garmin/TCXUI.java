package com.phoenixtc.garmin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

public class TCXUI extends JPanel implements ActionListener {

	private static final long serialVersionUID = 8886800841776203036L;

	private static final String HELP = "Help";
	private static final String ABOUT = "About";

	private static final String PROCESS_A_TCX_FILE = "Process a TCX file";
	private static final String HELP_MSG = "This app is aimed at correcting the time problems that occur from using the data \r\n"
			+ "from Garmin 310xts smart recording and WKO. It also aims to fill in the blanks \r\n"
			+ "for the HR and cadence.\r\n"	+ "To use select the TCX file from your run and hit process, \r\n"
			+ "a new TCX file will be generated in the same location as the input file.";

	private static final String ABOUT_MSG = "version 1.01 \r\n"
			+ "This is very much a work in progress. \r\n"
			+ "I'm not happy with the cadence numbers that are generated. They are better than WKOs numbers but not quite the same as Garmins. \r\n"
			+ "There are bugs in this and if you find some please email a description of the bug and the tcx file that was used to tunney@gmail.com "
			+ "\r\n --Dave";

	private static final String TITLE = "310xt TCX file polishing app";
	private static final String newline = "\n";

	private JButton processButton;
	private JTextArea log;
	private JFileChooser fc;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				UIManager.put("swing.boldMetal", Boolean.FALSE);
				createAndShowGUI();
			}
		});
	}

	public TCXUI() {
		super(new BorderLayout());

		log = new JTextArea(5, 34);
		log.setMargin(new Insets(5, 5, 5, 5));
		log.setEditable(false);
		JScrollPane logScrollPane = new JScrollPane(log);

		fc = new JFileChooser();

		fc.addChoosableFileFilter(new TCXFileFilter());

		processButton = new JButton(PROCESS_A_TCX_FILE);
		processButton.addActionListener(this);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(processButton);

		add(buttonPanel, BorderLayout.PAGE_START);
		add(logScrollPane, BorderLayout.CENTER);

	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == processButton) {
			int returnVal = fc.showOpenDialog(TCXUI.this);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				log.append("Processing: " + file.getName() + "." + newline);
				try {
					TCXProcessor processor = new TCXProcessor(
							file.getAbsolutePath(),
							createOutput(file.getAbsolutePath()));
					processor.process();

					log.append("Processing " + file.getName() + " finished."
							+ newline);
				} catch (DatatypeConfigurationException e1) {
					e1.printStackTrace();
					log.append("Processing " + file.getName() + " failed."
							+ newline);
				} catch (JAXBException e1) {
					e1.printStackTrace();
					log.append("Processing " + file.getName() + " failed."
							+ newline + "Unable to marshal XML." + newline);
				} catch (IOException e1) {
					e1.printStackTrace();
					log.append("Processing " + file.getName() + " failed."
							+ newline + "Unable to save file." + newline);
				}
			} else {
				log.append("Process command cancelled by user." + newline);
			}
			log.setCaretPosition(log.getDocument().getLength());
		}
	}

	/**
	 * Create a display a TCX processor
	 */
	private static void createAndShowGUI() {

		final JFrame frame = new JFrame(TITLE);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.add(new TCXUI());

		JMenuBar menuBar = new JMenuBar();

		frame.setJMenuBar(menuBar);

		JMenu helpMenu = new JMenu(HELP);

		menuBar.add(helpMenu);

		JMenuItem helpAction = new JMenuItem(HELP);

		helpMenu.add(helpAction);

		helpAction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JOptionPane.showMessageDialog(frame, HELP_MSG);
			}
		});

		JMenu aboutMenu = new JMenu(ABOUT);
		menuBar.add(aboutMenu);

		JMenuItem aboutAction = new JMenuItem(ABOUT);
		aboutMenu.add(aboutAction);
		aboutAction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JOptionPane.showMessageDialog(frame, ABOUT_MSG);
			}
		});

		frame.pack();
		frame.setVisible(true);
		frame.setSize(new Dimension(450, 200));
	}
	
	/**
	 * Creates the output filename given an input file name
	 * 
	 * @param in The name of the file for which to create an output filename
	 * @return The output file name
	 */
	public static String createOutput(String in) {
		int startIndex = in.toUpperCase().indexOf(".TCX");

		String prefix = in.substring(0, startIndex);
		return prefix + "_processed.tcx";
	}

}
