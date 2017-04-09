package org.aw.server;

import org.apache.log4j.Logger;
import org.aw.comman.Resource;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class ServerKernel {
	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	private Server server;
	private int status;
	private ConnectionHandler handler;
	private List<Resource> resources;
	private List<Server> servers;
	private static ServerKernel serverKernel;
	Logger logger=Logger.getLogger(ServerKernel.class);
	private ServerKernel() {
		resources=new ArrayList<>();
		servers=new ArrayList<>();

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

	public void initServer(Server server, boolean debug, int connectionInterval, int exchangeInterval,String hostname,String secret){
		this.server=server;
		servers.add(server);
		ServerConfig.DEBUG =debug;
		ServerConfig.CONNECTION_INTERVAL =connectionInterval;
		ServerConfig.EXCHANGE_INTERVAL=exchangeInterval;
		try {
			ServerConfig.HOST_NAME=(hostname==null? InetAddress.getLocalHost().getHostName().toString():hostname);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			ServerConfig.HOST_NAME="UnknowHost";
		}
		ServerConfig.SECRET = (secret == null ? UUID.randomUUID().toString() : secret);
		handler=new ConnectionHandler();
		logger.info("init server: "+server.getHostname()+":"+server.getPort());
	}

	public void startServer(){
		Thread listenThread = new Thread(new Runnable() {
			public void run() {
				handler.handleConnection(server);
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

	public List<Server> getServers() {
		return servers;
	}

	public void setServers(List<Server> servers) {
		this.servers = servers;

	}

	private void exchangeServers(){
		while (true){
			try {
				Thread.sleep(ServerConfig.EXCHANGE_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (servers.size() == 0) {
				continue;
			}
			Random random = new Random();
			List<Server> diedServer = new ArrayList<>();
			random.ints(0, servers.size()).distinct().limit(servers.size() / 2).forEach(r -> {
				Socket socket = null;
				try {
					socket = new Socket(servers.get(r).getHostname(), servers.get(r).getPort());
					DataInputStream inputStream = new DataInputStream(socket.getInputStream());
					DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
					JSONObject messageObject = new JSONObject();
					JSONArray serverArray = new JSONArray(servers);
					messageObject.put("command", "EXCHANGE");
					messageObject.put("serverList", serverArray);
					outputStream.writeUTF(messageObject.toString());
					outputStream.flush();
					JSONObject resultObject = new JSONObject(inputStream.readUTF());
					if (!resultObject.has("response") && resultObject.getString("response").equals("success"))
						diedServer.add(servers.get(r));
					outputStream.close();
					inputStream.close();
				} catch (IOException e) {
					diedServer.add(servers.get(r));
				} finally {
					if (socket != null) {
						try {
							socket.close();
						} catch (IOException e) {
						}
					}
				}
			});
//			System.out.println("current: "+servers);
			servers.removeAll(diedServer);
		}
	}
}
