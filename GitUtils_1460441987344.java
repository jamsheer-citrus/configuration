package com.netspective.precis.resource.persistence.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netspective.precis.common.ReadConfiguration;
import com.netspective.precis.exception.PrecisProtocolException;
import com.netspective.precis.resource.rest.application.settings.ApplicationSettingController;
import com.netspective.precis.resource.utils.Constants;

/**
 * 
 * @author justin
 *
 */
public class GitUtils {

	static Logger logger = Logger.getLogger(GitUtils.class);
	static StringWriter sw = new StringWriter();

	/**
	 * 
	 * @param fileName
	 * @param yamlString
	 * @param body
	 * @param response
	 * @throws PrecisProtocolException
	 */
	private static boolean pushFile(String fileName, String contents)
			throws PrecisProtocolException {
		ReadConfiguration readConfiguration = new ReadConfiguration();
		final String localPath = readConfiguration.getLocalRepository();
		final String remotePath = readConfiguration.getRemoteRepository();
		try {
			Yaml yaml = new Yaml();
			/*
			 * FileRepository localRepo = new FileRepository(localPath +
			 * "/.git");
			 */
			File theDir = new File(localPath);
			File gitdirectiory = new File(localPath + "/.git");
			if (!gitdirectiory.exists()) {
				// Clone Directory
				if (theDir.exists())
					FileUtils.deleteDirectory(theDir);
				Git.cloneRepository().setURI(remotePath)
						.setDirectory(new File(localPath)).call();
			}
			// Add a file
			FileWriter writer = new FileWriter(localPath + "/" + fileName);
			yaml.dump(yaml.load(contents), writer);
			writer.close();

			pushToGit(fileName, localPath);

		} catch (Exception e) {
			e.printStackTrace(new PrintWriter(sw));
			logger.error(sw.toString());
			throw new PrecisProtocolException(e);

		}
		return true;
	}

	private static void renameFile(String fileName)
			throws PrecisProtocolException {
		ReadConfiguration readConfiguration = new ReadConfiguration();
		final String localPath = readConfiguration.getLocalRepository();
		Repository localRepo;
		Git git;
		String newFileName = "deleted-" + fileName;
		File oldFile = new File(localPath + "/" + fileName);
		File newFile = new File(localPath + "/" + newFileName);
		oldFile.renameTo(newFile);
		try {
			localRepo = new FileRepository(localPath + "/.git");
			git = new Git(localRepo);
			git.add().addFilepattern(newFileName).call();
			git.rm().addFilepattern(fileName).call();
			// Commit Message
			git.commit().setMessage("deleted " + newFileName).call();
			// Push The data
			git.push()
					.setCredentialsProvider(
							new UsernamePasswordCredentialsProvider(
									readConfiguration.getGitUsername(),
									readConfiguration.getGitPassword())).call();
			// Track Master
			git.branchCreate().setName("master")
					.setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
					.setStartPoint("origin/master").setForce(true).call();
			// Pull changes
			git.pull().call();
		} catch (Exception e) {
			e.printStackTrace(new PrintWriter(sw));
			logger.error(sw.toString());
			throw new PrecisProtocolException(e);
		}
	}

	public static void pushToGit(String fileName, String localPath)
			throws Exception {
		ReadConfiguration readConfiguration = new ReadConfiguration();
		Repository localRepo = new FileRepository(localPath + "/.git");
		Git git = new Git(localRepo);
		logger.info("File name to push :" + fileName);
		git.add().addFilepattern(fileName).call();
		// Commit Message
		git.commit().setMessage("Added " + fileName).call();
		// Push The data
		git.push()
				.setCredentialsProvider(
						new UsernamePasswordCredentialsProvider(readConfiguration.getGitUsername(),
								readConfiguration.getGitPassword()))
				.call();
		// Track Master
		git.branchCreate().setName("master")
				.setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
				.setStartPoint("origin/master").setForce(true).call();
		// Pull changes
		git.pull().call();
	}

