package org.aw.client;

import org.aw.server.ServerConfig;

/**
 * Created by YURI-AK on 2017/5/22.
 */
public class ClientConfig {
	public static final String CLIENT_KEYSTORE_NAME = "sslserverkeys";
	public static final String CLIENT_TRUST_KEYSTORE_NAME = "clienttrust";
	public static String CLIENT_KEYSTORE_PATH = ServerConfig.class.getClassLoader().getResource(CLIENT_KEYSTORE_NAME).getPath();
	public static String CLIENT_TRUST_KEYSTORE_PATH = ServerConfig.class.getClassLoader().getResource(CLIENT_TRUST_KEYSTORE_NAME).getPath();
	public static final String CLIENT_KEYSTORE_PASSWD = "123456";
}
