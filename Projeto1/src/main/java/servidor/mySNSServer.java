//import javax.net.SocketFactory;
//import javax.net.ssl.SSLSocketFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.util.Scanner;


class mySNSServer {

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
				//String[] nomesFicheiros = (String[]) inStream.readObject();
				String opcaoCliente = (String) inStream.readObject();
				String nomeUtente = "";
				if(!opcaoCliente.isBlank()) {
					if(opcaoCliente.equals("-m")) {
						String nomeMedico = (String) inStream.readObject();
						nomeUtente = (String) inStream.readObject();
						criaDiretorio(nomeMedico);
						criaDiretorio(nomeUtente);
					}
					else if(opcaoCliente.equals("-u")) {
						nomeUtente = (String) inStream.readObject();
						criaDiretorio(nomeUtente);
					}
					else {
						outStream.writeBoolean(false);
						System.out.println("opção incorreta pelo cliente");
					}
				}
				//Recebe o comando a se fazer
				String opcao = (String) inStream.readObject();
				switch(opcao) {
					case "-sc":
						int qtdFicheiros =(int) inStream.readObject();
						for(int i = 0; i < qtdFicheiros; i+=2){
							VerificaFicheiro(qtdFicheiros,inStream, outStream, nomeUtente);
							RecebeFicheiros(qtdFicheiros,nomeUtente,outStream,inStream);
						}
						break;
					case "-sa":
						//
					case "-se":
						//
					case "-g":
						//
					case "":
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException c) {
				c.printStackTrace();
			}
		}

		private void RecebeFicheiros(int qtdFicheiros,String utente, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException {

			Path diretorioUtente = Paths.get(utente);
			if (!Files.exists(diretorioUtente)) {
				Files.createDirectories(diretorioUtente);
			}
			// Recebe o arquivo cifrado
			File ficheiroCifrado = (File) in.readObject();
			// Define o caminho do arquivo cifrado no diretório do usuário
			Path caminhoFicheiroCifrado = diretorioUtente.resolve(ficheiroCifrado.getName());
			// Copia o arquivo cifrado para o diretório do usuário
			Files.copy(ficheiroCifrado.toPath(), caminhoFicheiroCifrado);

			// Recebe o arquivo da chave cifrada
			File chaveCifrada = (File) in.readObject();
			// Define o caminho do arquivo da chave cifrada no diretório do usuário
			Path caminhoChaveCifrada = diretorioUtente.resolve(chaveCifrada.getName());
			// Copia o arquivo da chave cifrada para o diretório do usuário
			Files.copy(chaveCifrada.toPath(), caminhoChaveCifrada);
		}


		// Cria o diretório do usuário se não existir

		public static void criaDiretorio(String nomeUtente){
			File diretorio = new File(nomeUtente);

			if (!diretorio.exists()) {
				// Se o diretório não existir, crie-o
				boolean criadoComSucesso = diretorio.mkdir();
				if (criadoComSucesso) {
					System.out.println("Diretório criado com sucesso.");
				} else {
					System.out.println("Falha ao criar o diretório.");
				}
			} else {
				System.out.println("O diretório já existe.");
			}
		}

		public static void VerificaFicheiro(int qtd, ObjectInputStream inStream, ObjectOutputStream outStream, String nomeUtente){
			try {
				String nomeFicheiro = (String) inStream.readObject();
				File arquivo = new File(nomeUtente +"/"+nomeFicheiro);
				outStream.writeObject(arquivo.exists());
			} catch (IOException | ClassNotFoundException e) {
				throw new RuntimeException(e);
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