
/*
 * Copyright (c) 2012 Inline Marmalade ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.inlinemarmalade.util.http;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.io.file.FileConnection;

import net.rim.device.api.io.http.HttpHeaders;
import net.rim.device.api.io.http.HttpProtocolConstants;

public class HttpRequest {

	protected HttpHeaders _headers;
	protected String _url;
	protected String _method;
	protected Object _data;
	protected Object _output;
	
	public static HttpRequest head(String url) {
		HttpRequest request = new HttpRequest();
		request._url = url;
		request._method = HttpProtocolConstants.HTTP_METHOD_HEAD;
		return request;
	}
	
	public static HttpRequest get(String url, Hashtable args, HttpHeaders headers, FileConnection file) {
		HttpRequest r = get(url, args, headers);
		r._output = file;
		return r;
	}
	
	public static HttpRequest get(String url, FileConnection file) {
		return get(url, null, null, file);
	}
	
	public static HttpRequest get(String url, Hashtable args, HttpHeaders headers) {
		HttpRequest request = new HttpRequest();
		request._url = url;
		request._method = HttpProtocolConstants.HTTP_METHOD_GET;
		request._data = args;
		request._headers = headers;
		return request;
	}
	
	public static HttpRequest get(String url, Hashtable args) {
		return get(url, args, null);
	}
	
	public static HttpRequest get(String url) {
		return get(url, null, null);
	}
	
	public String getUrl() {
		return _url;
	}
	
	public String getMethod() {
		return _method;
	}
	
	public String getFullUrl() {
		if (HttpProtocolConstants.HTTP_METHOD_GET.equals(_method) && _data != null && _data instanceof Hashtable) {
			StringBuffer buf = new StringBuffer(_url);
			Hashtable args = (Hashtable) _data;
			int i = 0;
			for (Enumeration keys = args.keys(); keys.hasMoreElements();) {
				if (i == 0)
					buf.append('?');
				else
					buf.append('&');
				String key = (String) keys.nextElement();
				String val = (String) args.get(key);
				buf.append(HttpClient.urlencode(key));
				buf.append('=');
				buf.append(HttpClient.urlencode(val));
				i++;
			}
			return buf.toString();
		}
		else {
			return _url;
		}
	}
}
