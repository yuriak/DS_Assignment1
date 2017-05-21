package org.aw.server;

import org.apache.log4j.Logger;
import org.aw.comman.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class ServerKernel {
	public ServerBean getMyNormalServer() {
		return myNormalServer;
	}

	public void setMyNormalServer(ServerBean myServer) {
		this.myNormalServer = myServer;
	}

	private ServerBean myNormalServer;
	private ServerBean mySSLServer;
	private int status;

	public ServerConnectionManager getServerConnectionManager() {
		return serverConnectionManager;
	}

	public void setServerConnectionManager(ServerConnectionManager serverConnectionManager) {
		this.serverConnectionManager = serverConnectionManager;
	}

	private ServerConnectionManager serverConnectionManager;
	private List<Resource> resources;
	private List<ServerBean> normalServerList;
	public ServerBean getMySSLServer() {
		return mySSLServer;
	}
	
	public void setMySSLServer(ServerBean mySSLServer) {
		this.mySSLServer = mySSLServer;
	}
	
	public List<ServerBean> getSslServerList() {
		return sslServerList;
	}
	
	public void setSslServerList(List<ServerBean> sslServerList) {
		this.sslServerList = sslServerList;
	}
	
	private List<ServerBean> sslServerList;
	private static ServerKernel serverKernel;
	Logger logger=Logger.getLogger(ServerKernel.class);
	private ServerKernel() {
		resources= Collections.synchronizedList(new ArrayList<>());
		normalServerList = Collections.synchronizedList(new ArrayList<>());
		sslServerList=Collections.synchronizedList(new ArrayList<>());
	}

	public static ServerKernel getInstance() {
		if (serverKernel == null) {
			synchronized (ServerKernel.class) {
				if (serverKernel == null) {
					serverKernel = new ServerKernel();
				}
			}
		}
		return serverKernel;
	}

	public void initServer(){
		this.myNormalServer = new ServerBean(ServerConfig.HOST_NAME,ServerConfig.PORT);
		this.mySSLServer =new ServerBean(ServerConfig.HOST_NAME,ServerConfig.SPORT);
		normalServerList.add(myNormalServer);
		sslServerList.add(mySSLServer);
		serverConnectionManager =new ServerConnectionManager();
		logger.info("Init Server: "+ myNormalServer.getHostname()+":"+ myNormalServer.getPort()+", SSL:"+mySSLServer.getPort());
		logger.info("Using secret: "+ServerConfig.SECRET);
	}

	public void startServer(){
		Thread listenNormalThread = new Thread(new Runnable() {
			public void run() {
				logger.debug("Start to handle connection");
				serverConnectionManager.handleConnection(myNormalServer);
			}
		});
		Thread listenSSLThread=new Thread(new Runnable() {
			@Override
			public void run() {
				System.setProperty("javax.net.ssl.keyStore", CommonConfig.SERVER_KEYSTORE_PATH);
				System.setProperty("javax.net.ssl.keyStorePassword", CommonConfig.SERVER_KEYSTORE_PASSWD);
				logger.debug("Start to handle ssl connection");
				serverConnectionManager.handleSecureConnection(mySSLServer);
			}
		});
		Thread exchangeThread=new Thread(new Runnable() {
			@Override
			public void run() {
				exchangeServers();
			}
		});
		logger.debug("starting listening thread and exchange thread");
		listenNormalThread.start();
		listenSSLThread.start();
		exchangeThread.start();
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public List<Resource> getResources() {
		return resources;
	}

	public void setResources(List<Resource> resources) {
		this.resources = resources;
	}

	public List<ServerBean> getNormalServerList() {
		return normalServerList;
	}

	public void setNormalServerList(List<ServerBean> normalServerList) {
		this.normalServerList = normalServerList;
	}

	private void exchangeServers(){
		logger.debug("start to exchange servers");
		while (true){
			try {
				Thread.sleep(ServerConfig.EXCHANGE_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (normalServerList.size() == 0&& sslServerList.size()==0) {
				continue;
			}
			Random random = new Random();
			List<ServerBean> diedServer = new ArrayList<>();
			List<ServerBean> diedSSLServer=new ArrayList<>();
			JSONArray normalServerArray = new JSONArray();
			JSONArray sslServerArray=new JSONArray();
			synchronized (normalServerList){
				normalServerList.forEach(server -> {
					JSONObject serverObject = new JSONObject();
					serverObject.put("hostname", server.getHostname());
					serverObject.put("port", server.getPort());
					normalServerArray.add(serverObject);
				});
			}
			synchronized (sslServerList) {
				sslServerList.forEach(server -> {
					JSONObject serverObject = new JSONObject();
					serverObject.put("hostname", server.getHostname());
					serverObject.put("port", server.getPort());
					sslServerArray.add(serverObject);
				});
			}
			random.ints(0, normalServerList.size()).distinct().limit(normalServerList.size() / 2).forEach(r -> {
				JSONObject messageObject = new JSONObject();
				messageObject.put("command", "EXCHANGE");
				messageObject.put("serverList", normalServerArray);
				Message message = new Message(MessageType.STRING,messageObject.toString(),null,null);
				List<Message> messages = serverConnectionManager.establishConnection(normalServerList.get(r), message,false);
				if (messages.size()==0){
					diedServer.add(normalServerList.get(r));
				}else {
					JSONObject resultObject = null;
					try {
						resultObject = (JSONObject) (new JSONParser()).parse(messages.get(0).getMessage());
					} catch (ParseException e) {
						e.printStackTrace();
					}
					if (!resultObject.containsKey("response") && resultObject.get("response").equals("success"))
						diedServer.add(normalServerList.get(r));
				}
			});
			random.ints(0, sslServerList.size()).distinct().limit(sslServerList.size() / 2).forEach(r -> {
				JSONObject messageObject = new JSONObject();
				messageObject.put("command", "EXCHANGE");
				messageObject.put("serverList", sslServerArray);
				Message message = new Message(MessageType.STRING, messageObject.toString(), null, null);
				List<Message> messages = serverConnectionManager.establishConnection(normalServerList.get(r), message, true);
				if (messages.size() == 0) {
					diedSSLServer.add(sslServerList.get(r));
				} else {
					JSONObject resultObject = null;
					try {
						resultObject = (JSONObject) (new JSONParser()).parse(messages.get(0).getMessage());
					} catch (ParseException e) {
						e.printStackTrace();
					}
					if (!resultObject.containsKey("response") && resultObject.get("response").equals("success"))
						diedSSLServer.add(sslServerList.get(r));
				}
			});
			synchronized (normalServerList){
				normalServerList.removeAll(diedServer);
			}
			synchronized (normalServerList) {
				sslServerList.removeAll(diedServer);
			}
//			logger.debug("current servers:"+ normalServerList);
//			logger.debug("current secured servers:" + sslServerList);
		}
	}
	
	public List<ServerBean> getSecureSeverList() {
		return sslServerList;
	}
	
	public void setSecureSeverList(List<ServerBean> secureSeverList) {
		this.sslServerList = secureSeverList;
	}
	
}
