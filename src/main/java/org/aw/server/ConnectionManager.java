package org.aw.server;

import org.apache.log4j.Logger;
import org.aw.comman.Message;
import org.aw.comman.MessageType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class ConnectionManager {
	Logger logger=Logger.getLogger(ConnectionManager.class);

	private ThreadPoolExecutor executor;
	public ConnectionManager() {
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

	public Message establishConnection(Server server, Message message) {
		Socket socket = null;
		Message response=null;
		try {
			socket=new Socket(server.getAddress(),server.getPort());
			DataInputStream inputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
			outputStream.writeUTF(message.getMessage());
			outputStream.flush();
			response=new Message(MessageType.STRING,inputStream.readUTF(),null,null);
		} catch (IOException e) {
			logger.info("Lost connection: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
		} finally {
			try {
				if (socket!=null){
					socket.close();
					logger.info("Close connection: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
				}
			} catch (IOException e) {

			}
			return response;
		}
	}
}
