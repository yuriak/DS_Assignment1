package org.aw.server;

import org.apache.commons.cli.*;

/**
 * Created by YuriAntonov on 2017/4/11.
 */
public class Server {
	public static void main(String[] args) {
		Options options=new Options();
		options.addOption("advertisedhostname",true,"advertised hostname");
		options.addOption("connectionintervallimit",true,"connection interval limit in seconds");
		options.addOption("exchangeinterval",true,"exchange interval in seconds");
		options.addOption("port",true,"server port, an integer");
		options.addOption("secret",true,"secret");
		options.addOption("debug",true,"print debug information");
		CommandLineParser parser=new DefaultParser();
		CommandLine cmd=null;
		try {
			cmd=parser.parse(options,args);
		} catch (ParseException e) {
			e.printStackTrace();
			return;
		}
		if (cmd.hasOption("advertisedhostname")){
			ServerConfig.HOST_NAME=cmd.getOptionValue("advertisedhostname");
		}
		if (cmd.hasOption("connectionintervallimit")){
			ServerConfig.CONNECTION_INTERVAL=(int)Double.parseDouble(cmd.getOptionValue("connectionintervallimit"))*1000;
		}
		if (cmd.hasOption("exchangeinterval")){
			ServerConfig.EXCHANGE_INTERVAL=(int)Double.parseDouble(cmd.getOptionValue("exchangeinterval"))*1000;
		}
		if (cmd.hasOption("port")){
			ServerConfig.PORT=Integer.parseInt(cmd.getOptionValue("port"));
		}
		if (cmd.hasOption("secret")){
			ServerConfig.SECRET=cmd.getOptionValue("secret");
		}
		if (cmd.hasOption("debug")){
			ServerConfig.DEBUG=Boolean.parseBoolean(cmd.getOptionValue("debug"));
		}
		ServerKernel kernel = ServerKernel.getInstance();
		kernel.initServer();
		kernel.startServer();
	}
}
