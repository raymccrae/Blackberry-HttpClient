package com.inlinemarmalade.util.http;

public class HttpProxy {

	protected String _host;
	protected int _port;
	protected String _username;
	protected String _password;
	
	public HttpProxy(String host, int port, String username, String password) {
		_host = host;
		_port = port;
		_username = username;
		_password = password;
	}
}
