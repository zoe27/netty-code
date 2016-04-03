package netty.bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TimeServer {

    public static void main(String[] args) {
        
        int port = 8080;
        
        ServerSocket server = null;
        
        try {
            server = new ServerSocket(port);
            System.out.println("the server is started in port " + port);
            Socket socket = null;
            
            while(true){
                socket = server.accept();
                new Thread(new TimeServerHandler(socket)).start();
            }
        } catch (Exception e) {
            // TODO: handle exception
        }finally{
            if (server != null) {
                System.out.println("the bio time server close");
                try {
                    server.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                server = null;
            }
        }

    }

}
