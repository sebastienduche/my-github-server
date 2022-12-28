package com.sebastienduche;

import java.io.File;
import java.io.IOException;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

/**
 *
 * Title : My-GitHub-Server
 * Description : Votre description
 * Copyright : Copyright (c) 2022
 * Société : Seb Informatique
 * @author Sébastien Duché
 */
public abstract class MyLauncher {

	public MyLauncher(Server server, String lacalVersion, String mainJar) {

		Thread updateThread = new Thread(() -> {
					server.checkVersion();
					if (!server.hasAvailableUpdate(lacalVersion)) {
						return;
					}
					File downloadDirectory = server.downloadVersion();

					if (downloadDirectory != null && downloadDirectory.isDirectory()) {
						final File[] files = downloadDirectory.listFiles();
						install(files, downloadDirectory);
					} else {
						server.debug("ERROR: Missing download directory");
					}
					System.exit(0);
        });

        try {
					var pb = new ProcessBuilder("java","-Dfile.encoding=UTF8","-jar", mainJar);
					pb.redirectErrorStream(true);
					Process p = pb.start();
					p.waitFor();
					Runtime.getRuntime().addShutdownHook(updateThread);
					updateThread.start();
        } catch (IOException | InterruptedException ex) {
            showException(ex);
        }
	}

	public abstract void install(File[] files, File directoryToDelete);

	private static void showException(Exception e) {
		StackTraceElement[] st = e.getStackTrace();
		String error = "";
		for (StackTraceElement elem : st) {
			error = error.concat("\n" + elem);
		}
		showMessageDialog(null, e.toString(), "Error", ERROR_MESSAGE);
		System.exit(999);
	}

}
