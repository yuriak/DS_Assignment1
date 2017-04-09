package org.aw.server;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class ConnectionHandler {
	Logger logger=Logger.getLogger(ConnectionHandler.class);

	private ThreadPoolExecutor executor;
	public ConnectionHandler() {
		executor = new ThreadPoolExecutor(30, 30, 200, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	}

	public void handleConnection(Server server) {
		try {
			ServerSocket serverSocket = new ServerSocket(server.getPort());
			while (true) {
				Socket clientSocket = serverSocket.accept();
				clientSocket.setSoTimeout(ServerConfig.CONNECTION_INTERVAL);
				logger.info("handel connection: "+clientSocket.getInetAddress().getHostAddress()+":"+clientSocket.getPort());
				executor.execute(new Connection(clientSocket));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getActiveConnectionNumber() {
		return executor.getActiveCount();
	}

}
