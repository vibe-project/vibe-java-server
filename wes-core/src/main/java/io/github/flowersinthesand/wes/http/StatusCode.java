/*
 * Copyright 2013 Donghwan Kim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.flowersinthesand.wes.http;

/**
 * Represents the HTTP status code and reason phrase.
 * 
 * @see <a
 *      href="http://www.iana.org/assignments/http-status-codes/http-status-codes.xhtml">HTTP
 *      Status Code Registry</a>
 */
public class StatusCode {

	// 1xx: Informational - Request received, continuing process
	/**
	 * {@code 100 Continue}
	 */
	public static final StatusCode CONTINUE = new StatusCode(100, "Continue");
	/**
	 * {@code 101 Switching Protocols}
	 */
	public static final StatusCode SWITCHING_PROTOCOLS = new StatusCode(101, "Switching Protocols");
	/**
	 * {@code 102 Processing}
	 */
	public static final StatusCode PROCESSING = new StatusCode(102, "Processing");
	
	// 2xx: Success - The action was successfully received, understood, and accepted
	/**
	 * {@code 200 OK}
	 */
	public static final StatusCode OK = new StatusCode(200, "OK");
	/**
	 * {@code 201 Created}
	 */
	public static final StatusCode CREATED = new StatusCode(201, "Created");
	/**
	 * {@code 202 Accepted}
	 */
	public static final StatusCode ACCEPTED = new StatusCode(202, "Accepted");
	/**
	 * {@code 203 Non-Authoritative Information}
	 */
	public static final StatusCode NON_AUTHORITATIVE_INFORMATION = new StatusCode(203, "Non-Authoritative Information");
	/**
	 * {@code 204 No Content}
	 */
	public static final StatusCode NO_CONTENT = new StatusCode(204, "No Content");
	/**
	 * {@code 205 Reset Content}
	 */
	public static final StatusCode RESET_CONTENT = new StatusCode(205, "Reset Content");
	/**
	 * {@code 206 Partial Content}
	 */
	public static final StatusCode PARTIAL_CONTENT = new StatusCode(206, "Partial Content");
	/**
	 * {@code 207 Multi-Status}
	 */
	public static final StatusCode MULTI_STATUS = new StatusCode(207, "Multi-Status");
	/**
	 * {@code 208 Already Reported}
	 */
	public static final StatusCode ALREADY_REPORTED = new StatusCode(208, "Already Reported");
	/**
	 * {@code 226 IM Used}
	 */
	public static final StatusCode IM_USED = new StatusCode(226, "IM Used");
	
	// 3xx: Redirection - Further action must be taken in order to complete the request
	/**
	 * {@code 300 Multiple Choices}
	 */
	public static final StatusCode MULTIPLE_CHOICES = new StatusCode(300, "Multiple Choices");
	/**
	 * {@code 301 Moved Permanently}
	 */
	public static final StatusCode MOVED_PERMANENTLY = new StatusCode(301, "Moved Permanently");
	/**
	 * {@code 302 Found}
	 */
	public static final StatusCode FOUND = new StatusCode(302, "Found");
	/**
	 * {@code 303 See Other}
	 */
	public static final StatusCode SEE_OTHER = new StatusCode(303, "See Other");
	/**
	 * {@code 304 Not Modified}
	 */
	public static final StatusCode NOT_MODIFIED = new StatusCode(304, "Not Modified");
	/**
	 * {@code 305 Use Proxy}
	 */
	public static final StatusCode USE_PROXY = new StatusCode(305, "Use Proxy");
	/**
	 * {@code 306 Reserved}
	 */
	public static final StatusCode RESERVED = new StatusCode(306, "Reserved");
	/**
	 * {@code 307 Temporary Redirect}
	 */
	public static final StatusCode TEMPORARY_REDIRECT = new StatusCode(307, "Temporary Redirect");
	/**
	 * {@code 308 Permanent Redirect}
	 */
	public static final StatusCode PERMANENT_REDIRECT = new StatusCode(308, "Permanent Redirect");
	
