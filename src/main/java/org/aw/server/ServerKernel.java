package org.aw.server;

import org.apache.log4j.Logger;
import org.aw.comman.Resource;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class ServerKernel {
	private Server server;
	private int status;
	private ConnectionHandler handler;
	private Map<String,Map<String,Map<URI,Resource>>> resources;
	private Set<Server> servers;
	private static ServerKernel serverKernel;
	Logger logger=Logger.getLogger(ServerKernel.class);

	private ServerKernel() {

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

	public void initServer(Server server, boolean debug, int connectionInterval, int exchangeInterval){
		this.server=server;
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
		listenThread.start();
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Map<String, Map<String, Map<URI, Resource>>> getResources() {
		return resources;
	}

	public void setResources(Map<String, Map<String, Map<URI, Resource>>> resources) {
		this.resources = resources;
	}

	public Set<Server> getServers() {
		return servers;
	}

	public void setServers(Set<Server> servers) {
		this.servers = servers;
	}





}
