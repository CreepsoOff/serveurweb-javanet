import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

public class HttpServer {

  private static final String HTML_BODY = "<html><body>";

  public static void main(String[] args) throws IOException {
    int port;
    String root;
    boolean index;
    String[] accept = {};
    String[] reject = {};

    try {
      File file = new File("config.tld");
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(file);
      doc.getDocumentElement().normalize();

      NodeList nList = doc.getElementsByTagName("port");
      port = Integer.parseInt(nList.item(0).getTextContent());

      nList = doc.getElementsByTagName("root");
      root = nList.item(0).getTextContent();

      nList = doc.getElementsByTagName("index");
      // Boolean value
      index = Boolean.parseBoolean(nList.item(0).getTextContent());
      System.out.println("index: " + index);

      nList = doc.getElementsByTagName("accept");
      accept = nList.item(0).getTextContent().split(",");

      nList = doc.getElementsByTagName("reject");
      reject = nList.item(0).getTextContent().split(",");

    } catch (ParserConfigurationException | SAXException e) {
      throw new RuntimeException(e);
    }

    ServerSocket ss;
    boolean arret = false;
    String status200 = "HTTP/1.1 200 OK";
    String status404 = "HTTP/1.1 404 Not Found";
    String status501 = "HTTP/1.1 501 Not Implemented";
    String docroot = root;
    byte[] crlf = new byte[2];
    crlf[0] = 0x0D;
    crlf[1] = 0x0A;
    System.out.println("Serveur en attente de connexions");
    while (!arret) {
      ss.setReuseAddress(true);
      if (args.length == 0) ss = new ServerSocket(port);
      else ss = new ServerSocket(Integer.parseInt(args[0]));



      Socket clientS = ss.accept();

      System.out.println(
          "Nouveau client, adresse "
              + clientS.getInetAddress()
              + " sur le port "
              + clientS.getPort());
      OutputStream output = clientS.getOutputStream();
      InputStream input = clientS.getInputStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(input));
      /* lit la premiere ligne */
      String message = br.readLine();
      System.out.println("< " + message);
      /* Lit toutes les autres lignes en attente, jusqu'a la premiere ligne vide (fin de la requete) */
      String s;
      while (true) {
        s = br.readLine();
        System.out.println("< " + s);
        if (s.equals("")) break;
      }

      String[] requete = message.split(" ");
      /* /!\ IMPORTANT: pour lire le fichier et l'écrire sur la connexion TCP,
       * il faut utiliser un DataOutputStream (flux d'octets) et pas un
       * OutputStreamWriter/BufferWriter (flux de caractères). Sinon, les
       * conversions vont corrompre les images. */
      DataOutputStream data = new DataOutputStream(output);
      if (requete[0].equals("GET")) {
        /* Override manuel: / -> /index.html */
        if (requete[1].equals("/") && index) {
          requete[1] = "/index.html";
        } else if (requete[1].equals("/")) {
          File[] files = new File(docroot).listFiles();

          data.writeBytes(HTML_BODY);
          data.writeBytes("Liste des fichiers:<ul>");
          for (File file : Objects.requireNonNull(files)) {
            data.writeBytes(
                "<li><a href=\"" + file.getName() + "\">" + file.getName() + "</a></li>");
          }
          data.writeBytes("</ul></body></html>");
          data.flush();
          clientS.close();
          continue;
        } else if (requete[1].equals("/images")) {
          data.writeBytes(HTML_BODY);
          data.writeBytes("Liste des images:<ul>");
          File[] files = new File(docroot + "/images").listFiles();
          for (File file : files) {
            data.writeBytes(
                "<li><a href=\"/images/" + file.getName() + "\">" + file.getName() + "</a></li>");
          }
          // Ajouter un bouton retour en arrière
          data.writeBytes("</ul><a href=\"/\">Retour</a></body></html>");

          data.flush();
          clientS.close();
          continue;
        }

        if (new File(docroot + requete[1]).isFile()) {
          /* le fichier existe, on l'envoie */
          byte[] fichier;
          try (FileInputStream fis = new FileInputStream(docroot + requete[1])) {
            int size = fis.available();
            fichier = new byte[size];
            fis.read(fichier);
          }
          System.out.println("> [fichier] " + docroot + requete[1]);
          /* première solution: on utilise writeBytes().
           * attention, writeChars() ne marche pas, car les caractères
           * sont stockés sur 2 octets en Java. */

          data.writeBytes(status200 + "\r\n\r\n");
          data.write(fichier);

        } else {
          /* le fichier n'existe pas => erreur 404 */
          /* autre solution: on écrit avec write(String.getbyte()) */
          // Créer une page web et écrire "Erreur 404"
          data.writeBytes(status404 + "\r\n\r\n");
          data.writeBytes(HTML_BODY);
          data.writeBytes("<h1>Erreur 404</h1>");
          data.writeBytes("<p>Le fichier " + requete[1] + " n'existe pas.</p>");
          data.writeBytes("</body></html>");
          System.out.println("> " + status404);
        }
      } else {
        data.write(status501.getBytes());
        data.write(crlf);
        data.write(crlf);
        System.out.println("> " + status501);
      }
      data.close();
      output.close();
      br.close();
      clientS.close();
    }
    // ss.close();
  }
}
