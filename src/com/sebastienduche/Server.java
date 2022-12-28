package com.sebastienduche;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * Titre : Cave à vin
 * Description : Votre description
 * Copyright : Copyright (c) 2011
 * Société : Seb Informatique
 * @author Sébastien Duché
 * @version 3.2
 * @since 10/10/21
 */

public abstract class Server implements Runnable {
	private static final String VERSION = "2";
	private final String gitHubUrl;
	private final String versionFileName;
	private final String mainJarName;
	private final String debugFileName;

	enum Action {
		NONE,
		GET_VERSION,
		DOWNLOAD
	}

	private String serverVersion = "";
	private String availableVersion = "";

	private Action action = Action.NONE;

	private static final LinkedList<FileType> FILE_TYPES = new LinkedList<>();

	private boolean downloadError = false;

	private FileWriter debugFile = null;

	private static final String DOWNLOAD_DIRECTORY = "download";
	private static final String LIB_DIRECTORY = "lib";

	public Server(String gitHubUrl, String versionFileName, String mainJarName, String debugFileName) {
		this.gitHubUrl = gitHubUrl;
		this.versionFileName = versionFileName;
		this.mainJarName = mainJarName;
		this.debugFileName = debugFileName;
	}

	@Override
	public void run() {
		debug("Server version: " + VERSION);
		if (Action.GET_VERSION.equals(action)) {
			checkVersion();
		} else if (Action.DOWNLOAD.equals(action)) {
			downloadFromServer();
		}
		if (downloadError) {
			new File(DOWNLOAD_DIRECTORY).deleteOnExit();
		}
	}

	private File downloadFromServer() {
		action = Action.NONE;
		downloadError = false;
		File downloadDirectory = null;
		try {
			downloadDirectory = new File(DOWNLOAD_DIRECTORY);
			if (!downloadDirectory.exists()) {
				Files.createDirectory(downloadDirectory.toPath());
			}

			downloadError = downloadFromGitHub();
		} catch (IOException e) {
			showException(e);
			downloadError = true;
		}
		return downloadDirectory;
	}

	private File downloadVersionFileTxt() throws IOException {
		final File version = File.createTempFile("Version", "txt");
		version.deleteOnExit();
		downloadFileFromGitHub(versionFileName, version);
		return version;
	}

	public void checkVersion() {
		debug("Server version: " + VERSION);
		debug("Checking version from GitHub...");
		serverVersion = "";
		action = Action.NONE;
		FILE_TYPES.clear();
		try {
			final File fileVersion = downloadVersionFileTxt();
			try (var bufferedReader = new BufferedReader(new FileReader(fileVersion))) {
				serverVersion = bufferedReader.readLine();
				availableVersion = bufferedReader.readLine();
				String file = bufferedReader.readLine();
				while (file != null && !file.isEmpty()) {
					int index = file.indexOf('@');
					String md5 = "";
					if (index != -1) {
						final String[] split = file.split("@");
						file = split[0];
						md5 = split[1];
					}
					debug("File... " + file);
					boolean lib = (!file.contains(mainJarName) && file.endsWith(".jar"));
					FILE_TYPES.add(new FileType(file, md5, lib));
					file = bufferedReader.readLine();
				}
			}
			debug("GitHub version: " + serverVersion + "/" + availableVersion);
		} catch (IOException e) {
			showException(e);
		}
		debug("Checking version from GitHub Done");
	}

	public File downloadVersion() {
		debug("Downloading version from GitHub...");
		File downloadDirectory = downloadFromServer();
		if (downloadError) {
			downloadDirectory.deleteOnExit();
			return null;
		}
		debug("Downloading version from GitHub Done");
		return downloadDirectory;
	}

	public String getAvailableVersion() {
		return availableVersion;
	}

	public String getServerVersion() {
		if (serverVersion.isEmpty()) {
			try {
				action = Action.GET_VERSION;
				new Thread(this).start();
			} catch (RuntimeException a) {
				showException(a);
			}
		}

		return serverVersion;
	}

