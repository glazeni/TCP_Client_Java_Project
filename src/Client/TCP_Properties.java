/*
 * Class that defines TCP Socket Properties
 */
package Client;

import java.net.*;
import java.io.*;

public class TCP_Properties {

    public TCP_Properties(Socket s) throws SocketException {
        //s.setPerformancePreferences(0, 0, 1);
        s.setSendBufferSize(Constants.SOCKET_RCVBUF);
        s.setReceiveBufferSize(Constants.SOCKET_RCVBUF);
        s.setSoTimeout(Constants.SO_TIMEOUT);
        s.setTcpNoDelay(true);
        s.setSoLinger(false, 0);

    }
}
