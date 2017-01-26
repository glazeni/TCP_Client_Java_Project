// TCPClient.java
// A client program implementing TCP socket
package Client;

import java.net.*;
import java.io.*;

//Java Client Main Class
public class TCPClient extends Thread {

    public Socket s = null;
    private Connection connection = null;
    private TCP_Properties TCP_param = null;
    private DataMeasurement dataMeasurement = null;

    public TCPClient() {
        try {
            s = new Socket(Constants.SERVER_IP, Constants.SERVERPORT);
            TCP_param = new TCP_Properties(s);
            dataMeasurement = new DataMeasurement();
            connection = new Connection(s, dataMeasurement);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            connection.start();
            System.err.println("Client started connected to Port: " + Constants.SERVERPORT + "\n");
        } catch (Exception ex) {
            System.err.println("Client connection error: " + ex.getMessage());
        }

    }

    public static void main(String args[]) {
//        Constants.BLOCKSIZE_UPLINK = Integer.parseInt(args[0]);
//        Constants.BLOCKSIZE_DOWNLINK = Integer.parseInt(args[1]);
//        Constants.SOCKET_RCVBUF = Integer.parseInt(args[2]);
//        Constants.SOCKET_SNDBUF = Integer.parseInt(args[3]);
//        Constants.SO_TIMEOUT = Integer.parseInt(args[4]);
//        Constants.NUMBER_BLOCKS = Integer.parseInt(args[5]);

        //Client New instance
        TCPClient tcpClient = new TCPClient();
        tcpClient.start();
    }
}
