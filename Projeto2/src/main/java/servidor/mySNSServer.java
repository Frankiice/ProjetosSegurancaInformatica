package servidor;

//import javax.net.SocketFactory;
//import javax.net.ssl.SSLSocketFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
//import java.util.Scanner;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;


class mySNSServer {

    private static final String PATH_TO_MAC = "src/main/java/servidor/mac.txt";
    private static final String PATH_TO_USERS = "src/main/java/servidor/users.txt";

    private static  String ADMIN_PASSWORD;

    public static void main(String[] args) throws NoSuchAlgorithmException {
        System.setProperty("javax.net.ssl.keyStore", "src/main/java/servidor/server.keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        iniciarAdmin();
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
            ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
            serverSocket = ssf.createServerSocket(port);
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
                System.out.println(opcaoCliente);
                // para as opçoes do projeto 1
                String nomeUtente = "";
                String nomeMedico = "";

                // para as opçoes do projeto 2
                String username = "";
                String password = "";
                byte[] certificado = null;
                if(!opcaoCliente.equals("")) {
                    if(opcaoCliente.equals("-m")) {
                        nomeMedico = (String) inStream.readObject();
                        nomeUtente = (String) inStream.readObject();
                        // criaDiretorio(nomeMedico);
                        // criaDiretorio(nomeUtente);

                        username = nomeMedico;

                        password = (String) inStream.readObject();
                    }
                    else if(opcaoCliente.equals("-u")) {
                        outStream.writeObject(true);
                        nomeUtente = (String) inStream.readObject();
                        password = (String) inStream.readObject();
                        // criaDiretorio(nomeUtente);

                        username = nomeUtente;
                    }
                    else if(opcaoCliente.equals("-au")) {
                        username = (String) inStream.readUTF();
                        password = (String) inStream.readUTF();
                        certificado = (byte[]) inStream.readObject();
                        criaDiretorio(username);
                    }
                    else {
                        outStream.writeBoolean(false);
                        System.out.println("opção incorreta pelo cliente");
                    }
                }
                //Recebe o comando a se fazer
                String opcao = "";
                if (!opcaoCliente.equals("-au")) {
                    opcao = (String) inStream.readObject();
                } else {
                    opcao = "-au";
                }
                int qtdFicheiros = 0;
                if ("-sc".equals(opcao) || "-sa".equals(opcao) || "-se".equals(opcao)) {
                    System.out.println(Paths.get("src/main/java/servidor/" + nomeUtente).toFile().exists());
                    if (!Paths.get("src/main/java/servidor/" + nomeUtente).toFile().exists()) {
                        System.out.println("O utilizador " + nomeUtente + " não existe");

                        outStream.writeBoolean(true);
                        outStream.flush();

                        outStream.writeObject("o utente nao existe");
                        outStream.flush();

                        outStream.writeBoolean(true);
                        outStream.flush();

                        outStream.writeObject("o utente nao existe");
                        outStream.flush();
                    } else if (!doMacValidation(ADMIN_PASSWORD)) {
                        System.out.println("O mac do servidor e invalido");

                        outStream.writeBoolean(true);
                        outStream.flush();

                        outStream.writeObject("O mac do servidor e invalido");
                        outStream.flush();

                        outStream.writeBoolean(true);
                        outStream.flush();

                        outStream.writeObject("O mac do servidor e invalido");
                        outStream.flush();

                        System.exit(0);
                    } else {
                        qtdFicheiros = inStream.readInt();
                        // Recebe ficheiro cifrado, assinado ou seguro
                        //recebeFicheiros(nomeUtente, nomeMedico, outStream, inStream, qtdFicheiros);
                        if ("-sc".equals(opcao)) {
                            // Recebe chave secreta
                            if (validatePassword(username, password)) {
                                recebeFicheiros(nomeUtente, nomeMedico, outStream, inStream, qtdFicheiros);
                                recebeFicheiros(nomeUtente, nomeMedico, outStream, inStream, qtdFicheiros);
                            } else {
                                String filename = (String) inStream.readUTF();
                                System.out.println("Não foi possivel receber o/os ficheiro/os cifrados");
                                System.out.println("Password incorreta para o utilizador " + username);
                                outStream.writeBoolean(true);
                                outStream.flush();

                                outStream.writeObject("password incorreta");
                                outStream.flush();
                                
                                outStream.writeBoolean(true);
                                outStream.flush();

                                outStream.writeObject("password incorreta");
                                outStream.flush();

                                //true final do recebeFicheiros
                                // outStream.writeBoolean(true);
                                // outStream.flush();
                            }
                        } else if ("-sa".equals(opcao)) {
                            // Recebe assinatura do ficheiro
                            if (validatePassword(username, password)) {
                                recebeFicheiros(nomeUtente, nomeMedico, outStream, inStream, qtdFicheiros);
                                recebeFicheiros(nomeUtente, nomeMedico, outStream, inStream, qtdFicheiros);
                            } else {
                                String filename = (String) inStream.readUTF();
                                System.out.println("Não foi possivel receber o/os ficheiro/os assinados");
                                System.out.println("Password incorreta para o utilizador " + username);
                                outStream.writeBoolean(true);
                                outStream.flush();

                                outStream.writeObject("password incorreta");
                                outStream.flush();
                                
                                //true final do recebeFicheiros
                                // outStream.writeBoolean(true);
                                // outStream.flush();
                            }
                        } else if ("-se".equals(opcao)) {
                            if (validatePassword(username, password)) {
                                // Recebe ficheiro seguro
                                recebeFicheiros(nomeUtente, nomeMedico, outStream, inStream, qtdFicheiros);
                                // Recebe a chave secreta
                                recebeFicheiros(nomeUtente, nomeMedico, outStream, inStream, qtdFicheiros);

                                recebeFicheiros(nomeUtente, nomeMedico, outStream, inStream, qtdFicheiros);
                            } else {
                                String filename = (String) inStream.readUTF();
                                System.out.println("Não foi possivel receber o/os ficheiro/os assinados");
                                System.out.println("Password incorreta para o utilizador " + username);
                                outStream.writeBoolean(true);
                                outStream.flush();

                                outStream.writeObject("password incorreta");
                                outStream.flush();
                            }
                        }
                    }
                } else if ("-g".equals(opcao)) {
                    if (!Paths.get("src/main/java/servidor/" + nomeUtente).toFile().exists()) {
                        System.out.println("O utilizador " + nomeUtente + " não existe");

                        outStream.writeObject("o utente nao existe");
                        outStream.flush();
                    } else if (!doMacValidation(ADMIN_PASSWORD)) {
                        System.out.println("O mac do servidor e invalido");

                        outStream.writeObject("O mac do servidor e invalido");
                        outStream.flush();

                        System.exit(0);
                    } else {
                        //qtdFicheiros = inStream.readInt();
                        String utente = inStream.readUTF(); // 1
                        System.out.println("nome utente: "+utente);
                        int numFiles = inStream.readInt(); // 2
                        System.out.println("num files: "+numFiles);

                        if (validatePassword(username, password)) {
                            outStream.writeObject("OK");
                            outStream.flush();
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
                                    enviarAssinaturaGoption(utente, medico, signedFilename, signatureFilename, outStream);
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
                        } else {  
                            outStream.writeObject("password incorreta");
                            outStream.flush();
                        }
                    }
                } else if ("-au".equals(opcao)) {
                    // funçao para adicionar o certificado ao diretorio do username e criar o username no ficheiro users.txt do mesmo formato de sempre
                    createUser(username, password, certificado, outStream);
                    //calculate new MAC for users.txt
                    byte[] mac = calculateMAC(ADMIN_PASSWORD);
                    String macString = Base64.getEncoder().encodeToString(mac);
                    //rewrite the MAC file with new macString
                    try {
                        Files.write(Paths.get(PATH_TO_MAC), macString.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
                        System.out.println("MAC atualizado com sucesso!");
                    } catch (IOException e) {
                        System.err.println("Erro ao escrever o MAC para o users.txt file: " + e.getMessage());
                        System.exit(-1);
                    }
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                System.out.println("Ocorreu um erro de java");
               // e.printStackTrace();
            } catch (ClassNotFoundException c) {
                //c.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }



        private boolean verificaFicheiroServer(String filename, String utente, String medico) throws IOException {
            File diretorio = new File("src/main/java/servidor/"+utente + "/" + filename);
            if(diretorio.exists()){
                return true;
            }
            else{
                return false;
            }
        }
        private void salvarFicheiro(String filename, byte[] fileBytes, String utente, ObjectOutputStream out, ObjectInputStream in) {
            try {
                /*FileOutputStream fileOutputStream = new FileOutputStream("src/main/java/servidor/"+utente+ "/" +filename);
                fileOutputStream.write(fileBytes);*/
                // Open file output stream
                FileOutputStream fos = new FileOutputStream("src/main/java/servidor/" + utente + "/" +filename);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                // Read and write received data in blocks
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
                // Close streams
                bos.close();
                fos.close();

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
                        out.writeObject("Ficheiro ja existe");
                        out.flush();
                        continue;
                    }
                    out.writeObject("OK");
                    out.flush();
                    // Recebe tamanho do ficheiro
                    long fileSize = (long) in.readLong();
                    System.out.println("Recebi " + fileSize + " bytes");

                    // Recebe o conteúdo do ficheiro como um array de bytes
                    //byte[] fileBytes = (byte[]) in.readObject();
                    if(filename.contains(".assinatura") || filename.contains(".chave_secreta")){
                        // Caso o ficheiro seja a assinatura ou a chave, tem 256 bytes, entao nao precisa ser enviada por partes.
                        byte[] fileBytes = (byte[]) in.readObject();
                        FileOutputStream fileOutputStream = new FileOutputStream("src/main/java/servidor/"+utente+ "/" +filename);
                        fileOutputStream.write(fileBytes);
                        out.writeBoolean(true);
                        out.flush();
                    }
                    else{
                        // Caso nao seja assinatura ou chave, deve receber o ficheiro em blocos de 1024 bytes.
                        FileOutputStream fos = new FileOutputStream("src/main/java/servidor/" + utente + "/" + filename);
                        // Read and write received data in blocks
                        byte[] buffer = new byte[1024];
                        int bytesRead = 0;
                        int bytesToRead = (int) fileSize;
                        while (bytesToRead > 0) {
                            bytesRead = in.read(buffer);
                            fos.write(buffer, 0, bytesRead);
                            bytesToRead -= bytesRead;
                        }
                        out.writeBoolean(true);
                        out.flush();
                        // Close streams
                        fos.close();
                    }

                }
                return true;
            }catch (Exception e){
                return false;
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
    public static void criaDiretorio(String nomeUtente){
        File diretorio = new File("src/main/java/servidor/"+nomeUtente);

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

    private void enviarAssinaturaGoption(String utente, String medicoName, String signedFilename, String signatureFilename, ObjectOutputStream out) {
        try {
            // Check if both files exist
            File signedFile = new File("src/main/java/servidor/" + utente + "/" + signedFilename);
            File signatureFile = new File("src/main/java/servidor/" + utente + "/" + signatureFilename);

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
            byte[] signedBytes = Files.readAllBytes(Paths.get("src/main/java/servidor/"+utente, signedFilename));

            // Read signature file content
            byte[] signatureBytes = Files.readAllBytes(Paths.get("src/main/java/servidor/"+utente, signatureFilename));


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
        File userDir = new File("src/main/java/servidor/"+utente);

        // Check if the user directory exists
        if (userDir.exists() && userDir.isDirectory()) {
            // List all files in the user directory
            File[] files = userDir.listFiles();

            // Iterate through the files
            for (File file : files) {
                // Check if the filename starts with the original filename
                if (file.getName().startsWith(filename + ".assinatura")) {
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
        try {
            FileInputStream fis = new FileInputStream("src/main/java/servidor/"+utente+"/"+fileName+".cifrado");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

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

        try {
            FileInputStream fis = new FileInputStream("src/main/java/servidor/"+utente+"/"+fileName+".chave_secreta."+utente);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

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
        try {

            FileInputStream fis = new FileInputStream("src/main/java/servidor/"+utente+"/"+fileName+".seguro");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
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
        try {
            FileInputStream fis = new FileInputStream("src/main/java/servidor/"+utente+"/"+fileName+".assinatura."+medico);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

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
        File diretorio = new File("src/main/java/servidor/"+utente + "/" + filename);
        if(diretorio.exists()){
            return true;
        }
        else{
            return false;
        }
    }

    // public static void iniciarAdmin() {
    //     //file users.txt verifica se existe
    //     //File usersFile = new File("users.txt");

    //     File usersFile = new File("src/main/java/servidor/" + "users.txt");

    //     // Se o ficheiro users.txt não existir, cria-o
    //     if (!usersFile.exists()) {
    //         try {
    //             if (usersFile.createNewFile()) {
    //                 System.out.println("Ficheiro de passwords users criado com sucesso.");
    //             } else {
    //                 System.out.println("Houve um erro ao criar o ficheiro de passwords users.");
    //             }
    //         } catch (IOException e) {
    //             System.err.println("Houve um erro ao criar o ficheiro de passwords users: " + e.getMessage());
    //             System.exit(-1);
    //         }
    //     }

    //     try (Scanner scanner = new Scanner(usersFile)) {
    //         boolean adminFound = false;

    //         // Ler o ficheiro users.txt linha por linha
    //         while (scanner.hasNextLine()) {
    //             String line = scanner.nextLine();
    //             String[] parts = line.split(";");
    //             if (parts.length >= 3 && parts[0].equals("admin")) {
    //                 adminFound = true;
    //                 break;
    //             }
    //         }

    //         // Se o utilizador admin nao existir, cria-o
    //         if (!adminFound) {
    //             // Prompt user for password
    //             System.out.print("Dê uma password para o admin: ");

    //             // Ler password
    //             Scanner inputScanner = new Scanner(System.in);
    //             String password = inputScanner.nextLine();
    //             inputScanner.close();

    //             // Gerar salt
    //             byte[] salt = getNextSalt();

    //             // Hashed password com salt
    //             byte[] hashedPassword = hash(password.toCharArray(), salt);

    //             // Convert byte arrays to Base64 encoded strings
    //             String saltString = Base64.getEncoder().encodeToString(salt);
    //             String hashedPasswordString = Base64.getEncoder().encodeToString(hashedPassword);

    //             // Write user information to file
    //             try (FileWriter writer = new FileWriter(usersFile)) {
    //                 writer.write("admin;" + saltString + ";" + hashedPasswordString + "\n");
    //                 System.out.println("Admin account criada com sucesso no users.txt!");
    //             } catch (IOException e) {
    //                 System.err.println("Erro ao escrever para o users.txt: " + e.getMessage());
    //                 System.exit(-1);
    //             }
    //         } else {
    //             System.out.println("O utilizador admin ja existe no users.txt... a continuar");
    //         }
    //     } catch (IOException e) {
    //         System.err.println("Error ao ler o users.txt file: " + e.getMessage());
    //         System.exit(-1);
    //     }
    // }

    public static void iniciarAdmin() throws NoSuchAlgorithmException {
        //file users.txt verifica se existe
        //File usersFile = new File("users.txt");
        File usersFile = new File("src/main/java/servidor/" + "users.txt");

        // Se o ficheiro users.txt não existir, cria-o
        if (!usersFile.exists()) {
            try {
                if (usersFile.createNewFile()) {
                    System.out.println("Ficheiro de passwords users criado com sucesso.");
                } else {
                    System.out.println("Houve um erro ao criar o ficheiro de passwords users.");
                }
            } catch (IOException e) {
                System.err.println("Houve um erro ao criar o ficheiro de passwords users: " + e.getMessage());
                System.exit(-1);
            }
        }

        try (Scanner scanner = new Scanner(usersFile)) {
            boolean adminFound = false;

            // Ler o ficheiro users.txt linha por linha
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(";");
                if (parts.length >= 3 && parts[0].equals("admin")) {
                    adminFound = true;
                    break;
                }
            }
                // Prompt user for password
                System.out.print("Dê uma password para o admin: ");

                // Ler password
                Scanner inputScanner = new Scanner(System.in);
                String password = inputScanner.nextLine();
                ADMIN_PASSWORD = password;

                // Gerar salt
                byte[] salt = getNextSalt();

                // Hashed password com salt
                //byte[] hashedPassword = hash(password.toCharArray(), salt);
                byte[] hashedPassword = hash(password, salt);

                // Convert byte arrays to Base64 encoded strings
                String saltString = Base64.getEncoder().encodeToString(salt);
                String hashedPasswordString = Base64.getEncoder().encodeToString(hashedPassword);
            if (!adminFound) {
                // Write user information to file
                try (FileWriter writer = new FileWriter(usersFile)) {
                    writer.write("admin;" + saltString + ";" + hashedPasswordString + "\n");
                    System.out.println("Admin account criada com sucesso no users.txt!");
                } catch (IOException e) {
                    System.err.println("Erro ao escrever para o users.txt: " + e.getMessage());
                    System.exit(-1);
                }
            }

                //compare the password
                try {
                    Path usersFilePath = Paths.get(PATH_TO_USERS);
                    List<String> lines = Files.readAllLines(usersFilePath);
                    for (String line : lines) {
                        String[] userInfo = line.split(";");
                        if (userInfo.length > 0 && userInfo[0].equals("admin")) {
                            byte[] saltBytes = Base64.getDecoder().decode(userInfo[1]);
                            byte[] hashedPasswordBytes = Base64.getDecoder().decode(userInfo[2]);
                            byte[] hashedPasswordAttempt = hash(password, saltBytes);
                            if (Arrays.equals(hashedPasswordBytes, hashedPasswordAttempt)) {
                                //verify if exists a MAC file
                                if(Files.exists(Paths.get(PATH_TO_MAC))){
                                    if(doMacValidation(password)){
                                        System.out.println("MAC validado com sucesso!");
                                    } else {
                                        System.out.println("ERROR: MAC invalido");
                                        System.exit(0);
                                    }
                                }
                                else {
                                        // Prompt for MAC
                                        System.out.println("MAC não existe. Pretende criar um novo MAC? (s/n): ");

                                        // Ler password
                                        String MACoption = inputScanner.nextLine();
                                        inputScanner.close();

                                        if (MACoption.equals("s")) {
                                            System.out.println("A criar ficheiro MAC...");
                                            byte[] mac = calculateMAC(password);
                                            String macString = Base64.getEncoder().encodeToString(mac);
                                            try {
                                                Files.write(Paths.get(PATH_TO_MAC), macString.getBytes());
                                                System.out.println("MAC criado com sucesso!");
                                            } catch (IOException e) {
                                                System.err.println("Erro ao escrever o MAC para o users.txt file: " + e.getMessage());
                                                System.exit(-1);
                                            }
                                        } else {
                                            System.out.println("Servidor a fechar, MAC nao foi criado");
                                            System.exit(0);
                                        }
                                }

                            } else {
                                System.out.println("Password do admin incorreta!");
                                System.exit(0);
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao ler o users.txt file: " + e.getMessage());
                    System.exit(-1);
                }

        } catch (IOException e) {
            System.err.println("Error ao ler o users.txt file: " + e.getMessage());
            System.exit(-1);
        }
    }

    private static boolean doMacValidation(String password) {
        try{
            //get the MAC from the file
            Path macFilePath = Paths.get(PATH_TO_MAC);
            // read mac from file
            String macStringFromFile = new String(Files.readAllBytes(macFilePath));

            //calculate MAC for the file users.txt
            byte[] mac = calculateMAC(password);

            String macString = Base64.getEncoder().encodeToString(mac);

            //compare the MACs
         return macString.equals(macStringFromFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] calculateMAC(String password) {
        //calculate MAC for the file users.txt
        try {
            Path usersFilePath = Paths.get(PATH_TO_USERS);
            byte[] fileBytes = Files.readAllBytes(usersFilePath);
            byte[] mac = getMAC(fileBytes, password);
            return mac;
        } catch (IOException e) {
            System.err.println("Erro ao calcular o MAC para o users.txt file: " + e.getMessage());
            System.exit(-1);
        }
        return null;
    }

    private static byte[] getMAC(byte[] fileBytes, String password) {
        try{
            // Create a secret key from the password
            byte[] passwordBytes = password.getBytes();
            SecretKeySpec secretKey = new SecretKeySpec(passwordBytes, "HmacSHA256");

            // Create a MAC object
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);

            // Calculate the MAC
            return mac.doFinal(fileBytes);

        } catch (Exception e) {
            System.err.println("Erro ao calcular o MAC: " + e.getMessage());
            System.exit(-1);
            return null;
        }
    }

    private static void createUser(String username, String password, byte[] certificateBytes, ObjectOutputStream out) {
        try {
            boolean userAlreadyExists = false;
            // Save the certificate file inside the user directory
            String certificateFilePath = "src/main/java/servidor/" + username + "/" + username + ".cert";
            File certificateFile = new File(certificateFilePath);

            if (certificateFile.exists()) {
                System.out.println("Erro: O certificado já existe no servidor.");
            } else {
                FileOutputStream fos = new FileOutputStream(certificateFilePath);
                fos.write(certificateBytes);
                fos.close();
            }

            Path usersFilePath = Paths.get("src/main/java/servidor/users.txt");
            if (Files.exists(usersFilePath)) {
                List<String> lines = Files.readAllLines(usersFilePath);
                for (String line : lines) {
                    String[] userInfo = line.split(";");
                    if (userInfo.length > 0 && userInfo[0].equals(username)) {
                        System.out.println("Erro: O utilizador já existe no servidor.");
                        userAlreadyExists = true;
                    }
                }
            }

            if (userAlreadyExists == false ) {
                // Gerar salt
                byte[] salt = getNextSalt();

                // Hashed password com salt
                byte[] hashedPassword = hash(password, salt);

                // Convert byte arrays to Base64 encoded strings
                String saltString = Base64.getEncoder().encodeToString(salt);
                String hashedPasswordString = Base64.getEncoder().encodeToString(hashedPassword);

                // Write user information to file
                String userEntry = username + ";" + saltString + ";" + hashedPasswordString + "\n";
                Files.write(Paths.get("src/main/java/servidor/users.txt"), userEntry.getBytes(), StandardOpenOption.APPEND);
            }

            // Check if user was created successfully
            if (Files.exists(Paths.get("src/main/java/servidor/users.txt"))) {
                // Check if certificate file was sent correctly
                if (certificateFile.exists() && userAlreadyExists == false) {
                    // Send success message to client
                    System.out.println("Utilizador criado com sucesso!");
                    out.writeObject("Utilizador criado com sucesso!");
                    out.flush();
                } else if (certificateFile.exists() && userAlreadyExists == true) {
                    System.out.println("Utilizador já existe no servidor.");
                    out.writeObject("Utilizador já existe no servidor.");
                    out.flush();
                } else {
                    System.out.println("Erro: O arquivo de certificado nao foi recebido corretamente.");
                }
            } else {
                System.out.println("Erro: Falha ao criar o utilizador no file users.txt.");
                out.writeObject("Nao foi possivel criar o utilizador no servidor.");
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] getNextSalt() {
        byte[] salt = new byte[16];
        Random random = new Random();
        random.nextBytes(salt);
        return salt;
    }

    public static byte[] hash(String password, byte[] salt) {
        char[] passwordChar = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(passwordChar, salt, 65536, 256);
        Arrays.fill(passwordChar, Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new AssertionError("Error while hashing a password: " + ((Throwable) e).getMessage());
        }
        finally {
            spec.clearPassword();
        }
    }

    // public static byte[] hash(String password, byte[] salt) throws NoSuchAlgorithmException {
    //     MessageDigest md = MessageDigest.getInstance("SHA");

    //     byte[] passwordAndSalt = new byte[password.getBytes().length + salt.length];
    //     // byte PasswordBytes[] = password.getBytes();
    //     // byte hashedPassword[] = md.digest(PasswordBytes);
    //     // byte hashedSalt[] = md.digest(salt);
    //     // byte hash[] = md.digest(hashedPassword + hashedPassword);
    //     byte[] hash = md.digest(passwordAndSalt);

    //     return hash;
    // }

    public static boolean validatePassword(String username, String password) throws NoSuchAlgorithmException {
        try {
            // Ler o ficheiro users.txt
            List<String> lines = Files.readAllLines(Paths.get("src/main/java/servidor/users.txt"));

            // Encontrar o utilizador no ficheiro users.txt
            for (String line : lines) {
                String[] userInfo = line.split(";");
                System.out.println(userInfo.length + " " + userInfo[0] + " " + username);
                if (userInfo.length == 3 && userInfo[0].equals(username)) {
                    // Decodificar o salt e o hash do utilizador
                    byte[] salt = Base64.getDecoder().decode(userInfo[1]);
                    byte[] storedHashedPassword = Base64.getDecoder().decode(userInfo[2]);

                    // Gerar o hash da password input
                    byte[] hashedPassword = hash(password, salt);

                    // Comparar os hashes
                    boolean PassCheck = MessageDigest.isEqual(storedHashedPassword, hashedPassword);
                    System.out.println("Password check: " + PassCheck);
                    return PassCheck;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false; // Utilizador inexistente no servidor ou erro
    }
}