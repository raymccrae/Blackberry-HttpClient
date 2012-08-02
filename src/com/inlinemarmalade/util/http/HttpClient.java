//#preprocess

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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connection;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.SocketConnection;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.io.MalformedURIException;
import net.rim.device.api.io.NoCopyByteArrayOutputStream;
import net.rim.device.api.io.URI;
import net.rim.device.api.io.http.HttpProtocolConstants;

/**
 * HttpClient 
 *
 */
public class HttpClient {
	
	private static final int DEFAULT_COPY_BUFFER_SIZE = 64;
	private static final int DEFAULT_MAX_REDIRECT_COUNT = 16;

	protected com.inlinemarmalade.util.http.ConnectionFactory _factory;

	protected String _userAgent;
	protected int _maxRedirect = DEFAULT_MAX_REDIRECT_COUNT;
	protected HttpProxy _proxy;
	
	public HttpClient() {
		this(new com.inlinemarmalade.util.http.ConnectionFactory());
	}
	
	public HttpClient(com.inlinemarmalade.util.http.ConnectionFactory factory) {
	
		_factory = factory;
		_userAgent = System.getProperty("browser.useragent");
	}
	
	public HttpResponse execute(HttpRequest request) throws IOException {
		return executeDirectConnection(request.getFullUrl(), 0, request);
	}
	
	public void setProxy(HttpProxy proxy) {
		_proxy = proxy;
	}
	
	protected HttpResponse executeProxyConnection(String url, int redirectCount, HttpRequest request) throws IOException, IllegalArgumentException, MalformedURIException {
		if (_maxRedirect != -1 && redirectCount >= _maxRedirect) {
			throw new IOException("Exceeded max redirect count");
		}
		
		HttpResponse response = null;
		InputStream in = null;
		HttpConnection hconn = null;
		OutputStream out = null;
		
		URI uri = URI.create(url);
		
		String proxyUrl = "socket://" + _proxy._host + ":" + _proxy._port;
		try {
			Connection conn = _factory.getConnection(proxyUrl);

			if (conn == null)
				throw new IOException("Unable to create connection");
			SocketConnection sconn = (SocketConnection) conn;
			
			StringBuffer buf = new StringBuffer();
			buf.append(request._method);
			buf.append(' ');
			buf.append(url);
			buf.append(" HTTP/1.1");
			buf.append("\r\n");
			buf.append("Host: ");
			buf.append(uri.getHost());
			buf.append("\r\n");
			buf.append("\r\n");
			
			out = sconn.openOutputStream();
			out.write(buf.toString().getBytes());
			out.close();
			out = null;
			
			in = sconn.openInputStream();
			InputStreamReader reader = new InputStreamReader(in);
			
			response = new HttpResponse();
			response._headers = new Hashtable();
			
			buf.setLength(0);
			int ch = reader.read();
			while (ch != -1) {
				buf.append((char) ch);
				ch = reader.read();
				
				if (ch == '\r') {
					ch = reader.read();
					if (ch == '\n') {
						parseHeader(buf.toString(), response._headers);
						buf.setLength(0);
						
						ch = reader.read();
						if (ch == '\r') {
							ch = reader.read();
							if (ch == '\n') {
								break;
							}
						}
					}
					else {
						buf.append('\r');
						buf.append((char) ch);
					}
				}
			}
		}
		finally {
			if (hconn != null)
				hconn.close();
		}
		
		return response;
	}
	
	protected HttpResponse executeDirectConnection(String url, int redirectCount, HttpRequest request) throws IOException {
		if (_maxRedirect != -1 && redirectCount >= _maxRedirect) {
			throw new IOException("Exceeded max redirect count");
		}
		
		HttpResponse response = null;
		InputStream in = null;
		OutputStream out = null;
		HttpConnection hconn = null;
		OutputStream stream = null;
		
		try {
			Connection conn = _factory.getConnection(url);
			
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
				
				// If User-Agent is not set the set it
				if (_userAgent != null && request._headers.getPropertyValue(HttpProtocolConstants.HEADER_USER_AGENT) == null)
					hconn.setRequestProperty(HttpProtocolConstants.HEADER_USER_AGENT, _userAgent);
			}
			
			// If this is a POST or PUT request then write post data
			if (request._method.equals("POST") || request._method.equals("PUT")) {
				if (request._headers.getPropertyValues(HttpProtocolConstants.HEADER_CONTENT_TYPE) == null) {
					String contentType = request.getContentType();
					if (contentType != null)
						hconn.setRequestProperty(HttpProtocolConstants.HEADER_CONTENT_TYPE, contentType);
				}
				if (request._headers.getPropertyValues(HttpProtocolConstants.HEADER_CONTENT_LENGTH) == null) {
					long length = request.getContentLength();
					if (length != -1)
						hconn.setRequestProperty(HttpProtocolConstants.HEADER_CONTENT_LENGTH, String.valueOf(length));
				}
				
				out = hconn.openOutputStream();
				request.writePostData(out);
				out.close();
				out = null;
			}
			
			int responseCode = hconn.getResponseCode();

			switch (responseCode) {
			case HttpConnection.HTTP_OK:
				try {
					in = hconn.openInputStream();
					long contentLength = hconn.getLength();
					stream = getRequestOutputStream(request, contentLength);
					
					// Copy data from http connection
					dataPump(in, stream);
					
					response = new HttpResponse();
					response._responseCode = responseCode;
					response._headers = new Hashtable();
					
					// Read Response headers
					String key = "", value = null;
					for (int index = 0; key != null; index++) {
						key = hconn.getHeaderFieldKey(index);
						if (key != null) {
							value = hconn.getHeaderField(index);
							
							// If the header is duplicated store the values in a vector
							if (response._headers.contains(key)) {
								Object obj = response._headers.get(key);
								if (obj instanceof Vector) {
									((Vector)obj).addElement(value);
								}
								else {
									Vector v = new Vector();
									v.addElement(obj);
									v.addElement(value);
									response._headers.put(key, v);
								}
							}
							else { // else store the single string
								response._headers.put(key, value);
							}
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
			if (out != null)
				out.close();
			
			if (in != null)
				in.close();
			
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
		else if (request._output instanceof OutputStream) {
			return (OutputStream) request._output;
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
	
	public static String urlencode(String s) 
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

	private void parseHeader(String line, Hashtable headers) {
		
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
