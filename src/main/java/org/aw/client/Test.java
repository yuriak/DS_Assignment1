package org.aw.client;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.aw.comman.Message;
import org.aw.comman.MessageType;
import org.aw.comman.Resource;
import org.aw.server.ServerConnectionManager;
import org.aw.comman.ServerBean;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by YURI-AK on 2017/4/8.
 */
public class Test {
	public static void main(String[] args) {
//		publish("","","http://www.baidu.com","baidu","localhost",9888);
//		publish("", "", "http://www.facebook.com","facebook", "localhost", 9888);
//		publish("", "", "http://www.google.com","google", "localhost", 9889);
//		publish("", "", "http://www.sina.com.cn","sina", "localhost", 9889);
//		publish("", "", "http://www.sohu.com.cn", "sohu", "localhost", 9890);
//		share("", "", "file:///D:/IIJavaWorkspace/EZShare.zip", "EZShare", "hello", "localhost", 9890);
//		share("", "", "file:///D:/IIJavaWorkspace/UltraCompare.zip", "UltraCompare_null", "hello", "localhost", 9891);
//		share("", "", "file:///D:/IIJavaWorkspace/EZShare1.zip", "EZShare1", "hello", "localhost", 9891);
//		share("", "", "file:///D:/IIJavaWorkspace/UltraCompare.zip", "UltraCompare", "hello", "localhost", 9892);
//		share("", "", "file:///D:/IIJavaWorkspace/UltraCompare1.zip", "UltraCompare1", "hello", "localhost", 9892);
//		remove("o1","c1", "http://www.baidu.com");
//		remove("o2", "c2", "http://www.baidu.com/ada/gds");
//		remove("o2", "c2", "http://www.baidu.com/ada/gds");
//		remove("o1", "c1", "http://www.baidu.com/ada");
//		share("","","file:///D:/IIJavaWorkspace/EZShare.zip","a file","hello");
//		fetch("", "", "file:///usr/local/share/ezshare/photo.jpg", "Secret agent photo :-)");
//		remove("o1","c1","file:///E:/OnKeyDetector.log");
//		exchange();
//		query("","","","","",true,"localhost",9888);
	}

