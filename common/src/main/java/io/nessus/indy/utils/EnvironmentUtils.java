package io.nessus.indy.utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

/**
 * Borrowed from ...
 * indy-sdk/../wrappers/java/src/test/java/org/hyperledger/indy/sdk/utils/EnvironmentUtils.java 
 */
public class EnvironmentUtils {
	static String getTestPoolIP() {
		String testPoolIp = System.getenv("TEST_POOL_IP");
		return testPoolIp != null ? testPoolIp : "127.0.0.1";
	}

	public static String getIndyHomePath() {
		return FileUtils.getUserDirectoryPath() + "/.indy_client/";
	}

	public static String getIndyHomePath(String filename) {
		return getIndyHomePath() + filename;
	}

	public static void deleteIndyHomePath() throws IOException {
		File clientHome = new File(getIndyHomePath());
		if (clientHome.isDirectory())
			FileUtils.deleteDirectory(clientHome);
	}
	
	static String getTmpPath() {
		return FileUtils.getTempDirectoryPath() + "/indy/";
	}

	static String getTmpPath(String filename) {
		return getTmpPath() + filename;
	}
}
