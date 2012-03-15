
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

import javax.microedition.io.Connection;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.io.NoCopyByteArrayOutputStream;
import net.rim.device.api.io.http.HttpProtocolConstants;
import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.ui.component.Dialog;

/**
 * HttpClient 
 *
 */
public class HttpClient {
	
	private static final int DEFAULT_COPY_BUFFER_SIZE = 64;
	private static final int DEFAULT_MAX_REDIRECT_COUNT = 16;

	protected ConnectionFactory _factory;
	protected String _userAgent;
	protected int _maxRedirect = DEFAULT_MAX_REDIRECT_COUNT;
	
	public HttpClient(ConnectionFactory factory) {
		_factory = factory;
		_userAgent = System.getProperty("browser.useragent");
	}
	
	public HttpResponse execute(HttpRequest request) throws IOException {
		return executeDirectConnection(request.getFullUrl(), 0, request);
	}
	
	public void setProxy(HttpProxy proxy) {
		
	}
	
	protected HttpResponse executeDirectConnection(String url, int redirectCount, HttpRequest request) throws IOException {
		if (_maxRedirect != -1 && redirectCount >= _maxRedirect) {
			throw new IOException("Exceeded max redirect count");
		}
		
		HttpResponse response = null;
		InputStream in = null;
		HttpConnection hconn = null;
		OutputStream stream = null;
		
		Dialog.alert(url);
		try {
			ConnectionDescriptor desc = _factory.getConnection(url);
			Connection conn = desc.getConnection();
			if (conn == null)
				throw new IOException("Unable to create connection");
			
			hconn = (HttpConnection) conn;
			hconn.setRequestMethod(request.getMethod());
			
			// Set Headers
			if (request._headers != null) {
				int size = request._headers.size();
				for (int i = 0; i < size; i++) {
					String key = request._headers.getPropertyKey(i);
					String value = request._headers.getPropertyValue(i);
					hconn.setRequestProperty(key, value);
				}
				
				if (_userAgent != null && request._headers.getPropertyValue(HttpProtocolConstants.HEADER_USER_AGENT) == null)
					hconn.setRequestProperty(HttpProtocolConstants.HEADER_USER_AGENT, _userAgent);
			}
			
			if (request._data != null) {
				//OutputStream out = hconn.openOutputStream();
				//out.write(request._dataBytes);
			}
			
			int responseCode = hconn.getResponseCode();
			Dialog.alert("Code " + responseCode);
			switch (responseCode) {
			case HttpConnection.HTTP_OK:
				try {
					in = hconn.openInputStream();
					long contentLength = hconn.getLength();
					stream = getRequestOutputStream(request, contentLength);
					
					
					dataPump(in, stream);
					
					response = new HttpResponse();
					response._responseCode = responseCode;
					response._headers = new Hashtable();
					
					
					
					String key = null, value = null;
					for (int index = 0; key != null; index++) {
						key = hconn.getHeaderFieldKey(index);
						if (key != null) {
							value = hconn.getHeaderField(index);
							response._headers.put(key, value);
						}
					}
					
				}
				finally {
					if (stream != null)
						closeRequestOutputStream(request, response, stream);
					
					if (in != null)
						in.close();
				}
				break;
				
			case HttpConnection.HTTP_TEMP_REDIRECT:
			case HttpConnection.HTTP_MOVED_TEMP:
			case HttpConnection.HTTP_MOVED_PERM:
				String location = hconn.getHeaderField(HttpProtocolConstants.HEADER_LOCATION);
				response = executeDirectConnection(location, redirectCount + 1, request);
			}
		}
		finally {
			if (hconn != null)
				hconn.close();
		}
		
		return response;
	}
	
	protected OutputStream getRequestOutputStream(HttpRequest request, long contentLength) throws IOException {
		if (request._output == null) {
			NoCopyByteArrayOutputStream bufferStream = null;
			if (contentLength != -1) {
				bufferStream = new NoCopyByteArrayOutputStream((int) contentLength);
			}
			else {
				bufferStream = new NoCopyByteArrayOutputStream();
			}
			
			return bufferStream;
		}
		else if (request._output instanceof FileConnection) {
			return ((FileConnection)request._output).openOutputStream();
		}
		
		return null;
	}
	
	protected void closeRequestOutputStream(HttpRequest request, HttpResponse response, OutputStream stream) {
		try {
			if (stream instanceof NoCopyByteArrayOutputStream) {
				response._response = ((NoCopyByteArrayOutputStream)stream).getByteArray();
				response._responseSize = ((NoCopyByteArrayOutputStream)stream).size();
			}
			stream.close();
		} catch (IOException e) {
		}
	}
	
	protected static String urlencode(String s) 
	{ 
	    if (s!=null) { 
	        StringBuffer tmp = new StringBuffer(); 
	        int i=0; 
	        try { 
	            while (true) { 
	                int b = (int)s.charAt(i++); 
	                if ((b>=0x30 && b<=0x39) || (b>=0x41 && b<=0x5A) || (b>=0x61 && b<=0x7A)) { 
	                    tmp.append((char)b); 
	                } 
	                else { 
	                    tmp.append("%"); 
	                    if (b <= 0xf) tmp.append("0"); 
	                    tmp.append(Integer.toHexString(b)); 
	                } 
	            } 
	        } 
	        catch (Exception e) {} 
	        return tmp.toString(); 
	    } 
	    return null; 
	} 

	
	private static void dataPump(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[DEFAULT_COPY_BUFFER_SIZE];
		dataPump(in, out, buffer);
	}
	
	/**
	 * Copy data from the input stream to the output stream
	 * @param in
	 * @param out
	 * @param buffer
	 * @throws IOException
	 */
	private static void dataPump(InputStream in, OutputStream out, byte[] buffer) throws IOException {
		int length = in.read(buffer);
		while (length != -1) {
			out.write(buffer, 0, length);
			length = in.read(buffer);
		}
	}
}
