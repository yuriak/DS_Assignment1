package org.aw.server;

import org.aw.util.SSLUtil;

import java.io.InputStream;
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
	public static int SPORT=3781;
	public static int TIME_OUT=1000;

	public static final String SERVER_KEYSTORE_NAME = "AbyssWatchersServer.keystore";
	public static final String SERVER_TRUST_KEYSTORE_NAME = "AbyssWatchersClient.keystore";
	public static String SERVER_KEYSTORE_PATH = ServerConfig.class.getClassLoader().getResource(SERVER_KEYSTORE_NAME).getPath();
	public static InputStream SERVER_KEYSTORE_INPUTSTREAM=ServerConfig.class.getClassLoader().getResourceAsStream(SERVER_KEYSTORE_NAME);
	public static String SERVER_TRUST_KEYSTORE_PATH = ServerConfig.class.getClassLoader().getResource(SERVER_TRUST_KEYSTORE_NAME).getPath();
	public static InputStream SERVER_TRUST_KEYSTORE_INPUTSTREAM=ServerConfig.class.getClassLoader().getResourceAsStream(SERVER_TRUST_KEYSTORE_NAME);
	public static final String SERVER_KEYSTORE_PASSWD = "password";
	
	static {
//		System.setProperty("javax.net.ssl.keyStore", ServerConfig.SERVER_KEYSTORE_PATH);
//		System.setProperty("javax.net.ssl.keyStorePassword", ServerConfig.SERVER_KEYSTORE_PASSWD);
//		System.setProperty("javax.net.ssl.trustStore", ServerConfig.SERVER_TRUST_KEYSTORE_PATH);
//		System.setProperty("javax.net.ssl.trustStorePassword", ServerConfig.SERVER_KEYSTORE_PASSWD);
		try {
			SSLUtil.setSSLServerFactories(SERVER_KEYSTORE_INPUTSTREAM,SERVER_KEYSTORE_PASSWD,SERVER_TRUST_KEYSTORE_INPUTSTREAM,SERVER_KEYSTORE_PASSWD);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			HOST_NAME= InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		SECRET= UUID.randomUUID().toString();
	}
}
