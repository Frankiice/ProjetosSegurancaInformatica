package cliente;

import javax.crypto.*;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
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
        String opcao = args[6];
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
                //
            case "-g":
                //
            case "":
        }


    }
    public static void DoScOption(String [] args, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        //Envio a quantidade de ficheiros para o loop do lado do servidor saber quantas vezes
        //ler o InputStream
        out.writeObject((args.length-7)*2);
        for (int i = args.length-1; i>=7; i--){

            if(PodeCifrar(args[i], in, out)){
                //Faço a cifra hibrida
                CifraHibrida(args[i], args[5]);
                //Envio para o servidor
                File ficheiroCifrado = new File(args[i]+".cifrado");
                out.writeObject(ficheiroCifrado);
                File chaveSecretaCifrada = new File(args[i]+".chave_secreta."+args[5]);
                out.writeObject(chaveSecretaCifrada);
            }

            else
                System.out.println("Erro ao cifrar ficheiro " + args[i]);
        }

    }

    private static void CifraHibrida(String arg, String utente) {
        try {
            //Cria o ficheiro cifrado com o nome <arg>.cifrado e retorna a chave usada
            SecretKey secretKey = DoCifraSimetrica(arg);
            //Cifra a chave simetrica e escreve no ficheiro <arg>.chave_secreta.<utente>
            CifraChaveSimetrica(secretKey, arg, utente);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void CifraChaveSimetrica(SecretKey secretKey, String nomeFicheiro, String utente) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException, CertificateException, KeyStoreException, UnrecoverableKeyException {
        //Carregar keystore
        String keystorePass = "123456";
        String keystorePath = "src/main/java/cliente/keystores/" + utente + ".keystore";
        FileInputStream keystoreInputStream = new FileInputStream(keystorePath);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(keystoreInputStream, keystorePass.toCharArray());

        //Gera par de chaves
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(utente, keystorePass.toCharArray());
        PublicKey publicKey = keyStore.getCertificate(utente).getPublicKey();

        //Cifra a chave simetrica
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] chaveCifrada = cipher.doFinal(secretKey.getEncoded());

        //Escreve para um ficheiro
        FileOutputStream fos = new FileOutputStream(nomeFicheiro + ".chave_secreta." + utente);
        FileInputStream inputFile;

        inputFile = new FileInputStream(nomeFicheiro + ".chave_secreta." + utente);
        CipherOutputStream cos = new CipherOutputStream(fos, cipher);

        byte[]b = new byte[256];
        int i = inputFile.read(b);
        while(i !=-1){
            cos.write(chaveCifrada,0,i);
            i=inputFile.read();
        }
        cos.close();
        inputFile.close();
        fos.close();
    }


    private static byte[] readFile(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fileInputStream.read(data);
            return data;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return new byte[0];
    }

    private static SecretKey DoCifraSimetrica(String arg) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException {
        //Gera a chave simetrica
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey secretKey = keyGen.generateKey();

        FileInputStream inputFile;
        CipherOutputStream cos;

        //Cifra o ficheiro
        inputFile = new FileInputStream(arg);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        FileOutputStream fos = new FileOutputStream(arg + ".cifrado");
        cos = new CipherOutputStream(fos, cipher);

        byte[]b = new byte[256];
        int i = inputFile.read(b);
        while(i !=-1){
            cos.write(b,0,i);
            i=inputFile.read();
        }

        cos.close();
        inputFile.close();
        fos.close();
        return secretKey;
    }

    public static boolean PodeCifrar(String nomeFicheiro, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        Boolean podeCifrar = false;
        File arquivo = new File(nomeFicheiro);
        podeCifrar = arquivo.exists();
        out.writeObject(nomeFicheiro);
        podeCifrar = !(Boolean) in.readObject();
        return podeCifrar;
    }
    // Gabriel

    private static void doGoption(String[] args, ObjectInputStream in, ObjectOutputStream out) throws IOException {
        try {
            // Envia a quantidade de ficheiros
            out.writeInt(args.length-5);

            // Senha padrão da keystore
            String defaultKeystorePassword = "123456";
            String aliasUtente = args[3];

            String aliasMedico = "silva";

            // Obter a chave publica da keystore do medico
            PublicKey publicKey = getPublicKey(aliasMedico, defaultKeystorePassword);

            // envia quantidade de ficheiros
            //out.writeInt(args.length-7);
            // Assinar cada arquivo
            for (int i = 5; i < args.length; i++) {
                System.out.println("inside for loop");
                //verificaAssinatura(args[i], in, out);

                //assinaFicheiro(args[i], aliasMedico, aliasUtente, privateKey, defaultKeystorePassword, in, out);
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | FileNotFoundException | UnrecoverableKeyException e) {
            System.out.println("Ocorreu um erro, verifique se existe keystore para o medico e tente novamente");
            System.out.println();
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verificaAssinatura(String filename, ObjectInputStream in, ObjectOutputStream out) throws IOException {
        // Ler o arquivo original
        File file = new File(filename);
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] fileBytes = new byte[(int) file.length()];
        fileInputStream.read(fileBytes);

        // Ler a assinatura do arquivo

        //String signatureFilename = (String) in.readUTF();
        //byte[] signatureBytes = (byte[]) in.readObject();

        //String signedFilename = (String) in.readUTF();
        //byte[] signedBytes = (byte[]) in.readObject();

        //FileInputStream signatureInputStream = new FileInputStream(signatureFilename);
        //byte[] signatureBytes = new byte[(int) file.length()];
        //signatureInputStream.read(signatureBytes);

        // Criar objeto Signature para verificar a assinatura
        //Signature signature = Signature.getInstance("MD5withRSA");
        //signature.initVerify(publicKey);

        // Atualizar a assinatura com os dados do arquivo original
        //signature.update(fileBytes);

        // Verificar a assinatura
        //return signature.verify(signatureBytes);
        return false;
    }
    public static void DoSaOption(String [] args, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        try {
            String defaultKeystorePassword = "123456"; // Senha padrão da keystore
            String aliasMedico = args[3];
            String aliasUtente = args[5];

            // Obter a chave privada da keystore
            PrivateKey privateKey = getPrivateKey(aliasMedico, defaultKeystorePassword);

            // envia quantidade de ficheiros
            out.writeInt(args.length-7);
            // Assinar cada arquivo
            for (int i = 7; i < args.length; i++) {
                assinaFicheiro(args[i], aliasMedico, aliasUtente, privateKey, defaultKeystorePassword, in, out);
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | FileNotFoundException | UnrecoverableKeyException e) {
            System.out.println("Ocorreu um erro, verifique se existe keystore para o medico e tente novamente");
            System.out.println();
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void assinaFicheiro(String filename, String aliasMedico, String aliasUtente, PrivateKey privateKey, String keystorePassword, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {

        try {
            // Criar objeto Signature para assinar o arquivo
            Signature signature = Signature.getInstance("MD5withRSA");
            signature.initSign(privateKey);

            // Ler o arquivo para assinar
            File file = new File(filename);
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] fileBytes = new byte[(int) file.length()];
            fileInputStream.read(fileBytes);

            // Atualizar a assinatura com os dados do arquivo
            signature.update(fileBytes);

            // Gerar a assinatura
            byte[] signatureBytes = signature.sign();
            Boolean servidorRecebeu;
            Boolean ficheiroDuplicado;
            // Enviar o arquivo assinado para o servidor
            out.writeUTF(filename + ".assinado");
            out.flush();

            // Recebe do servidor um boolean que e true caso o ficheiro ja exista no servidor.
            ficheiroDuplicado = (Boolean) in.readBoolean();
            if(ficheiroDuplicado)
            {
                System.out.println("O ficheiro " + filename + " ja existe no diretorio de " + aliasUtente);
                return;
            }
            else{
                System.out.println("Ficheiro " + filename + " foi enviado para o servidor no diretorio de " + aliasUtente);
            }

            out.writeObject(fileBytes);
            out.flush();
            servidorRecebeu = (Boolean) in.readBoolean();

            // Enviar a assinatura para o servidor
            out.writeUTF(filename + ".assinatura." + aliasMedico);
            out.flush();
            out.writeObject(signatureBytes);
            out.flush();
            servidorRecebeu = (Boolean) in.readBoolean();

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
    }

    public static void salvaFicheiro(String filename, byte[] fileBytes) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(filename)) {
            fileOutputStream.write(fileBytes);
        }
    }
    //verificaAssinatura(args[i], aliasMedico, aliasUtente, publicKey, defaultKeystorePassword, in, out);


    private static PublicKey getPublicKey(String alias, String password) throws Exception {
        // Carregar a keystore do servidor...
        FileInputStream keystoreInputStream = new FileInputStream("src/main/java/cliente/keystores/" + alias + ".keystore");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(keystoreInputStream, password.toCharArray());

        // Obter o certificado do alias especificado...
        Certificate certificate = keyStore.getCertificate(alias);

        // Retornar a chave pública do certificado...
        return certificate.getPublicKey();
    }

    private static PrivateKey getPrivateKey(String alias, String password) throws Exception {
        // Carregar a keystore do usuário
        String keystoreName = alias + ".keystore"; // Supondo que o nome da keystore é passado como argumento
        String keystorePath = "src/main/java/cliente/keystores/" + keystoreName;
        FileInputStream keystoreInputStream = new FileInputStream(keystorePath);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(keystoreInputStream, password.toCharArray()); // Usando a senha padrão da keystore

        // Obter a chave privada da keystore
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray()); // Usando a senha padrão da keystore
        return privateKey;
    }

}
