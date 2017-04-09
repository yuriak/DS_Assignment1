package org.aw.server;

import org.aw.comman.ResponseType;

import java.io.File;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class Response {
	private ResponseType type;
	private String message;
	private byte[] bytes;

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	private File file;

	public Response(){}
	public Response(ResponseType type,String message,byte[] bytes,File file){
		this.type=type;
		this.message=message;
		this.bytes=bytes;
		this.file=file;
	}
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
