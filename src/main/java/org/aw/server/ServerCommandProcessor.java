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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
		subscribers = Collections.synchronizedList(new ArrayList<>());
	}
	private List<Subscriber> subscribers;
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

	public void processCommand(String command, boolean secure, DataInputStream inputStream,ProcessorListener messageListener){
		List<Message> messages = new ArrayList<Message>();
		try {
			JSONObject jsonObject= (JSONObject) (new JSONParser()).parse(command);
			String cmd= (String) jsonObject.get("command");
			logger.debug(cmd);
			switch (cmd){
				case "PUBLISH":
					messages.addAll(publish(jsonObject,secure));
					messageListener.onProcessFinished(messages,true);
					break;
				case "REMOVE":
					messages.addAll(remove(jsonObject, secure));
					messageListener.onProcessFinished(messages,true);
					break;
				case "SHARE":
					messages.addAll(share(jsonObject, secure));
					messageListener.onProcessFinished(messages,true);
					break;
				case "QUERY":
					messages.addAll(query(jsonObject, secure));
					messageListener.onProcessFinished(messages,true);
					break;
				case "FETCH":
					messages.addAll(fetch(jsonObject, secure));
					messageListener.onProcessFinished(messages,true);
					break;
				case "EXCHANGE":
					messages.addAll(exchange(jsonObject, secure));
					messageListener.onProcessFinished(messages,true);
					break;
				case "SUBSCRIBE":
					subscribe(jsonObject,inputStream,messageListener,secure);
					break;
				default:
					messages.addAll(sendErrorMessage("Invalid Command"));
					messageListener.onProcessFinished(messages,true);
					break;
			}
		}catch (ParseException e){
			messageListener.onProcessFinished(sendErrorMessage("Invalid Json String"),true);
		}
	}
	
	public void processPersistentCommand(){
	
	}

	private synchronized List<Message> publish(JSONObject jsonObject,boolean secure) {
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
				for (Subscriber subscriber : subscribers) {
					if (subscriber!=null){
						subscriber.onResourceChanged(resource);
					}
				}
			} else {
				resources.add(resource);
				for (Subscriber subscriber:subscribers){
					if (subscriber!=null){
						subscriber.onResourceChanged(resource);
					}
				}
			}
		}
//		resources.forEach(re-> System.out.println(Resource.toJson(re).toString()));
		return sendSuccessMessage();
	}



	private synchronized List<Message> remove(JSONObject jsonObject, boolean secure){
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

	private synchronized List<Message> share(JSONObject jsonObject, boolean secure){
		if (!jsonObject.containsKey("resource")||!jsonObject.containsKey("secret"))
			return sendErrorMessage("missing resource and/or secret");
		if(!((String)jsonObject.get("secret")).equals(ServerConfig.SECRET))
			return sendErrorMessage("incorrect secret");
		JSONObject resourceObject = (JSONObject) jsonObject.get("resource");
		if (!Resource.checkValidity(resourceObject))
			return sendErrorMessage("invalid resource");
		Resource resource = Resource.parseJson(resourceObject);
		resource.setServerBean(secure?kernel.getMySSLServer():kernel.getMyNormalServer());
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
				for (Subscriber subscriber : subscribers) {
					subscriber.onResourceChanged(resource);
				}
			} else {
				resources.add(resource);
				for (Subscriber subscriber : subscribers) {
					subscriber.onResourceChanged(resource);
				}
			}
		}
