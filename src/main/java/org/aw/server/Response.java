package org.aw.server;

import org.aw.comman.ResponseType;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class Response {
	private ResponseType type;
	private String message;
	private byte[] bytes;

	public ResponseType getType() {
		return type;
	}

	public void setType(ResponseType type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

}
