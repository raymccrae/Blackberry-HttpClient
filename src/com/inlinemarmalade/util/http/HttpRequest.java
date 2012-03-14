
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

import java.util.Hashtable;

import net.rim.device.api.io.http.HttpHeaders;
import net.rim.device.api.io.http.HttpProtocolConstants;

public class HttpRequest {

	protected String _url;
	protected String _method;
	protected Hashtable _data;
	protected byte[] _dataBytes;
	protected HttpHeaders _headers;
	
	public static HttpRequest head(String url) {
		HttpRequest request = new HttpRequest();
		request._url = url;
		request._method = HttpProtocolConstants.HTTP_METHOD_HEAD;
		return request;
	}
	
	public static HttpRequest get(String url, HttpHeaders headers) {
		HttpRequest request = new HttpRequest();
		request._url = url;
		request._method = HttpProtocolConstants.HTTP_METHOD_GET;
		request._headers = headers;
		return request;
	}
	
	public static HttpRequest get(String url) {
		return get(url, null);
	}
	
	public String getUrl() {
		return _url;
	}
	
	public String getMethod() {
		return _method;
	}
	
}
