package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class RequestParser {
	private static final String[][] HttpReplies = { { "100", "Continue" },
			{ "101", "Switching Protocols" }, { "200", "OK" },
			{ "201", "Created" }, { "202", "Accepted" },
			{ "203", "Non-Authoritative Information" },
			{ "204", "No Content" }, { "205", "Reset Content" },
			{ "206", "Partial Content" }, { "300", "Multiple Choices" },
			{ "301", "Moved Permanently" }, { "302", "Found" },
			{ "303", "See Other" }, { "304", "Not Modified" },
			{ "305", "Use Proxy" }, { "306", "(Unused)" },
			{ "307", "Temporary Redirect" }, { "400", "Bad Request" },
			{ "401", "Unauthorized" }, { "402", "Payment Required" },
			{ "403", "Forbidden" }, { "404", "Not Found" },
			{ "405", "Method Not Allowed" }, { "406", "Not Acceptable" },
			{ "407", "Proxy Authentication Required" },
			{ "408", "Request Timeout" }, { "409", "Conflict" },
			{ "410", "Gone" }, { "411", "Length Required" },
			{ "412", "Precondition Failed" },
			{ "413", "Request Entity Too Large" },
			{ "414", "Request-URI Too Long" },
			{ "415", "Unsupported Media Type" },
			{ "416", "Requested Range Not Satisfiable" },
			{ "417", "Expectation Failed" },
			{ "500", "Internal Server Error" }, { "501", "Not Implemented" },
			{ "502", "Bad Gateway" }, { "503", "Service Unavailable" },
			{ "504", "Gateway Timeout" },
			{ "505", "HTTP Version Not Supported" } };

	private BufferedReader reader;
	private String method, url;

	private String cType;
	private HashMap<String, String> headers, parameters;
	private int[] ver;

	private int index;

	public RequestParser(InputStream is) {
		reader = new BufferedReader(new InputStreamReader(is));
		method = "";
		url = "";
		cType = "";
		headers = new HashMap<String, String>();
		parameters = new HashMap<String, String>();
		ver = new int[2];
	}

	public int parseRequest() throws IOException {
		String initial, cmd[], temp[];
		int ret;

		ret = 200; // default is OK now
		initial = reader.readLine(); // First Http Line
		if (initial == null || initial.length() == 0)
			return 0;
		if (Character.isWhitespace(initial.charAt(0))) {
			// starting whitespace, return bad request
			return 400;
		}

		cmd = initial.split("\\s"); // split use \s mean space.
		if (cmd.length != 3) {
			return 400; // Must be 3 words like 'GET' 'URI' 'HTTP/1.1'.
		}

		// version detect
		if (cmd[2].indexOf("HTTP/") == 0 && cmd[2].indexOf('.') > 5) {
			temp = cmd[2].substring(5).split("\\.");
			// checking HTTP version. use '.'.
			try {
				ver[0] = Integer.parseInt(temp[0]);
				ver[1] = Integer.parseInt(temp[1]);
			} catch (NumberFormatException nfe) {
				ret = 400;
			}
		} else
			ret = 400;
		if (cmd[0].equals("GET") || cmd[0].equals("HEAD")) {
			method = cmd[0];
			index = cmd[1].indexOf("?");
			url = URLDecoder.decode(cmd[1], "UTF-8");
			if (!(index < 0)) {
				url = URLDecoder.decode(cmd[1], "UTF-8");
				String requestPath = url.substring(0, index); 
				String params = url.substring(index+1);
				System.out.println(requestPath);
				System.out.println(params);
				parameters = HttpRequestUtils.parseQueryString(params);
			}
			parseHeaders();
			
			
			if (headers == null) {
				ret = 400;
			} else if (cmd[0].equals("POST")) {
//				ret = 501; // not implemented
//				IOUtils.readData(reader, contentLength)
				
				
				
			} else if (ver[0] == 1 && ver[1] >= 1) {
				if (cmd[0].equals("OPTIONS") || cmd[0].equals("PUT")
						|| cmd[0].equals("DELETE") || cmd[0].equals("TRACE")
						|| cmd[0].equals("CONNECT")) {
					ret = 501; // not implemented
				}
			} else {
				// meh not understand, bad request
				ret = 400;
			}
			if (ver[0] == 1 && ver[1] >= 1 && getHeader("Host") == null) {
				ret = 400;
			}
		}
		String accept = getHeader("Accept");
		String[] tmp = accept.split("\\,");
		cType = tmp[0];
		return ret;
	}

	private void parseHeaders() throws IOException {
		String line;
		int idx;

		// that fscking rfc822 allows multiple lines, we don't care now
		line = reader.readLine();
		while (!line.equals("")) {
			idx = line.indexOf(':'); // count ':'
			if (idx < 0) {
				headers = null;
				break;
			} else {
				headers.put(line.substring(0, idx).toLowerCase(), line
						.substring(idx + 1).trim());
			}
			line = reader.readLine();
		}
	}

	public String getMethod() {
		return method;
	}

	public String getHeader(String key) {
		if (headers != null)
			return (String) headers.get(key.toLowerCase());
		else
			return null;
	}

	public HashMap<String, String> getHeaders() {
		return headers;
	}

	public String getRequestURL() {
		return url;
	}

	public String getParam(String key) {
		return (String) parameters.get(key);
	}

	public HashMap<String, String> getParams() {
		return parameters;
	}

	public String getVersion() {
		return ver[0] + "." + ver[1];
	}

	public int compareVersion(int major, int minor) {
		if (major < ver[0])
			return -1;
		else if (major > ver[0])
			return 1;
		else if (minor < ver[1])
			return -1;
		else if (minor > ver[1])
			return 1;
		else
			return 0;
	}

	public static String getHttpReply(int codevalue) {
		String key, ret;
		int i;

		ret = null;
		key = "" + codevalue;
		for (i = 0; i < HttpReplies.length; i++) {
			if (HttpReplies[i][0].equals(key)) {
				ret = codevalue + " " + HttpReplies[i][1];
				break;
			}
		}

		return ret;
	}

	public static String getDateHeader() {
		SimpleDateFormat format;
		String ret;

		format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.KOREA);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		ret = "Date: " + format.format(new Date()) + " GMT";
		return ret;
	}

	public  String getConetentType() {
		return cType;
	}
}
