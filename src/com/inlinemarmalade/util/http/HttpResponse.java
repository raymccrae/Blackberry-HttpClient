
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

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Vector;

import net.rim.device.api.io.http.HttpProtocolConstants;

public class HttpResponse {
	
	/**
	 * Default text encoding used for converting response body to text
	 * when no charset is specified.
	 */
	public static final String DEFAULT_TEXT_ENCODING = "UTF-8";

	protected int _responseCode;
	protected byte[] _response;
	protected int _responseSize;
	protected Hashtable _headers;
	
	/**
	 * Get the HTTP response code.
	 * @return The response code
	 */
	public int getResponseCode() {
		return _responseCode;
	}
	
	/**
	 * Get the response body as a text string. The content is converted using the specified
	 * text encoding.
	 * @param encoding
	 * @return The response body converted to a string.
	 * @throws UnsupportedEncodingException
	 */
	public String getResponseText(String encoding) throws UnsupportedEncodingException {
		return new String(_response, 0, _responseSize, encoding);
	}
	
	/**
	 * Get the response body as a text string. The content is converted using the charset
	 * specified in the Content-Type header of the response. If no charset is given then
	 * UTF-8 is used.
	 * @return The response body converted to a string.
	 * @throws UnsupportedEncodingException
	 */
	public String getResponseText() throws UnsupportedEncodingException {
		String encoding = DEFAULT_TEXT_ENCODING;
		
		// Check if encoding is specified in the content-type charset
		if (_headers.containsKey(HttpProtocolConstants.HEADER_CONTENT_TYPE)) {
			String contentType = getFirstHeaderValue(HttpProtocolConstants.HEADER_CONTENT_TYPE);
			int index = contentType.indexOf("charset");
			if (index != -1) {
				encoding = contentType.substring(index + 8);
			}
		}
		
		return getResponseText(encoding);
	}
	
	public boolean containsHeader(String header) {
		return _headers.contains(header);
	}
	
	public String getFirstHeaderValue(String header) {
		String value = null;
		
		if (_headers.contains(header)) {
			Object val = _headers.get(header);
			if (val instanceof Vector)
				value = (String) ((Vector)val).elementAt(0);
			else if (val instanceof String)
				value = (String) val;
			else
				value = val.toString();
		}
		
		return value;
	}
}
