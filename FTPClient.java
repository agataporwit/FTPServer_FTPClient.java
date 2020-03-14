import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Scanner;
import java.net.Socket;
import java.io.IOException;
import java.io.PrintWriter;

/*
**********************************
****** Agata Porwit (Jelen)*******
****** Tom Abbott CSD 322 ********
****** Assignment 6, FTP *********
**********************************
*/


//username: anonymous

public class FTPClient {
    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.err.println("Pass the server IP as the sole command line argument");
            return;
        }
        try (Socket socket = new Socket(args[0], 8080)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner in = new Scanner(socket.getInputStream());
            Scanner input = new Scanner(System.in);
            String[] command; // command holder
            String line = ""; // String init.
            String fileToReceive = "StoredFiles";
            makeDirectory(fileToReceive);
            line = getPrintWriterResponse(line, in);
            while (!line.equals("goodbye")) {
                System.out.print("ftp> ");
                line = input.nextLine();  // what we entered
                System.out.println("You entered " + line);
                command = line.split(" ", 3); // split string up

                // handling commands on a client side <(._.)>
                switch (command[0]) {
                    case "get":
                        out.println(line);
                        if ((command.length == 2) && in.nextLine().equals("Ready")) {
                            line = in.nextLine();
                            int current = in.nextInt();
                            byte[] byteArray = new byte[current];
                            FileOutputStream fos = new FileOutputStream(fileToReceive + "/" + line);
                            //while on till we get all the info in file
                            while (current >= 1) {
                                //read current amount of bits
                                int bytesRead = socket.getInputStream().read(byteArray, 0, current);
                                // write that many bits
                                fos.write(byteArray, 0, bytesRead);
                                current -= bytesRead;
                                byteArray = new byte[current];
                            }
                            fos.flush();
                            in.nextLine();
                        }
                        line = getPrintWriterResponse(line, in);

                        break;
                    case "put":
                        if (command.length == 2) {
                            try {
                                //get the file we want to send over
                                File myFile = new File(command[1]);
                                if (myFile.exists()) {
                                    //output the command
                                    out.println(line);
                                    // get the name of the file and send it over
                                    out.println(myFile.getName());
                                    byte[] byteArray = new byte[(int) myFile.length()];
                                    // send the size of the file
                                    out.println(byteArray.length);
                                    FileInputStream fis = new FileInputStream(myFile);
                                    BufferedInputStream bis = new BufferedInputStream(fis);
                                    bis.read(byteArray, 0, byteArray.length);
                                    System.out.println("Sending " + command[1] + "(" + byteArray.length + " bytes)");
                                    // send byte array over Stream
                                    socket.getOutputStream().write(byteArray, 0, byteArray.length);
                                    socket.getOutputStream().flush();
                                    bis.close();
                                    //look for response
                                    line = getPrintWriterResponse(line, in);
                                }
                            } catch (IOException e) {
                                System.out.println("An error occurred.");
                            }
                        }
                        break;
                    default:
                        out.println(line);
                        line = getPrintWriterResponse(line, in);

                }
            }
            out.close();
            in.close();
            input.close();
            socket.close();
        }
    }
    // getting response from the server
    private static String getPrintWriterResponse(String line, Scanner in) {
        while (true) {
            line = in.nextLine();
            if (line.equals("") || line.equals("goodbye")) {
                return line;
            }
            System.out.println("Server response: " + line);
        }
    }
    // make a Directory for files 
    private static boolean makeDirectory(String name) {
        try {
            File myObj = new File(name);
            if (myObj.mkdir()) {
                System.out.println("Folder created: " + myObj.getName());
            } else {
                System.out.println("Folder already exists.");
                return false;
            }
        } catch (Exception e) {
            System.out.println("An error occurred."); //Huston we have a problem
            return false;
        }
        return true;
    }
}

