import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.io.*;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

public class client_app {
	Socket mySocket = null;
	static int Num_req = 1;
	static String cid;

	public static void main(String[] args) {
		client_app client = new client_app();
		System.out.println("사용자의 CID(NickName)를 설정해주세요. ※ 10자 이하");
		Scanner scanner = new Scanner(System.in);
		cid = scanner.nextLine();

		try {
			client.mySocket = new Socket("localhost", 55555);
			System.out.println("Client > 서버로 연결되었습니다.");

			Client c = new Client(client.mySocket);
			c.start();

		} catch (Exception e) {
			System.out.println("Connection Fail");
		}
	}
}

class Client extends Thread {
	Socket socket;
	String cid = "noname";
	boolean run = true;
	InputStream is;
	DataInputStream dis;
	String msg = "";
	String request_msg = "";
	OutputStream os;
	DataOutputStream dos;
	Scanner sn = new Scanner(System.in);

	Client(Socket _socket) {
		this.socket = _socket;
	}

	public void run() {
		try {
			is = socket.getInputStream();
			dis = new DataInputStream(is);
			os = socket.getOutputStream();
			dos = new DataOutputStream(os);
			Loss loss = new Loss();

			
			System.out.println("Command(Hi, CurrentTime, ConnectionTime, ClientList, Quit)※대소문자를 구분합니다.");
			while (run) {
				System.out.print("Command:");
				String command = sn.nextLine();
				if (command.equals("Hi")) {
					this.cid = client_app.cid;
					request_msg = Request("Hi");
					loss.Send(socket, request_msg);
				}

				else if (command.equals("CurrentTime")) {
					request_msg = Request("CurrentTime");
					loss.Send(socket, request_msg);
				}

				else if (command.equals("ConnectionTime")) {
					request_msg = Request("ConnectionTime");
					loss.Send(socket, request_msg);
				}

				else if (command.equals("ClientList")) {
					request_msg = Request("ClientList");
					loss.Send(socket, request_msg);
				}

				else if (command.equals("Quit")) {
					request_msg = Request("Quit");
					loss.Send(socket, request_msg);
					run = false;
				}

				else {
					request_msg = Request(command);
					loss.Send(socket, request_msg);
				}

				Timer ACK_timer = new Timer();
				TimerTask ACK_task = new TimerTask() {
					public void run() {
						loss.Send(socket, request_msg);
						System.out.println("ACK타임아웃!");
					}
				};
				
				ACK_timer.schedule(ACK_task, 500, 500);
				msg = dis.readUTF();
				String[] ACK = msg.split("///");
				while(!ACK[0].equals("ACK")){
					msg = dis.readUTF();
					ACK = msg.split("///");
				}
				ACK_timer.cancel();
				System.out.println(msg);

				// response
				
				Timer response_timer = new Timer();
				TimerTask response_task = new TimerTask() {
					public void run() {
						loss.Send(socket, request_msg);
						System.out.println("Response타임아웃!");
					}
				};
				
				response_timer.schedule(response_task, 500, 500);
				msg = dis.readUTF();
				String[] Response = msg.split("///");
				while(!Response[0].equals("Type:type2")) {
					msg = dis.readUTF();
					Response = msg.split("///");
				}
				response_timer.cancel();
				System.out.println(msg);
				System.out.println(Response[2]);
			}
			System.out.println("이용해주셔서 감사합니다.");
		} catch (Exception e) {
			System.out.println("Exception");
		}
	}

	String Request(String request) {
		String msg = "Type:type1///Request:" + request + "///cid:" + this.cid + "///Num_Req:" + client_app.Num_req
				+ "///END_MSG";
		client_app.Num_req++;
		return msg;
	}
}

class Loss {
	public void Send(Socket socket, String value) {
		if (((Math.random() * 10) + 1) % 10 < 7) {
			try {
				OutputStream os = socket.getOutputStream();
				DataOutputStream dos = new DataOutputStream(os);
				dos.writeUTF(value);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}