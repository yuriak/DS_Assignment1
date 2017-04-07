package org.aw.server;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class Server {

	private String hostname;
	private int port;

	public Server(String hostname,int port){
		this.hostname=hostname;
		this.port=port;
	}
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	@Override
	public String toString() {
		return this.hostname+":"+port;
	}
}
