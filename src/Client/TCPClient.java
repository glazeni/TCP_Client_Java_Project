// TCPClient.java
// A client program implementing TCP socket
package Client;

import java.net.*;
import java.io.*;

//Java Client Main Class
public class TCPClient extends Thread {

    private Socket s_up = null;
    private Connection connection = null;
    private TCP_Properties TCP_param = null;
    private DataMeasurement dataMeasurement = null;
    private int ID = 0;
    private boolean isIperfSettings;
    public boolean isNagleDisable;
    
    public TCPClient(boolean _isIperfSettings, boolean _isNagleDisable) {
        try {
            this.isIperfSettings = _isIperfSettings;
            this.isNagleDisable = _isNagleDisable;
            //Data Measurement
            dataMeasurement = new DataMeasurement();
            //Socket Uplink + Connection
            s_up = new Socket(Constants.SERVER_IP, Constants.SERVERPORT);
            //Register Client in Server to get ID
            DataOutputStream dos = new DataOutputStream(s_up.getOutputStream());
            dos.writeInt(ID);
            dos.flush();
            TCP_param = new TCP_Properties(s_up, isNagleDisable);
            //Receive Client ID from Server
            DataInputStream dis = new DataInputStream(s_up.getInputStream());
            ID = dis.readInt();
            dos.writeBoolean(isIperfSettings);
            dos.flush();
            dos.writeBoolean(isNagleDisable);
            dos.flush();
            System.err.println("isIperfSettings: " + isIperfSettings + "  " +"isNagleDisable"+isNagleDisable );
            connection = new Connection(ID, s_up, dataMeasurement, isIperfSettings, isNagleDisable);

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
}
