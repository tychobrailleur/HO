package core;

import core.db.DBManager;
import core.db.user.User;
import core.db.backup.BackupHelper;
import core.db.user.UserManager;
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
import javax.swing.*;
import java.text.NumberFormat;
import java.util.Locale;

public class HO {

    public static double VERSION;  // Version is set in build.gradle and exposed to HO via the manifest
	public static int RevisionNumber;
    private static String versionType;
	private static boolean portableVersion; // Used to determined the location of the DB

	public static String getVersionType() {
		return versionType;
	}
	public static int getRevisionNumber() {
		return RevisionNumber;
	}
	public static boolean isPortableVersion() {
		return portableVersion;
	}
	public static boolean isDevelopment() {
		return "DEV".equalsIgnoreCase(versionType);
	}
	public static boolean isBeta() {
		return "BETA".equalsIgnoreCase(versionType);
	}
	public static boolean isRelease() {
		return "RELEASE".equalsIgnoreCase(versionType);
	}

	public static String getVersionString() {
		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMinimumFractionDigits(3);
		String txt = nf.format(VERSION);

		if (isBeta()) {
			txt += " BETA (r" + RevisionNumber + ")";
		}
		else if (isDevelopment()) {
			txt += " DEV (r" + RevisionNumber + ")";
		}

		return txt;
	}


	/**
	 *  HO entry point
	 */
	public static void main(String[] args) {
		portableVersion = true;

		if (OSUtils.isMac()) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("apple.awt.showGroupBox", "true");
		}

		System.setProperty("sun.awt.exception.handler", ExceptionHandler.class.getName());
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

		if (args != null) {
			String arg;
			for (String _arg : args) {
				arg = _arg.trim().toUpperCase();
				switch (arg) {
					case "INFO" -> HOLogger.instance().setLogLevel(HOLogger.INFORMATION);
					case "DEBUG" -> HOLogger.instance().setLogLevel(HOLogger.DEBUG);
					case "WARNING" -> HOLogger.instance().setLogLevel(HOLogger.WARNING);
					case "ERROR" -> HOLogger.instance().setLogLevel(HOLogger.ERROR);
					case "INSTALLED" -> portableVersion = false;
				}
			}
		}

		// Get HO version from manifest
		String version = HO.class.getPackage().getImplementationVersion();
		if (version != null) {
			String[] aVersion = version.split("\\.");

			VERSION = Double.parseDouble(aVersion[0] + "." + aVersion[1]);
			RevisionNumber = Integer.parseInt(aVersion[3]);
            switch (aVersion[2]) {
                case "0" -> versionType = "DEV";
                case "1" -> versionType = "BETA";
                default -> versionType = "RELEASE";
            }
        } else {
        	HOLogger.instance().error(HO.class, "Launched from IDE otherwise there is a bug !");
        	VERSION = 0d;
        	versionType = "DEV";
        }

		// Login selection in case of multi-users DB
		try {
			if (!UserManager.instance().isSingleUser()) {

                String[] teamNames = UserManager.instance()
                    .getAllUser()
                    .stream()
                    .map(User::getTeamName)
                    .toArray(String[]::new);

				final JComboBox<String> comboBox = new JComboBox<>(teamNames);
				int choice = JOptionPane.showConfirmDialog(null, comboBox, "Login",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

				if (choice == JOptionPane.OK_OPTION) {
					UserManager.instance().setINDEX(comboBox.getSelectedIndex());
				} else {
					System.exit(0);
				}
			}
		} catch (Exception ex) {
			HOLogger.instance().log(HO.class, ex);
		}

		// start display splash image
		final SplashFrame interuptionsWindow = new SplashFrame();

		// Backup
		interuptionsWindow.setInfoText(1, "Backup Database");
		BackupHelper.backup(new File(UserManager.instance().getCurrentUser().getDbFolder()));


		// Load user parameters from the DB
		interuptionsWindow.setInfoText(2, "Initialize Database");
		DBManager.instance().loadUserParameter();

		// init Theme
		try {
			ThemeManager.instance().setCurrentTheme();
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

		// Load table columns information
		UserColumnController.instance().load();

		// Set the currency from HRF
		float fxRate = (float) HOVerwaltung.instance().getModel().getXtraDaten().getCurrencyRate();
		if (fxRate > -1) UserParameter.instance().faktorGeld = fxRate;


		// Training
		interuptionsWindow.setInfoText(7, "Initialize Training");

		// Training estimation calculated on DB manual entries
		TrainingManager.instance().refreshTrainingWeeks();

		interuptionsWindow.setInfoText(8, "Prepare to show");
		SwingUtilities.invokeLater(() -> {
			HOMainFrame.instance().setVisible(true);

			// stop display splash image
			interuptionsWindow.setVisible(false);
			interuptionsWindow.dispose();
		});
	}
}
