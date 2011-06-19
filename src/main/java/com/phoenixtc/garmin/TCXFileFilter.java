package com.phoenixtc.garmin;
import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * A filter to list only the Garmin TCX files in the File Chooser
 */
public class TCXFileFilter extends FileFilter {
	private static final String TCX_FILES_DESC = "TCX files";
	private static final String TCX_FILE_EXT = "tcx";

	@Override
	public boolean accept(File f) {
		if (f.isDirectory()) {
			return true;
		}

		String extension = getExtension(f);
		if (extension != null) {
			if (extension.equalsIgnoreCase(TCX_FILE_EXT)) {
				return true;
			} else {
				return false;
			}
		}

		return false;
	}

	public String getExtension(File f) {
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');

		if (i > 0 && i < s.length() - 1) {
			ext = s.substring(i + 1).toLowerCase();
		}
		return ext;
	}

	@Override
	public String getDescription() {
		return TCX_FILES_DESC;
	}

}
