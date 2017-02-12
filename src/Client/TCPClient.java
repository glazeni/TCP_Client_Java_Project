// TCPClient.java
// A client program implementing TCP socket
package Client;

import java.net.*;
import java.io.*;

//Java Client Main Class
public class TCPClient extends Thread {

    public Socket s_up = null;
    public Socket s_down = null;
    public Socket sControl = null;
    private Connection connection_up = null;
    private Connection connection_down = null;
    private Connection connectionControl = null;
    private TCP_Properties TCP_param = null;
    private DataMeasurement dataMeasurement = null;
    private String ALGORITHM = "MV_readVector";

    public TCPClient() {
        try {
            if (ALGORITHM == "MV" || ALGORITHM=="MV_readVector" || ALGORITHM == "ACKTiming") {
                s_up = new Socket(Constants.SERVER_IP, Constants.SERVERPORT);
                s_down = new Socket(Constants.SERVER_IP, Constants.SERVERPORT);
                sControl = new Socket(Constants.SERVER_IP, Constants.SERVERPORT);
                TCP_param = new TCP_Properties(s_up);
                TCP_param = new TCP_Properties(s_down);
                TCP_param = new TCP_Properties(sControl);
                dataMeasurement = new DataMeasurement();
                connection_up = new Connection(s_up, dataMeasurement);
                connection_down = new Connection(s_down, dataMeasurement);
                connectionControl = new Connection(sControl, dataMeasurement);
            } else {
                s_up = new Socket(Constants.SERVER_IP, Constants.SERVERPORT);
                TCP_param = new TCP_Properties(s_up);
                dataMeasurement = new DataMeasurement();
                connection_up = new Connection(s_up, dataMeasurement);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            if (ALGORITHM == "MV"|| ALGORITHM=="MV_readVector" || ALGORITHM == "ACKTiming") {
                connection_up.start();
                connection_down.start();
                connectionControl.start();
            } else {
                connection_up.start();
            }
            System.err.println("Client started connected to Port: " + Constants.SERVERPORT + "\n");
        } catch (Exception ex) {
            System.err.println("Client connection error: " + ex.getMessage());
        }
    }
    
    public static void main(String args[]){
        TCPClient tcpClient = new TCPClient();
        tcpClient.start();
    }
}
