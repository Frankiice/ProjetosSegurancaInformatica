# Projetos
 Projetos de Segurança informática

Projeto de segurança GRUPO 009

Felipe Habib - 57157
Francisco Pinto - 56929
Gabriel Azevedo - 57160

Formas de correr o projeto:

Ter o projeto na seguinte organização:

src
---main
-------java
-----------cliente
------------------keystores (Onde guardamos as keystores do lado do cliente)
------------------mySNS
-----------servidor
------------------mySNSServer
teste.txt (Ficheiros exemplo fora da src dentro da pasta do projeto [junto do pom])
a.txt
Biblia.pdf


Sendo o "silva" um medico, e a "maria" uma utente, podemos usar por exemplo os comandos a seguir:
------------------------------------------------------ CRIAR KEYSTORES -----------------------------

 - Para criar o keystore do user silva:

keytool -genkeypair -alias silva -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore silva.keystore

 - Para criar o keystore do user maria:

keytool -genkeypair -alias maria -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore maria.keystore

------------------------------------- EXPORTAR CERTIFICADO DO KEYSTORE -----------------------------

 - Para exportar o certificado de dentro do keystore da maria para o ficheiro maria_certificate.crt

keytool -exportcert -keystore maria.keystore -alias maria -storetype PKCS12 -file maria_certificate.crt

 - Para exportar o certificado de dentro do keystore do silva para o ficheiro silva_certificate.crt

keytool -exportcert -keystore silva.keystore -alias silva -storetype PKCS12 -file silva_certificate.crt

------------------------------------ IMPORTAR CERTIFICADO PARA KEYSTORE -------------------------

Estamos a importar o certificado da maria para o keystore do silva pois precisamos da chave publica da maria para usar na cifra hibrida ao cifrar os ficheiros.

 - Para importar o certificado de dentro do ficheiro maria_certificate.crt para o silva.keystore
keytool -importcert -keystore silva.keystore -alias maria -file maria_certificate.crt -storetype PKCS12

Estamos a importar o certificado do silva para o keystore da maria pois precisamos da chave publica do silva para que a maria possa verificar uma assinatura feita pelo silva

 - Para importar o certificado de dentro do ficheiro silva_certificate.crt para o maria.keystore
keytool -importcert -keystore maria.keystore -alias silva -file silva_certificate.crt -storetype PKCS12

-------------------------------------- SETUP DO SERVIDOR E DO CLIENTE -------------------------------------------

 - Para compilar o servidor

javac src/main/java/servidor/mySNSServer.java -d target/classes/servidor

 - Para compilar o cliente

javac src/main/java/cliente/mySNS.java -d target/classes

 - Para correr o servidor:

java -cp target/classes/servidor servidor.mySNSServer

 - Para correr o cliente

java -cp target/classes/cliente cliente.mySNS


CORRER OS SEGUINTES COMANDOS DO LADO DO CLIENTE

---------------------------------- COMANDO -SC PARA CIFRAR -----------------------------------------------------
Tendo por exemplo o ficheiro 83800_por.pdf

java -cp target/classes cliente.mySNS -a localhost:12345 -m silva -u maria -sc 83800_por.pdf


-------------------------------- COMANDO -SA PARA ASSINAR ------------------------------------------------------
Tendo por exemplo os ficheiros 83800_por.pdf e teste.txt

java -cp target/classes cliente.mySNS -a localhost:12345 -m silva -u maria -sa 83800_por.pdf teste.txt

-------------------------------- COMANDO -SE PARA ASSINAR E CIFRAR ------------------------------------------------------
Tendo por exemplo os ficheiros 83800_por.pdf, teste.txt e abcd.xml

java -cp target/classes cliente.mySNS -a localhost:12345 -m silva -u maria -se 83800_por.pdf teste.txt abcd.xml


-------------- COMANDO -G PARA DECIFRAR, VERIFICAR ASSINATURA OU VERIFICAR .SEGURO (AMBOS) -------------------
Tendo por exemplo o ficheiro 83800_por.pdf

java -cp target/classes cliente.mySNS -a localhost:12345 -u maria -g 83800_por.pdf
