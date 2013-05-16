package org.followdream.http.impl.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import eu.medsea.mimeutil.MimeUtil;

public class HttpConnection extends Thread {

	private Socket socket = null;
	private InputStream in = null;
	private OutputStream out = null;
	private BufferedReader bufferedReader = null;
	private BufferedWriter bufferedWriter = null;
	private int fileBuffSize = HttpServer.fileBuffSize; // 文件缓存区大小

	private String method = null;
	private String action = null;
	private String Reason = null;
	private String Http_Version = null;
	private String Host = null;
	private String Connection = null;
	private String Cache_Control = null;
	private String Accept = null;
	private String User_Agent = null;
	private String Accept_Encoding = null;
	private String Accept_Language = null;
	private String Cookie = null;
	private int Content_Length = 0;
	private HashMap<String, String> datas = new HashMap<String, String>(); // 提交给服务器的数据集

	private static final byte CR = '\r';
	private static final byte LF = '\n';
	private static final String CRLF = "\r\n";

	/**
	 * 默认无参的构造方法
	 */
	public HttpConnection() {
		this(null);
	}

	/**
	 * 含有一个与客户端连接的套接字的构造方法
	 * 
	 * @param socket
	 */
	public HttpConnection(Socket socket) {
		this.socket = socket;
		// 注册mineUtil类，用于判断文件类型
		MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
	}

