
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

import net.rim.device.api.io.NoCopyByteArrayOutputStream;
import net.rim.device.api.io.http.HttpProtocolConstants;
import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.io.transport.ConnectionFactory;

public class HttpClient {
	
	private static final int DEFAULT_COPY_BUFFER_SIZE = 64;

	protected ConnectionFactory _factory;
	protected String _userAgent;
	protected int _maxRedirect = 16;
	
	public HttpClient(ConnectionFactory factory) {
		_factory = factory;
		_userAgent = System.getProperty("browser.useragent");
	}
	
	public HttpResponse execute(HttpRequest request) throws IOException {
		return execute(request._url, 0, request);
	}
	
	protected HttpResponse execute(String url, int redirectCount, HttpRequest request) throws IOException {
		if (_maxRedirect != -1 && redirectCount >= _maxRedirect) {
			throw new IOException("Exceeded max redirect count");
		}
		
		HttpResponse response = null;
		InputStream in = null;
		NoCopyByteArrayOutputStream bufferStream = null;
		ConnectionDescriptor desc = _factory.getConnection(url);
		Connection conn = desc.getConnection();
		
		HttpConnection hconn = (HttpConnection) conn;
		hconn.setRequestMethod(request.getMethod());
		
		// Set Headers
		if (request._headers != null) {
			int size = request._headers.size();
			for (int i = 0; i < size; i++) {
				String key = request._headers.getPropertyKey(i);
				String value = request._headers.getPropertyValue(i);
				hconn.setRequestProperty(key, value);
			}
			
			if (request._headers.getPropertyValue(HttpProtocolConstants.HEADER_USER_AGENT) == null)
				hconn.setRequestProperty(HttpProtocolConstants.HEADER_USER_AGENT, _userAgent);
		}
		
		if (request._dataBytes != null) {
			OutputStream out = hconn.openOutputStream();
			out.write(request._dataBytes);
		}
		
		int responseCode = hconn.getResponseCode();
		
		switch (responseCode) {
		case HttpConnection.HTTP_OK:
			try {
				in = hconn.openInputStream();
				bufferStream = null;
				
				try {
					// Pre-size the byte array if the content length is available.
					int contentLength = (int) hconn.getLength();
					if (contentLength != -1) {
						bufferStream = new NoCopyByteArrayOutputStream(contentLength);
					}
				}
				catch (NumberFormatException e) {
				}
				finally {
					if (bufferStream == null)
						bufferStream = new NoCopyByteArrayOutputStream();
				}
				
				dataPump(in, bufferStream);
				
				response = new HttpResponse();
				response._responseCode = responseCode;
				response._response = bufferStream.getByteArray();
				response._responseSize = bufferStream.size();
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
				if (bufferStream != null)
					bufferStream.close();
				
				if (in != null)
					in.close();
			}
			break;
			
		case HttpConnection.HTTP_TEMP_REDIRECT:
		case HttpConnection.HTTP_MOVED_TEMP:
		case HttpConnection.HTTP_MOVED_PERM:
			String location = hconn.getHeaderField(HttpProtocolConstants.HEADER_LOCATION);
			response = execute(location, redirectCount + 1, request);
		}
		
		
		hconn.close();
		
		return response;
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
		int length = 0;
		while (length != -1) {
			length = in.read(buffer);
			if (length != -1)
				out.write(buffer, 0, length);
		}
	}
}
