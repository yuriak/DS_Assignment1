package org.aw.server;

import org.apache.log4j.Logger;
import org.aw.comman.Message;
import org.aw.comman.MessageType;
import org.aw.comman.ServerBean;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class ServerConnectionManager {
	Logger logger=Logger.getLogger(ServerConnectionManager.class);
	private Map<String,Long> intervalMap;
	private ThreadPoolExecutor executor;
	public ServerConnectionManager() {
		executor = new ThreadPoolExecutor(50, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		intervalMap=new ConcurrentHashMap<>();
	}

	public void handleConnection(ServerBean serverBean) {
		try {
			ServerSocket serverSocket = new ServerSocket(serverBean.getPort());
			while (true) {
				Socket clientSocket = serverSocket.accept();
				clientSocket.setSoTimeout(ServerConfig.TIME_OUT);
				String ipAddress=clientSocket.getInetAddress().getHostAddress();
				logger.debug("Handel connection: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
				if (!checkConnectionInterval(ipAddress)){
					clientSocket.close();
					continue;
				}
				executor.execute(new Connection(clientSocket));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void handleSecureConnection(ServerBean serverBean){
		try {
			SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			SSLServerSocket sslserversocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(ServerConfig.SPORT);
			while (true) {
				SSLSocket sslClientSocket = (SSLSocket) sslserversocket.accept();
				sslClientSocket.setSoTimeout(ServerConfig.TIME_OUT);
				String ipAddress = sslClientSocket.getInetAddress().getHostAddress();
				logger.debug("Handel secure connection: " + sslClientSocket.getInetAddress().getHostAddress() + ":" + sslClientSocket.getPort());
				if (!checkConnectionInterval(ipAddress)) {
					sslClientSocket.close();
					continue;
				}
				executor.execute(new SecureConnection(sslClientSocket));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private boolean checkConnectionInterval(String ipAddress){
		if (!intervalMap.containsKey(ipAddress)) {
			intervalMap.put(ipAddress, System.currentTimeMillis());
			return true;
		} else {
			if (System.currentTimeMillis() - intervalMap.get(ipAddress) < ServerConfig.CONNECTION_INTERVAL) {
				logger.info("Client: " + ipAddress + " violates the connection interval.");
				return false;
			}else {
				return true;
			}
		}
		
	}
	
	public int getActiveConnectionNumber() {
		return executor.getActiveCount();
	}

	public List<Message> establishConnection(ServerBean serverBean, Message message,boolean secure) {
		
		Message response = null;
		List<Message> messages = new ArrayList<>();
		
		SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		Socket socket = null;
		try {
			if (secure){
				socket = (SSLSocket) sslsocketfactory.createSocket(serverBean.getAddress(), serverBean.getPort());
			}else {
				socket=new Socket(serverBean.getAddress(),serverBean.getPort());
			}
			socket.setSoTimeout(ServerConfig.TIME_OUT);
			DataInputStream inputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
			outputStream.writeUTF(message.getMessage().replaceAll("\0", "").trim());
			outputStream.flush();
			logger.debug("Securely sent: " + message.getMessage());
			String data = null;
			while ((data = inputStream.readUTF()) != null) {
				logger.info("Securely received: " + data);
				response = new Message(MessageType.STRING, data.replaceAll("\0", ""), null, null);
				messages.add(response);
			}
		} catch (IOException e) {
			logger.info("Lost secure connection: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
		} finally {
			try {
				if (socket != null) {
					socket.close();
					logger.info("Close secure connection: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return messages;
		}
	}
}
