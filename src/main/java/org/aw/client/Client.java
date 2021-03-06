package org.aw.client;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by YuriAntonov on 2017/4/11.
 */
public class Client {

	private static Logger logger=Logger.getLogger(Client.class);

	public static void main(String[] args) {
		Options options=new Options();
		options.addOption("channel",true,"channel");
		options.addOption("debug","print debug information");
		options.addOption("description",true,"resource description");
		options.addOption("exchange","exchange server list with server");
		options.addOption("fetch","fetch resources from server");
		options.addOption("host",true,"server host, a domain name or IP address");
		options.addOption("name",true,"resource name");
		options.addOption("owner",true,"owner");
		options.addOption("port",true,"server port, an integer");
		options.addOption("publish","publish resource on server");
		options.addOption("query","query for resources from server");
		options.addOption("remove","remove resource from server");
		options.addOption("secret",true,"secret");
		options.addOption("servers",true,"server list, host1:port1,host2:port2,...");
		options.addOption("share","share resource on server");
		options.addOption("tags",true,"resource tags, tag1,tag2,tag3,...");
		options.addOption("uri",true,"resource URI");
		options.addOption("secure",false,"use secure connection");
		options.addOption("subscribe","subscribe matched resources");
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			List<Option> opts = new ArrayList<>(options.getOptions());
			System.out.println("Usage:");
			for (Option opt : opts) {
				System.out.println(opt.getOpt() + "\t" + opt.getDescription());
			}
			return;
		}
		ClientKernel kernel=new ClientKernel();
		kernel.processCommand(cmd);
	}
}
