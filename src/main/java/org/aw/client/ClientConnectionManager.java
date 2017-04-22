package org.aw.client;

import org.apache.log4j.Logger;
import org.aw.comman.Message;
import org.aw.comman.MessageType;
import org.aw.comman.ServerBean;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by YURI-AK on 2017/4/11.
 */
public class ClientConnectionManager {
	private static Logger logger=Logger.getLogger(ClientConnectionManager.class);
	public static List<Message> establishConnection(ServerBean serverBean, Message message) {
		Socket socket = null;
		Message response = null;
		List<Message> messages = new ArrayList<>();
		try {
			socket = new Socket(serverBean.getAddress(), serverBean.getPort());
			socket.setSoTimeout(1000);
			DataInputStream inputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
			outputStream.writeUTF(message.getMessage());
			outputStream.flush();
			String data = null;
			while ((data = inputStream.readUTF()) != null) {
				response = new Message(MessageType.STRING, data, null, null);
				messages.add(response);
			}
		} catch (IOException e) {
//			logger.info("Lost connection: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
		} finally {
			try {
				if (socket != null) {
					socket.close();
//					logger.info("Close connection: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
				}
			} catch (IOException e) {

			}
			return messages;
		}
	}
}
