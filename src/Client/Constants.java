/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author glazen
 */
package Client;

public class Constants {
    
    public static int NUMBER_BLOCKS = 100;
    public static int BLOCKSIZE_UPLINK = 1500; //Mobiperf uses 1358 1460 without TCPHeader
    public static int BLOCKSIZE_DOWNLINK = 1500; //Mobiperf uses 2600
    public static int SO_TIMEOUT = 10000; //10sec - Receiving Timeout
    public static int SOCKET_SNDBUF = 64000; //64 Kb
    public static int SOCKET_RCVBUF = 64000; //64 Kb
    public static int SERVERPORT = 20000;
    public static String SERVER_IP = "10.22.124.250";

}
