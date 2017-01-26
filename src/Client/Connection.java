/*
 * Class that create the connection and that handles the sending of random bytes
 */
package Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;

public class Connection extends Thread {

    private Socket s = null;
    private RTInputStream RTin;
    private RTOutputStream RTout;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private DataMeasurement dataMeasurement;
    private ReminderClient reminderClient;
    public static int byteCnt = 0;

    public Connection(Socket _s, DataMeasurement _dataMeasurement) {
        try {
            this.s = _s;
            this.dataMeasurement = _dataMeasurement;
            RTin = new RTInputStream(s.getInputStream());
            RTout = new RTOutputStream(s.getOutputStream());
            dataIn = new DataInputStream(RTin);
            dataOut = new DataOutputStream(RTout);

        } catch (Exception e) {
            System.out.println("Error in connection:" + e.getMessage());
        }
    }

    public void run() {
        try {
            uplink_Client_snd();
            sleep(1000);
            downlink_Client_rcv();
            calculateDeltas();
            try {
                //Send length
                if (dataMeasurement.deltaINVector_uplink.size() == dataMeasurement.deltaOUTVector_downlink.size()) {
                    dataOut.writeInt(dataMeasurement.deltaINVector_uplink.size());
                    dataOut.flush();
                }
                //Send Delta Vectors
                for (int j = 0; j < dataMeasurement.deltaINVector_uplink.size(); j++) {
                    dataOut.writeLong(dataMeasurement.deltaINVector_uplink.get(j));
                    dataOut.writeLong(dataMeasurement.deltaOUTVector_downlink.get(j));
                    dataOut.flush();
                }
                //Send 1secBytes Vector, sending size first
                dataOut.writeInt(dataMeasurement.SampleSecond.size());
                for (int k = 0; k < dataMeasurement.SampleSecond.size(); k++) {
                    dataOut.writeInt(dataMeasurement.SampleSecond.get(k).bytesRead);
                    dataOut.flush();
                    dataOut.writeLong(dataMeasurement.SampleSecond.get(k).sampleTime);
                    dataOut.flush();
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Sending Data Failure:" + ex.getMessage());
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
            } catch (IOException ex) {
                System.err.println("Closing Client Side Socket Failure:" + ex.getMessage());
            }
        }
    }

    private void uplink_Client_snd() {
        try {
            int num_blocks = Constants.NUMBER_BLOCKS;
            byte[] snd_buf = new byte[Constants.BLOCKSIZE_UPLINK];
            new Random().nextBytes(snd_buf);

            dataOut.writeInt(num_blocks);
            dataOut.flush();

            System.out.println("\n uplink_Client_snd with " + "Number Blocks=" + num_blocks);
            for (int i = 0; i < num_blocks; i++) {
                RTout.write(snd_buf);
                RTout.writeTimeVector.add(System.currentTimeMillis());
            }
        } catch (IOException ex) {
            System.err.println("uplink_Client_snd " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void downlink_Client_rcv() {
        try {
            byte[] rcv_buf = new byte[Constants.BLOCKSIZE_DOWNLINK];
            int num_blocks = 0, n = 0;
            num_blocks = dataIn.readInt();
            System.out.println("\n downlink_Client_rcv with " + "Number Blocks=" + num_blocks);
            //Initialize Timer
            reminderClient = new ReminderClient(1, dataMeasurement);
            for (int i = 0; i < num_blocks; i++) {
                byteCnt = 0;
                //Cycle to read each block
                do {
                    n = RTin.read(rcv_buf, byteCnt, Constants.BLOCKSIZE_DOWNLINK - byteCnt);

                    if (n > 0) {
                        byteCnt += n;
                    }

                    if (byteCnt < Constants.BLOCKSIZE_DOWNLINK) {
                        //Keep reading MTU
                    } else {
                        RTin.readTimeVector.add(System.currentTimeMillis());
                        dataMeasurement.deltaByteCount_downlink.add(byteCnt);
                        System.out.println("Reach the end of the block " + i + " with " + n + " bytes read & byteCount=" + byteCnt);
                        break;
                    }
                } while ((n > 0) && (byteCnt < Constants.BLOCKSIZE_DOWNLINK));

                if (n == -1) {
                    System.out.println("Exited with n=-1");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            reminderClient.timer.cancel();
        }
    }

    private void calculateDeltas() {
        for (int i = 1; i < RTin.readTimeVector.size(); i++) {
            dataMeasurement.deltaINVector_uplink.add(RTout.writeTimeVector.get(i) - RTout.writeTimeVector.get(i - 1));
            dataMeasurement.deltaOUTVector_downlink.add(RTin.readTimeVector.get(i) - RTin.readTimeVector.get(i - 1));
        }
    }

}