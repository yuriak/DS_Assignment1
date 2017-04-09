package org.aw.client;

import org.aw.comman.Resource;
import org.aw.server.Server;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
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
//		publish("o1","c1","http://www.baidu.com","d1");
//		publish("o1", "c1", "http://www.baidu.com/ada","d2");
//		publish("o2", "c2", "http://www.baidu.com/ada/gds","d3");
//		publish("o1", "c1", "http://www.baidu.com/ada","d4");
//		remove("o1","c1", "http://www.baidu.com");
//		remove("o2", "c2", "http://www.baidu.com/ada/gds");
//		remove("o2", "c2", "http://www.baidu.com/ada/gds");
//		remove("o1", "c1", "http://www.baidu.com/ada");
//		share("o1","c1","file:///D:/IIJavaWorkspace/EZShare.zip","a file","hello");
//		fetch("o1", "c1", "file:///D:/IIJavaWorkspace/EZShare.zip", "a file");
//		remove("o1","c1","file:///E:/OnKeyDetector.log");
		exchange();
	}

	public static void publish(String owner,String channel,String uri,String description){
		Socket socket=null;
		try {
			socket = new Socket("localhost", 9888);
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

	public static void share(String owner, String channel, String uri, String description,String secret){
		Socket socket = null;
		try {
			socket = new Socket("localhost", 9888);
			DataInputStream inputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
			Resource resource = new Resource();
			resource.setName(owner + "|" + channel + "|" + uri);
			resource.setTags(new ArrayList<>());
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
			socket = new Socket("localhost", 9888);
			DataInputStream inputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
			Resource resource = new Resource();
			resource.setName(owner + "|" + channel + "|" + uri);
			resource.setTags(new ArrayList<>());
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
			request.put("resourceTemplate", resourceObject);
			request.put("command", "FETCH");
			String message = request.toString();
			outputStream.writeUTF(message);
			outputStream.flush();
			System.out.println(inputStream.readUTF());
			JSONObject resourceInfo = new JSONObject(inputStream.readUTF());
			URI uri1 = new URI(resourceInfo.getString("uri"));
			Long size=resourceInfo.getLong("resourceSize");
			String fileName = uri1.getPath().split("/")[uri1.getPath().split("/").length - 1];
			File file = new File(fileName);
			file.createNewFile();
//			FileUtils.writeByteArrayToFile(file, IOUtils.toByteArray(inputStream, size));
			int bufferSize=1024;
			byte[] buffer = new byte[bufferSize];
			int read;
			FileOutputStream fileOutputStream=new FileOutputStream(file);
			while(size>0){
				if (size>=bufferSize){
					inputStream.read(buffer);
					fileOutputStream.write(buffer);
				}else {
					bufferSize = Integer.valueOf(String.valueOf(size));
					buffer = new byte[bufferSize];
					inputStream.read(buffer);
					fileOutputStream.write(buffer);
				}
				size -= bufferSize;
			}
			fileOutputStream.close();
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
			List<Server> servers=new ArrayList<>();
			Server server1=new Server("localhost",9889);
			Server server2=new Server("localhost",9890);
			Server server3 = new Server("localhost", 9891);
			Server server4 = new Server("localhost", 9892);
			servers.add(server1);
			servers.add(server2);
			servers.add(server3);
			servers.add(server4);
			JSONArray serverArray=new JSONArray(servers);
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
}
