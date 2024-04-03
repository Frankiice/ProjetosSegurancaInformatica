import javax.crypto.*;
//import javax.net.SocketFactory;
//import javax.net.ssl.SSLSocketFactory;
import javax.crypto.spec.SecretKeySpec;

import java.io.*;
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
        String opcao = args[6];
        out.writeObject(opcaoUsername);
        switch (opcaoUsername){
            case "-u":
                out.writeObject(args[3]);
                opcao = "-g";
                break;
            case "-m":
                out.writeObject(args[3]);
                out.writeObject(args[5]);
                break;
        }
        //Envia ao servidor o comando a executar
        out.writeObject(args[6]);
        switch(opcao) {
            case "-sc":
                DoScOption(args, in, out);
                break;
            case "-sa":
                //
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
        String keystorePath = "keystores/" + utente + ".keystore";
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
}