	// 4xx: Client Error - The request contains bad syntax or cannot be fulfilled
	/**
	 * {@code 400 Bad Request}
	 */
	public static final StatusCode BAD_REQUEST = new StatusCode(400, "Bad Request");
	/**
	 * {@code 401 Unauthorized}
	 */
	public static final StatusCode UNAUTHORIZED = new StatusCode(401, "Unauthorized");
	/**
	 * {@code 402 Payment Required}
	 */
	public static final StatusCode PAYMENT_REQUIRED = new StatusCode(402, "Payment Required");
	/**
	 * {@code 403 Forbidden}
	 */
	public static final StatusCode FORBIDDEN = new StatusCode(403, "Forbidden");
	/**
	 * {@code 404 Not Found}
	 */
	public static final StatusCode NOT_FOUND = new StatusCode(404, "Not Found");
	/**
	 * {@code 405 Method Not Allowed}
	 */
	public static final StatusCode METHOD_NOT_ALLOWED = new StatusCode(405, "Method Not Allowed");
	/**
	 * {@code 406 Not Acceptable}
	 */
	public static final StatusCode NOT_ACCEPTABLE = new StatusCode(406, "Not Acceptable");
	/**
	 * {@code 407 Proxy Authentication Required}
	 */
	public static final StatusCode PROXY_AUTHENTICATION_REQUIRED = new StatusCode(407, "Proxy Authentication Required");
	/**
	 * {@code 408 Request Timeout}
	 */
	public static final StatusCode REQUEST_TIMEOUT = new StatusCode(408, "Request Timeout");
	/**
	 * {@code 409 Conflict}
	 */
	public static final StatusCode CONFLICT = new StatusCode(409, "Conflict");
	/**
	 * {@code 410 Gone}
	 */
	public static final StatusCode GONE = new StatusCode(410, "Gone");
	/**
	 * {@code 411 Length Required}
	 */
	public static final StatusCode LENGTH_REQUIRED = new StatusCode(411, "Length Required");
	/**
	 * {@code 412 Precondition Failed}
	 */
	public static final StatusCode PRECONDITION_FAILED = new StatusCode(412, "Precondition Failed");
	/**
	 * {@code 413 Request Entity Too Large}
	 */
	public static final StatusCode REQUEST_ENTITY_TOO_LARGE = new StatusCode(413, "Request Entity Too Large");
	/**
	 * {@code 414 Request-URI Too Long}
	 */
	public static final StatusCode REQUEST_URI_TOO_LONG = new StatusCode(414, "Request-URI Too Long");
	/**
	 * {@code 415 Unsupported Media Type}
	 */
	public static final StatusCode UNSUPPORTED_MEDIA_TYPE = new StatusCode(415, "Unsupported Media Type");
	/**
	 * {@code 416 Requested Range Not Satisfiable}
	 */
	public static final StatusCode REQUESTED_RANGE_NOT_SATISFIABLE = new StatusCode(416, "Requested Range Not Satisfiable");
	/**
	 * {@code 417 Expectation Failed}
	 */
	public static final StatusCode EXPECTATION_FAILED = new StatusCode(417, "Expectation Failed");
	/**
	 * {@code 422 Unprocessable Entity}
	 */
	public static final StatusCode UNPROCESSABLE_ENTITY = new StatusCode(422, "Unprocessable Entity");
	/**
	 * {@code 423 Locked}
	 */
	public static final StatusCode LOCKED = new StatusCode(423, "Locked");
	/**
	 * {@code 424 Failed Dependency}
	 */
	public static final StatusCode FAILED_DEPENDENCY = new StatusCode(424, "Failed Dependency");
	/**
	 * {@code 426 Upgrade Required}
	 */
	public static final StatusCode UPGRADE_REQUIRED = new StatusCode(426, "Upgrade Required");
	/**
	 * {@code 428 Precondition Required}
	 */
	public static final StatusCode PRECONDITION_REQUIRED = new StatusCode(428, "Precondition Required");
	/**
	 * {@code 429 Too Many Requests}
	 */
	public static final StatusCode TOO_MANY_REQUESTS = new StatusCode(429, "Too Many Requests");
	/**
	 * {@code 431 Request Header Fields Too Large}
	 */
	public static final StatusCode REQUEST_HEADER_FIELDS_TOO_LARGE = new StatusCode(431, "Request Header Fields Too Large");

	// 5xx: Server Error - The server failed to fulfill an apparently valid
	// request
	/**
	 * {@code 500 Internal Server Error}
	 */
	public static final StatusCode INTERNAL_SERVER_ERROR = new StatusCode(500, "Internal Server Error");
	/**
	 * {@code 501 Not Implemented}
	 */
	public static final StatusCode NOT_IMPLEMENTED = new StatusCode(501, "Not Implemented");
	/**
	 * {@code 502 Bad Gateway}
	 */
	public static final StatusCode BAD_GATEWAY = new StatusCode(502, "Bad Gateway");
	/**
	 * {@code 503 Service Unavailable}
	 */
	public static final StatusCode SERVICE_UNAVAILABLE = new StatusCode(503, "Service Unavailable");
	/**
	 * {@code 504 Gateway Timeout}
	 */
	public static final StatusCode GATEWAY_TIMEOUT = new StatusCode(504, "Gateway Timeout");
	/**
	 * {@code 505 HTTP Version Not Supported}
	 */
	public static final StatusCode HTTP_VERSION_NOT_SUPPORTED = new StatusCode(505, "HTTP Version Not Supported");
	/**
	 * {@code 506 Variant Also Negotiates (Experimental)}
	 */
	public static final StatusCode VARIANT_ALSO_NEGOTIATES = new StatusCode(506, "Variant Also Negotiates (Experimental)");
	/**
	 * {@code 507 Insufficient Storage}
	 */
	public static final StatusCode INSUFFICIENT_STORAGE = new StatusCode(507, "Insufficient Storage");
	/**
	 * {@code 508 Loop Detected}
	 */
	public static final StatusCode LOOP_DETECTED = new StatusCode(508, "Loop Detected");
	/**
	 * {@code 510 Not Extended}
	 */
	public static final StatusCode NOT_EXTENDED = new StatusCode(510, "Not Extended");
	/**
	 * {@code 511 Network Authentication Required}
	 */
	public static final StatusCode NETWORK_AUTHENTICATION_REQUIRED = new StatusCode(511, "Network Authentication Required");
	
	private int code;
	private String reason;

	/**
	 * Creates a status with the given status code.
	 */
	public StatusCode(int code) {
		this(code, null);
	}

	/**
	 * Creates a status with the given status code and reason.
	 */
	public StatusCode(int code, String reason) {
		this.code = code;
		this.reason = reason;
	}

	/**
	 * Returns the status code.
	 */
	public int code() {
		return code;
	}

	/**
	 * Returns the reason phrase.
	 */
	public String reason() {
		return reason;
	}

	/**
	 * Creates a status with new reason.
	 */
	public StatusCode newReason(String reason) {
		return new StatusCode(code, reason);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + code;
		result = prime * result + ((reason == null) ? 0 : reason.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StatusCode other = (StatusCode) obj;
		if (code != other.code)
			return false;
		if (reason == null) {
			if (other.reason != null)
				return false;
		} else if (!reason.equals(other.reason))
			return false;
		return true;
	}

}