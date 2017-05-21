package org.aw.server;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by YuriAntonov on 2017/4/11.
 */
public class Server {
	private static Logger logger=Logger.getLogger(Server.class);
	public static void main(String[] args) {
		Options options=new Options();
		options.addOption("advertisedhostname",true,"advertised hostname");
		options.addOption("connectionintervallimit",true,"connection interval limit in seconds");
		options.addOption("exchangeinterval",true,"exchange interval in seconds");
		options.addOption("port",true,"server port, an integer");
		options.addOption("sport", true, "server secure port, an integer");
		options.addOption("secret",true,"secret");
		options.addOption("debug",false,"print debug information");
		CommandLineParser parser=new DefaultParser();
		CommandLine cmd=null;
		try {
			cmd=parser.parse(options,args);
		} catch (ParseException e) {
			List<Option> opts=new ArrayList<>(options.getOptions());
			System.out.println("Usage:");
			for (Option opt:opts){
				System.out.println(opt.getOpt()+"\t"+opt.getDescription());
			}
			return;
		}
		if (cmd.hasOption("advertisedhostname")){
			ServerConfig.HOST_NAME=cmd.getOptionValue("advertisedhostname");
		}
		if (cmd.hasOption("connectionintervallimit")){
			try {
				ServerConfig.CONNECTION_INTERVAL = Integer.parseInt(cmd.getOptionValue("connectionintervallimit")) * 1000;
			}catch (Exception e){
				logger.error("Connection interval must be an integer, using default connection interval: "+ServerConfig.CONNECTION_INTERVAL);
			}
		}
		if (cmd.hasOption("exchangeinterval")){
			try {
				ServerConfig.EXCHANGE_INTERVAL = Integer.parseInt(cmd.getOptionValue("exchangeinterval")) * 1000;
			}catch (Exception e){
				logger.error("Exchange interval must be an integer, using default exchange interval: " + ServerConfig.EXCHANGE_INTERVAL);
			}

		}
		if (cmd.hasOption("port")){
			try {
				int port= Integer.parseInt(cmd.getOptionValue("port"));
				if (port<0||port>65535){
					logger.error("Port must be an integer between 0 and 65535");
				}else {
					if(port==ServerConfig.SPORT){
						logger.error("Port should not be same as secure port");
					}else{
						ServerConfig.PORT = port;
					}
				}
			}catch (Exception e){
				logger.error("Port must be an integer between 0 and 65535");
			}
		}
		if (cmd.hasOption("sport")) {
			try {
				int sport = Integer.parseInt(cmd.getOptionValue("sport"));
				if (sport < 0 || sport > 65535) {
					logger.error("Secure port must be an integer between 0 and 65535");
				} else {
					if(sport==ServerConfig.PORT){
						logger.error("Secure port should not be same as port");
					}else{
						ServerConfig.SPORT = sport;
					}
				}
			} catch (Exception e) {
				logger.error("Secure port must be an integer between 0 and 65535");
			}
		}
		if (cmd.hasOption("secret")){
			ServerConfig.SECRET=cmd.getOptionValue("secret");
		}
		if (cmd.hasOption("debug")){
			ServerConfig.DEBUG=true;
		}
		ServerKernel kernel = ServerKernel.getInstance();
		kernel.initServer();
		kernel.startServer();
	}
}
