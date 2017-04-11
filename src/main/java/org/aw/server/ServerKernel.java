package org.aw.server;

import org.apache.log4j.Logger;
import org.aw.comman.Message;
import org.aw.comman.MessageType;
import org.aw.comman.Resource;
import org.aw.comman.ServerBean;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class ServerKernel {
	public ServerBean getMyServer() {
		return myServer;
	}

	public void setMyServer(ServerBean myServer) {
		this.myServer = myServer;
	}

	private ServerBean myServer;
	private int status;

	public ServerConnectionManager getServerConnectionManager() {
		return serverConnectionManager;
	}

	public void setServerConnectionManager(ServerConnectionManager serverConnectionManager) {
		this.serverConnectionManager = serverConnectionManager;
	}

	private ServerConnectionManager serverConnectionManager;
	private List<Resource> resources;
	private List<ServerBean> serverList;
	private static ServerKernel serverKernel;
	Logger logger=Logger.getLogger(ServerKernel.class);
	private ServerKernel() {
		resources=new ArrayList<>();
		serverList =new ArrayList<>();
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
		this.myServer = new ServerBean(ServerConfig.HOST_NAME,ServerConfig.PORT);
		serverList.add(myServer);
		serverConnectionManager =new ServerConnectionManager();
		logger.info("init myServer: "+ myServer.getHostname()+":"+ myServer.getPort());
	}

	public void startServer(){
		Thread listenThread = new Thread(new Runnable() {
			public void run() {
				serverConnectionManager.handleConnection(myServer);
				logger.info("start to handle connection");
			}
		});
		Thread exchangeThread=new Thread(new Runnable() {
			@Override
			public void run() {
				exchangeServers();
			}
		});
		listenThread.start();
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

	public List<ServerBean> getServerList() {
		return serverList;
	}

	public void setServerList(List<ServerBean> serverList) {
		this.serverList = serverList;
	}

	private void exchangeServers(){
		while (true){
			try {
				Thread.sleep(ServerConfig.EXCHANGE_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (serverList.size() == 0) {
				continue;
			}
			Random random = new Random();
			List<ServerBean> diedServer = new ArrayList<>();
			random.ints(0, serverList.size()).distinct().limit(serverList.size() / 2).forEach(r -> {
				JSONObject messageObject = new JSONObject();
				JSONArray serverArray = new JSONArray(serverList);
				messageObject.put("command", "EXCHANGE");
				messageObject.put("serverList", serverArray);
				Message message = new Message(MessageType.STRING,messageObject.toString(),null,null);
				List<Message> messages = serverConnectionManager.establishConnection(serverList.get(r), message);
				if (messages.size()==0){
					diedServer.add(serverList.get(r));
				}else {
					JSONObject resultObject = new JSONObject(messages.get(0).getMessage());
					if (!resultObject.has("response") && resultObject.getString("response").equals("success"))
						diedServer.add(serverList.get(r));
				}
			});
			serverList.removeAll(diedServer);
			logger.debug(serverList);
		}
	}
}
