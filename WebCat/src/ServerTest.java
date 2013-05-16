import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

import org.followdream.http.impl.server.HttpServer;


public class ServerTest {
	
	public static void main(String[] args) {
		try {
			// 新建HttpServer类
			HttpServer httpServer = new HttpServer(9999);
			// 设置网页文件根地址
			HttpServer.WEB_ROOT = "F:";
			// 启动服务器
			httpServer.start();
			System.out.println("server port = " + httpServer.getPort());
			System.out.println("seerver ip = " + httpServer.getIp());
//			Scanner scanner = new Scanner(System.in);
//			String s = null;
//			while(true) {
//				s = scanner.next();
//				Map map=Thread.getAllStackTraces();
//				System.out.println(map.size());
//			}
//			Scanner scanner = new Scanner(System.in);
//			String x = scanner.next();
//			if(x.equals("exit")) {
//				System.exit(0);
//			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