//		resources.forEach(re -> System.out.println(Resource.toJson(re).toString()));
		return sendSuccessMessage();
	}


	private synchronized List<Message> query(JSONObject jsonObject, boolean secure){
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
						candidateResource.setServerBean(kernel.getMyNormalServer());
						candidates.add(candidateResource);
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		if (relay){
			List<ServerBean> serverBeen =secure?kernel.getSslServerList():kernel.getNormalServerList();
			for (ServerBean serverBean : serverBeen){
				if (serverBean.equals(secure?kernel.getMySSLServer():kernel.getMyNormalServer())){
					continue;
				}
				jsonObject.put("relay", false);
				JSONObject templateObject=(JSONObject)jsonObject.get("resourceTemplate");
				templateObject.put("owner","");
				templateObject.put("channel","");
				List<Message> results = kernel.getServerConnectionManager().establishConnection(serverBean, new Message(MessageType.STRING, jsonObject.toString(), null, null),secure);
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

	private List<Message>  fetch(JSONObject jsonObject, boolean secure){
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
		resource.setServerBean(kernel.getMyNormalServer());
		messages.addAll(sendSuccessMessage());
		messages.add(new Message(MessageType.STRING,Resource.toJson(resource).toString(),null,null));
		messages.add(new Message(MessageType.FILE,null,null,file));
		messages.add(new Message(MessageType.STRING,"{\"resultSize\":1}",null,null));
		return messages;
	}

	private synchronized List<Message> exchange(JSONObject jsonObject, boolean secure){
		if (!jsonObject.containsKey("serverList"))return sendErrorMessage("missing or invalid server list");
		JSONArray serverArray= (JSONArray) jsonObject.get("serverList");
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
			if(secure){
				if (!kernel.getSslServerList().contains(serverBean) && !serverBean.equals(kernel.getMySSLServer())) {
					synchronized (kernel.getSslServerList()) {
						kernel.getSslServerList().add(serverBean);
					}
				}
			}else{
				if (!kernel.getNormalServerList().contains(serverBean) && !serverBean.equals(kernel.getMyNormalServer())) {
					synchronized (kernel.getNormalServerList()) {
						kernel.getNormalServerList().add(serverBean);
					}
				}
			}
		}
//		System.out.println("receive: "+serverArray.toString());
		return sendSuccessMessage();
	}
	
	private void subscribe(JSONObject jsonObject,DataInputStream inputStream,ProcessorListener processorListener,boolean secure){
		if (!jsonObject.containsKey("resourceTemplate") || !jsonObject.containsKey("relay")||!jsonObject.containsKey("id")){
			processorListener.onProcessFinished(sendErrorMessage("missing resourceTemplate"), true);
			return;
		}
		boolean relay = (boolean) jsonObject.get("relay");
		String id= (String) jsonObject.get("id");
		JSONObject resourceObject = (JSONObject) jsonObject.get("resourceTemplate");
		if (!Resource.checkValidity(resourceObject)){
			processorListener.onProcessFinished(sendErrorMessage("invalid resourceTemplate"), true);
			return;
		}
		Resource templateResource = Resource.parseJson(resourceObject);
		if (templateResource == null || templateResource.getOwner().equals("*")){
			processorListener.onProcessFinished(sendErrorMessage("invalid resourceTemplate"), true);
			return;
		}
		List<Message> SuccessMessage=Message.makeAMessage("{\"response\":\"success\",\"id\":\"" + id + "\"}");
		processorListener.onProcessFinished(SuccessMessage,false);
		Subscriber subscriber=new Subscriber(processorListener,id, templateResource,inputStream,relay,secure);
		subscribers.add(subscriber);
		Thread subscribingThread=new Thread(subscriber);
		subscribingThread.start();
		while (true){
			if (subscriber.getState()==Subscriber.STOPPED){
				subscribingThread.interrupt();
			}
		}
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				String string=null;
//				while (true){
//					try {
//						if((string=inputStream.readUTF())!=null){
//							System.out.println(string);
//							if(processorListener.onProcessFinished(resultSize,true)){
//								break;
//							}
//						}
//					} catch (IOException e) {
//						e.printStackTrace();
//						break;
//					}
//				}
//			}
//		}).start();
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
	
	interface ProcessorListener {
		boolean onProcessFinished(List<Message> messages,boolean closeConnection);
	}
	
	interface ResourceListener{
		void onResourceChanged(Resource resource);
	}
	
	private void setResourceListener(ResourceListener resourceListener){
	
	}
	
	class Subscriber implements Runnable,ResourceListener{
		private ProcessorListener processorListener;
		private Resource template;
		private DataInputStream inputStream;
		private boolean relay;
		private boolean secure;
		private String id;
		private int state;
		public static final int RUNNING=1;
		public static final int STOPPED = 0;
		private int resultSize=0;
		Subscriber(ProcessorListener messageListener,String id,Resource template,DataInputStream inputStream,boolean relay,boolean secure){
			this.processorListener =messageListener;
			this.template=template;
			this.inputStream=inputStream;
			this.relay=relay;
			this.secure=secure;
			this.id=id;
		}
		@Override
		public void run() {
			this.state=RUNNING;
			Thread listeningUnsubscribeThread=new Thread(new Runnable() {
				@Override
				public void run() {
					String string=null;
					while (true){
						try {
							if ((string=inputStream.readUTF())!=null){
								JSONObject commandObject= (JSONObject) (new JSONParser()).parse(string);
								if (commandObject.containsKey("command")&&commandObject.containsKey("id")){
									if (((String)commandObject.get("command")).equals("UNSUBSCRIBE")&&((String)commandObject.get("id")).equals(id)){
										JSONObject resultSizeObject=new JSONObject();
										resultSizeObject.put("resultSize",resultSize);
										processorListener.onProcessFinished(Message.makeAMessage(resultSizeObject.toString()),true);
										state = Subscriber.STOPPED;
									}
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
							state=Subscriber.STOPPED;
							break;
						} catch (ParseException e) {
							e.printStackTrace();
							state = Subscriber.STOPPED;
							break;
						}
					}
				}
			});
			if (relay){
				List<ServerBean> serverList=secure?kernel.getSslServerList():kernel.getNormalServerList();
				for (ServerBean serverBean:serverList){
					Thread thread=new Thread(new Runnable() {
						@Override
						public void run() {
							JSONObject subscribeObject=new JSONObject();
							subscribeObject.put("id",id);
							subscribeObject.put("resourceTemplate",Resource.toJson(template));
							subscribeObject.put("command","SUBSCRIBE");
							subscribeObject.put("relay",false);
							kernel.getServerConnectionManager().establishPersistentConnection(serverBean, new Message(subscribeObject.toString()), new ServerConnectionManager.ConnectionManagerMessageListener() {
								@Override
								public boolean onMessageReceived(Message message, DataOutputStream outputStream) {
									processorListener.onProcessFinished(Message.makeMessage(message),false);
									resultSize++;
									return false;
								}
							},secure);
						}
					});
					thread.start();
				}
			}
			listeningUnsubscribeThread.start();
			
		}

		@Override
		public void onResourceChanged(Resource resource) {
			if ((resource.getChannel().equals(this.template.getChannel())) && (resource.getOwner().equals("") || resource.getOwner().equals(this.template.getOwner())) &&
					(resource.getTags().size() == 0 || resource.getTags().stream().anyMatch(tag -> this.template.getTags().contains(tag))) &&
					(resource.getUri().toString().equals("") || resource.getUri().equals(this.template.getUri())) &&
					((resource.getName().equals("") || this.template.getName().contains(resource.getName())) || (resource.getDescription().equals("") || this.template.getDescription().contains(resource.getDescription())))) {
				this.resultSize++;
				processorListener.onProcessFinished(Message.makeAMessage(Resource.toJson(resource).toString()),false);
			}
		}
		
		public int getState() {
			return state;
		}
		
		public void setState(int state) {
			this.state = state;
		}
	}
}
