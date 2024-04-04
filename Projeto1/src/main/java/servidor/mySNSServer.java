package servidor;

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
                opcao = (String) inStream.readObject();
                int qtdFicheiros = 0;
                switch(opcao) {
                    case "-sc":
                        qtdFicheiros =(int) inStream.readObject();
                        for(int i = 0; i < qtdFicheiros; i+=2){
                            VerificaFicheiro(qtdFicheiros,inStream, outStream, nomeUtente);
                            RecebeFicheiros(qtdFicheiros,nomeUtente,outStream,inStream);
                        }
                        break;
                    case "-sa":
                        qtdFicheiros = inStream.readInt();
                        RecebeFicheirosGabriel(nomeUtente, nomeMedico, outStream,inStream, qtdFicheiros);
                        break;
                    case "-se":
                        break;
                        //
                    case "-g":
                        //qtdFicheiros = inStream.readInt();
                        System.out.println("inside g option");
                        String utente = inStream.readUTF(); // 1
                        System.out.println("nome utente:"+utente);
                        int numFiles = inStream.readInt(); // 2
                        System.out.println("num files:"+numFiles);
                        for (int i = 0; i < numFiles; i++) {
                            Boolean fileExists = (Boolean) inStream.readObject(); // 1-1 1-2
                            if(fileExists) {
                                String filename = (String) inStream.readUTF(); // 3
                                System.out.println("Verificacao do ficheiro:"+filename);
                                // Assuming signature file has the same name as the original file with ".assinatura" extension
                                String signatureFilename = filename + ".assinatura." + getMedicoName(utente, filename);
                                String signedFilename = filename + ".assinado";
                                enviarBytesGoption(utente, signedFilename, signatureFilename, outStream);
                            } else {
                                System.out.println("O ficheiro não existe no cliente.");
                                continue;
                            }
                            
                        }
                        break;
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

    public static void VerificaFicheiro(int qtd, ObjectInputStream inStream, ObjectOutputStream outStream, String nomeUtente){
        try {
            String nomeFicheiro = (String) inStream.readObject();
            File arquivo = new File(nomeUtente +"/"+nomeFicheiro);
            outStream.writeObject(arquivo.exists());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void enviarBytesGoption(String utente, String signedFilename, String signatureFilename, ObjectOutputStream out) {
        try {
            System.out.println("Entra no enviarBytesGoption");
            // Check if both files exist
            File signedFile = new File(utente + "/" + signedFilename);
            File signatureFile = new File(utente + "/" + signatureFilename);

            // Check if the signed file and signature file exist
            if (!signedFile.exists() || !signatureFile.exists()) {
                System.out.println("Um ou ambos os ficheiros não existem");
                return;
            }
            System.out.println("Todos os ficheiros .assinado e .assinatura existem");

            // Read signed file content
            byte[] signedBytes = Files.readAllBytes(Paths.get(utente, signedFilename));

            // Read signature file content
            byte[] signatureBytes = Files.readAllBytes(Paths.get(utente, signatureFilename));

            // Nome do medico
            String medicoName = getMedicoName(utente, signatureFilename);

            // Send signed file content to client
            out.writeObject(signedBytes); // 4
            out.flush();
            out.writeObject(signatureBytes); // 6
            out.flush();
            out.writeUTF(medicoName); // 7
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getMedicoName(String utente, String filename) {
        // Get the directory of the user
        File userDir = new File(utente);

        // Check if the user directory exists
        if (userDir.exists() && userDir.isDirectory()) {
            // List all files in the user directory
            File[] files = userDir.listFiles();

            // Iterate through the files
            for (File file : files) {
                // Check if the filename starts with the original filename
                if (file.getName().startsWith(filename)) {
                    // Split the filename by dots (.)
                    String[] parts = file.getName().split("\\.");

                    // Check if there are 4 parts after splitting
                    if (parts.length == 4) {
                        // Return the third part, which should be the medic's name
                        return parts[3];
                    }
                }
            }
        }
        // Return a default message if the medic's name cannot be found
        return "Diretorias do utente não existem / Ficheiro não tem nome do médico";
    }
}
