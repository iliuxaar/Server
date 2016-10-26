
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Server {
    BufferedReader in = null;
    PrintWriter out= null;
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    String input,request[],output;

    public Server() throws IOException {
        System.out.println("Welcome to ServerLauncher side");
    }

    private void initSocket() throws IOException {
        try {
            serverSocket = new ServerSocket(4444);
        } catch (IOException e) {
            System.out.println("Couldn't listen to port 4444");
            System.exit(-1);
        }
        accept();
        initBuffers();
    }

    private void accept(){
        try {
            System.out.print("Waiting for a client...");
            clientSocket= serverSocket.accept();
            System.out.println("ClientLauncher connected");
        } catch (IOException e) {
            System.out.println("Can't accept");
            System.exit(-1);
        }
    }

    private void initBuffers() throws IOException {
        in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(),true);
    }

    public void run() throws IOException, InterruptedException {
        System.out.println("Wait for messages");
        clientSocket.setSoTimeout(45000);
        try{
            while ((input = in.readLine()) != null) {
                out.flush();
                output = new String();
                request = input.split(" ");
                chooseCommand(request);
                System.out.println(input);
            }
        }catch (SocketTimeoutException e){
            System.out.println("Socket timeout");
            out.println("timeout");
        }catch (SocketException e){
            System.out.println("Client Socket Error!");
        }
    }

    private void closeConnection() throws IOException {
        out.close();
        in.close();
        clientSocket.close();
        serverSocket.close();
    }

    private void timeCommand(){
        Date date = new Date();
        DateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        output = formatter.format(date);
        out.println("time ::: " + output);
    }

    private void exitCommand() throws IOException {
        closeConnection();
    }

    private void echoCommand(String[] request){
        for (int i = 1;i<request.length;i++){
            output += request[i] + " ";
        }
        out.println("echo ::: " + output);
    }

    private void chooseCommand(String[] request) throws IOException {
        switch (request[0]){
            case "echo":case"ECHO":
                echoCommand(request);
                break;
            case "time":case "TIME":
                timeCommand();
                break;
            case "exit":case "EXIT":
                exitCommand();
                return;
            case "upload":case"UPLOAD":
                recieveFile();
                break;
            case "download":case"DOWNLOAD":
                sendFile(request[1],0);
                break;
            default:
                out.println("Unknown command");
                break;

        }
    }


    private void sendFile(String path, int number){
        File f = new File(path);
        BufferedOutputStream bos = null;
        FileInputStream fis = null;
        System.out.println("Sending " + f.getName() + "...");
        out.println("DOWNLOAD "+f.getName());
        try {
            if(number == 0) {
                byte[] byteArray = new byte[64 * 1024];
                fis = new FileInputStream(f.getPath());
                long s;
                s = f.length();
                double left = f.length();
                out.println("READY");
                out.println(s);
                bos = new BufferedOutputStream(clientSocket.getOutputStream());
                Thread.sleep(500);
                while (s > 0) {
                    int i = fis.read(byteArray);
                    left -= i;
                    System.out.println("left " + left/1024 + " Mb");
                    bos.write(byteArray, 0, i);
                    s -= i;
                }
            }
            else{
                long s = number;
                double left = number;
            }
            System.out.println("Upload complete");
            out.flush();
            bos.flush();
            fis.close();
        } catch (FileNotFoundException e) {
            System.err.println("File not found!");
            out.println("FILEERROR");
            System.out.println("File not found!");
            return;
        } catch (SocketException e){
            System.out.println("Socket disconnected. Trying to reconnect ...");

        } catch(IOException e) {
            System.err.println("IOException");
            return;
        } catch (Exception e) {

        }
    }

    private void recieveFile() {
        try {
            out.println("Ready to upload");
            String filename = in.readLine();
            if(in.readLine().equals("FILEERROR")){
                System.out.println("File not found!");
                return;
            }
            long s = Long.parseLong(in.readLine());
            double left = s;
            System.out.println("File size: " + s);
            byte[] byteArray = new byte[1024*64];
            File f = new File(filename);
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(f);
            BufferedInputStream bis = new BufferedInputStream(clientSocket.getInputStream());
            System.out.println("Begin downloading");
            while (s > 0) {
                int i = bis.read(byteArray);
                left -=i;
                fos.write(byteArray, 0, i);
                System.out.println("left " + left/1024 + " Mb");
                s-= i;
            }
            fos.close();
            System.out.println("Complete downloading " + filename);
        } catch (IOException e) {
            System.err.println("Recieve IO Error");
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = new Server();
        server.initSocket();
        while(true){
            server.accept();
            server.initBuffers();
            server.run();
        }

    }
}
