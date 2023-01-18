package com.sebastienduche;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

/**
 * Title : My-GitHub-Server
 * Description : Votre description
 * Copyright : Copyright (c) 2022
 * Société : Seb Informatique
 *
 * @author Sébastien Duché
 */
public abstract class MyLauncher {

    public MyLauncher(Server server, String localVersion, String mainJar) {

        Thread updateThread = new Thread(() -> {
            server.debug("Start update thread");
            server.checkVersion();
            if (!server.hasAvailableUpdate(localVersion)) {
                startApplication(server, mainJar);
                shutDown();
                return;
            }
            File downloadDirectory = server.downloadVersion();

            if (downloadDirectory != null && downloadDirectory.isDirectory()) {
                final File[] files = downloadDirectory.listFiles();
                install(files, downloadDirectory);
                startApplication(server, mainJar);
            } else {
                server.debug("ERROR: Missing download directory");
            }
            shutDown();
        });

        Runtime.getRuntime().addShutdownHook(updateThread);
    }

    public abstract void install(File[] files, File directoryToDelete);

    private void shutDown() {
        Runtime.getRuntime().halt(0);
    }

    private void startApplication(Server server, String mainJar) {
        try {
            server.debug("Create process with " + mainJar);
            var pb = new ProcessBuilder("java", "-Dfile.encoding=UTF8", "-jar", mainJar);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                server.debug(line);
            }
            p.waitFor();
            p.destroy();
        } catch (IOException | InterruptedException ex) {
            showException(ex);
        }
    }

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
