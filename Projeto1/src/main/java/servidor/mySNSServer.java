package servidor;

import javax.crypto.*;
//import javax.net.SocketFactory;
//import javax.net.ssl.SSLSocketFactory;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
//import java.util.Scanner;
import java.util.Map;
public class mySNSServer {
	
	public static void main(String[] args) {
		System.out.println("servidor: main");
		if (args.length==1 && isConvertibleToInt(args[0])) {
			mySNSServer server = new mySNSServer();
			server.startServer(Integer.parseInt(args[0]));
		}
		else {
			System.out.println("AVISO: Estrutura do comando incorreta!");
			System.out.println("ESTRUTURA CORRETA: java mySNSServer <portNumber>");
			System.out.println("EXEMPLO: java mySNSServer 57160");
			System.exit(0);
		}
		
	}
	
	public void startServer(int port) {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
		
		while (true) {
			try {
				Socket inSoc = serverSocket.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	class ServerThread extends Thread {
		private Socket socket = null;
		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}
		public void run() {
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
				String[] nomesFicheiros = (String[]) inStream.readObject();
				if (nomesFicheiros.length!=0) {
					if (nomesFicheiros[0].equals("REQ")) {
						//sendFiles(socket,outStream,inStream,removerPrimeiroElemento(nomesFicheiros));
					} 
					else {
						//receiveFiles(socket,outStream,inStream,nomesFicheiros);
					}
				}
				else {
					String[] arrayVazio = {};
					outStream.writeObject(arrayVazio);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException c) {
				c.printStackTrace();
			}
		}
		
		
	}
	
	
	
	public static boolean isConvertibleToInt(String str) {
	    try {
	        Integer.parseInt(str);
	        return true;
	    } catch (NumberFormatException e) {
	        return false;
	    }
	}
	
	
	
}