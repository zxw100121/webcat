package org.followdream.http.impl.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer extends Thread {

	public static String WEB_ROOT = ""; // 默认的web根地址为当前目录
	public static String INDEX = "index.html"; // 默认的网页文件为"index.html"
	public static int fileBuffSize = 65536; // 文件缓存区大小
	
	public static final String serverName = "webcat"; // 服务器名称
	public static final String seerverVersion = "1.01"; // 服务器版本号

	private int port = 0; // 服务器端口号
	private String ip = null; // 服务器IP地址
	private ServerSocket serverSocket = null; // 服务器套机制

	/**
	 * 默认无参的构造方法
	 * @throws IOException
	 */
	public HttpServer() throws IOException {
		serverSocket = new ServerSocket(port);
		this.setPort(serverSocket.getLocalPort());
		this.setIp(serverSocket.getInetAddress().getHostAddress());
	}

	/**
	 * 带有一个参数port的构造方法
	 * @param port
	 * @throws IOException
	 */
	public HttpServer(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		this.setPort(port);
		this.setIp(serverSocket.getInetAddress().getHostAddress());
	}

	/**
	 * 带有3个参数的构造方法
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
	 * 重载线程的run方法
	 */
	@Override
	public void run() {
		super.run();
		if (this.serverSocket == null) {
			// 如果服务器套接字为空，直接返回退出
			return;
		}
		while (true) {
			try {
				Socket socket = serverSocket.accept(); // 接受客户端连接请求
				Thread connection = new HttpConnection(socket); // 创建客户端连接响应线程
				connection.start(); // 启动客户端连接响应线程
			} catch (IOException e) {
				// 网络连接异常
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 获取服务器的端口号
	 * @return 服务器的port
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * 设置服务器的端口
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 获取服务器的IP地址
	 * @return 服务器的ip
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * 设置服务器的IP
	 * @param ip
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}
}
