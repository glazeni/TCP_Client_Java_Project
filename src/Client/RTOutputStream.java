
/* To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.OutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.util.Vector;

public class RTOutputStream extends FilterOutputStream {

    public Vector<Long> writeTimeVector = null;

    public RTOutputStream(OutputStream out) {
        super(out);
        writeTimeVector = new Vector<Long>();
    }

    @Override
    public void write(int b) throws IOException {
        long start = 0;
        super.write(b);
        super.flush();
    }

    @Override
    public void write(byte data[]) throws IOException {
        long start = 0;
        super.write(data);
        super.flush();
    }

    @Override
    public void write(byte data[], int off, int len) throws IOException {
        long start = 0;
        super.write(data, off, len);
        super.flush();
    }
}
