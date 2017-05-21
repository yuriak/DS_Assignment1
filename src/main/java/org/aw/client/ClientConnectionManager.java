package org.aw.client;

import org.apache.log4j.Logger;
import org.aw.comman.Message;
import org.aw.comman.MessageType;
import org.aw.comman.ServerBean;
import org.aw.server.ServerConfig;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by YURI-AK on 2017/4/11.
 */
public class ClientConnectionManager {
	private static Logger logger=Logger.getLogger(ClientConnectionManager.class);
	public static List<Message> establishConnection(ServerBean serverBean, Message message,boolean secure) {
		
		Message response = null;
		List<Message> messages = new ArrayList<>();
		if(secure){
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			SSLSocket sslSocket=null;
			try {
				sslSocket = (SSLSocket) sslsocketfactory.createSocket(serverBean.getAddress(), serverBean.getPort());
				sslSocket.setSoTimeout(ServerConfig.TIME_OUT);
				DataInputStream inputStream = new DataInputStream(sslSocket.getInputStream());
				DataOutputStream outputStream = new DataOutputStream(sslSocket.getOutputStream());
				outputStream.writeUTF(message.getMessage().replaceAll("\0", "").trim());
				outputStream.flush();
				logger.debug("Securely sent: " + message.getMessage());
				String data = null;
				while ((data = inputStream.readUTF()) != null) {
					logger.info("Securely received: " + data);
					response = new Message(MessageType.STRING, data.replaceAll("\0", ""), null, null);
					messages.add(response);
				}
			}catch (IOException e){
				logger.info("Lost secure connection: " + sslSocket.getInetAddress().getHostAddress() + ":" + sslSocket.getPort());
			}finally {
				try {
					if (sslSocket != null) {
						sslSocket.close();
						logger.info("Close secure connection: " + sslSocket.getInetAddress().getHostAddress() + ":" + sslSocket.getPort());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				return messages;
			}
		}else{
			Socket socket = null;
			try {
				socket = new Socket(serverBean.getAddress(), serverBean.getPort());
				socket.setSoTimeout(ServerConfig.TIME_OUT);
				DataInputStream inputStream = new DataInputStream(socket.getInputStream());
				DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
				outputStream.writeUTF(message.getMessage().replaceAll("\0", "").trim());
				outputStream.flush();
				logger.debug("Sent: " + message.getMessage());
				String data = null;
				while ((data = inputStream.readUTF()) != null) {
					logger.info("Received: " + data);
					response = new Message(MessageType.STRING, data.replaceAll("\0", ""), null, null);
					messages.add(response);
				}
			} catch (IOException e) {
				logger.info("Lost connection: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
			} finally {
				try {
					if (socket != null) {
						socket.close();
						logger.info("Close connection: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				return messages;
			}
		}
	}
	
	public static void establishPersistentConnection(ServerBean serverBean, Message initialMessage, KeyBoardListener keyPressListener, MessageListener messageReceivedListener, boolean secure) {
		try {
			Socket socket = null;
			if (secure) {
				SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
				socket = (SSLSocket) sslsocketfactory.createSocket(serverBean.getAddress(), serverBean.getPort());
			} else {
				socket=new Socket(serverBean.getAddress(),serverBean.getPort());
			}
			BufferedReader sysReader = new BufferedReader(new InputStreamReader(System.in));
			DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
			DataInputStream inputStream = new DataInputStream(socket.getInputStream());
			outputStream.writeUTF(initialMessage.getMessage());
			Socket finalSocket = socket;
			new Thread(new Runnable() {
				@Override
				public void run() {
					String string = null;
					try {
						while ((string = inputStream.readUTF()) != null) {
							Message response=new Message(string);
							if(messageReceivedListener.onMessageReceived(response)){
								break;
							}
						}
						finalSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
			
			new Thread(new Runnable() {
				@Override
				public void run() {
					String string = null;
					try {
						while ((string = sysReader.readLine()) != null) {
							if(keyPressListener.onKeyPressed(outputStream,string)){
								break;
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	interface KeyBoardListener {
		boolean onKeyPressed(DataOutputStream sslOut, String string);
	}
	
	interface MessageListener {
		boolean onMessageReceived(Message message);
	}
}
