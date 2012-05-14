
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.io.file.FileConnection;

import net.rim.blackberry.api.browser.URLEncodedPostData;
import net.rim.device.api.io.http.HttpHeaders;
import net.rim.device.api.io.http.HttpProtocolConstants;

public class HttpRequest {

	protected HttpHeaders _headers;
	protected String _url;
	protected String _method;
	protected String _contentType;
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
	
	public static HttpRequest get(String url, Hashtable args, HttpHeaders headers, OutputStream out) {
		HttpRequest r = get(url, args, headers);
		r._output = out;
		return r;
	}
	
	public static HttpRequest get(String url, OutputStream out) {
		return get(url, null, null, out);
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
	
	public static HttpRequest post(String url, Hashtable args, HttpHeaders headers) {
		HttpRequest request = new HttpRequest();
		request._url = url;
		request._method = HttpProtocolConstants.HTTP_METHOD_POST;
		if (args != null)
			request._data = convertFormParametersToBytes(args);
		request._headers = headers;
		request._contentType = "application/x-www-form-urlencoded;charset=UTF-8";
		return request;
	}
	
	public static HttpRequest post(String url, byte[] postData, HttpHeaders headers) {
		HttpRequest request = new HttpRequest();
		request._url = url;
		request._method = HttpProtocolConstants.HTTP_METHOD_POST;
		request._data = postData;
		request._headers = headers;
		return request;
	}
	
	public static HttpRequest post(String url, Hashtable args) {
		return post(url, args, null);
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
	
	protected void writePostData(OutputStream out) throws IOException {
		if (_data != null) {
			if (_data instanceof byte[]) {
				out.write((byte[]) _data);
			}
			else if (_data instanceof Hashtable) {
				Hashtable ht = (Hashtable) _data;
				URLEncodedPostData encoder = new URLEncodedPostData("UTF-8", false);
				
				for (Enumeration keys = ht.keys(); keys.hasMoreElements();) {
					String key = (String) keys.nextElement();
					String value = (String) ht.get(key);
					
					encoder.append(key, value);
				}
				
				out.write(encoder.getBytes());
			}
		}
	}
	
	protected String getContentType() {
		String type = null;
		
		if (_data != null) {
			type = _contentType;
		}
		
		return type;
	}
	
	protected long getContentLength() {
		long size = -1;
		
		if (_data != null) {
			if (_data instanceof byte[]) {
				size = ((byte[]) _data).length;
			}
		}
		
		return size;
	}
	
	private static byte[] convertFormParametersToBytes(Hashtable params) {
		URLEncodedPostData encoder = new URLEncodedPostData("UTF-8", false);
		
		for (Enumeration keys = params.keys(); keys.hasMoreElements();) {
			String key = (String) keys.nextElement();
			String value = (String) params.get(key);
			
			encoder.append(key, value);
		}
		
		return encoder.getBytes();
	}
}