	/**
	 * 
	 * @param object
	 * @return
	 * @throws JSONException
	 */

	public static Map jsonToMap(JSONObject object) throws JSONException {
		Map<String, Object> map = new HashMap<String, Object>();

		Iterator<String> keysItr = object.keys();
		while (keysItr.hasNext()) {
			String key = keysItr.next();
			Object value = object.get(key);

			if (value instanceof JSONArray) {
				value = jsontoList((JSONArray) value);
			}

			else if (value instanceof JSONObject) {
				value = jsonToMap((JSONObject) value);
			}
			map.put(key, value);
		}
		return map;
	}

	/**
	 * 
	 * @param object
	 * @return
	 * @throws JSONException
	 */
	public static HashMap ConvertjsonStringToMap(String data) {
		HashMap<String, Object> map = null;
		;
		try {
			if (data != null && !data.equals("")) {
				map = new ObjectMapper().readValue(data, HashMap.class);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	}

	/**
	 * 
	 * @param array
	 * @return
	 * @throws JSONException
	 */
	public static List jsontoList(JSONArray array) throws JSONException {
		List<Object> list = new ArrayList<Object>();
		for (int i = 0; i < array.length(); i++) {
			Object value = array.get(i);
			if (value instanceof JSONArray) {
				value = jsontoList((JSONArray) value);
			}

			else if (value instanceof JSONObject) {
				value = jsonToMap((JSONObject) value);
			}
			list.add(value);
		}
		return list;
	}

	/**
	 * 
	 * @param body
	 * @return
	 */
	public static String jsonToYaml(String body) {
		Yaml yaml = new Yaml();
		String out = null;
		try {
			out = yaml.dump(yaml.load(body));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}

	/**
	 * 
	 * @param body
	 * @param response
	 * @throws PrecisProtocolException
	 */
	public static boolean pushConfigurationToGit(String body, String fileName)
			throws PrecisProtocolException {

		try {
			JSONObject jsonBody = new JSONObject(body);
			String data = jsonBody.toString().replaceAll("\\\\/", "/");
			String yamlData = jsonToYaml(data);
			pushFile(fileName, yamlData);
		} catch (JSONException e) {
			throw new PrecisProtocolException(e);
		}
		return true;

	}

	public static void renameConfigurationToGit(final String settingId,
			final String accountId, final String organizationId, String type)
			throws PrecisProtocolException {
		ReadConfiguration readConfiguration = new ReadConfiguration();
		final String localPath = readConfiguration.getLocalRepository();
		String fileName = "";
		if (type.equals(Constants.APP_SETTINGS_CONST)) {
			fileName = DatabaseConstants.FILE_START_APP + "-" + accountId + "-"
					+ organizationId + "-" + settingId + "."
					+ DatabaseConstants.FILE_EXTENSION;
		} else {
			fileName = DatabaseConstants.FILE_START_PREFERENCES + "-"
					+ accountId + "-" + organizationId + "-" + settingId + "."
					+ DatabaseConstants.FILE_EXTENSION;
		}
		File file = new File(localPath + "/" + fileName);
		if (file.exists()) {
			renameFile(fileName);
		}
	}

	public static void updateConfigurationToGit(final String body,
			final String response, final String accountId,
			final String organizationId) throws PrecisProtocolException {

		try {
			JSONObject jsonObj = new JSONObject(body);
			String data = jsonObj.toString().replaceAll("\\\\/", "/");
			String yamlString = jsonToYaml(data);
			JSONObject json = new JSONObject(response);
			String settingsId = json.getString("settingsId");
			String fileName = DatabaseConstants.FILE_START_APP + "-"
					+ accountId + "-" + organizationId + "-" + settingsId + "."
					+ DatabaseConstants.FILE_EXTENSION;
			pushFile(fileName, yamlString);
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

}
