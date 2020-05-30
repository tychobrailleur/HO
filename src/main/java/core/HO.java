package core;

import core.db.DBManager;
import core.db.User;
import core.db.backup.BackupHelper;
import core.gui.HOMainFrame;
import core.gui.SplashFrame;
import core.gui.model.UserColumnController;
import core.gui.theme.ThemeManager;
import core.model.HOVerwaltung;
import core.model.UserParameter;
import core.training.TrainingManager;
import core.util.ExceptionHandler;
import core.util.HOLogger;
import core.util.OSUtils;
import java.io.File;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Main HO starter class.
 *
 * @author thomas.werth
 */



public class HO {


    public static double VERSION;  // Version is set in build.gradle and exposed to HO via the manifest
	public static int RevisionNumber;
    private static String versionType;

	/**
	 * Is this a development version? Note that a "development" version can a
	 * release ("Beta" or "DEV" version). The DEVELOPMENT flag is used by the
	 * ant build script. Keep around.
	 */

	/**
	 * A RELEASE is when a build artifact gets delivered to users. Note that
	 * even a DEVELOPMENT version can be a RELEASE ("Beta"). So when a version
	 * is build (no matter if DEVELOPMENT or not), this flag should be set to
	 * true. The main purpose for the flag is to disable code (unfinished new
	 * features, debug code) which should not be seen in a release.
	 */

	public static boolean isDevelopment() {
		return "DEV".equalsIgnoreCase(versionType);
	}

	public static boolean isBeta() {
		return "BETA".equalsIgnoreCase(versionType);
	}

	public static boolean isRelease() {
		return "RELEASE".equalsIgnoreCase(versionType);
	}

	public static String getVersionType() {
		return versionType;
	}

	public static String getVersionString() {
		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMinimumFractionDigits(3);
		String txt = nf.format(VERSION);

		if (isBeta()) {
			txt += " BETA (r" + RevisionNumber + ")";
		} else if (isDevelopment()) {
			txt += " DEV (r" + RevisionNumber + ")";
		}

		return txt;
	}


	/**
	 * Main method to start a HOMainFrame.
	 *
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		final long start = System.currentTimeMillis();

		if (OSUtils.isMac()) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("apple.awt.showGroupBox", "true");
		}

		System.setProperty("sun.awt.exception.handler", ExceptionHandler.class.getName());
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

		if ((args != null) && (args.length > 0)) {
			String debugLvl = args[0].trim().toUpperCase();

			if (debugLvl.equals("INFO")) {
				HOLogger.instance().setLogLevel(HOLogger.INFORMATION);
			} else if (debugLvl.equals("DEBUG")) {
				HOLogger.instance().setLogLevel(HOLogger.DEBUG);
			} else if (debugLvl.equals("WARNING")) {
				HOLogger.instance().setLogLevel(HOLogger.WARNING);
			} else if (debugLvl.equals("ERROR")) {
				HOLogger.instance().setLogLevel(HOLogger.ERROR);
			}
		}

		// Get HO version from manifest
		String sVERSION = HO.class.getPackage().getImplementationVersion();
		if (sVERSION != null) {
			String[] aVersion = sVERSION.split("\\.");

			VERSION = Double.parseDouble(aVersion[0] + "." + aVersion[1]);
			RevisionNumber = Integer.parseInt(aVersion[3]);
			switch (aVersion[2]) {
				case "0":
					versionType = "DEV";
					break;
				case "1":
					versionType = "BETA";
					break;
				default:
					versionType = "RELEASE";
					break;
			}
        } else {
        	HOLogger.instance().error(HO.class, "Launched from IDE otherwise there is a bug !");
        	VERSION = 0d;
        	versionType = "DEV";
        }

		// Usermanagement Login-Dialog
		try {
			if (!User.getCurrentUser().isSingleUser()) {
				JComboBox comboBox = new JComboBox(User.getAllUser().toArray());
				int choice = JOptionPane.showConfirmDialog(null, comboBox, "Login",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

				if (choice == JOptionPane.OK_OPTION) {
					User.INDEX = comboBox.getSelectedIndex();
				} else {
					System.exit(0);
				}
			}
		} catch (Exception ex) {
			HOLogger.instance().log(HO.class, ex);
		}

		// Startbild
		final SplashFrame interuptionsWindow = new SplashFrame();

		// Backup
		if (User.getCurrentUser().isHSQLDB()) {
			interuptionsWindow.setInfoText(1, "Backup Database");
			BackupHelper.backup(new File(User.getCurrentUser().getDBPath()));
		}

		// Standardparameter aus der DB holen
		interuptionsWindow.setInfoText(2, "Initialize Database");
		DBManager.instance().loadUserParameter();

		// init Theme
		try {
			ThemeManager.instance().setCurrentTheme(UserParameter.instance().theme);
		} catch (Exception e) {
			HOLogger.instance().log(HO.class, "Can´t load Theme:" + UserParameter.instance().theme);
			JOptionPane.showMessageDialog(null, e.getMessage(), "Can´t load Theme: "
					+ UserParameter.instance().theme, JOptionPane.WARNING_MESSAGE);
		}
		// Init!
		interuptionsWindow.setInfoText(3, "Initialize Data-Administration");

		// Ask for language at first start
		if (DBManager.instance().isFirstStart()) {
			interuptionsWindow.setVisible(false);
			new core.option.InitOptionsDialog();
			interuptionsWindow.setVisible(true);
		}

		// Check if language file available
		interuptionsWindow.setInfoText(4, "Check Languagefiles");
		HOVerwaltung.checkLanguageFile(UserParameter.instance().sprachDatei);
		HOVerwaltung.instance().setResource(UserParameter.instance().sprachDatei);

		if (DBManager.instance().isFirstStart()) {
			interuptionsWindow.setVisible(false);
			JOptionPane.showMessageDialog(null,
					HOVerwaltung.instance().getLanguageString("firststartup.infowinmessage"),
					HOVerwaltung.instance().getLanguageString("firststartup.infowinmessage.title"), JOptionPane.INFORMATION_MESSAGE);
			interuptionsWindow.setVisible(true);
		}

		interuptionsWindow.setInfoText(5, "Load latest Data");
		HOVerwaltung.instance().loadLatestHoModel();
		interuptionsWindow.setInfoText(6, "Load  XtraDaten");

		// TableColumn
		UserColumnController.instance().load();

		// Die Währung auf die aus dem HRF setzen
		float faktorgeld = (float) HOVerwaltung.instance().getModel().getXtraDaten()
				.getCurrencyRate();

		if (faktorgeld > -1) {
			UserParameter.instance().faktorGeld = faktorgeld;
		}

		// Training
		interuptionsWindow.setInfoText(7, "Initialize Training");

		// Training erstellen -> dabei Trainingswochen berechnen auf Grundlage
		// der manuellen DB Einträge
		TrainingManager.instance().refreshTrainingWeeks();

		interuptionsWindow.setInfoText(8, "Prepare to show");
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				HOMainFrame.instance().setVisible(true);

				// Startbild weg
				interuptionsWindow.setVisible(false);
				interuptionsWindow.dispose();

				HOLogger.instance().log(HO.class, "Zeit:" + (System.currentTimeMillis() - start));
			}
		});
	}

	public static int getRevisionNumber() {
		return RevisionNumber;
	}
}
