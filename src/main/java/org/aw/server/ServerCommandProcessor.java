package org.aw.server;

import org.apache.log4j.Logger;
import org.aw.comman.Resource;
import org.aw.comman.ResponseType;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class ServerCommandProcessor {

	private Logger logger=Logger.getLogger(ServerCommandProcessor.class);
	private ServerKernel kernel;
	private static ServerCommandProcessor processor;
	private ServerCommandProcessor(){

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
		if (!jsonObject.has("resource")){
			return sendErrorMessage("missing resource");
		}
		JSONObject resourceObject=jsonObject.getJSONObject("resource");
		if (!Resource.checkValidity(resourceObject)){
			return sendErrorMessage("invalid resource");
		}
		Resource resource=Resource.parseJson(resourceObject);
		if (resource==null|| !resource.getUri().isAbsolute() || resource.getUri().getScheme().equals("file")){
			return sendErrorMessage("invalid resource");
		}
		Map<String, Map<String, Map<URI, Resource>>> resources = kernel.getResources();
		if (resources.containsKey(resource.getOwner())){

		}
//		resources.put(resource.getOwner(),MapUtil.getAMap(resource.getChannel(),MapUtil.getAMap(resource.getUri(),resource)));
		return null;
	}

	private List<Response> remove(JSONObject jsonObject){
		List<Response> responses = new ArrayList<>();
		return null;
	}

	private List<Response> share(JSONObject jsonObject){
		List<Response> responses = new ArrayList<>();
		return null;
	}

	private List<Response> query(JSONObject jsonObject){
		List<Response> responses = new ArrayList<>();
		return null;
	}

	private List<Response>  fetch(JSONObject jsonObject){
		List<Response> responses = new ArrayList<>();
		return null;
	}

	private List<Response> exchange(JSONObject jsonObject){
		List<Response> responses = new ArrayList<>();
		return null;
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

}
