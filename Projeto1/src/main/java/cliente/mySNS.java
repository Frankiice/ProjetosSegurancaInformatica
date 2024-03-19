package cliente;

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

public class mySNS {
	public static void main(String[] args) throws Exception,IOException, ClassNotFoundException {
		if (args.length < 5) {
            System.out.println("Argumentos invalidos (ex: -a <serverAddress> -u <username do utente> -g {<filenames>}+).");
            System.exit(0);        
        }
		if (!args[0].equals("-a")) {
            System.out.println("Eh preciso fornecer a opcao -a (ex: -a <serverAddress> )");
            System.exit(0);
        }
		String[] parts = args[1].split(":");
        if (parts.length != 2) {
            System.out.println("Eh preciso fornecer um endereco IP e o porto do servidor (ex: 127.0.0.1:23456).");
            System.exit(0);
        }
        //System.out.println("servidor:" + args[1]);
        //criar socket para ligar ao server
        Socket echoSocket = new Socket(parts[0], Integer.parseInt(parts[1])); //127.0.0.1
        //criar stream object
        ObjectInputStream in = new ObjectInputStream(echoSocket.getInputStream()); 
        ObjectOutputStream out = new ObjectOutputStream(echoSocket.getOutputStream());
        
        String opcaoUsername = args[2];
        String opcao = "";
        out.writeObject(opcaoUsername);
        if(opcaoUsername.equals("-m")) {
        	opcao = args[6];
        }
        if(opcaoUsername.equals("-u")) {
        	opcao = args[4];
        }
        switch(opcao) {
	        case "-sc":
	        	//
	        case "-sa":
	        	//
	        case "-se":
	        	//
	        case "-g":
	        	//
	        case "":
        }
        
	}
}