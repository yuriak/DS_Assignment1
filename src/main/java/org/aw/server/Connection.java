package org.aw.server;

import org.apache.log4j.Logger;
import org.aw.comman.ResponseType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
	Logger logger=Logger.getLogger(Connection.class);
	public Connection(Socket clientSocket) {

		this.clientSocket = clientSocket;
		this.processor=ServerCommandProcessor.getInstance();
		try {
			this.inputStream = new DataInputStream(clientSocket.getInputStream());
			this.outputStream = new DataOutputStream(clientSocket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			List<Response> responses = processor.processCommand(inputStream.readUTF());
			for (Response response : responses) {
				if(response.getType()== ResponseType.MESSAGE){
					outputStream.writeUTF(response.getMessage());
					outputStream.flush();
				}else{
					outputStream.write(response.getBytes());
					outputStream.flush();
				}
			}
		} catch (IOException e) {
			logger.info("Lost connection: "+clientSocket.getInetAddress().getHostAddress()+":"+clientSocket.getPort());
		}
	}
}
