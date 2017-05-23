package org.aw.server;

import org.apache.log4j.Logger;
import org.aw.comman.Message;
import org.aw.comman.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class Connection implements Runnable {
	private Socket clientSocket;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	private ServerCommandProcessor processor;
	boolean persistent;
	boolean secure;
	Logger logger=Logger.getLogger(Connection.class);
	public Connection(Socket clientSocket,boolean persistent,boolean secure) {
		this.clientSocket = clientSocket;
		this.persistent=persistent;
		this.secure=secure;
		this.processor=ServerCommandProcessor.getInstance();
		try {
			this.inputStream = new DataInputStream(clientSocket.getInputStream());
			this.outputStream = new DataOutputStream(clientSocket.getOutputStream());
		} catch (IOException e) {
//			e.printStackTrace();
		}
	}

	public void run() {
		String commandString = null;
		try {
			commandString = inputStream.readUTF();
			logger.debug((secure ? "Securely" : "") + " Received: " + commandString);
			processor.processCommand(commandString, secure,inputStream, new ServerCommandProcessor.ProcessorListener() {
				@Override
				public boolean onProcessFinished(List<Message> messages,boolean closeConnection) {
					try {
						for (Message message : messages) {
							if (message.getType() == MessageType.STRING) {
								outputStream.writeUTF(message.getMessage());
								outputStream.flush();
								logger.debug((secure ? "Securely" : "") + " Sent: " + message.getMessage() +" to "+clientSocket.getInetAddress()+":"+clientSocket.getPort());
							} else if (message.getType() == MessageType.BYTES) {
								outputStream.write(message.getBytes());
								outputStream.flush();
								logger.debug((secure ? "Securely" : "") + " Sent: " + message.getBytes().length + " of bytes");
							} else if (message.getType() == MessageType.FILE) {
								BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(message.getFile()));
								int bufferSize = 1024;
								byte[] bufferArray = new byte[bufferSize];
								int read = 0;
								while ((read = bufferedInputStream.read(bufferArray)) != -1) {
									outputStream.write(bufferArray, 0, read);
								}
								outputStream.flush();
								bufferedInputStream.close();
								logger.debug((secure ? "Securely" : "") + " Sent file: " + message.getFile().getName());
							}
						}
						return true;
					} catch (IOException e) {
						logger.debug("Lost " + (secure ? "secure" : "") + " connection: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
						try {
							clientSocket.close();
						} catch (IOException e1) {
//							e1.printStackTrace();
							return false;
						}
						return false;
					} finally {
						try {
							if (closeConnection&&!clientSocket.isClosed()){
								clientSocket.close();
								logger.debug("Close " + (secure ? "secure" : "") + " connection: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
								return false;
							}
						} catch (IOException e) {
//							e.printStackTrace();
							return false;
						}
					}
				}
			});
		} catch (IOException e) {
//			e.printStackTrace();
		}
		
	}
	
	interface ConnectionMessageListener {
		void onMessageReceived(Message message);
	}
}
