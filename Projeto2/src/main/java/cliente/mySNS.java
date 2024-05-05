package cliente;

import javax.crypto.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import javax.crypto.spec.SecretKeySpec;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

public class mySNS {

    private static String defaultKeystorePassword;
    public static void main(String[] args) throws Exception,IOException, ClassNotFoundException {
        try{
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
            String nomeCliente = args[3];

            String passCliente = ""; // Alterar de acordo com linha de comando
            if(!args[2].equals("-au")){
                passCliente = args[5]; // Alterar de acordo com linha de comando
                String keystorePath = "src/main/java/cliente/keystores/" + nomeCliente + ".keystore";
            }
            else{
                passCliente = args[4];
                String keystorePath = "src/main/java/cliente/keystores/" + args[3];
            }
            String keystorePath = "src/main/java/cliente/keystores/" + nomeCliente + ".keystore";

            System.setProperty("javax.net.ssl.trustStore","src/main/java/cliente/truststores/" + nomeCliente + ".truststore");
            System.setProperty("javax.net.ssl.trustStorePassword", passCliente);

            //criar socket para ligar ao server
            SocketFactory sf = SSLSocketFactory.getDefault();

            Socket echoSocket = sf.createSocket(parts[0], Integer.parseInt(parts[1])); //127.0.0.1
            //criar stream object
            BufferedInputStream bufferedInputStream = new BufferedInputStream(echoSocket.getInputStream(), 8192);
            ObjectInputStream in = new ObjectInputStream(bufferedInputStream);

            // Set up ObjectOutputStream with a larger buffer size
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(echoSocket.getOutputStream(), 8192);
            ObjectOutputStream out = new ObjectOutputStream(bufferedOutputStream);

            String opcaoUsername = args[2];
            String opcao = "";
            out.writeObject(opcaoUsername);

            if ("-u".equals(opcaoUsername)) {
                out.writeObject(nomeCliente);
                String password = args[5];
                defaultKeystorePassword = password;
                out.writeObject(password);
                opcao = "-g";
            } else if ("-m".equals(opcaoUsername)) {
                opcao = args[8];
                String nomeUtente = args[7];
                out.writeObject(nomeCliente);
                out.writeObject(nomeUtente);

                String password = args[5];
                defaultKeystorePassword = password;
                out.writeObject(password);
                String certificateUtentePath = "src/main/java/cliente/keystores/" + nomeUtente + ".cert";
                addCertificateIfNotExists(keystorePath, passCliente, nomeUtente, certificateUtentePath);
            }
            else if ("-au".equals(opcaoUsername)) {
                String username = args[3];
                String password = args[4];
                defaultKeystorePassword = password;
                opcao = "-au";
                // Send username, password, and certificate to the server
                out.writeUTF(username);
                out.flush();

                out.writeUTF(password);
                out.flush();

                String certificateFilePath = "src/main/java/cliente/keystores/" + args[5];

                // Ler o certificado do ficheiro e converte-o em um array de bytes
                byte[] certificateBytes = lerCertificado(certificateFilePath);

                if (certificateBytes == null) {
                    System.exit(0);
                }
                out.writeObject(certificateBytes);
                out.flush();
            }

            //Envia ao servidor o comando a executar
            out.writeObject(opcao);
            if ("-sc".equals(opcao)) {
                DoScOption(args, in, out);
            } else if ("-sa".equals(opcao)) {
                DoSaOption(args, in, out);
            } else if ("-se".equals(opcao)) {
                DoSeOption(args, in, out);
            } else if ("-g".equals(opcao)) {
                doGoption(args, in, out);
            } else if ("-au".equals(opcao)) {
                System.out.println("A criar o utilizador " + args[3] + "...");
                String mensagemResposta = (String) in.readObject();
                System.out.println(mensagemResposta);
                //String certificateFilePath = "src/main/java/cliente/keystores/" + args[5];
            } else {
                System.out.println("Argumentos invalidos (ex: -a <serverAddress> -u <username do utente> -g {<filenames>}+).");
                System.exit(0);
            }
        } 
        catch (SocketException e) {
            //e.printStackTrace();
            System.out.println("Password incorreta ou ocorreu algum erro na socket");
        }
        
        catch (Exception e){
            //e.printStackTrace();
            System.out.println("Verifique se o a keystore do cliente possui o certificado do servidor");
        }


    }
    public static boolean enviaFicheiro(String filename, String extension, String aliasUtente, byte[] content, ObjectInputStream in, ObjectOutputStream out) throws IOException {
        try{
            String nomeFicheiro = filename + extension;
            out.writeUTF(nomeFicheiro); // envia nome do ficheiro
            out.flush();
            // Recebe do servidor um boolean que e true caso o ficheiro ja exista no servidor.
            boolean ficheiroDuplicado = (Boolean) in.readBoolean(); // recebe confirmacao do servidor se o ficheiro esta duplicado
            if(ficheiroDuplicado)
            {
                String msgRecebida = (String) in.readObject();
                if (msgRecebida.equals("password incorreta")) {
                    System.out.println("A password que introduziu está incorreta OU o médico não existe.");
                    System.exit(0);
                } else if (msgRecebida.equals("o utente nao existe")) {
                    System.out.println("O utente que introduziu não existe no servidor.");
                    System.exit(0);
                } else if (msgRecebida.equals("O mac do servidor e invalido")) {
                    System.out.println("O MAC do servidor é invalido");
                    System.exit(0);
                }
                System.out.println("O ficheiro " + nomeFicheiro + " ja existe no diretorio de " + aliasUtente);
                return false;
            }
            else{
                String msgRecebida = (String) in.readObject();
                // Envia conteudo do ficheiro apos receber confirmacao que o ficheiro nao existe no servidor
                long fileSize = (long) content.length;
                // Envia tamanho do ficheiro
                out.writeLong(fileSize);
                System.out.println("enviei tamanho: " + fileSize);

                if(nomeFicheiro.contains(".assinatura") || nomeFicheiro.contains(".chave_secreta")){
                    // Caso o ficheiro seja a assinatura ou a chave, tem 256 bytes, entao nao precisa ser enviada por partes.
                    out.writeObject(content);
                    out.flush();
                    System.out.println("enviei assinatura ou chave secreta");
                }
                else{
                    // Caso o ficheiro nao seja assinatura ou chave secreta, pode ter qualquer tamanho e deve ser enviado em blocos de 1024 bytes
                    File file = new File(filename);
                    FileInputStream fis = new FileInputStream(file);
                    // Read and send file in blocks
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    int bytesToRead = (int) fileSize;
                    while (bytesToRead > 0) {
                        bytesRead = fis.read(buffer);
                        out.write(buffer, 0, bytesRead);
                        bytesToRead -= bytesRead;
                    }
                    out.flush();
                    // Close streams and socket
                    fis.close();
                }


                /*String recebeuCompleto = in.readUTF();
                if(recebeuCompleto.equals("NOK")){
                    System.out.println("Ocorreu um erro com o envio do ficheiro, o servidor nao recebeu o conteudo por completo");
                    return false;
                }*/
                boolean confirmacaoRececao = (Boolean) in.readBoolean();
                System.out.println("recebi confirmacao");
                if(confirmacaoRececao){
                    System.out.println("Ficheiro " + nomeFicheiro + " foi enviado para o servidor no diretorio de " + aliasUtente);
                    return true;
                }
                else{
                    System.out.println("Ocorreu um erro ao enviar o ficheiro " + nomeFicheiro);
                    return false;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    private static void DoSeOption(String[] args, ObjectInputStream in, ObjectOutputStream out) {
        try{
            out.writeInt(args.length-9);
            String aliasMedico = args[3];
            String aliasUtente = args[7];
            PrivateKey privateKey = getPrivateKey("src/main/java/cliente/keystores/" + aliasMedico + ".keystore", aliasMedico, defaultKeystorePassword);
            //PrivateKey privateKey = getPrivateKey("keystores/" + aliasMedico + ".keystore", aliasMedico, defaultKeystorePassword);
            // envia 3 ficheiros para o servidor para cada ficheiro lido
            for (int i = args.length-1; i>=9; i--){
                String filename = args[i];
                boolean ficheiroExiste = verificaFicheiroCliente(filename);
                if (!ficheiroExiste) {
                    System.out.println("O ficheiro " + filename + " nao existe.");
                    out.writeUTF("NOK");
                    out.flush();
                    continue;
                }
                byte[] ficheiroEmBytes = leFicheiro(filename);

                // Gera a chave secreta AES para ser usada na cifra
                SecretKey secretKey = GeraChaveSecretaAES();

                // Cifra a chave simetrica com a chave do utente
                byte[] chaveSimetrica = CifraChaveSimetrica(secretKey, filename, aliasMedico, aliasUtente, out, in);
                if (chaveSimetrica == null) {
                    System.out.println("Chave cifrada nao é válida para "+ filename + ", terminando...");
                    return;
                }
                // Assina o ficheiro e ENVIA a assinatura para o servidor
                assinaFicheiro(filename, ficheiroEmBytes, aliasMedico, aliasUtente, privateKey, defaultKeystorePassword, in, out);
                // Cifra o ficheiro utilizando a chave AES e retorna o conteudo em bytes
                DoCifraSimetrica(aliasUtente,filename, ".seguro", secretKey, out, in);
                // ENVIA o ficheiro seguro para o servidor
                //enviaFicheiro(filename, ".seguro", aliasUtente, encryptedBytes, in, out);
                // Envia a chave secreta cifrada para o servidor
                enviaFicheiro(filename, ".chave_secreta." + aliasUtente, aliasUtente, chaveSimetrica, in, out);
            }
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void DoScOption(String [] args, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        //Envio a quantidade de ficheiros para o loop do lado do servidor saber quantas vezes
        //ler o InputStream
        out.writeInt(args.length-9);
        String aliasMedico = args[3];
        String aliasUtente = args[7];
        for (int i = args.length-1; i>=9; i--){
            //Faço a cifra hibrida
            boolean ficheiroExiste = verificaFicheiroCliente(args[i]);
            if (!ficheiroExiste) {
                System.out.println("O ficheiro " + args[i] + " nao existe.");
                out.writeUTF("NOK");
                out.flush();
                continue;
            }
            CifraHibrida(args[i], aliasMedico, aliasUtente, out,in);
        }
    }

    private static void CifraHibrida(String fileName, String aliasMedico, String aliasUtente, ObjectOutputStream out, ObjectInputStream in) {
        try {
            // Gera a chave secreta AES para ser usada na cifra
            SecretKey secretKey = GeraChaveSecretaAES();
            // Cifra o ficheiro utilizando a chave AES e retorna o conteudo em bytes
            DoCifraSimetrica(aliasUtente,fileName, ".cifrado", secretKey, out, in);
            //Cifra a chave simetrica e escreve no ficheiro <arg>.chave_secreta.<utente>
            byte[] chaveCifrada = CifraChaveSimetrica(secretKey, fileName, aliasMedico, aliasUtente, out, in);

            if (chaveCifrada == null) {
                System.out.println("Chave cifrada nao é válida para "+ fileName + ", terminando...");
                return;
            }
            // Envia o ficheiro cifrado
            //enviaFicheiro(fileName, ".cifrado", aliasUtente, encryptedBytes, in, out);
            // Envia a chave secreta
            enviaFicheiro(fileName, ".chave_secreta."+aliasUtente, aliasUtente, chaveCifrada, in, out);
        }
        catch (Exception e) {
            System.out.println("Ficheiro não existe localmente: " + fileName);
            e.printStackTrace();
        }
    }

    private static byte[] CifraChaveSimetrica(SecretKey secretKey, String fileName, String aliasMedico, String aliasUtente, ObjectOutputStream out, ObjectInputStream in) throws Exception {
        // Obtem par de chaves do utente
        PublicKey publicKey = getPublicKey("src/main/java/cliente/keystores/" + aliasMedico + ".keystore", aliasUtente);
        if (publicKey == null) {
            System.out.println("Public key nao é válida");
            return null;
        }
        //PublicKey publicKey = getPublicKey("keystores/" + aliasMedico + ".keystore", aliasUtente, keystorePass);
        // Cifra a chave simétrica com a chave pública
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] chaveCifrada = cipher.doFinal(secretKey.getEncoded());
        return chaveCifrada;
    }

    private static SecretKey GeraChaveSecretaAES() throws NoSuchAlgorithmException {
        // Gera a chave simétrica
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey secretKey = keyGen.generateKey();
        return secretKey;
    }

    private static void DoCifraSimetrica(String utente, String fileName, String extension, SecretKey secretKey, ObjectOutputStream out, ObjectInputStream in) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, ClassNotFoundException {

        // Cifra o arquivo
        FileInputStream inputFile = new FileInputStream(fileName);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // Cria um ficheiro temporario com o conteudo criptografado
        File tempFile = File.createTempFile("encrypted", ".tmp");

        try (FileOutputStream outputStream = new FileOutputStream(tempFile);
             CipherOutputStream cos = new CipherOutputStream(outputStream, cipher)) {

            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = inputFile.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
        } finally {
            inputFile.close();
        }

        // Envia o nome do ficheiro
        String nomeFicheiro = fileName + extension;
        out.writeUTF(nomeFicheiro); // envia nome do ficheiro
        out.flush();
        boolean ficheiroDuplicado = (Boolean) in.readBoolean(); // recebe confirmacao do servidor se o ficheiro esta duplicado
        if(ficheiroDuplicado)
            {
                String msgRecebida = (String) in.readObject();
                if (msgRecebida.equals("password incorreta")) {
                    //System.out.println("A password que introduziu está incorreta OU o médico não existe.");
                    return;
                } else if (msgRecebida.equals("o utente nao existe")) {
                    //System.out.println("O utente que introduziu não existe.");
                    return;
                } else if (msgRecebida.equals("O mac do servidor e invalido")) {
                    //System.out.println("O utente que introduziu não existe.");
                    return;
                }
                System.out.println("O ficheiro " + nomeFicheiro + " ja existe no diretorio de " + utente);
                return;
            }
        else {
            String msgRecebida = (String) in.readObject();
            // Envia conteudo do ficheiro apos receber confirmacao que o ficheiro nao existe no servidor
            FileInputStream fis = new FileInputStream(tempFile);
            long fileSize = tempFile.length();
            System.out.println("Enviei " + fileSize + " bytes");
            // Envia tamanho do ficheiro
            out.writeLong(fileSize);

            // Sending encrypted file

            byte[] buffer = new byte[1024];
            int bytesRead;
            int bytesToRead = (int) fileSize;
            while (bytesToRead > 0) {
                bytesRead = fis.read(buffer);
                out.write(buffer, 0, bytesRead);
                bytesToRead -= bytesRead;
            }
            out.flush();
            // Close streams and socket
            fis.close();
            boolean confirmacaoRececao = (Boolean) in.readBoolean();
            if(confirmacaoRececao){
                System.out.println("Ficheiro " + nomeFicheiro + " foi enviado para o servidor no diretorio de " + utente);
            }
            else{
                System.out.println("Ocorreu um erro ao enviar o ficheiro " + nomeFicheiro);
            }
        }
    }


    private static void doGoption(String[] args, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassCastException {
        try {
            // comando = mySNS -a ipaddress -u utente -g ficheiro1.pdf ficheiro2.pdf
            // Enviar o número de ficheiros para o servidor
            int numFiles = args.length - 7;
            String nomeUtente = args[3];
            String passUtente = args[5];
            String keystorePathUtente = "src/main/java/cliente/keystores/" + nomeUtente + ".keystore";


            out.writeUTF(nomeUtente); // 1
            out.flush();

            out.writeInt(args.length - 7); // 2
            out.flush();

            Object ola = in.readObject();

            String msgRecebida = (String) in.readObject();
            System.out.println(msgRecebida);
            if (msgRecebida.equals("password incorreta")) {
                System.out.println("A password que introduziu está incorreta");
                return;
            } else {
                // Receber e verificar cada ficheiro
                for (int i = 0; i < numFiles; i++) {
                    Object verifiedCifrado = null;
                    Object verifiedSignature = null;
                    Object verifiedSeguro = null;
                    // Enviar o nome do ficheiro
                    String filename = args[7+i];
                    System.out.println("Verificação do ficheiro: " + filename + "...");

                    out.writeUTF(filename); // 3
                    out.flush();

                    // // Ignorar lol
                    // if (i == 0) {
                    //     Object receivedObject = in.readObject();
                    // }

                    Boolean signatureSignedCheck = (Boolean) in.readBoolean();

                    if (signatureSignedCheck) {
                        String serverMessage = in.readUTF();
                        System.out.println("Mensagem de verificação: " + serverMessage);
                        if (serverMessage.equals("Um ou ambos os ficheiros não existem no servidor")) {
                            System.exit(0);
                        } else {
                            // Proceed with the rest of your client logic
                        }

                        // Receber o conteúdo do ficheiro assinado
                        byte[] signedBytes = (byte[]) in.readObject(); // 3

                        // Receber o conteúdo da assinatura
                        byte[] signatureBytes = (byte[]) in.readObject(); // 4

                        // Nome do médico
                        String medico = in.readUTF(); // 5
                        String certMedicoPath = "src/main/java/cliente/keystores/" + medico + ".cert";
                        System.out.println(keystorePathUtente);
                        System.out.println(passUtente);
                        System.out.println(medico);
                        System.out.println(certMedicoPath);
                        addCertificateIfNotExists(keystorePathUtente, passUtente, medico, certMedicoPath);

                        // Verificar a assinatura
                        verifiedSignature = verificaAssinatura(filename, in, out, signedBytes, signatureBytes, medico, nomeUtente);
                    }

                    Boolean chaveCifradoCheck = (Boolean) in.readBoolean();

                    if (chaveCifradoCheck) {
                        Decifra(filename, nomeUtente, out, in);
                        // Check if the decrypted file exists locally
                        File decifrado = new File(filename + ".decifrado");
                        if (decifrado.exists()) {
                            verifiedCifrado = true;
                        } else {
                            verifiedCifrado = false;
                        }
                    }

                    Boolean seguroCheck = (Boolean) in.readBoolean();

                    if (seguroCheck) {
                        Decifra(filename, nomeUtente, out, in);
                        byte[] assinaturaSeguroBytes = (byte[]) in.readObject();

                        String medico = in.readUTF();

                        verifiedSeguro = verificaAssinaturaSeguro(filename, in, out, assinaturaSeguroBytes, medico, nomeUtente);
                    }

                    // Imprimir o resultado da verificação
                    if (verifiedSignature != null) {
                        boolean signatureVerified = (Boolean) verifiedSignature;
                        if (signatureVerified) {
                            System.out.println("Assinatura verificada com sucesso para o ficheiro: " + filename);
                        } else {
                            System.out.println("Não foi possível verificar a assinatura para o ficheiro: " + filename);
                        }
                    }

                    if (verifiedCifrado != null) {
                        boolean cifradoVerified = (Boolean) verifiedCifrado;
                        if (cifradoVerified) {
                            System.out.println("Ficheiro decifrado com sucesso para o ficheiro: " + filename);
                        } else {
                            System.out.println("Não foi possível decifrar o ficheiro: " + filename);
                        }
                    }

                    if (verifiedSeguro != null) {
                        boolean seguroVerified = (Boolean) verifiedSeguro;
                        if (seguroVerified) {
                            System.out.println("Ficheiro seguro decifrado e verificado com sucesso para o ficheiro: " + filename);
                        } else {
                            System.out.println("Não foi possível verificar a assinatura para o ficheiro seguro: " + filename);
                        }
                    }

                    if (verifiedSignature == null && verifiedCifrado == null && verifiedSeguro == null) {
                        System.out.println("Não foi possível verificar ou decifrar qualquer ficheiro");
                    }

                }
            }
        } catch (Exception  e) {
            System.out.println("Ocorreu um erro, verifique se existe keystore para o médico / se o tamanho do ficheiro é maior do que o esperado e tente novamente");
            System.out.println();
            e.printStackTrace();
        }
    }

    private static boolean verificaAssinatura(String filename, ObjectInputStream in, ObjectOutputStream out, byte[] signedBytes, byte[] signatureBytes, String aliasMedico, String utente) throws IOException, ClassNotFoundException {
        try {
            // Get the public key of the medico from the keystore
            PublicKey publicKey = getPublicKey("src/main/java/cliente/keystores/" + utente + ".keystore", aliasMedico);

            // Create object Signature to verify the signature
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);

            // Update the signature with the data of the signed file
            signature.update(signedBytes);

            // Verify the signature
            boolean verified = signature.verify(signatureBytes);

            return verified;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean verificaAssinaturaSeguro(String filename, ObjectInputStream in, ObjectOutputStream out, byte[] signatureBytes, String aliasMedico, String utente) throws IOException, ClassNotFoundException {
        try {
            // Get the public key of the medico from the keystore
            PublicKey publicKey = getPublicKey("src/main/java/cliente/keystores/" + utente + ".keystore", aliasMedico);

            // Create object Signature to verify the signature
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);

            byte[] seguroAssinado = leFicheiro(filename + ".decifrado");

            // Update the signature with the data of the signed file
            signature.update(seguroAssinado);

            // Verify the signature
            boolean verified = signature.verify(signatureBytes);

            return verified;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    private static void Decifra(String fileName, String utente, ObjectOutputStream out, ObjectInputStream in) throws Exception {
        // Envio o nome do ficheiro para o server
        // out.writeUTF(fileName);
        // out.flush(); // Certifique-se de esvaziar o buffer para garantir que os dados sejam enviados imediatamente

        boolean existeFicheiro = in.readBoolean();
        if (!existeFicheiro){
            System.out.println("Não existe o ficheiro no servidor");
            return;
        }
        else {
            // Se o arquivo existe no servidor, então lemos os bytes do arquivo cifrado
            byte[] chaveCifrada = (byte[]) in.readObject();
            System.out.println("Recebi a chave!");
            // Decifra a chave cifrada usando a chave privada
            Cipher cipher = Cipher.getInstance("RSA");
            PrivateKey privateKey = getPrivateKey("src/main/java/cliente/keystores/" + utente + ".keystore", utente, "123456");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] chaveDecifrada = cipher.doFinal(chaveCifrada);

            // Construímos a SecretKey usando os bytes decifrados
            SecretKey chaveSecreta = new SecretKeySpec(chaveDecifrada, "AES");
            Cipher cipherAES = Cipher.getInstance("AES");
            cipherAES.init(Cipher.DECRYPT_MODE, chaveSecreta);

            // Leia os bytes do arquivo cifrado
            byte[] arquivoCifrado = (byte[]) in.readObject();

            // Decifre os bytes do arquivo cifrado
            byte[] arquivoDecifrado = cipherAES.doFinal(arquivoCifrado);

            // Escreva os bytes decifrados em um novo arquivo
            FileOutputStream fos = new FileOutputStream(fileName + ".decifrado");
            fos.write(arquivoDecifrado);
            fos.close();
        }
    }

    public static void DoSaOption(String [] args, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        try {
            String aliasMedico = args[3];
            String aliasUtente = args[7];

            // Obter a chave privada da keystore
            // PrivateKey privateKey = getPrivateKey("src/main/java/cliente/keystores/" + aliasMedico + ".keystore", aliasMedico, defaultKeystorePassword);
            PrivateKey privateKey = getPrivateKey("src/main/java/cliente/keystores/" + aliasMedico + ".keystore", aliasMedico, defaultKeystorePassword);

            // envia quantidade de ficheiros
            out.writeInt(args.length-9);
            // Assinar cada arquivo
            for (int i = 9; i < args.length; i++) {
                boolean ficheiroExiste = verificaFicheiroCliente(args[i]);
                if (!ficheiroExiste) {
                    System.out.println("O ficheiro " + args[i] + " nao existe.");
                    out.writeUTF("NOK");
                    out.flush();
                    continue;
                }
                // Le o ficheiro e transforma em bytes
                byte[] ficheiroEmBytes = leFicheiro(args[i]);
                // Assina o ficheiro lido e envia a assinatura para o servidor
                if (privateKey == null) {
                    System.out.println("Keystore nao existe/Private key nao é válida");
                    return;
                }
                assinaFicheiro(args[i], ficheiroEmBytes, aliasMedico, aliasUtente, privateKey, defaultKeystorePassword, in, out);
                // Enviar o ficheiro que foi previamente assinado para o servidor
                enviaFicheiro(args[i], ".assinado", aliasUtente, ficheiroEmBytes, in, out);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Ocorreu um erro, verifique se existe keystore para o medico e tente novamente");
            System.out.println();
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] assinaFicheiro(String filename, byte[] ficheiroParaAssinar, String aliasMedico, String aliasUtente, PrivateKey privateKey, String keystorePassword, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {

        try {
            // Criar objeto Signature para assinar o arquivo
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);

            // Atualizar a assinatura com os dados do arquivo
            signature.update(ficheiroParaAssinar);

            // Gerar a assinatura
            byte[] signatureBytes = signature.sign();

            // Enviar a assinatura para o servidor
            String extensaoAssinatura = ".assinatura." + aliasMedico;
            enviaFicheiro(filename, extensaoAssinatura, aliasUtente, signatureBytes, in, out);

            return ficheiroParaAssinar;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static byte[] leFicheiro(String filename) throws IOException {
        // Ler o ficheiro para assinar
        File file = new File(filename);
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] fileBytes = new byte[(int) file.length()];
        fileInputStream.read(fileBytes);
        return fileBytes;
    }

    public static PrivateKey getPrivateKey(String keystorePath,  String alias, String keystorePassword) {
        try {
            FileInputStream fis = new FileInputStream(keystorePath);
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(fis, keystorePassword.toCharArray());

            // Get private key
            PrivateKey key = (PrivateKey) keystore.getKey(alias, keystorePassword.toCharArray());
            return (PrivateKey) key;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("A keystore não existe/o path da keystore nao está correto");
            return null;
        }
    }

    public static PublicKey getPublicKey(String keystorePath,  String alias) {
        try {
            FileInputStream fis = new FileInputStream(keystorePath);
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(fis, defaultKeystorePassword.toCharArray());

            // Get certificate (contains public key)
            Certificate cert = keystore.getCertificate(alias);
            if (cert != null) {
                return cert.getPublicKey();
            } else {
                throw new IllegalArgumentException("Certificado nao encontrado para o alias '" + alias + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("A keystore não existe/o path da keystore nao está correto");
            return null;
        }
    }

    private static boolean verificaFicheiroCliente(String filename) throws IOException {
        // File diretorio = new File("src/main/java/cliente/" + filename);
        File diretorio = new File(filename);
        if(diretorio.exists()){
            return true;
        }
        else{
            return false;
        }
    }

    private static byte[] lerCertificado(String certificateFilePath) {
        try {
            File file = new File(certificateFilePath);

            if (!file.exists()) {
                System.out.println("Erro: Certificado nao encontrado no path: " + certificateFilePath);
                return null;
            }
            // Check if the file extension is .cert
            if (!certificateFilePath.toLowerCase().endsWith(".cert") && !certificateFilePath.toLowerCase().endsWith(".cer") && !certificateFilePath.toLowerCase().endsWith(".crt")) {
                System.out.println("Erro: Certificado de segurança deve ter uma extensão .cert, .cer ou .crt");
                System.exit(0); // Terminate the JVM
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] certificateBytes = new byte[(int) file.length()];
            fis.read(certificateBytes);
            fis.close();
            return certificateBytes;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void addCertificateIfNotExists(String keystorePath, String keystorePassword, String alias, String certificatePath) {
        try {
            if (!aliasExistsInKeystore(keystorePath, keystorePassword, alias)) {
                // Load the keystore
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                char[] password = keystorePassword.toCharArray();
                FileInputStream fis = new FileInputStream(keystorePath);
                keyStore.load(fis, password);
                fis.close();

                // Load the certificate file
                FileInputStream certInputStream = new FileInputStream(certificatePath);
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                Certificate cert = certFactory.generateCertificate(certInputStream);
                certInputStream.close();

                // Add the certificate to the keystore with the specified alias
                keyStore.setCertificateEntry(alias, cert);

                // Save the keystore
                FileOutputStream fos = new FileOutputStream(keystorePath);
                keyStore.store(fos, password);
                fos.close();

                System.out.println("Certificado adicionado com o alias: " + alias);
            } else {
                System.out.println("Alias already exists in the keystore.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean aliasExistsInKeystore(String keystorePath, String keystorePassword, String alias) {
        try {
            // Load the keystore
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] password = keystorePassword.toCharArray();
            FileInputStream fis = new FileInputStream(keystorePath);
            keyStore.load(fis, password);
            fis.close();

            // Check if the alias exists
            return keyStore.containsAlias(alias);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void createKeystore(String path, String alias, String keystorePassword) {
        try {
            // Create a new keystore
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] password = keystorePassword.toCharArray();
            keyStore.load(null, password);

            // Save the keystore to a file
            String keystoreFileName = Paths.get(path, alias + ".keystore").toString();
            FileOutputStream fos = new FileOutputStream(keystoreFileName);
            keyStore.store(fos, password);
            fos.close();

            System.out.println("Keystore created: " + keystoreFileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}