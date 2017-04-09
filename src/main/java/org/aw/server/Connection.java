package org.aw.server;

import org.apache.log4j.Logger;
import org.aw.comman.ResponseType;

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
			String message=inputStream.readUTF();
			List<Response> responses = processor.processCommand(message);
			for (Response response : responses) {
				if(response.getType()== ResponseType.MESSAGE){
					outputStream.writeUTF(response.getMessage());
					outputStream.flush();
				}else if (response.getType()==ResponseType.BYTES){
					outputStream.write(response.getBytes());
					outputStream.flush();
				}else {
					BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(response.getFile()));
					int bufferSize=1024;
					byte[] bufferArray=new byte[bufferSize];
					int read = 0;
					while ((read = bufferedInputStream.read(bufferArray))!=-1){
						outputStream.write(bufferArray,0,read);
					}
					outputStream.flush();
					bufferedInputStream.close();
				}
			}
			inputStream.close();
			outputStream.close();
		} catch (IOException e) {
			logger.info("Lost connection: "+clientSocket.getInetAddress().getHostAddress()+":"+clientSocket.getPort());
		}finally{
			try {
				this.clientSocket.close();
				logger.info("Close connection: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
