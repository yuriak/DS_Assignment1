package org.aw.comman;

import org.aw.server.Server;
import org.aw.server.ServerKernel;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class Launcher {

	public static void main(String[] args) {
//		PropertyConfigurator.configure("resources/log4j.properties");
		ServerKernel kernel=ServerKernel.getInstance();
		kernel.initServer(new Server("127.0.0.1",9888),false,10,10);
		kernel.startServer();
	}
}
