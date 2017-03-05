/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.util.Vector;

public class RTInputStream extends FilterInputStream {

    private int bytesTotal = 0;
    public static int bytesGraph=0;

    public RTInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int cnt = super.read();
        return cnt;
    }
    

    @Override
    public int read(byte data[]) throws IOException {
        int cnt = super.read(data);
        return cnt;
    }

    @Override
    public int read(byte data[], int off, int len) throws IOException {
        int count = super.read(data, off, len);

        bytesTotal += count;//Sum of all read bytes
        bytesGraph += count * 8; //Sum of all read bits to be shown in bandwidth graph
        return count;
    }
    
    public int getBytes(){
        return bytesTotal;
    }
    
    public int getBytes2Bits(){
        return bytesTotal*8;
    }
    
    public void clearBytes(){
        bytesTotal=0;
    }
}
