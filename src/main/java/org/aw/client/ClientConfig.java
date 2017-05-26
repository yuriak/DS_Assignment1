package org.aw.client;

import org.aw.server.ServerConfig;

import java.io.InputStream;

/**
 * Created by YURI-AK on 2017/5/22.
 */
public class ClientConfig {
	public static final String CLIENT_KEYSTORE_NAME = "AbyssWatchersClient.keystore";
	public static final String CLIENT_TRUST_KEYSTORE_NAME = "AbyssWatchersClient.keystore";
	public static String CLIENT_KEYSTORE_PATH = ServerConfig.class.getClassLoader().getResource(CLIENT_KEYSTORE_NAME).getPath();
	public static String CLIENT_TRUST_KEYSTORE_PATH = ServerConfig.class.getClassLoader().getResource(CLIENT_TRUST_KEYSTORE_NAME).getPath();
	public static InputStream CLIENT_KEYSTORE_INPUTSTREAM=ClientConfig.class.getClassLoader().getResourceAsStream(CLIENT_KEYSTORE_NAME);
	public static InputStream CLIENT_TRUST_KEYSTORE_INPUTSTREAM=ClientConfig.class.getClassLoader().getResourceAsStream(CLIENT_TRUST_KEYSTORE_NAME);
	public static final String CLIENT_KEYSTORE_PASSWD = "password";
}
