package org.aw.client;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.aw.comman.Message;
import org.aw.comman.Resource;
import org.aw.comman.ServerBean;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by YURI-AK on 2017/4/10.
 */
public class ClientKernel {
	

	private static Logger logger=Logger.getLogger(ClientKernel.class);
	private ServerBean targetServer;
	public void processCommand(CommandLine cmd){
		if (cmd.hasOption("debug")){
			logger.info("set debug on");
			logger.setLevel(Level.DEBUG);
		}
		if (!cmd.hasOption("host")||!cmd.hasOption("port")){
			logger.error("require host and port");
			return;
		}
		targetServer=new ServerBean(cmd.getOptionValue("host"),Integer.valueOf(cmd.getOptionValue("port")));
		if (cmd.hasOption("publish")) {
			publish(cmd);
		} else if (cmd.hasOption("remove")) {
			remove(cmd);
		} else if (cmd.hasOption("share")) {
			share(cmd);
		} else if (cmd.hasOption("query")) {
			query(cmd);
		} else if (cmd.hasOption("fetch")) {
			fetch(cmd);
		} else if (cmd.hasOption("exchange")) {
			exchange(cmd);
		}
	}

	private void publish(CommandLine cmd){
		Resource resource = parseResourceCmd(cmd, true);
		if (resource==null){
			return;
		}
		JSONObject jsonObject=new JSONObject();
		jsonObject.put("command","PUBLISH");
		jsonObject.put("resource",Resource.toJson(resource));
		logger.debug(jsonObject.toString());
		List<Message> messages = ClientConnectionManager.establishConnection(targetServer, new Message(jsonObject.toString()));
		if (messages!=null){
			messages.forEach(message -> logger.info(message.getMessage()));
		}
	}

	private void remove(CommandLine cmd) {
		Resource resource = parseResourceCmd(cmd, true);
		if (resource == null) {
			return;
		}
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("command", "REMOVE");
		jsonObject.put("resource", Resource.toJson(resource));
		logger.debug(jsonObject.toString());
		List<Message> messages = ClientConnectionManager.establishConnection(targetServer, new Message(jsonObject.toString()));
		if (messages != null) {
			messages.forEach(message -> logger.info(message.getMessage()));
		}
	}

	private void share(CommandLine cmd) {
		if (!cmd.hasOption("secret")){
			logger.error("require secret");
			return;
		}
		Resource resource = parseResourceCmd(cmd, true);
		if (resource == null) {
			return;
		}
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("command", "SHARE");
		jsonObject.put("resource", Resource.toJson(resource));
		jsonObject.put("secret",cmd.getOptionValue("secret"));
		logger.debug(jsonObject.toString());
		List<Message> messages = ClientConnectionManager.establishConnection(targetServer, new Message(jsonObject.toString()));
		if (messages != null) {
			messages.forEach(message -> logger.info(message.getMessage()));
		}
	}

	private void fetch(CommandLine cmd) {
		Resource resource = parseResourceCmd(cmd, true);
		if (resource == null) {
			return;
		}
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("command", "FETCH");
		jsonObject.put("resourceTemplate", Resource.toJson(resource));
		logger.debug(jsonObject.toString());
		Socket socket = null;
		try {
			socket = new Socket(targetServer.getHostname(), targetServer.getPort());
			DataInputStream inputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
			outputStream.writeUTF(jsonObject.toString());
			outputStream.flush();
			if (inputStream.available()>-1){
				String response=inputStream.readUTF();
				logger.info(response);
				if (response.contains("error"))
					return;
				if (inputStream.available()>-1){
					String resourceInfoStr = inputStream.readUTF();
					logger.info(resourceInfoStr);
					JSONParser parser=new JSONParser();
					JSONObject resourceInfo = (JSONObject) (new JSONParser()).parse(resourceInfoStr);
					Long size = (Long) resourceInfo.get("resourceSize");
					String fileName = resource.getUri().getPath().split("/")[resource.getUri().getPath().split("/").length - 1];
					if (inputStream.available()>-1){
						File file = new File(fileName);
						file.createNewFile();
						FileUtils.writeByteArrayToFile(file, IOUtils.toByteArray(inputStream, size));
						if (inputStream.available()>-1){
							logger.info(inputStream.readUTF());
						}
					}
				}
			}
			inputStream.close();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void query(CommandLine cmd) {
		Resource resource = parseResourceCmd(cmd, false);
		if (resource == null) {
			return;
		}
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("command", "QUERY");
		jsonObject.put("relay", true);
		jsonObject.put("resourceTemplate", Resource.toJson(resource));
		logger.debug(jsonObject.toString());
		List<Message> messages = ClientConnectionManager.establishConnection(targetServer, new Message(jsonObject.toString()));
		if (messages != null) {
			messages.forEach(message -> System.out.println(message.getMessage()));
		}
	}

	private void exchange(CommandLine cmd) {
		if (!cmd.hasOption("servers")){
			logger.error("require servers");
			return;
		}
		String[] serverStrings=cmd.getOptionValue("servers").split(",");
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("command", "EXCHANGE");
		JSONArray serverArray=new JSONArray();
		for (int i=0;i<serverStrings.length;i++){
			JSONObject serverObject=new JSONObject();
			String hostname=serverStrings[i].split(":")[0].trim();
			int port=Integer.valueOf(serverStrings[i].split(":")[1].trim());
			serverObject.put("hostname",hostname);
			serverObject.put("port",port);
			serverArray.add(serverObject);
		}
		jsonObject.put("serverList",serverArray);
		logger.debug(jsonObject.toString());
		List<Message> messages = ClientConnectionManager.establishConnection(targetServer, new Message(jsonObject.toString()));
		if (messages != null) {
			messages.forEach(message -> System.out.println(message.getMessage()));
		}
	}

	private Resource parseResourceCmd(CommandLine cmd,boolean requireURI){
		Resource resource = new Resource();
		if (requireURI&&(!cmd.hasOption("uri")||cmd.getOptionValue("uri").equals(""))) {
			logger.error("require uri");
			return null;
		}else{
			try {
				if (cmd.hasOption("uri"))
					resource.setUri(new URI(cmd.getOptionValue("uri")));
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		if (cmd.hasOption("owner")) {
			if (cmd.getOptionValue("owner").trim().equals("*")) {
				logger.error("owner cannot be \"*\"");
				return null;
			}
			resource.setOwner(cmd.getOptionValue("owner").trim());
		} else {
			resource.setOwner("");
		}
		if (cmd.hasOption("channel")) {
			resource.setChannel(cmd.getOptionValue("channel").trim());
		} else {
			resource.setChannel("");
		}
		if (cmd.hasOption("name")) {
			resource.setName(cmd.getOptionValue("name").trim());
		} else {
			resource.setName("");
		}
		if (cmd.hasOption("description")) {
			resource.setDescription(cmd.getOptionValue("description").trim());
		} else {
			resource.setDescription("");
		}
		List<String> tagList = new ArrayList<>();
		if (cmd.hasOption("tags")) {
			String[] tags = cmd.getOptionValue("tags").split(",");
			for (int i = 0; i < tags.length; i++) {
				tagList.add(tags[i].trim());
			}
		}
		resource.setTags(tagList);
		return resource;
	}
}
