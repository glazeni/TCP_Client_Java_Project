
/* To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.OutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;

public class RTOutputStream extends PrintStream {

    RTOutputStream(OutputStream out) {
        super(out);
    }


    @Override
    public void write(byte data[]) throws IOException {
        super.write(data);
        super.flush();  
    }

    @Override
    public void write(byte data[], int off, int len) {
        super.write(data, off, len);
        super.flush();       
    }
    
}
