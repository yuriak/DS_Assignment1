package org.aw.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class Server {

	private String hostname;
	private int port;

	public InetAddress getAddress() {
		return address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	private InetAddress address;

	public Server(String hostname,int port){
		this.hostname=hostname;
		this.port=port;
		try {
			this.address=InetAddress.getByName(hostname);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
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
		return this.hostname+":"+this.port;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Server)) return false;
		Server server= (Server) obj;
		return this.address.equals(server.getAddress())&&this.port==server.getPort();
	}
}
