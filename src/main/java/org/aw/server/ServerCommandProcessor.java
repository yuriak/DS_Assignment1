package org.aw.server;

import org.apache.log4j.Logger;
import org.aw.comman.Message;
import org.aw.comman.MessageType;
import org.aw.comman.Resource;
import org.aw.comman.ServerBean;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class ServerCommandProcessor {

	private Logger logger=Logger.getLogger(ServerCommandProcessor.class);
	private ServerKernel kernel;
	private static ServerCommandProcessor processor;
	private ServerCommandProcessor(){
		kernel=ServerKernel.getInstance();
	}
	public static ServerCommandProcessor getInstance(){
		if (processor==null){
			synchronized (ServerCommandProcessor.class){
				if (processor==null){
					processor=new ServerCommandProcessor();
				}
			}
		}
		return processor;
	}

	public List<Message> processCommand(String command){
		List<Message> messages = new ArrayList<Message>();
		try {
			JSONObject jsonObject= (JSONObject) (new JSONParser()).parse(command);
			String cmd= (String) jsonObject.get("command");
			logger.debug(cmd);
			switch (cmd){
				case "PUBLISH": messages.addAll(publish(jsonObject));
					break;
				case "REMOVE": messages.addAll(remove(jsonObject));
					break;
				case "SHARE":
					messages.addAll(share(jsonObject));
					break;
				case "QUERY":
					messages.addAll(query(jsonObject));
					break;
				case "FETCH":
					messages.addAll(fetch(jsonObject));
					break;
				case "EXCHANGE":
					messages.addAll(exchange(jsonObject));
					break;
				default:
					messages.addAll(sendErrorMessage("Invalid Command"));
			}
		}catch (ParseException e){
			return sendErrorMessage("Invalid Json String");
		}finally {
			return messages;
		}

	}

	private synchronized List<Message> publish(JSONObject jsonObject) {
		if (!jsonObject.containsKey("resource"))
			return sendErrorMessage("missing resource");
		JSONObject resourceObject= (JSONObject) jsonObject.get("resource");
		if (!Resource.checkValidity(resourceObject))
			return sendErrorMessage("invalid resource");
		Resource resource=Resource.parseJson(resourceObject);
		if (resource==null|| !resource.getUri().isAbsolute() || resource.getUri().getScheme().equals("file")||resource.getOwner().equals("*"))
			return sendErrorMessage("cannot publish resource");
		List<Resource> resources = kernel.getResources();
		synchronized (resources){
			if (resources.stream().anyMatch(re -> re.getChannel().equals(resource.getChannel()) && re.getUri().equals(resource.getUri()) && !re.getOwner().equals(resource.getOwner())))
				return sendErrorMessage("cannot publish resource");
			List<Resource> sameResource = resources.stream().filter(re -> re.getChannel().equals(resource.getChannel()) && re.getUri().equals(resource.getUri()) && re.getOwner().equals(resource.getOwner())).collect(Collectors.toList());
			if (sameResource.size() > 0) {
				resources.set(resources.indexOf(sameResource.get(0)), resource);
			} else {
				resources.add(resource);
			}
		}
//		resources.forEach(re-> System.out.println(Resource.toJson(re).toString()));
		return sendSuccessMessage();
	}



	private synchronized List<Message> remove(JSONObject jsonObject){
		if (!jsonObject.containsKey("resource"))
			return sendErrorMessage("missing resource");
		JSONObject resourceObject = (JSONObject) jsonObject.get("resource");
		if (!Resource.checkValidity(resourceObject))
			return sendErrorMessage("invalid resource");
		Resource resource = Resource.parseJson(resourceObject);
		if (resource == null || !resource.getUri().isAbsolute() || resource.getOwner().equals("*"))
			return sendErrorMessage("cannot remove resource");
		List<Resource> resources = kernel.getResources();
		synchronized (resources){
			List<Resource> targetList = resources.stream().filter(re -> re.getOwner().equals(resource.getOwner()) && re.getChannel().equals(resource.getChannel()) && re.getUri().equals(resource.getUri())).collect(Collectors.toList());
			if (targetList.size() == 0)
				return sendErrorMessage("cannot remove resource");
			resources.remove(targetList.get(0));
		}
//		resources.forEach(re -> System.out.println(Resource.toJson(re).toString()));
		return sendSuccessMessage();
	}

	private synchronized List<Message> share(JSONObject jsonObject){
		if (!jsonObject.containsKey("resource")||!jsonObject.containsKey("secret"))
			return sendErrorMessage("missing resource and/or secret");
		if(!((String)jsonObject.get("secret")).equals(ServerConfig.SECRET))
			return sendErrorMessage("incorrect secret");
		JSONObject resourceObject = (JSONObject) jsonObject.get("resource");
		if (!Resource.checkValidity(resourceObject))
			return sendErrorMessage("invalid resource");
		Resource resource = Resource.parseJson(resourceObject);
		resource.setServerBean(kernel.getMyServer());
		if (resource == null || !resource.getUri().isAbsolute() || !resource.getUri().getScheme().equals("file")||resource.getUri().getAuthority()!=null ||resource.getOwner().equals("*"))
			return sendErrorMessage("cannot share resource");
		File file=new File(resource.getUri().getPath());
		if (!file.exists()||!file.isFile()) return sendErrorMessage("cannot share resource");
		List<Resource> resources = kernel.getResources();
		synchronized (resources){
			if (resources.stream().anyMatch(re -> re.getChannel().equals(resource.getChannel()) && re.getUri().equals(resource.getUri()) && !re.getOwner().equals(resource.getOwner())))
				return sendErrorMessage("cannot share resource");
			List<Resource> sameResource = resources.stream().filter(re -> re.getChannel().equals(resource.getChannel()) && re.getUri().equals(resource.getUri()) && re.getOwner().equals(resource.getOwner())).collect(Collectors.toList());
			if (sameResource.size() > 0) {
				resources.set(resources.indexOf(sameResource.get(0)), resource);
			} else {
				resources.add(resource);
			}
		}
//		resources.forEach(re -> System.out.println(Resource.toJson(re).toString()));
		return sendSuccessMessage();
	}


	private synchronized List<Message> query(JSONObject jsonObject){
		List<Message> messages = new ArrayList<>();
		if (!jsonObject.containsKey("resourceTemplate")||!jsonObject.containsKey("relay"))
			return sendErrorMessage("missing resourceTemplate");
		boolean relay= (boolean) jsonObject.get("relay");
		JSONObject resourceObject = (JSONObject) jsonObject.get("resourceTemplate");
		if (!Resource.checkValidity(resourceObject)) return sendErrorMessage("invalid resourceTemplate");
		Resource resource = Resource.parseJson(resourceObject);
		if (resource==null|| resource.getOwner().equals("*"))
			return sendErrorMessage("invalid resourceTemplate");
		List<Resource> resources = kernel.getResources();
		List<Resource> candidates=new ArrayList<>();
		synchronized (resources){
			for (Resource re : resources) {
				if ((resource.getChannel().equals(re.getChannel())) && (resource.getOwner().equals("") || resource.getOwner().equals(re.getOwner())) &&
						(resource.getTags().size() == 0 || resource.getTags().stream().anyMatch(tag -> re.getTags().contains(tag))) &&
						(resource.getUri().toString().equals("") || resource.getUri().equals(re.getUri())) &&
						((resource.getName().equals("") || re.getName().contains(resource.getName())) || (resource.getDescription().equals("") || re.getDescription().contains(resource.getDescription())))) {
					try {
						Resource candidateResource = re.clone();
						if (!candidateResource.getOwner().equals(""))
							candidateResource.setOwner("*");
						candidateResource.setServerBean(kernel.getMyServer());
						candidates.add(candidateResource);
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		if (relay){
			List<ServerBean> serverBeen =kernel.getServerList();
			for (ServerBean serverBean : serverBeen){
				if (serverBean.equals(kernel.getMyServer())){
					continue;
				}
				jsonObject.put("relay", false);
				JSONObject templateObject=(JSONObject)jsonObject.get("resourceTemplate");
				templateObject.put("owner","");
				templateObject.put("channel","");
				List<Message> results = kernel.getServerConnectionManager().establishConnection(serverBean, new Message(MessageType.STRING, jsonObject.toString(), null, null));
				if (results == null || results.size() == 0) {
					continue;
				}
				results.forEach(result -> {
					JSONObject resultObject = null;
					try {
						resultObject = (JSONObject) (new JSONParser()).parse(result.getMessage());
					} catch (ParseException e) {
						e.printStackTrace();
					}
					if (Resource.checkValidity(resultObject)) {
						Resource externalResource = Resource.parseJson(resultObject);
						candidates.add(externalResource);
					}
				});
			}
		}
		messages.addAll(sendSuccessMessage());
		candidates.forEach(candidate->{
			messages.add(new Message(MessageType.STRING, Resource.toJson(candidate).toString(),null,null));
		});
		messages.add(new Message(MessageType.STRING, "{\"resultSize\":" + candidates.size() + "}",null,null));
		return messages;
	}

	private List<Message>  fetch(JSONObject jsonObject){
		List<Message> messages = new ArrayList<>();
		if (!jsonObject.containsKey("resourceTemplate")) return sendErrorMessage("missing resourceTemplate");
		JSONObject resourceObject= (JSONObject) jsonObject.get("resourceTemplate");
		if (!Resource.checkValidity(resourceObject)) return sendErrorMessage("invalid resourceTemplate");
		Resource resource=Resource.parseJson(resourceObject);
		if (resource == null || !resource.getUri().isAbsolute() || !resource.getUri().getScheme().equals("file") || resource.getUri().getAuthority() != null || resource.getOwner().equals("*"))
			return sendErrorMessage("invalid resourceTemplate");
		List<Resource> resources = kernel.getResources();
		List<Resource> targetList = resources.stream().filter(re -> re.getChannel().equals(resource.getChannel()) && re.getOwner().equals(resource.getOwner()) && re.getUri().equals(resource.getUri())).collect(Collectors.toList());
		if (targetList.size()==0)
			return sendErrorMessage("cannot fetch resourse");
		File file=new File(resource.getUri().getPath());
		if (!file.exists()||!file.isFile())
			return sendErrorMessage("cannot fetch resource");
		if (!resource.getOwner().equals("")) resource.setOwner("*");
		resource.setSize(file.length());
		resource.setServerBean(kernel.getMyServer());
		resourceObject=Resource.toJson(resource);
		messages.addAll(sendSuccessMessage());
		messages.add(new Message(MessageType.STRING,Resource.toJson(resource).toString(),null,null));
		messages.add(new Message(MessageType.FILE,null,null,file));
		messages.add(new Message(MessageType.STRING,"{\"resultSize\":1}",null,null));
		return messages;
	}



	private synchronized List<Message> exchange(JSONObject jsonObject){
		if (!jsonObject.containsKey("serverList"))return sendErrorMessage("missing or invalid server list");
		JSONArray serverArray= (JSONArray) jsonObject.get("serverList");
//		System.out.println(jsonObject);
		for (int i = 0; i < serverArray.size(); i++) {
			JSONObject serverObject = (JSONObject) serverArray.get(i);
			if (!serverObject.containsKey("hostname")||!serverObject.containsKey("port")) continue;
			String hostname= (String) serverObject.get("hostname");
			int port=0;
			try {
				port = Integer.parseInt(serverObject.get("port").toString());
				if (port<0||port>65535){
					return sendErrorMessage("missing or invalid server list");
				}
			}catch (Exception e){
				return sendErrorMessage("missing or invalid server list");
			}
			ServerBean serverBean =new ServerBean(hostname,port);
			if (!kernel.getServerList().contains(serverBean)&&!serverBean.equals(kernel.getMyServer())){
				kernel.getServerList().add(serverBean);
			}
		}
//		System.out.println("receive: "+serverArray.toString());
		return sendSuccessMessage();
	}


	private static List<Message> sendErrorMessage(String message){
		List<Message> messages =new ArrayList<>();
		Message response=new Message();
		response.setType(MessageType.STRING);
		JSONObject jsonObject=new JSONObject();
		jsonObject.put("response","error");
		jsonObject.put("errorMessage",message);
		response.setMessage(jsonObject.toString());
		messages.add(response);
		return messages;
	}


	public static void main(String[] args) throws URISyntaxException, CloneNotSupportedException {
//		URI uri = new URI("");
//		File file = new File(uri.getPath());
//		System.out.println(file.exists());
//		System.out.println("http://www.baidu.com".replaceAll("\\/","\\\\/"));
//		System.out.println("http:\\/\\/www.baidu.com".replaceAll("\\\\/","\\/"));
	}

	private static List<Message> sendSuccessMessage(){
		List<Message> messages = new ArrayList<>();
		Message message = new Message();
		message.setType(MessageType.STRING);
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("response", "success");
		message.setMessage(jsonObject.toString());
		messages.add(message);
		return messages;
	}

}
