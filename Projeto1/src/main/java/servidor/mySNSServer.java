package servidor;

//import javax.net.SocketFactory;
//import javax.net.ssl.SSLSocketFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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
                String nomeMedico = "";
                if(!opcaoCliente.isBlank()) {
                    if(opcaoCliente.equals("-m")) {
                        nomeMedico = (String) inStream.readObject();
                        nomeUtente = (String) inStream.readObject();
                        criaDiretorio(nomeMedico);
                        criaDiretorio(nomeUtente);
                    }
                    else if(opcaoCliente.equals("-u")) {
                        outStream.writeObject(true);
                        nomeUtente = (String) inStream.readObject();
                        criaDiretorio(nomeUtente);
                    }
                    else {
                        outStream.writeBoolean(false);
                        System.out.println("opção incorreta pelo cliente");
                    }
                }
                //Recebe o comando a se fazer
                String opcao = "";
                if(opcaoCliente.equals("-m")){
                    opcao = (String) inStream.readObject();
                }
                else{
                    opcao = "-g";
                }
                int qtdFicheiros = 0;
                switch(opcao) {
                    case "-sc":
                        qtdFicheiros =(int) inStream.readObject();
                        VerificaFicheiros(qtdFicheiros,inStream, outStream, nomeUtente);
                        RecebeFicheiros(nomeUtente,outStream,inStream);
                        break;
                    case "-sa":
                        qtdFicheiros = inStream.readInt();
                        RecebeFicheirosGabriel(nomeUtente, nomeMedico, outStream,inStream, qtdFicheiros);
                        break;
                    case "-se":
                        //
                    case "-g":
                        qtdFicheiros = inStream.readInt();
                        //nomeUtente = inStream.readUTF();

						//TODO verificar nome de ficheiro dentro do for no goption do cliente, e se existir mandar para o cliente o ficheiro
                        verificaExiste("teste.txt", "maria", qtdFicheiros);
                    case "":
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ClassNotFoundException c) {
                c.printStackTrace();
            }
        }

        private void RecebeFicheiros(String utente, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException {
            File diretorioUtente = new File(utente);
            File ficheiroCifrado = (File) in.readObject();
            File chaveCifrada = (File) in.readObject();
        }

        private void RecebeFicheirosGabriel(String utente, String medico, ObjectOutputStream out, ObjectInputStream in, int quantidadeFicheiros) throws IOException, ClassNotFoundException {
            for(int i = 1; i < quantidadeFicheiros + 1; i++) {
                File diretorioUtente = new File(utente);
                // Receber o nome do arquivo
                String filename = (String) in.readUTF();
                System.out.println(filename);

                boolean ficheiroJaExiste = verificaFicheiroAssinadoServer(filename, utente, medico);
                out.writeBoolean(ficheiroJaExiste);
                out.flush();
                if(ficheiroJaExiste){
                    continue;
                }

                // Receber o conteúdo do arquivo como um array de bytes
                byte[] fileBytes = (byte[]) in.readObject();

                // Salvar o conteúdo do arquivo em um novo arquivo
                salvarArquivo(filename, fileBytes, utente);

                // Confirmar ao cliente que o arquivo foi recebido com sucesso
                out.writeBoolean(true);
                out.flush();

                // Receber nome do arquivo assinado
                String signatureFilename = (String) in.readUTF();

                // Recebe conteudo da assinatura
                byte[] signatureBytes = (byte[]) in.readObject();

                // Salvar o conteudo da assinatura em um novo arquivo
                salvarArquivo(signatureFilename, signatureBytes, utente);

                // Confirma para o cliente que a assinatura foi recebida corretamente
                out.writeBoolean(true);
                out.flush();
            }

        }

        private void salvarArquivo(String filename, byte[] fileBytes, String utente) {
            try (FileOutputStream fileOutputStream = new FileOutputStream(utente+ "/" +filename)) {
                fileOutputStream.write(fileBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private boolean verificaFicheiroAssinadoServer(String filename, String utente, String medico) throws IOException {
            File diretorio = new File(utente + "/" + filename);
            if(diretorio.exists()){
                return true;
            }
            else{
                return false;
            }
        }

        private boolean verificaExiste(String filename, String utente, int qtdFicheiros){
            File directory = new File("utente/");

            // Define a FilenameFilter to accept files with names containing "teste.txt"
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith(filename);
                }
            };

            // List files in the directory matching the filter
            File[] matchingFiles = directory.listFiles(filter);

            // Check if any matching file is found
            if (matchingFiles != null && matchingFiles.length > 0) {
                System.out.println("Files matching the pattern found:");
                for (File file : matchingFiles) {
                    System.out.println(file.getName());
                }
            } else {
                System.out.println("No files matching the pattern found.");
            }
            return true;
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

    public static void VerificaFicheiros(int qtd, ObjectInputStream inStream, ObjectOutputStream outStream, String nomeUtente){
        try {
            for(int i =1; i<qtd;i++){
                String nomeFicheiro = (String) inStream.readObject();
                File arquivo = new File(nomeUtente +"/"+nomeFicheiro);
                outStream.writeObject(arquivo.exists());
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}