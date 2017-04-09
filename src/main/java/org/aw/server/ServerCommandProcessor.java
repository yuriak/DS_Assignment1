package org.aw.server;

import org.apache.log4j.Logger;
import org.aw.comman.Resource;
import org.aw.comman.ResponseType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
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

	public List<Response> processCommand(String command){
		List<Response> responses = new ArrayList<Response>();
		try {
			JSONObject jsonObject=new JSONObject(command);
			String cmd=jsonObject.getString("command");
			System.out.println(cmd);
			switch (cmd){
				case "PUBLISH": responses.addAll(publish(jsonObject));
					break;
				case "REMOVE": responses.addAll(remove(jsonObject));
					break;
				case "SHARE":responses.addAll(share(jsonObject));
					break;
				case "FETCH":responses.addAll(fetch(jsonObject));
					break;
				case "EXCHANGE":responses.addAll(exchange(jsonObject));
					break;
				default:
					responses.addAll(sendErrorMessage("Invalid Command"));
			}
		}catch (JSONException e){
			return sendErrorMessage("Invalid Json String");
		}finally {
			return responses;
		}

	}

	private synchronized List<Response> publish(JSONObject jsonObject) {
		List<Response> responses=new ArrayList<>();
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



	private synchronized List<Response> remove(JSONObject jsonObject){
		List<Response> responses = new ArrayList<>();
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

	private synchronized List<Response> share(JSONObject jsonObject){
		List<Response> responses = new ArrayList<>();
		if (!jsonObject.has("resource"))
			return sendErrorMessage("missing resource");
		if (!jsonObject.has("secret"))
			return sendErrorMessage("cannot publish resource1");
		if(!jsonObject.getString("secret").equals(ServerConfig.SECRET))
			return sendErrorMessage("cannot publish resource2");
		JSONObject resourceObject = jsonObject.getJSONObject("resource");
		if (!Resource.checkValidity(resourceObject))
			return sendErrorMessage("invalid resource");
		Resource resource = Resource.parseJson(resourceObject);
		if (resource == null || !resource.getUri().isAbsolute() || !resource.getUri().getScheme().equals("file")||resource.getUri().getAuthority()!=null ||resource.getOwner().equals("*"))
			return sendErrorMessage("cannot share resource3");
		File file=new File(resource.getUri().getPath());
		if (!file.exists()||!file.isFile()) return sendErrorMessage("cannot publish resource4");
		List<Resource> resources = kernel.getResources();
		if (resources.stream().anyMatch(re -> re.getChannel().equals(resource.getChannel()) && re.getUri().equals(resource.getUri()) && !re.getOwner().equals(resource.getOwner())))
			return sendErrorMessage("cannot share resource5");
		List<Resource> sameResource = resources.stream().filter(re -> re.getChannel().equals(resource.getChannel()) && re.getUri().equals(resource.getUri()) && re.getOwner().equals(resource.getOwner())).collect(Collectors.toList());
		if (sameResource.size() > 0) {
			resources.set(resources.indexOf(sameResource.get(0)), resource);
		} else {
			resources.add(resource);
		}
		resources.forEach(re -> System.out.println(Resource.toJson(re).toString()));
		return sendSuccessMessage();
	}


	private synchronized List<Response> query(JSONObject jsonObject){
		List<Response> responses = new ArrayList<>();
		return null;
	}

	private List<Response>  fetch(JSONObject jsonObject){
		List<Response> responses = new ArrayList<>();
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
		responses.addAll(sendSuccessMessage());
		responses.add(new Response(ResponseType.MESSAGE,Resource.toJson(resource).toString(),null,null));
		responses.add(new Response(ResponseType.FILE,null,null,file));
		responses.add(new Response(ResponseType.MESSAGE,"{\"resultSize\":1}",null,null));
		return responses;
	}



	private synchronized List<Response> exchange(JSONObject jsonObject){
		List<Response> responses = new ArrayList<>();
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

	public static void main(String[] args) throws URISyntaxException {
		URI uri = new URI("file:///E:/pathbc.txt");
		File file = new File(uri.getPath());
		System.out.println(file.exists());
	}


	private static List<Response> sendErrorMessage(String message){
		List<Response> responses=new ArrayList<>();
		Response response=new Response();
		response.setType(ResponseType.MESSAGE);
		JSONObject jsonObject=new JSONObject();
		jsonObject.put("response","error");
		jsonObject.put("errorMessage",message);
		response.setMessage(jsonObject.toString());
		responses.add(response);
		return responses;
	}

	private static List<Response> sendSuccessMessage(){
		List<Response> responses = new ArrayList<>();
		Response response = new Response();
		response.setType(ResponseType.MESSAGE);
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("response", "success");
		response.setMessage(jsonObject.toString());
		responses.add(response);
		return responses;
	}

}
