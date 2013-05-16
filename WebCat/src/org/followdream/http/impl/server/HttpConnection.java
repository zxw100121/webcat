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
	private int fileBuffSize = HttpServer.fileBuffSize; // �ļ���������С

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
	private HashMap<String, String> datas = new HashMap<String, String>(); // �ύ�������������ݼ�

	private static final byte CR = '\r';
	private static final byte LF = '\n';
	private static final String CRLF = "\r\n";

	/**
	 * Ĭ���޲εĹ��췽��
	 */
	public HttpConnection() {
		this(null);
	}

	/**
	 * ����һ����ͻ������ӵ��׽��ֵĹ��췽��
	 * 
	 * @param socket
	 */
	public HttpConnection(Socket socket) {
		this.socket = socket;
		// ע��mineUtil�࣬�����ж��ļ�����
		MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
	}

	/**
	 * �����̵߳�run����
	 */
	@Override
	public void run() {
		super.run();
		if (this.socket == null) {
			// ����׽���Ϊ�գ�ֱ���˳������߳�
			return;
		}
		try {
			// ��ȡ���������
			in = socket.getInputStream();
			out = socket.getOutputStream();
			bufferedReader = new BufferedReader(new InputStreamReader(in));
			// bufferedWriter = new BufferedWriter(new OutputStreamWriter(out));
			String requestLine = null; // ������
			requestLine = bufferedReader.readLine(); // ��ȡhttp��������
			// ��%E4%BD%A0ת��Ϊ����
			requestLine = unescape(requestLine);
			String[] requestStrings = requestLine.split(" "); // �Կո�ָ�����ͷ��
			method = requestStrings[0]; // ����ʽ
			action = requestStrings[1]; // �����action
			Http_Version = requestStrings[2]; // httpЭ��İ汾
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
			// ���������쳣
			e.printStackTrace();
		} finally {
			// ��ձ������ӵ����ݼ�
			datas.clear();
			// �ر������׽�������
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
	 * ��%E4%BD%A0ת��Ϊ����
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
	 * doPost����������post����
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
			// ��ȡ��post��ʽ�ύ�������������ݼ�
			String[] postData = byteArrayOutputStream.toString().split("&");
			for (int i = 0; i < postData.length; i++) {
				String[] data = postData[i].split("=");
				if (data.length == 2) {
					datas.put(data[0], data[1]);
				}
			}
		}
		// ����doGet����
		doGet();
	}

	/**
	 * doGet����������get����
	 * @throws IOException
	 */
	private void doGet() throws IOException {
		int p = action.indexOf('?');
		if (p == -1) { // �ж�url���Ƿ�������ݼ�
			Reason = action;
		} else {
			Reason = action.substring(0, p);
			// ��ȡ��get��ʽ�ύ�������������ݼ�
			String[] getData = action.substring(p + 1).split("&");
			for (int i = 0; i < getData.length; i++) {
				String[] data = getData[i].split("=");
				if (data.length == 2) {
					datas.put(data[0], data[1]);
				}
			}
		}
		// ���������ļ���Ŀ¼�Ĳ�������
		File file = new File(HttpServer.WEB_ROOT, Reason);
		if (file.exists()) { // �ж�������ļ���Ŀ¼�Ƿ����
			if (file.isFile()) { // �����������ļ����ȡ���ļ������ظ��ͻ���
				Collection<?> mimeTypes = MimeUtil.getMimeTypes(file); // �ж��ļ�����
				String head = Http_Version + " 200 OK" + CRLF + "Server: "
						+ HttpServer.serverName + " "
						+ HttpServer.seerverVersion + CRLF + "Content-length: "
						+ file.length() + CRLF + "Content-type: " + mimeTypes
						+ CRLF + CRLF; // HTTPͷ��
				out.write(head.getBytes()); // ����HTTPͷ��
				FileInputStream fileInputStream = new FileInputStream(file); // ��ȡ�����ļ���������
				int len = 0;
				byte[] b = new byte[fileBuffSize];
				while ((len = fileInputStream.read(b, 0, fileBuffSize)) != -1) { // ��ȡ�ļ����ݲ����͸�������
					out.write(b, 0, len);
				}
				out.flush(); // ˢ�����������
				fileInputStream.close(); // �ر��ļ�������
			} else if (file.isDirectory()) { // ����������Ŀ¼�򷵻�Ŀ¼�ṹ��html�ļ�
				String fileName[] = file.list(); // ��ȡ����Ŀ¼���ļ��б�
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
		} else { // ���������ļ���Ŀ¼�������򷵻�404
			String head = Http_Version + " 404 NOT FOUND" + CRLF + "Server: "
					+ HttpServer.serverName + " " + HttpServer.seerverVersion
					+ CRLF + "Content-length: 0" + CRLF
					+ "Content-type: text/plain" + CRLF + CRLF;
			out.write(head.getBytes());
			out.flush(); // ˢ���ļ������
		}
	}
}
