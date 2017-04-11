package org.aw.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Created by YURI-AK on 2017/4/6.
 */
public class ServerConfig {
	
	
	public static final String ROOT_PACKAGE_NAME = "org.aw";
	public static boolean DEBUG =false;
	public static int CONNECTION_INTERVAL =1000;
	public static int EXCHANGE_INTERVAL =10000;
	public static String SECRET="";
	public static String HOST_NAME="";
	public static int PORT=9888;
	
	static {
		try {
			HOST_NAME= InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		SECRET= UUID.randomUUID().toString();
	}
}