	/**
	 * 重载线程的run方法
	 */
	@Override
	public void run() {
		super.run();
		if (this.socket == null) {
			// 如果套接字为空，直接退出当期线程
			return;
		}
		try {
			// 获取输入输出流
			in = socket.getInputStream();
			out = socket.getOutputStream();
			bufferedReader = new BufferedReader(new InputStreamReader(in));
			// bufferedWriter = new BufferedWriter(new OutputStreamWriter(out));
			String requestLine = null; // 请求行
			requestLine = bufferedReader.readLine(); // 获取http的请求行
			// 将%E4%BD%A0转换为汉字
			requestLine = unescape(requestLine);
			String[] requestStrings = requestLine.split(" "); // 以空格分隔请求头部
			method = requestStrings[0]; // 请求方式
			action = requestStrings[1]; // 请求的action
			Http_Version = requestStrings[2]; // http协议的版本
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.length() == 0 || line.equals("")) {
					break;
				}
				String[] s = line.split(" ");
				if (s[0].equals("Host:")) {
					Host = s[1];
				}
				if (s[0].equals("Connection:")) {
					Connection = s[1];
				}
				if (s[0].equals("Cache-Control:")) {
					Cache_Control = s[1];
				}
				if (s[0].equals("Accept:")) {
					Accept = s[1];
				}
				if (s[0].equals("User-Agent:")) {
					User_Agent = s[1];
				}
				if (s[0].equals("Accept-Encoding:")) {
					Accept_Encoding = s[1];
				}
				if (s[0].equals("Accept-Language:")) {
					Accept_Language = s[1];
				}
				if (s[0].equals("Cookie:")) {
					Cookie = s[1];
				}
				if (s[0].equals("Content-Length:")) {
					Content_Length = Integer.parseInt(s[1]);
				}
			}
			if ("GET".equalsIgnoreCase(method)) {
				doGet();
			}
			if ("POST".equalsIgnoreCase(method)) {
				doPost();
			}
		} catch (IOException e) {
			// 网络连接异常
			e.printStackTrace();
		} finally {
			// 清空本次连接的数据集
			datas.clear();
			// 关闭网络套接字连接
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
				if (socket != null) {
					socket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 将%E4%BD%A0转换为汉字
	 * @param s
	 * @return
	 */
	protected String unescape(String s) {
		if (s == null) {
			return null;
		}
		StringBuffer sbuf = new StringBuffer();
		int l = s.length();
		int ch = -1;
		int b, sumb = 0;
		for (int i = 0, more = -1; i < l; i++) {
			/* Get next byte b from URL segment s */
			switch (ch = s.charAt(i)) {
			case '%':
				ch = s.charAt(++i);
				int hb = (Character.isDigit((char) ch) ? ch - '0'
						: 10 + Character.toLowerCase((char) ch) - 'a') & 0xF;
				ch = s.charAt(++i);
				int lb = (Character.isDigit((char) ch) ? ch - '0'
						: 10 + Character.toLowerCase((char) ch) - 'a') & 0xF;
				b = (hb << 4) | lb;
				break;
			case '+':
				b = ' ';
				break;
			default:
				b = ch;
			}
			/* Decode byte b as UTF-8, sumb collects incomplete chars */
			if ((b & 0xc0) == 0x80) { // 10xxxxxx (continuation byte)
				sumb = (sumb << 6) | (b & 0x3f); // Add 6 bits to sumb
				if (--more == 0)
					sbuf.append((char) sumb); // Add char to sbuf
			} else if ((b & 0x80) == 0x00) { // 0xxxxxxx (yields 7 bits)
				sbuf.append((char) b); // Store in sbuf
			} else if ((b & 0xe0) == 0xc0) { // 110xxxxx (yields 5 bits)
				sumb = b & 0x1f;
				more = 1; // Expect 1 more byte
			} else if ((b & 0xf0) == 0xe0) { // 1110xxxx (yields 4 bits)
				sumb = b & 0x0f;
				more = 2; // Expect 2 more bytes
			} else if ((b & 0xf8) == 0xf0) { // 11110xxx (yields 3 bits)
				sumb = b & 0x07;
				more = 3; // Expect 3 more bytes
			} else if ((b & 0xfc) == 0xf8) { // 111110xx (yields 2 bits)
				sumb = b & 0x03;
				more = 4; // Expect 4 more bytes
			} else /* if ((b & 0xfe) == 0xfc) */{ // 1111110x (yields 1 bit)
				sumb = b & 0x01;
				more = 5; // Expect 5 more bytes
			}
			/* We don't test if the UTF-8 encoding is well-formed */
		}
		return sbuf.toString();
	}

	/**
	 * doPost方法，处理post请求
	 * @throws IOException
	 */
	private void doPost() throws IOException {
		if (Content_Length != 0) {
			int b = 0;
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			while ((Content_Length--) != 0) {
				b = bufferedReader.read();
				byteArrayOutputStream.write(b);
			}
			// 获取以post方式提交给服务器的数据集
			String[] postData = byteArrayOutputStream.toString().split("&");
			for (int i = 0; i < postData.length; i++) {
				String[] data = postData[i].split("=");
				if (data.length == 2) {
					datas.put(data[0], data[1]);
				}
			}
		}
		// 调用doGet方法
		doGet();
	}

	/**
	 * doGet方法，处理get请求
	 * @throws IOException
	 */
	private void doGet() throws IOException {
		int p = action.indexOf('?');
		if (p == -1) { // 判断url中是否带有数据集
			Reason = action;
		} else {
			Reason = action.substring(0, p);
			// 获取以get方式提交给服务器的数据集
			String[] getData = action.substring(p + 1).split("&");
			for (int i = 0; i < getData.length; i++) {
				String[] data = getData[i].split("=");
				if (data.length == 2) {
					datas.put(data[0], data[1]);
				}
			}
		}
		// 获得请求的文件或目录的操作对象
		File file = new File(HttpServer.WEB_ROOT, Reason);
		if (file.exists()) { // 判断请求的文件或目录是否存在
			if (file.isFile()) { // 如果请求的是文件则读取该文件并返回给客户端
				Collection<?> mimeTypes = MimeUtil.getMimeTypes(file); // 判断文件类型
				String head = Http_Version + " 200 OK" + CRLF + "Server: "
						+ HttpServer.serverName + " "
						+ HttpServer.seerverVersion + CRLF + "Content-length: "
						+ file.length() + CRLF + "Content-type: " + mimeTypes
						+ CRLF + CRLF; // HTTP头部
				out.write(head.getBytes()); // 返回HTTP头部
				FileInputStream fileInputStream = new FileInputStream(file); // 获取请求文件的输入流
				int len = 0;
				byte[] b = new byte[fileBuffSize];
				while ((len = fileInputStream.read(b, 0, fileBuffSize)) != -1) { // 读取文件内容并发送给请求者
					out.write(b, 0, len);
				}
				out.flush(); // 刷新输出缓存区
				fileInputStream.close(); // 关闭文件输入流
			} else if (file.isDirectory()) { // 如果请求的是目录则返回目录结构的html文件
				String fileName[] = file.list(); // 获取请求目录的文件列表
				StringBuffer contentBuffer = new StringBuffer();
				contentBuffer
						.append("<html><head><title>HttpServer</title></head><body>");
				contentBuffer.append("<h1>Index of " + Reason + "</h1>");
				contentBuffer
						.append("<ul><li><a href='..'>Parent Directory</a></li>");
				for (int i = 0; i < fileName.length; i++) {
					contentBuffer.append("<li><a href=\"" + fileName[i]
							+ "/\">" + fileName[i] + "</a></li>");
				}
				contentBuffer.append("</ul></body></html>");
				byte[] content = contentBuffer.toString().getBytes();
				String head = Http_Version + " 200 OK" + CRLF + "Server: "
						+ HttpServer.serverName + " "
						+ HttpServer.seerverVersion + CRLF + "Content-length: "
						+ content.length + CRLF + "Content-type: text/html"
						+ CRLF + CRLF;
				out.write(head.getBytes());
				out.write(content);
				out.flush();
			}
		} else { // 如果请求的文件或目录不存在则返回404
			String head = Http_Version + " 404 NOT FOUND" + CRLF + "Server: "
					+ HttpServer.serverName + " " + HttpServer.seerverVersion
					+ CRLF + "Content-length: 0" + CRLF
					+ "Content-type: text/plain" + CRLF + CRLF;
			out.write(head.getBytes());
			out.flush(); // 刷新文件输出流
		}
	}
}
