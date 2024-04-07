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
                        qtdFicheiros = inStream.readInt();
                        // Recebe ficheiro cifrado
                        recebeFicheiros(nomeUtente,nomeMedico,outStream,inStream, qtdFicheiros);
                        // Recebe chave secreta
                        recebeFicheiros(nomeUtente, nomeMedico, outStream,inStream, qtdFicheiros);
                        break;
                    case "-sa":
                        qtdFicheiros = inStream.readInt();
                        // Recebe ficheiro assinado
                        recebeFicheiros(nomeUtente, nomeMedico, outStream,inStream, qtdFicheiros);
                        // Recebe assinatura do ficheiro
                        recebeFicheiros(nomeUtente, nomeMedico, outStream,inStream, qtdFicheiros);
                        break;
                    case "-se":
                        qtdFicheiros = inStream.readInt();
                        // Recebe a assinatura do ficheiro
                        recebeFicheiros(nomeUtente, nomeMedico, outStream,inStream, qtdFicheiros);
                        // Recebe o ficheiro seguro
                        recebeFicheiros(nomeUtente, nomeMedico, outStream,inStream, qtdFicheiros);
                        // Recebe a chave secreta
                        recebeFicheiros(nomeUtente, nomeMedico, outStream,inStream, qtdFicheiros);
                        break;
                    case "-g":
                        //qtdFicheiros = inStream.readInt();
                        String utente = inStream.readUTF(); // 1
                        System.out.println("nome utente:"+utente);
                        int numFiles = inStream.readInt(); // 2
                        System.out.println("num files:"+numFiles);
                        for (int i = 0; i < numFiles; i++) {
                            String filename = (String) inStream.readUTF(); // 3
                            System.out.println("Verificacao do ficheiro:"+filename);
                            String medico = getMedicoName(utente, filename);
                            if (!medico.equals("Diretorias do utente não existem / Ficheiro não tem nome do médico")) {
                                nomeMedico = medico;
                            } else {
                                nomeMedico = "";
                            }

                            // Assuming signature file has the same name as the original file with ".assinatura" extension
                            String signatureFilename = filename + ".assinatura." + nomeMedico;
                            String signedFilename = filename + ".assinado";
                            String cifradoFilename = filename + ".cifrado";
                            String chaveSecretaFilename = filename + ".chave_secreta." + nomeUtente;
                            String seguroFilename = filename + ".seguro";

                            Boolean verifiedSignature = verificaFicheiroServer(signatureFilename, utente, nomeMedico);
                            Boolean verifiedSigned = verificaFicheiroServer(signedFilename, utente, nomeMedico);
                            Boolean verifiedCifrado = verificaFicheiroServer(cifradoFilename, utente, nomeMedico);
                            Boolean verifiedChaveSecreta = verificaFicheiroServer(chaveSecretaFilename, utente, nomeMedico);
                            Boolean verifiedSeguro = verificaFicheiroServer(seguroFilename, utente, nomeMedico);

                            Boolean signatureSignedCheck = false;
                            Boolean chaveCifradoCheck = false;
                            Boolean seguroCheck = false;

                            if (verifiedSignature && verifiedSigned) {
                                signatureSignedCheck = true;
                                outStream.writeBoolean(signatureSignedCheck);
                                outStream.flush();
                                enviarAssinaturaGoption(utente, signedFilename, signatureFilename, outStream);
                            } else {
                                outStream.writeBoolean(signatureSignedCheck);
                                outStream.flush();
                            }

                            if (verifiedCifrado && verifiedChaveSecreta) {
                                chaveCifradoCheck = true;
                                outStream.writeBoolean(chaveCifradoCheck);
                                outStream.flush();
                                EnviaChaveSecreta(filename, utente, nomeMedico, outStream, inStream);
                                EnviaFicheiroCifrado(filename, utente, nomeMedico, outStream, inStream);
                            } else {
                                outStream.writeBoolean(chaveCifradoCheck);
                                outStream.flush();
                            }

                            if (verifiedSeguro && verifiedChaveSecreta && verifiedSignature) {
                                seguroCheck = true;
                                outStream.writeBoolean(seguroCheck);
                                outStream.flush();
                                EnviaChaveSecreta(filename, utente, nomeMedico, outStream, inStream);
                                EnviaFicheiroSeguro(filename, utente, nomeMedico, outStream, inStream);
                                EnviaFicheiroAssinatura(filename, utente, nomeMedico, outStream, inStream);
                            } else {
                                outStream.writeBoolean(seguroCheck);
                                outStream.flush();
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
        private boolean verificaFicheiroServer(String filename, String utente, String medico) throws IOException {
            File diretorio = new File(utente + "/" + filename);
            if(diretorio.exists()){
                return true;
            }
            else{
                return false;
            }
        }
        private void salvarFicheiro(String filename, byte[] fileBytes, String utente) {
            try (FileOutputStream fileOutputStream = new FileOutputStream(utente+ "/" +filename)) {
                fileOutputStream.write(fileBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private boolean recebeFicheiros(String utente, String medico, ObjectOutputStream out, ObjectInputStream in, int quantidadeFicheiros){
            try{
                for(int i = 0; i < quantidadeFicheiros; i++) {
                    // Receber o nome do ficheiro
                    String filename = (String) in.readUTF();
                    if (filename.equals("NOK")) {
                        System.out.println("O servidor não recebeu o ficheiro");
                        continue;
                    }
                    // Verifica se o ficheiro existe
                    boolean ficheiroJaExiste = verificaFicheiroServer(filename, utente, medico);
                    // Envia para o cliente acerca da existencia do ficheiro
                    out.writeBoolean(ficheiroJaExiste);
                    out.flush();
                    if(ficheiroJaExiste){
                        continue;
                    }
                    // Recebe tamanho do ficheiro
                    long fileSize = (long) in.readLong();
                    System.out.println("Recebi tamanho: " + fileSize);
                    // Recebe o conteúdo do ficheiro como um array de bytes
                    byte[] fileBytes = (byte[]) in.readObject();
                    if(fileBytes.length != fileSize){
                        out.writeUTF("NOK");
                        System.out.println("O servidor recebeu um ficheiro com tamanho diferente do esperado.");
                        continue;
                    }
                    else{
                        out.writeUTF("OK");
                    }
                    // Salvar o conteúdo do ficheiro em um novo arquivo
                    salvarFicheiro(filename, fileBytes, utente);
                    // Confirmar ao cliente que o ficheiro foi recebido com sucesso
                    out.writeBoolean(true);
                    out.flush();
                }
                return true;
            }catch (Exception e){
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

    private void enviarAssinaturaGoption(String utente, String signedFilename, String signatureFilename, ObjectOutputStream out) {
        try {
            System.out.println("Entra no enviarBytesGoption");
            // Check if both files exist
            File signedFile = new File(utente + "/" + signedFilename);
            File signatureFile = new File(utente + "/" + signatureFilename);

            // Check if the signed file and signature file exist
            if (!signedFile.exists() || !signatureFile.exists()) {
                System.out.println("Um ou ambos os ficheiros não existem");
                out.writeUTF("Um ou ambos os ficheiros não existem no servidor");
                out.flush();
                return;
            } else {
                System.out.println("Todos os ficheiros .assinado e .assinatura existem");

                // If the files exist, send a simple acknowledgment to the client
                out.writeUTF("Ficheiros .assinado e .assinatura existem no servidor");
                out.flush();
            }

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

    private void EnviaFicheiroCifrado(String fileName, String utente, String medico, ObjectOutputStream outputStream, ObjectInputStream inputStream){
            try (FileInputStream fis = new FileInputStream(utente+"/"+fileName+".cifrado");
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

                // Crie um buffer para ler os bytes do arquivo
                byte[] buffer = new byte[2048];
                int bytesRead;

                // Leia os bytes do arquivo e escreva-os no ByteArrayOutputStream
                while ((bytesRead = fis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }

                // Obtenha os bytes lidos do ByteArrayOutputStream
                byte[] fileBytes = bos.toByteArray();

                // Faça o que quiser com o array de bytes, como enviar pela rede, etc.
                // Neste exemplo, apenas exibiremos o tamanho do array de bytes
                System.out.println("Tamanho do arquivo em bytes: " + fileBytes.length);
                outputStream.writeObject(fileBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void EnviaChaveSecreta(String fileName, String utente, String medico, ObjectOutputStream outputStream, ObjectInputStream inputStream) throws IOException {
            // boolean existeFicheiroCifrado = verificaFicheiroServer(fileName+".cifrado", utente, medico);
            boolean existeFicheiroChaveSecreta = verificaFicheiroServer(fileName+".chave_secreta."+utente, utente, medico);
            outputStream.writeBoolean(existeFicheiroChaveSecreta);
            outputStream.flush(); // Certifique-se de esvaziar o buffer para garantir que os dados sejam enviados imediatamente

            try (FileInputStream fis = new FileInputStream(utente+"/"+fileName+".chave_secreta."+utente);

                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

                // Crie um buffer para ler os bytes do arquivo
                byte[] buffer = new byte[2048];
                int bytesRead;

                // Leio os bytes do arquivo
                while ((bytesRead = fis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
                // Obtenha os bytes lidos do ByteArrayOutputStream
                byte[] fileBytes = bos.toByteArray();

                //(TODO) Neste caso apenas printa o size, mas temos que enviar o size
                System.out.println("Tamanho do arquivo em bytes: " + fileBytes.length);
                outputStream.writeObject(fileBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void EnviaFicheiroSeguro(String fileName, String utente, String medico, ObjectOutputStream outputStream, ObjectInputStream inputStream){
            try (FileInputStream fis = new FileInputStream(utente+"/"+fileName+".seguro");
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

                // Crie um buffer para ler os bytes do arquivo
                byte[] buffer = new byte[2048];
                int bytesRead;

                // Leia os bytes do arquivo e escreva-os no ByteArrayOutputStream
                while ((bytesRead = fis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }

                // Obtenha os bytes lidos do ByteArrayOutputStream
                byte[] fileBytes = bos.toByteArray();

                // Faça o que quiser com o array de bytes, como enviar pela rede, etc.
                // Neste exemplo, apenas exibiremos o tamanho do array de bytes
                System.out.println("Tamanho do arquivo em bytes: " + fileBytes.length);
                outputStream.writeObject(fileBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void EnviaFicheiroAssinatura(String fileName, String utente, String medico, ObjectOutputStream outputStream, ObjectInputStream inputStream){
            try (FileInputStream fis = new FileInputStream(utente+"/"+fileName+".assinatura."+medico);
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                System.out.println("entra envia assinatura");
                // Crie um buffer para ler os bytes do arquivo
                byte[] buffer = new byte[2048];
                int bytesRead;

                // Leia os bytes do arquivo e escreva-os no ByteArrayOutputStream
                while ((bytesRead = fis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }

                // Obtenha os bytes lidos do ByteArrayOutputStream
                byte[] fileBytes = bos.toByteArray();

                // Faça o que quiser com o array de bytes, como enviar pela rede, etc.
                // Neste exemplo, apenas exibiremos o tamanho do array de bytes
                System.out.println("Tamanho do arquivo em bytes: " + fileBytes.length);
                outputStream.writeObject(fileBytes);
                outputStream.flush();

                outputStream.writeUTF(medico);
                outputStream.flush();
            } catch (IOException e) {   
                e.printStackTrace();
            }
        }

        private boolean verificaFicheiroServer(String filename, String utente, String medico) throws IOException {
            File diretorio = new File(utente + "/" + filename);
            if(diretorio.exists()){
                return true;
            }
            else{
                return false;
            }
        }
}