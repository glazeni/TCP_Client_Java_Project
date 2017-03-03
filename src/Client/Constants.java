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

    public static int NUMBER_PACKETS = 50;
    public static int PACKETSIZE = 512; //Only used in Packet Train Method
    public static int BUFFERSIZE = 256000; // 256Kb
    public static int SO_TIMEOUT = 30000; //30sec - Receiving/Writting Timeout
    public static int SOCKET_SNDBUF = 128000; //128 Kb
    public static int SOCKET_RCVBUF = 128000; //128 Kb
    public static int SERVERPORT = 11008;
    // the gap of the size (in ns = 10^(-9)s)
    public static final double msParameter = 0.5;
    public static final long pktGapNS = (long) (msParameter * java.lang.Math.pow(10.0, 6.0));
    public static String SERVER_IP = "193.136.127.218";

}
