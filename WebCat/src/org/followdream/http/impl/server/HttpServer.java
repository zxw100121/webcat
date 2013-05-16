package org.followdream.http.impl.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer extends Thread {

	public static String WEB_ROOT = ""; // Ĭ�ϵ�web����ַΪ��ǰĿ¼
	public static String INDEX = "index.html"; // Ĭ�ϵ���ҳ�ļ�Ϊ"index.html"
	public static int fileBuffSize = 65536; // �ļ���������С
	
	public static final String serverName = "webcat"; // ����������
	public static final String seerverVersion = "1.01"; // �������汾��

	private int port = 0; // �������˿ں�
	private String ip = null; // ������IP��ַ
	private ServerSocket serverSocket = null; // �������׻���

	/**
	 * Ĭ���޲εĹ��췽��
	 * @throws IOException
	 */
	public HttpServer() throws IOException {
		serverSocket = new ServerSocket(port);
		this.setPort(serverSocket.getLocalPort());
		this.setIp(serverSocket.getInetAddress().getHostAddress());
	}

	/**
	 * ����һ������port�Ĺ��췽��
	 * @param port
	 * @throws IOException
	 */
	public HttpServer(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		this.setPort(port);
		this.setIp(serverSocket.getInetAddress().getHostAddress());
	}

	/**
	 * ����3�������Ĺ��췽��
	 * @param port
	 * @param backlog
	 * @param bindAddr
	 * @throws IOException
	 */
	public HttpServer(int port, int backlog, InetAddress bindAddr)
			throws IOException {
		serverSocket = new ServerSocket(port, backlog, bindAddr);
		this.setPort(port);
		this.setIp(serverSocket.getInetAddress().getHostAddress());
	}

	/**
	 * �����̵߳�run����
	 */
	@Override
	public void run() {
		super.run();
		if (this.serverSocket == null) {
			// ����������׽���Ϊ�գ�ֱ�ӷ����˳�
			return;
		}
		while (true) {
			try {
				Socket socket = serverSocket.accept(); // ���ܿͻ�����������
				Thread connection = new HttpConnection(socket); // �����ͻ���������Ӧ�߳�
				connection.start(); // �����ͻ���������Ӧ�߳�
			} catch (IOException e) {
				// ���������쳣
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * ��ȡ�������Ķ˿ں�
	 * @return ��������port
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * ���÷������Ķ˿�
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * ��ȡ��������IP��ַ
	 * @return ��������ip
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * ���÷�������IP
	 * @param ip
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}
}
