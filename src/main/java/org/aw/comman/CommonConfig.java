package org.aw.comman;

import org.aw.server.ServerConfig;

/**
 * Created by YuriAntonov on 2017/5/19.
 */
public class CommonConfig {
	public static final String SERVER_KEYSTORE_NAME = "sslserverkeys";
	public static final String CLIENT_TRUST_KEYSTORE_NAME="sslclientkeys";
	public static String SERVER_KEYSTORE_PATH = ServerConfig.class.getClassLoader().getResource(SERVER_KEYSTORE_NAME).getPath();
	public static final String SERVER_KEYSTORE_PASSWD = "123456";
}
