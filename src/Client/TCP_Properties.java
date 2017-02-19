/*
 * Class that defines TCP Socket Properties
 */
package Client;

import java.net.*;

public class TCP_Properties {

    public TCP_Properties(Socket s, boolean isNagleDisable) throws SocketException {
        //s.setPerformancePreferences(0, 0, 1); 

        s.setSendBufferSize(Constants.SOCKET_RCVBUF);
        s.setReceiveBufferSize(Constants.SOCKET_RCVBUF);
        s.setSoTimeout(Constants.SO_TIMEOUT);
        s.setTcpNoDelay(isNagleDisable);//true- Disable Nagle's Algorithm / false-otherwise
        s.setSoLinger(false, 0); //Set to false: The connection will be closed only when the data transmitted 
        //to the socket has been successfully delivered
    }
}