	public static void publish(String owner,String channel,String uri,String description,String address,int port){
		Socket socket=null;
		try {
			socket = new Socket(address, port);
			DataInputStream inputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
			Resource resource = new Resource();
			resource.setName(owner+"|"+channel+"|"+uri);
			resource.setTags(new ArrayList<>());
			resource.setUri(new URI(uri));
			resource.setDescription(description);
			resource.setChannel(channel);
			resource.setOwner(owner);
			List<String> tagList=new ArrayList<>();
			tagList.add(owner);
			tagList.add(channel);
			tagList.add(uri);
			tagList.add(description);
			resource.setTags(tagList);
			JSONObject resourceObject = Resource.toJson(resource);
			JSONObject request = new JSONObject();
			request.put("resource", resourceObject);
			request.put("command", "PUBLISH");
			String message = request.toString();
			outputStream.writeUTF(message);
			outputStream.flush();
			System.out.println(inputStream.readUTF());
			inputStream.close();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}finally {
			if (socket!=null){
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void remove(String owner,String channel,String uri){
		Socket socket = null;
		try {
			socket = new Socket("localhost", 9888);
			DataInputStream inputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
			Resource resource = new Resource();
			resource.setUri(new URI(uri));
			resource.setChannel(channel);
			resource.setOwner(owner);
			JSONObject resourceObject = Resource.toJson(resource);
			JSONObject request = new JSONObject();
			request.put("resource", resourceObject);
			request.put("command", "REMOVE");
			String message = request.toString();
			outputStream.writeUTF(message);
			outputStream.flush();
			System.out.println(inputStream.readUTF());
			inputStream.close();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void share(String owner, String channel, String uri, String description,String secret,String address,int port){
		Socket socket = null;
		try {
			socket = new Socket(address, port);
			DataInputStream inputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
			Resource resource = new Resource();
			resource.setName(owner + "|" + channel + "|" + uri);
			resource.setUri(new URI(uri));
			resource.setDescription(description);
			resource.setChannel(channel);
			resource.setOwner(owner);
			List<String> tagList = new ArrayList<>();
			tagList.add(owner);
			tagList.add(channel);
			tagList.add(uri);
			tagList.add(description);
			resource.setTags(tagList);
			JSONObject resourceObject = Resource.toJson(resource);
			JSONObject request = new JSONObject();
			request.put("secret",secret);
			request.put("resource", resourceObject);
			request.put("command", "SHARE");
			String message = request.toString();
			outputStream.writeUTF(message);
			outputStream.flush();
			System.out.println(inputStream.readUTF());
			inputStream.close();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void fetch(String owner, String channel, String uri,String description){
		Socket socket = null;
		try {
			socket = new Socket("sunrise.cis.unimelb.edu.au", 3780);
			DataInputStream inputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
			Resource resource = new Resource();
//			resource.setName(owner + "|" + channel + "|" + uri);
//			resource.setTags(new ArrayList<>());
			resource.setUri(new URI(uri));
			resource.setDescription(description);
			resource.setChannel(channel);
			resource.setOwner(owner);
//			List<String> tagList = new ArrayList<>();
//			tagList.add(owner);
//			tagList.add(channel);
//			tagList.add(uri);
//			tagList.add(description);
//			resource.setTags(tagList);
			JSONObject resourceObject = Resource.toJson(resource);
			JSONObject request = new JSONObject();
			request.put("resourceTemplate", resourceObject);
			request.put("command", "FETCH");
			String message = request.toString();
			System.out.println(message);
			outputStream.writeUTF(message);
			outputStream.flush();
			System.out.println(inputStream.readUTF());
			String resourceInfoStr=inputStream.readUTF();
			System.out.println(resourceInfoStr);
			JSONObject resourceInfo = new JSONObject(resourceInfoStr);
			System.out.println(resourceInfo.toString());
			URI uri1 = new URI(resourceInfo.getString("uri"));
			Long size=resourceInfo.getLong("resourceSize");
			String fileName = uri1.getPath().split("/")[uri1.getPath().split("/").length - 1];
			File file = new File(fileName);
			file.createNewFile();
			FileUtils.writeByteArrayToFile(file, IOUtils.toByteArray(inputStream, size));
//			int bufferSize=1024;
//			byte[] buffer = new byte[bufferSize];
//			int read;
//			FileOutputStream fileOutputStream=new FileOutputStream(file);
//			while(size>0){
//				if (size>=bufferSize){
//					inputStream.read(buffer);
//					fileOutputStream.write(buffer);
//				}else {
//					bufferSize = Integer.valueOf(String.valueOf(size));
//					buffer = new byte[bufferSize];
//					inputStream.read(buffer);
//					fileOutputStream.write(buffer);
//				}
//				size -= bufferSize;
//			}
//			fileOutputStream.close();
			System.out.println(inputStream.readUTF());
			inputStream.close();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void exchange(){
		Socket socket = null;
		try {
			socket = new Socket("localhost", 9888);
			DataInputStream inputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
			List<ServerBean> serverBeen =new ArrayList<>();
			ServerBean serverBean1 =new ServerBean("localhost",9889);
			ServerBean serverBean2 =new ServerBean("localhost",9890);
			ServerBean serverBean3 = new ServerBean("localhost", 9891);
			ServerBean serverBean4 = new ServerBean("localhost", 9892);
			serverBeen.add(serverBean1);
			serverBeen.add(serverBean2);
			serverBeen.add(serverBean3);
			serverBeen.add(serverBean4);
			JSONArray serverArray=new JSONArray(serverBeen);
			JSONObject request = new JSONObject();
			request.put("serverList", serverArray);
			request.put("command", "EXCHANGE");
			String message = request.toString();
			outputStream.writeUTF(message);
			outputStream.flush();
			System.out.println(inputStream.readUTF());
			inputStream.close();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void query(String owner,String channel, String description,String name,String uri,boolean relay,String address,int port){
		JSONObject jsonObject=new JSONObject();
		Resource resource=new Resource();
		resource.setOwner(owner);
		resource.setChannel(channel);
		resource.setDescription(description);
		resource.setName(name);
		List<String> tagList=new ArrayList<>();
//		tagList.add("linux");
		resource.setTags(tagList);
//		try {
//			resource.setUri(new URI("http://*.com"));
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
//		}
		JSONObject resourceObject = Resource.toJson(resource);
		jsonObject.put("resourceTemplate",resourceObject);
		jsonObject.put("relay",relay);
		jsonObject.put("command","QUERY");
		System.out.println(jsonObject);
		ServerConnectionManager serverConnectionManager =new ServerConnectionManager();
//		List<Message> messages = serverConnectionManager.establishConnection(new ServerBean("sunrise.cis.unimelb.edu.au", 3780), new Message(MessageType.STRING, jsonObject.toString(), null, null));
		List<Message> messages = serverConnectionManager.establishConnection(new ServerBean(address, port), new Message(MessageType.STRING, jsonObject.toString(), null, null));
		messages.forEach(message -> System.out.println(message.getMessage()));
	}
}