	public boolean hasAvailableUpdate(String localVersion) {
		if (serverVersion.isEmpty()) {
			return false;
		}

		return (serverVersion.compareTo(localVersion) > 0);
	}

	private List<String> getLibFiles() {
		String path = "." + File.separator + LIB_DIRECTORY;
		if (!new File(path).exists()) {
			debug("Inexisting path " + path);
			return new ArrayList<>();
		}
		try {
			return Files.walk(Path.of(path), 1, FileVisitOption.FOLLOW_LINKS)
					.map(Path::toFile)
					.filter(File::isFile)
					.map(File::getName)
					.collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	private boolean downloadFromGitHub() {
		MyLauncherLoading download;
		try {
			download = new MyLauncherLoading("Downloading...");
			download.setText("Downloading in progress...", "Downloading...");
			download.setVisible(true);
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}

		downloadError = false;
		try {
			if (FILE_TYPES.isEmpty()) {
				checkVersion();
			}
			final List<String> jarsOnServer = FILE_TYPES
					.stream()
					.map(FileType::getFile)
					.collect(Collectors.toList());
			final List<String> libFiles = getLibFiles();
			libFiles.removeAll(jarsOnServer);
			// Creation des fichiers pour lister les fichiers à supprimer
			File destination = new File(DOWNLOAD_DIRECTORY);
			for (String fileNameToRemove : libFiles) {
				debug("Creating file to delete... " + fileNameToRemove);
				new File(destination, fileNameToRemove + ".myCellar").createNewFile();
			}
			debug("Connecting to GitHub...");

			int size = FILE_TYPES.size();
			int percent = 80;
			if (size > 0) {
				percent = 80 / size;
			}
			for (int i = 0; i < size; i++) {
				FileType fileType = FILE_TYPES.get(i);
				String name = fileType.getFile();
				String serverMd5 = fileType.getMd5();
				if (fileType.isForLibDirectory()) {
					File libFile = new File(LIB_DIRECTORY, name);
					if (libFile.exists()) {
						String localMd5 = getMD5Checksum(libFile.getAbsolutePath());
						if (localMd5.equals(serverMd5)) {
							debug("Skipping downloading file: " + name + " Md5 OK");
							continue;
						}	else {
							debug("Need to download file: " + name + " " + serverMd5 + " " + localMd5 + " KO");
						}
					}
				}
				downloadError = false;
				debug("Downloading... " + name);
				download.setValue(20 + i * percent);

				final File file = new File(destination, name);
				try {
					String serverDirectory = "";
					if (fileType.isForLibDirectory()) {
						serverDirectory = LIB_DIRECTORY + File.separator;
					}
					downloadFileFromGitHub(serverDirectory + name, file);
				} catch (IOException e) {
					showException(e);
					debug("Error Downloading " + name);
					downloadError = true;
				}

				if (!serverMd5.isEmpty() && !file.isDirectory()) {
					int fileSize;
					try (InputStream stream = new FileInputStream(file)) {
						fileSize = stream.available();
						String localMd5 = getMD5Checksum(file.getAbsolutePath());
						if (localMd5.equals(serverMd5)) {
							debug(name + " Md5 OK");
						}	else {
							debug(name + " " + serverMd5 + " " + localMd5 + " KO");
							downloadError = true;
						}
					}
					if (fileSize == 0) {
						downloadError = true;
					}
				}
				if (downloadError) {
					debug("Error " + name);
					file.deleteOnExit();
				}
			}
		} catch (IOException e) {
			debug("Server IO Exception:");
			showException(e);
			download.dispose();
			downloadError = true;
		} catch (Exception e) {
			debug("Exception:");
			showException(e);
			download.dispose();
			downloadError = true;
		} finally {
			download.setValue(100);
			download.dispose();
		}
		return downloadError;
	}

	public void downloadFileFromGitHub(String name, File destination) throws IOException {
		URL url = new URL(gitHubUrl + name);
		HttpURLConnection http = (HttpURLConnection)url.openConnection();
		Map< String, List< String >> header = http.getHeaderFields();
		while (isRedirected(header)) {
			String link = header.get("Location").get(0);
			url = new URL(link);
			http = (HttpURLConnection)url.openConnection();
			header = http.getHeaderFields();
		}

		try (InputStream input = http.getInputStream();
				 var output = new FileOutputStream(destination)) {
			int n;
			byte[] buffer = new byte[4096];
			while ((n = input.read(buffer)) != -1) {
				output.write(buffer, 0, n);
			}
		}
	}

	private static byte[] createChecksum(String filename) throws Exception {
		try (var fis = new FileInputStream(filename)) {
			byte[] buffer = new byte[1024];
			MessageDigest complete = MessageDigest.getInstance("MD5");
			int numRead;
			do {
				numRead = fis.read(buffer);
				if (numRead > 0) {
					complete.update(buffer, 0, numRead);
				}
			} while (numRead != -1);
			fis.close();
			return complete.digest();
		}
	}

	private static String getMD5Checksum(String filename) throws Exception {
		byte[] b = createChecksum(filename);
		StringBuilder result = new StringBuilder();
		for (byte v : b) {
			result.append(Integer.toString((v & 0xff) + 0x100, 16).substring(1));
		}
		return result.toString();
	}

	public void debug(String sText) {
		try {
			if (debugFile == null) {
				String sDir = System.getProperty("user.home");
				if (!sDir.isEmpty()) {
					sDir += File.separator + debugFileName;
				}
				File f_obj = new File(sDir);
				if (!f_obj.exists()) {
					Files.createDirectory(f_obj.toPath());
				}
				Calendar oCal = Calendar.getInstance();
				String sDate = oCal.get(Calendar.DATE) + "-" + (oCal.get(Calendar.MONTH) + 1) + "-" + oCal.get(Calendar.YEAR);
				debugFile = new FileWriter(new File(sDir, "DebugFtp-" + sDate + ".log"), true);
			}
			debugFile.write("[" + Calendar.getInstance().getTime().toString() + "]: " + sText + "\n");
			debugFile.flush();
		}
		catch (Exception ignored) {}
	}

	private void showException(Exception e) {
		StackTraceElement[] st = e.getStackTrace();
		String error = "";
		for (StackTraceElement element : st) {
			error = error.concat("\n" + element);
		}
		String sDir = System.getProperty("user.home");
		if (!sDir.isEmpty()) {
			sDir += File.separator + debugFileName;
		}
		try (var fileWriter = new FileWriter(sDir + File.separator + "Errors.log")) {
			fileWriter.write(e.toString());
			fileWriter.write(error);
			fileWriter.flush();
		}
		catch (IOException ignored) {}
		debug("Server: ERROR:");
		debug("Server: " + e);
		debug("Server: " + error);
		e.printStackTrace();
	}

	private static boolean isRedirected(Map<String, List<String>> header) {
		if (header == null) {
			return false;
		}
		try {
			for (String hv : header.get(null)) {
				if (hv == null) {
					return false;
				}
				if (hv.contains(" 301 ") || hv.contains(" 302 "))
					return true;
			}
		} catch(Exception ignored) {}
		return false;
	}

	static class FileType {

		private final String file;
		private final String md5;
		private final boolean forLibDirectory;

		private FileType(String file, String md5, boolean forLibDirectory) {
			this.file = file;
			this.md5 = md5;
			this.forLibDirectory = forLibDirectory;
		}

		public String getFile() {
			return file;
		}

		private String getMd5() {
			return md5;
		}

		private boolean isForLibDirectory() {
			return forLibDirectory;
		}
	}

	public String getGitHubUrl() {
		return gitHubUrl;
	}

	public String getVersionFileName() {
		return versionFileName;
	}

	public String getMainJarName() {
		return mainJarName;
	}

	public String getDebugFileName() {
		return debugFileName;
	}
}
