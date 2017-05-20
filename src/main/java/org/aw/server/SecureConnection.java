package org.aw.server;

import org.apache.log4j.Logger;
import org.aw.comman.Message;
import org.aw.comman.MessageType;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.List;

/**
 * Created by YuriAntonov on 2017/5/19.
 */
public class SecureConnection implements Runnable {
	private SSLSocket clientSocket;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	private ServerCommandProcessor processor;
	Logger logger = Logger.getLogger(SecureConnection.class);
	
	public SecureConnection(SSLSocket clientSocket) {
		this.clientSocket = clientSocket;
		this.processor = ServerCommandProcessor.getInstance();
		try {
			this.inputStream = new DataInputStream(clientSocket.getInputStream());
			this.outputStream = new DataOutputStream(clientSocket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		try {
			String commandString = inputStream.readUTF();
			logger.debug("Securely Received: " + commandString);
			List<Message> messages = processor.processCommand(commandString,true);
			for (Message message : messages) {
				if (message.getType() == MessageType.STRING) {
					outputStream.writeUTF(message.getMessage());
					outputStream.flush();
					logger.debug("Securely Sent: " + message.getMessage());
				} else if (message.getType() == MessageType.BYTES) {
					outputStream.write(message.getBytes());
					outputStream.flush();
					logger.debug("Securely Sent: " + message.getBytes().length + " of bytes");
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
					logger.debug("Securely Sent file: " + message.getFile().getName());
				}
			}
			inputStream.close();
			outputStream.close();
		} catch (IOException e) {
			logger.debug("Lost secure connection: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
			e.printStackTrace();
		} finally {
			try {
				this.clientSocket.close();
				logger.debug("Close secure connection: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
