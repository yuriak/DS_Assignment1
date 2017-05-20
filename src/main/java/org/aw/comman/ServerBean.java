package org.aw.comman;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class ServerBean {

	private String hostname;
	private int port;
	public InetAddress getAddress() {
		return address;
	}
	

	public void setAddress(InetAddress address) {
		this.address = address;
	}
	
	private InetAddress address;
	
	public ServerBean(String hostname, int port){
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
		if (!(obj instanceof ServerBean)) return false;
		ServerBean serverBean = (ServerBean) obj;
		return this.address.equals(serverBean.getAddress())&&this.port== serverBean.getPort();
	}
	
}
