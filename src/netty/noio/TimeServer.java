package netty.noio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import netty.bio.TimeServerHandler;

public class TimeServer {

    public static void main(String[] args) throws IOException{
        int port = 8080;
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
            System.out.println("the false nio server start in port :" + port);
            Socket socket = null;
            
            TimeServerHandlerExecutoPool singleExecutor = new TimeServerHandlerExecutoPool(50, 1000);
            while(true){
                socket = server.accept();
                singleExecutor.execute(new TimeServerHandler(socket));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally{
            if (server != null) {
                System.out.println("false nio server close");
                server.close();
                server = null;
            }
        }
        

    }

}
