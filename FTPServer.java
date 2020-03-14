import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/*
 **********************************
 ****** Agata Porwit (Jelen)*******
 ****** Tom Abbott CSD 322 ********
 ****** Assignment 6, FTP *********
 **********************************
 */


//If you're reading this, then my program is probably a success


// simply copied from another code assignment
public class FTPServer {
    public static void main(String[] args) throws Exception {
        try (ServerSocket listener = new ServerSocket(8080)) {
            System.out.println("The FTP-server server is running...");
            ExecutorService pool = Executors.newFixedThreadPool(20);
            while (true) {
                pool.execute(new FTP(listener.accept()));
            }
        }
    }
    // class for Socket - Please work (>_<)
    private static class FTP implements Runnable {
        private Socket socket;
        FTP(Socket socket) {
            this.socket = socket;
        }
        @Override
        public void run() {

            System.out.println("Connected: " + socket);

            try {
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();
                PrintWriter outWriter = new PrintWriter(out,true);
                Scanner inScanner = new Scanner(in);
                int bytesRead = 0;
                int current = 0;
                boolean loggedin = false;
                String[] command;
                String line;
                String fileToReceive="StoredFiles";
                makeDirectory(fileToReceive);
                outWriter.println("Please provide your user name \n");
                while (true) {
                    line = inScanner.nextLine();
                    command = line.split(" ", 3);
                    // checking if the user is logged in
                    if (loggedin == true){
                        //get, put,dir,
                        switch(command[0]) {
                            case "get":
                                if(command.length == 2){
                                    File myFile = new File (fileToReceive+"/"+command[1]);
                                    if(myFile.exists()){
                                        outWriter.println("Ready");

                                        outWriter.println(myFile.getName());
                                        // bytes array for fill info
                                        byte [] byteArray  = new byte [(int)myFile.length()];
                                        // send the size of the file
                                        outWriter.println(byteArray.length);
                                        FileInputStream fis = new FileInputStream(myFile);
                                        BufferedInputStream bis = new BufferedInputStream(fis);
                                        bis.read(byteArray,0,byteArray.length);

                                        out.write(byteArray,0,byteArray.length);

                                        out.flush();
                                        bis.close();
                                        fis.close();

                                        outWriter.println("Ready\n");
                                    }
                                    else {
                                        outWriter.println("does not exist");
                                        outWriter.println("...huh?...\n");
                                    }
                                }

                                break;
                            case "put":
                                if(command.length == 2){
                                    line = inScanner.nextLine();
                                    current = inScanner.nextInt();
                                    byte [] byteArray  = new byte [current];
                                    FileOutputStream fos = new FileOutputStream(fileToReceive+"/"+line);
                                    while(current >=1 ){
                                        //read current amount of bits
                                        bytesRead = in.read(byteArray,0,current);
                                        // write that many bits
                                        fos.write(byteArray, 0 , bytesRead);
                                        current -= bytesRead;
                                        byteArray  = new byte [current];
                                    }
                                    fos.flush();
                                    outWriter.println("Ready\n");
                                }
                                else {
                                    outWriter.println("...huh?...\n");
                                }
                                inScanner.nextLine();
                                break;
                            case "dir":
                                outWriter.println(dir(fileToReceive));
                                break;
                            case "help":
                                outWriter.println(help());
                                break;
                            case "del":
                                if(command.length == 2){
                                    if(del(fileToReceive+"/"+command[1])){
                                        outWriter.println("done\n");
                                    }
                                    else{
                                        outWriter.println("could not find the file\n");
                                    }
                                }
                                else {
                                    outWriter.println("...huh?...\n");
                                }
                                break;
                            case "rename":
                                if(command.length == 3){
                                    if(rename(fileToReceive+"/"+command[1],fileToReceive+"/"+command[2])){
                                        outWriter.println("done\n");
                                    }
                                    else{
                                        outWriter.println("could not find\n");
                                    }
                                }
                                else {
                                    outWriter.println("...huh?...\n");
                                }
                                break;
                            case "bye": // This comment is self explanatory.
                                outWriter.println("goodbye");
                                out.close();
                                in.close();
                                outWriter.close();
                                inScanner.close();
                                bye();
                                break;
                            default:
                                outWriter.println("...huh?...\n");
                        }
                    }
                    else{
                        //login
                        switch(line) {
                            case "anonymous":
                                outWriter.println("Please enter password \n");
                                inScanner.nextLine();
                                outWriter.println("You are now logged in \n");
                                loggedin = true;
                                break;
                            default:{
                                outWriter.println("...huh?...\n");
                            }
                        }
                    }
                }
            }
            catch (IOException e) {
                System.out.println("Error:" + socket);
            }
        }
        //list of files in directory
        private String dir (String fileToReceive){
            String files ="";
            File folder = new File(fileToReceive);
            File[] listOfFiles = folder.listFiles();
            for (File listOfFile : listOfFiles) {
                if (listOfFile.isFile()) {
                    files +=("File " + listOfFile.getName()+"\n");
                } else if (listOfFile.isDirectory()) {
                    files += ("Directory " + listOfFile.getName()+"\n");
                }
            }
            return files;
        }
        // get a String of commands
        private String help () {
            return "commands \n"
                    + "get <filename> - " + "retrieves a file with specified name\n"
                    + "put <filename> - " + "put a file in designated directory,  with an absolute path\n"
                    + "dir - " + "shows files list located in directory\n"
                    + "help - " + "shows menu of commands\n"
                    + "del <filename> - " + "deletes the file from directory with a specified name\n"
                    + "rename <from> <to> - " + "rename a file from <from> <to>\n"
                    + "bye - " + "closes the server socket\n"
                    + "";
        }
        // delete a file
        private boolean del (String filename){
            try{
                File myObj = new File(filename);
                if (myObj.delete()) {
                    System.out.println("File deleted" + filename);
                }
                else {
                    System.out.println("File not found.");
                    return false;
                }
            }
            catch (Exception e) {
                System.out.println("An error occurred.");
                return false;
            }
            return true;
        }
        // rename a file
        private boolean rename (String from , String to){
            try {
                File f1 = new File(from);
                File f2 = new File(to);
                return f1.renameTo(f2);
            }
            catch (Exception e) {
                System.out.println("An error occurred.");
                return false;
            }
        }
        // close the socket
        private void bye (){
            try {
                socket.close();
            }
            catch (IOException e) {}
            System.out.println("Closed: " + socket);
        }
        // create a Directory for managing files
        private boolean makeDirectory(String name){
            try {
                File myObj = new File(name);
                if (myObj.mkdir()) {
                    System.out.println("Folder created: " + myObj.getName());
                }
                else {
                    System.out.println("Folder already exists.");
                    return false;
                }
            }
            catch (Exception e) {
                System.out.println("An error occurred.");
                return false;
            }
            return true;
        }
    }
}