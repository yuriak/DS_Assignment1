package org.aw.server;

import org.apache.log4j.Logger;
import org.aw.comman.Message;
import org.aw.comman.MessageType;
import org.aw.comman.Resource;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
			JSONObject jsonObject=new JSONObject(command);
			String cmd=jsonObject.getString("command");
			System.out.println(cmd);
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
		}catch (JSONException e){
			return sendErrorMessage("Invalid Json String");
		}finally {
			return messages;
		}

	}

	private synchronized List<Message> publish(JSONObject jsonObject) {
		if (!jsonObject.has("resource"))
			return sendErrorMessage("missing resource");
		JSONObject resourceObject=jsonObject.getJSONObject("resource");
		if (!Resource.checkValidity(resourceObject))
			return sendErrorMessage("invalid resource");
		Resource resource=Resource.parseJson(resourceObject);
		if (resource==null|| !resource.getUri().isAbsolute() || resource.getUri().getScheme().equals("file")||resource.getOwner().equals("*"))
			return sendErrorMessage("cannot publish resource");
		List<Resource> resources = kernel.getResources();
		if(resources.stream().anyMatch(re->re.getChannel().equals(resource.getChannel())&&re.getUri().equals(resource.getUri())&&!re.getOwner().equals(resource.getOwner())))
			return sendErrorMessage("cannot publish resource");
		List<Resource> sameResource=resources.stream().filter(re -> re.getChannel().equals(resource.getChannel()) && re.getUri().equals(resource.getUri()) && re.getOwner().equals(resource.getOwner())).collect(Collectors.toList());
		if (sameResource.size()>0){
			resources.set(resources.indexOf(sameResource.get(0)),resource);
		}else {
			resources.add(resource);
		}
		resources.forEach(re-> System.out.println(Resource.toJson(re).toString()));
		return sendSuccessMessage();
	}



	private synchronized List<Message> remove(JSONObject jsonObject){
		if (!jsonObject.has("resource"))
			return sendErrorMessage("missing resource");
		JSONObject resourceObject = jsonObject.getJSONObject("resource");
		if (!Resource.checkValidity(resourceObject))
			return sendErrorMessage("invalid resource");
		Resource resource = Resource.parseJson(resourceObject);
		if (resource == null || !resource.getUri().isAbsolute() || resource.getOwner().equals("*"))
			return sendErrorMessage("cannot publish resource");
		List<Resource> resources = kernel.getResources();
		List<Resource> targetList = resources.stream().filter(re -> re.getOwner().equals(resource.getOwner()) && re.getChannel().equals(resource.getChannel()) && re.getUri().equals(resource.getUri())).collect(Collectors.toList());
		if (targetList.size()==0)
			return sendErrorMessage("cannot remove resource");
		resources.remove(targetList.get(0));
		resources.forEach(re -> System.out.println(Resource.toJson(re).toString()));
		return sendSuccessMessage();
	}

	private synchronized List<Message> share(JSONObject jsonObject){
		if (!jsonObject.has("resource")||!jsonObject.has("secret"))
			return sendErrorMessage("missing resource and/or secret");
		if(!jsonObject.getString("secret").equals(ServerConfig.SECRET))
			return sendErrorMessage("incorrect secret");
		JSONObject resourceObject = jsonObject.getJSONObject("resource");
		if (!Resource.checkValidity(resourceObject))
			return sendErrorMessage("invalid resource");
		Resource resource = Resource.parseJson(resourceObject);
		resource.setServer(kernel.getServer());
		if (resource == null || !resource.getUri().isAbsolute() || !resource.getUri().getScheme().equals("file")||resource.getUri().getAuthority()!=null ||resource.getOwner().equals("*"))
			return sendErrorMessage("cannot share resource");
		File file=new File(resource.getUri().getPath());
		if (!file.exists()||!file.isFile()) return sendErrorMessage("cannot share resource");
		List<Resource> resources = kernel.getResources();
		if (resources.stream().anyMatch(re -> re.getChannel().equals(resource.getChannel()) && re.getUri().equals(resource.getUri()) && !re.getOwner().equals(resource.getOwner())))
			return sendErrorMessage("cannot share resource");
		List<Resource> sameResource = resources.stream().filter(re -> re.getChannel().equals(resource.getChannel()) && re.getUri().equals(resource.getUri()) && re.getOwner().equals(resource.getOwner())).collect(Collectors.toList());
		if (sameResource.size() > 0) {
			resources.set(resources.indexOf(sameResource.get(0)), resource);
		} else {
			resources.add(resource);
		}
//		resources.forEach(re -> System.out.println(Resource.toJson(re).toString()));
		return sendSuccessMessage();
	}


	private synchronized List<Message> query(JSONObject jsonObject){
		List<Message> messages = new ArrayList<>();
		if (!jsonObject.has("resourceTemplate")||!jsonObject.has("relay"))
			return sendErrorMessage("missing resourceTemplate");
		boolean relay=jsonObject.getBoolean("relay");
		JSONObject resourceObject = jsonObject.getJSONObject("resourceTemplate");
		if (!Resource.checkValidity(resourceObject)) return sendErrorMessage("invalid resourceTemplate");
		Resource resource = Resource.parseJson(resourceObject);
		if (resource==null|| resource.getOwner().equals("*"))
			return sendErrorMessage("invalid resourceTemplate");
		List<Resource> resources = kernel.getResources();
		List<Resource> candidates=new ArrayList<>();
		for (Resource re:resources){
			if ((resource.getChannel().equals(re.getChannel())) && (resource.getOwner().equals("") ? true : resource.getOwner().equals(re.getOwner())) &&
							(resource.getTags().size() == 0 ? true : resource.getTags().stream().anyMatch(tag -> re.getTags().contains(tag))) &&
							(resource.getUri().toString().equals("") ? true : resource.getUri().equals(re.getUri())) &&
							((resource.getName().equals("") ? true : re.getName().contains(resource.getName())) || (resource.getDescription().equals("") ? true : re.getDescription().contains(resource.getDescription())))) {
				try {
					Resource candidateResource = re.clone();
					if (!candidateResource.getOwner().equals(""))
						candidateResource.setOwner("*");
					candidateResource.setServer(kernel.getServer());
					candidates.add(candidateResource);
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			}
		}
		if (relay){
			List<Server> servers=kernel.getServers();
			for (Server server:servers){
				if (server.equals(kernel.getServer())){
					continue;
				}
				jsonObject.put("relay", false);
				List<Message> results = kernel.getConnectionManager().establishConnection(server, new Message(MessageType.STRING, jsonObject.toString(), null, null));
				if (results == null || results.size() == 0) {
					continue;
				}
				results.forEach(result -> {
					JSONObject resultObject = new JSONObject(result.getMessage());
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
		if (!jsonObject.has("resourceTemplate")) return sendErrorMessage("missing resourceTemplate");
		JSONObject resourceObject=jsonObject.getJSONObject("resourceTemplate");
		if (!Resource.checkValidity(resourceObject)) return sendErrorMessage("invalid resourceTemplate");
		Resource resource=Resource.parseJson(resourceObject);
		if (resource == null || !resource.getUri().isAbsolute() || !resource.getUri().getScheme().equals("file") || resource.getUri().getAuthority() != null || resource.getOwner().equals("*"))
			return sendErrorMessage("invalid resourceTemplate");
		List<Resource> resources = kernel.getResources();
		List<Resource> targetList = resources.stream().filter(re -> re.getChannel().equals(resource.getChannel()) && re.getOwner().equals(resource.getOwner()) && re.getUri().equals(resource.getUri())).collect(Collectors.toList());
		if (targetList.size()==0)
			return sendErrorMessage("invalid resourceTemplate");
		File file=new File(resource.getUri().getPath());
		if (!file.exists()||!file.isFile())
			return sendErrorMessage("cannot fetch resource");
		resource.setOwner("*");
		resource.setSize(file.length());
		resource.setServer(kernel.getServer());
		resourceObject=Resource.toJson(resource);
		messages.addAll(sendSuccessMessage());
		messages.add(new Message(MessageType.STRING,Resource.toJson(resource).toString(),null,null));
		messages.add(new Message(MessageType.FILE,null,null,file));
		messages.add(new Message(MessageType.STRING,"{\"resultSize\":1}",null,null));
		return messages;
	}



	private synchronized List<Message> exchange(JSONObject jsonObject){
		if (!jsonObject.has("serverList"))return sendErrorMessage("missing or invalid server list");
		JSONArray serverArray=jsonObject.getJSONArray("serverList");
		for (int i = 0; i < serverArray.length(); i++) {
			JSONObject serverObject = serverArray.getJSONObject(i);
			if (!serverObject.has("hostname")||!serverObject.has("port")) continue;
			String hostname=serverObject.getString("hostname");
			int port=serverObject.getInt("port");
			Server server=new Server(hostname,port);
			if (!kernel.getServers().contains(server)&&!server.equals(kernel.getServer())){
				kernel.getServers().add(server);
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
//		System.out.println(false&&false);
//		File file = new File(uri.getPath());
//		System.out.println(file.exists());
		System.out.println("http://www.baidu.com".replaceAll("\\/","\\\\/"));
		System.out.println("http:\\/\\/www.baidu.com".replaceAll("\\\\/","\\/"));
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
