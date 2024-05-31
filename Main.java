import java.net.*;
import java.io.*;

import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jsoup.Jsoup;
import org.w3c.dom.NodeList;

public class Main{

    private static final int MAX_ITEMS = 5;
    private static String[][] websites = new String[500][4];

    public static void main(final String[] args) throws Exception
    {
        mainMenu();
    }

    public static void mainMenu() throws Exception{

        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Rss Reader!");

        //reading previous data from file
        File f = new File("src/data.txt");
        FileReader fileReader = new FileReader(f);
        BufferedReader reader = new BufferedReader(fileReader);
        String line;
        int t = 0;
        reader.readLine();
        while ((line = reader.readLine()) != null){

            //adding previous data to the array
            String[] split = line.split(";");
            websites[t][0] = split[0];
            websites[t][1] = fetchPageSource(split[1]);
            websites[t][2] = extractRssUrl(split[1]);
            websites[t][3] = split[1].substring(0,split[1].length()-10);
            t++;
        }

        while (true){
            //updating the file
            FileWriter writer = new FileWriter(f);
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            for (String[] w : websites){
                if (w[0]!= null && w[3]!=null)
                    bufferedWriter.write("\n"+w[0]+";"+w[3]+"index.html"+';'+w[3]+"rss.xml\n");
            }
            bufferedWriter.close();

            //Main Menu options display
            System.out.println("Type a valid number for your desired action:\n"
                    +"[1] Show updates\n[2] Add URL\n[3] Remove URL\n[4] Exit");
            int opt = scanner.nextInt();
            switch (opt) {
                case 1:
                    showUpdates();
                    break;
                case 2:
                    addUrl();
                    break;
                case 3:
                    removeUrl();
                    break;
                case 4:
                    System.exit(0);
                default:
                    break;
            }
        }
    }

    public static void addUrl() throws Exception {

        System.out.println("Please enter website URL to add:");
        Scanner scanner = new Scanner(System.in);
        String url = scanner.next();
        String source = fetchPageSource(url);
        String title = extractPageTitle(source);

        //check if data already exists
        boolean exists = false;
        for (String[] w : websites){
            if (title.equals(w[0])){
                exists = true;
                break;
            }
        }

        if (!exists){
        // adding new data to array
            int i = 0;
            while (websites[i][0]!=null)
                i++;

            websites[i][0] = title;
            websites[i][1] = source;
            websites[i][2] = extractRssUrl(url);
            websites[i][3] = url;

            System.out.println("Added "+ url +" succesfully.");
        }
        else{
            System.out.println(url + " already exists.");
        }

    }

    public static void removeUrl() throws Exception {
        System.out.println("Please enter website URL to remove:");
        Scanner scanner = new Scanner(System.in);
        String link = scanner.next();
        String title = extractPageTitle(fetchPageSource(link));

        //check if the given url exists
        boolean exists = false;
        int i = 0;
        for (String[] w : websites){
            if (title.equals(w[0])){
                exists = true;
                break;
            }
            i++;
        }
        if (exists){
            websites[i][0] = null;
            websites[i][1] = null;
            websites[i][2] = null;
            websites[i][3] = null;

            System.out.println("Removed "+link+" succesfully.");
        }else{
            System.out.println("Couldn't find " + link);
        }

    }

    public static int showUpdates(){

        System.out.println("Show updates for:");
        System.out.println("[0] All websites");
        int t = 1;
        for (String[] w : websites){
            if (w[0]!=null) {
                System.out.println("[" + t + "]" + w[0]);
                t++;
            }
        }
        System.out.println("Enter -1 to return.");

        Scanner scanner = new Scanner(System.in);
        int opt = scanner.nextInt();
        switch (opt){
            case(0):
                //showing all websites
                for (String[] w : websites){
                    if (w[2]==null) continue;
                    retrieveRssContent(w[2]);
                }
                break;
            case(-1):
                //returning to main menu
                return -1;
            default:
                //showing selected website
                retrieveRssContent(websites[opt-1][2]);
        }
        return 1;
    }

    ///////////////////////// extract methodes:

    public static String extractPageTitle(String html){
        try{
            org.jsoup.nodes.Document doc = Jsoup.parse(html);
            return doc.select("title").first().text();
        }
        catch (Exception e){
            return "Error: no title tag found in page source!";
        }
    }

    public static String extractRssUrl(String url) throws IOException
    {
        org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
        return doc.select("[type='application/rss+xml']").attr("abs:href");
    }

    public static String fetchPageSource(String urlString) throws Exception
    {
        URI uri = new URI(urlString);
        URL url = uri.toURL();
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML ,like Gecko) Chrome/108.0.0.0 Safari/537.36");
        return toString(urlConnection.getInputStream());
    }

    private static String toString(InputStream inputStream) throws IOException
    {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream , "UTF-8"));
        String inputLine;
        StringBuilder stringBuilder = new StringBuilder();
        while ((inputLine = bufferedReader.readLine()) != null)
            stringBuilder.append(inputLine);

        return stringBuilder.toString();
    }

    public static void retrieveRssContent(String rssUrl)
    {
        try {
            String rssXml = fetchPageSource(rssUrl);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            StringBuilder xmlStringBuilder = new StringBuilder();
            xmlStringBuilder.append(rssXml);
            ByteArrayInputStream input = new ByteArrayInputStream(
                    xmlStringBuilder.toString().getBytes("UTF-8"));
            org.w3c.dom.Document doc = documentBuilder.parse(input);
            NodeList itemNodes = doc.getElementsByTagName("item");

            for (int i = 0; i < MAX_ITEMS; ++i) {
                org.w3c.dom.Node itemNode = itemNodes.item(i);
                if (itemNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    org.w3c.dom.Element element = (org.w3c.dom.Element) itemNode;
                    System.out.println("Title: " + element.getElementsByTagName("title").item(0).getTextContent())
                    ;
                    System.out.println("Link: " + element.getElementsByTagName("link").item(0).getTextContent());
                    System.out.println("Description: " + element.getElementsByTagName("description").item(0).
                            getTextContent());
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Error in retrieving RSS content for " + rssUrl + ": " + e.getMessage());
        }
    }
}