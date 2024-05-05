package cliente;

import javax.crypto.*;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import javax.crypto.spec.SecretKeySpec;

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
        BufferedInputStream bufferedInputStream = new BufferedInputStream(echoSocket.getInputStream(), 8192);
        ObjectInputStream in = new ObjectInputStream(bufferedInputStream);

        // Set up ObjectOutputStream with a larger buffer size
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(echoSocket.getOutputStream(), 8192);
        ObjectOutputStream out = new ObjectOutputStream(bufferedOutputStream);

        String opcaoUsername = args[2];
        String opcao = "";
        out.writeObject(opcaoUsername);
        switch (opcaoUsername){
            case "-u":
                out.writeObject(args[3]);
                opcao = "-g";
                break;
            case "-m":
                opcao = args[6];
                out.writeObject(args[3]);
                out.writeObject(args[5]);
                break;
        }
        //Envia ao servidor o comando a executar
        out.writeObject(opcao);
        switch(opcao) {
            case "-sc":
                DoScOption(args, in, out);
                //Nao enviar ficheiro, apenas nome e bytes(olhar o do gabriel como exemplo)
                //Fazer o decifrar
                //adicionar certificado do utente no keystore do medico
                break;
            case "-sa":
                DoSaOption(args, in, out);
                break;
            case "-se":
                DoSeOption(args, in, out);
                break;
            case "-g":
                System.out.println("Dentro da -g option:");
                doGoption(args, in, out);
                break;
            case "":
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
                System.out.println("O ficheiro " + nomeFicheiro + " ja existe no diretorio de " + aliasUtente);
                return false;
            }
            else{
                // Envia conteudo do ficheiro apos receber confirmacao que o ficheiro nao existe no servidor
                long fileSize = (long) content.length;
                // Envia tamanho do ficheiro
                out.writeLong(fileSize);
                System.out.println("enviei tamanho: " + fileSize);
                out.writeObject(content);
                out.flush();
                String recebeuCompleto = in.readUTF();
                if(recebeuCompleto.equals("NOK")){
                    System.out.println("Ocorreu um erro com o envio do ficheiro, o servidor nao recebeu o conteudo por completo");
                    return false;
                }
                boolean confirmacaoRececao = (Boolean) in.readBoolean();
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
            return false;
        }
    }

    private static void DoSeOption(String[] args, ObjectInputStream in, ObjectOutputStream out) {
        try{
            out.writeInt(args.length-7);
            String aliasMedico = args[3];
            String aliasUtente = args[5];
            String defaultKeystorePassword = "123456";
            //PrivateKey privateKey = getPrivateKey("src/main/java/cliente/keystores/" + aliasMedico + ".keystore", aliasMedico, defaultKeystorePassword);
            PrivateKey privateKey = getPrivateKey("keystores/" + aliasMedico + ".keystore", aliasMedico, defaultKeystorePassword);
            // envia 3 ficheiros para o servidor para cada ficheiro lido
            for (int i = args.length-1; i>=7; i--){
                String filename = args[i];
                boolean ficheiroExiste = verificaFicheiroCliente(filename);
                if (!ficheiroExiste) {
                    System.out.println("O ficheiro " + filename + " nao existe.");
                    out.writeUTF("NOK");
                    out.flush();
                    continue;
                }
                byte[] ficheiroEmBytes = leFicheiro(filename);
                // Assina o ficheiro e ENVIA a assinatura para o servidor
                assinaFicheiro(filename, ficheiroEmBytes, aliasMedico, aliasUtente, privateKey, defaultKeystorePassword, in, out);
                // Gera a chave secreta AES para ser usada na cifra
                SecretKey secretKey = GeraChaveSecretaAES();
                // Cifra o ficheiro utilizando a chave AES e retorna o conteudo em bytes
                byte[] encryptedBytes = DoCifraSimetrica(aliasUtente,filename, secretKey, out, in);
                // ENVIA o ficheiro seguro para o servidor
                enviaFicheiro(filename, ".seguro", aliasUtente, encryptedBytes, in, out);
                // Cifra a chave simetrica com a chave do utente
                byte[] chaveSimetrica = CifraChaveSimetrica(secretKey, filename, aliasMedico, aliasUtente, out, in);
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
        out.writeInt(args.length-7);
        String aliasMedico = args[3];
        String aliasUtente = args[5];
        for (int i = args.length-1; i>=7; i--){
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
            byte[] encryptedBytes = DoCifraSimetrica(aliasUtente,fileName, secretKey, out, in);
            // Envia o ficheiro cifrado
            enviaFicheiro(fileName, ".cifrado", aliasUtente, encryptedBytes, in, out);
            //Cifra a chave simetrica e escreve no ficheiro <arg>.chave_secreta.<utente>
            byte[] chaveCifrada = CifraChaveSimetrica(secretKey, fileName, aliasMedico, aliasUtente, out, in);
            // Envia a chave secreta
            enviaFicheiro(fileName, ".chave_secreta."+aliasUtente, aliasUtente, chaveCifrada, in, out);
        }
        catch (Exception e) {
            System.out.println("Ficheiro não existe localmente: " + fileName);
            // throw new RuntimeException(e);
        }
    }

    private static byte[] CifraChaveSimetrica(SecretKey secretKey, String fileName, String aliasMedico, String aliasUtente, ObjectOutputStream out, ObjectInputStream in) throws Exception {
        // Carregar keystore
        String keystorePass = "123456";
        // Obtem par de chaves do utente
        // PrivateKey privateKey = getPrivateKey("src/main/java/cliente/keystores/" + aliasMedico + ".keystore", aliasUtente, keystorePass);
        // PublicKey publicKey = getPublicKey("src/main/java/cliente/keystores/" + aliasMedico + ".keystore", aliasUtente, keystorePass);
        PublicKey publicKey = getPublicKey("keystores/" + aliasMedico + ".keystore", aliasUtente, keystorePass);
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

    private static byte[] DoCifraSimetrica(String utente,String fileName, SecretKey secretKey, ObjectOutputStream out, ObjectInputStream in) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException {

        // Cifra o arquivo
        FileInputStream inputFile = new FileInputStream(fileName);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        CipherOutputStream cos = new CipherOutputStream(byteArrayOutputStream, cipher);

        byte[] buffer = new byte[2048];
        int bytesRead;
        while ((bytesRead = inputFile.read(buffer)) != -1) {
            cos.write(buffer, 0, bytesRead);
        }
        cos.close();
        inputFile.close();

        // Obtém os bytes cifrados como um array de bytes
        byte[] encryptedBytes = byteArrayOutputStream.toByteArray();
        return encryptedBytes;
    }


    private static void doGoption(String[] args, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassCastException {
        try {
            // comando = mySNS -a ipaddress -u utente -g ficheiro1.pdf ficheiro2.pdf
            // Enviar o número de ficheiros para o servidor
            int numFiles = args.length - 5;
            String nomeUtente = args[3];

            out.writeUTF(nomeUtente); // 1
            out.flush();

            out.writeInt(args.length - 5); // 2
            out.flush();
            // Receber e verificar cada ficheiro
            for (int i = 0; i < numFiles; i++) {
                Object verifiedCifrado = null;
                Object verifiedSignature = null;
                Object verifiedSeguro = null;
                // Enviar o nome do ficheiro
                String filename = args[5+i];
                System.out.println("Verificação do ficheiro: " + filename + "...");

                // Verificar se o ficheiro existe localmente
                // File file = new File(filename);
                // if (!file.exists()) {
                //     System.out.println("O ficheiro " + filename + " não existe localmente.");
                //     out.writeObject(false); // 1-1
                //     out.flush();
                //     continue; // Saltar o processamento deste ficheiro
                // }

                // out.writeObject(true); // 1-2
                // out.flush();

                out.writeUTF(filename); // 3
                out.flush();

                // Ignorar lol
                if (i == 0) {
                    Object receivedObject = in.readObject();
                }

                Boolean signatureSignedCheck = in.readBoolean();
                System.out.println("signatureSignedCheck: " + signatureSignedCheck);

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

                    // Verificar a assinatura
                    verifiedSignature = verificaAssinatura(filename, in, out, signedBytes, signatureBytes, medico, nomeUtente);
                }

                Boolean chaveCifradoCheck = (Boolean) in.readBoolean();
                System.out.println("chaveCifradoCheck: " + chaveCifradoCheck);

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

                Boolean seguroCheck = in.readBoolean();
                System.out.println("seguroCheck: " + seguroCheck);

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
        } catch (Exception  e) {
            System.out.println("Ocorreu um erro, verifique se existe keystore para o médico / se o tamanho do ficheiro é maior do que o esperado e tente novamente");
            System.out.println();
            e.printStackTrace();
        }
    }

    private static boolean verificaAssinatura(String filename, ObjectInputStream in, ObjectOutputStream out, byte[] signedBytes, byte[] signatureBytes, String aliasMedico, String utente) throws IOException, ClassNotFoundException {
        try {
            String defaultKeystorePassword = "123456"; // Senha padrão da keystore

            // Get the public key of the medico from the keystore
            PublicKey publicKey = getPublicKey("keystores/" + utente + ".keystore", aliasMedico, defaultKeystorePassword);

            // Create object Signature to verify the signature
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);

            // Update the signature with the data of the signed file
            signature.update(signedBytes);

            // Verify the signature
            boolean verified = signature.verify(signatureBytes);

            return verified;
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
    }

    private static boolean verificaAssinaturaSeguro(String filename, ObjectInputStream in, ObjectOutputStream out, byte[] signatureBytes, String aliasMedico, String utente) throws IOException, ClassNotFoundException {
        try {
            String defaultKeystorePassword = "123456"; // Senha padrão da keystore

            // Get the public key of the medico from the keystore
            PublicKey publicKey = getPublicKey("keystores/" + utente + ".keystore", aliasMedico, defaultKeystorePassword);

            // Create object Signature to verify the signature
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);

            byte[] seguroAssinado = leFicheiro(filename + ".decifrado");

            // Update the signature with the data of the signed file
            signature.update(seguroAssinado);

            // Verify the signature
            boolean verified = signature.verify(signatureBytes);

            return verified;
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
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
            PrivateKey privateKey = getPrivateKey("keystores/" + utente + ".keystore", utente, "123456");
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
            String defaultKeystorePassword = "123456"; // Senha padrão da keystore
            String aliasMedico = args[3];
            String aliasUtente = args[5];

            // Obter a chave privada da keystore
            // PrivateKey privateKey = getPrivateKey("src/main/java/cliente/keystores/" + aliasMedico + ".keystore", aliasMedico, defaultKeystorePassword);
            PrivateKey privateKey = getPrivateKey("keystores/" + aliasMedico + ".keystore", aliasMedico, defaultKeystorePassword);

            // envia quantidade de ficheiros
            out.writeInt(args.length-7);
            // Assinar cada arquivo
            for (int i = 7; i < args.length; i++) {
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
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
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
            // e.printStackTrace();
            System.out.println("A keystore não existe/o path da keystore nao está correto");
            return null;
        }
    }

    public static PublicKey getPublicKey(String keystorePath,  String alias, String keystorePassword) {
        try {
            FileInputStream fis = new FileInputStream(keystorePath);
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(fis, keystorePassword.toCharArray());

            // Get certificate (contains public key)
            Certificate cert = keystore.getCertificate(alias);
            if (cert != null) {
                return cert.getPublicKey();
            } else {
                throw new IllegalArgumentException("Certificado nao encontrado para o alias '" + alias + "'");
            }
        } catch (Exception e) {
            // e.printStackTrace();
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

}