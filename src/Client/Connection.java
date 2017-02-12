/*
 * Class that create the connection and that handles the sending of random bytes
 */
package Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.Vector;

public class Connection extends Thread {

    private Socket s = null;
    private RTInputStream RTin;
    private RTOutputStream RTout;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private DataMeasurement dataMeasurement;
    private ReminderClient reminderClient;
    private Socket socketControl = null;
    public static int byteCnt = 0;
    public static int byteSecond = 0;
    private String METHOD = null;
    private double AvaBW = 0;
    private Vector<Double> AvailableBW = null;
    private long runningTime = 0;

    public Connection(Socket _s, DataMeasurement _dataMeasurement) {
        try {
            this.s = _s;
            this.dataMeasurement = _dataMeasurement;
            RTin = new RTInputStream(s.getInputStream());
            RTout = new RTOutputStream(s.getOutputStream());
            dataIn = new DataInputStream(RTin);
            dataOut = new DataOutputStream(RTout);
            AvailableBW = new Vector<Double>();
        } catch (Exception e) {
            System.out.println("Error in connection:" + e.getMessage());
        }
    }

    public void run() {
        try {
            METHOD = dataIn.readUTF();
            System.err.println("METHOD: " + METHOD);
            switch (METHOD) {
                case "PGM":
                    Method_PGM();
                    break;
                case "PT":
                    Method_PT();
                    break;
                case "MV_Uplink":
                    Method_MV_Uplink_Client();
                    break;
                case "MV_Downlink":
                    Method_MV_Downlink_Client();
                    break;
                case "MV_Report":
                    Method_MV_Report_Client();
                    break;
                case "MV_readVectorUP":
                    Method_MV_UP_readVector_Client();
                    break;
                case "MV_readVectorDOWN":
                    Method_MV_DOWN_readVector_Client();
                    break;
                case "MV_Report_readVector":
                    Method_MV_Report_readVector_Client();
                    break;
                case "ACKTiming_UP":
                    Method_ACKTimingUP_Client();
                    break;
                case "ACKTiming_DOWN":
                    Method_ACKTimingDOWN_Client();
                    break;
                case "ACKTiming_Report":
                    Method_ACKTiming_Report_Client();
                    break;
                case "ACKTIMING":
                    //Parameters
                    Constants.SOCKET_RCVBUF = 14600;
                    Constants.SOCKET_RCVBUF = 14600;
                    Constants.NUMBER_BLOCKS = 10000;
                    //Measurements
                    dataOut.writeByte(1);
                    while (System.currentTimeMillis() < System.currentTimeMillis() + 10000) {
                        //uplink_Client_snd();
                    }
                    dataOut.writeByte(2);
                    while (System.currentTimeMillis() < System.currentTimeMillis() + 10000) {
                        downlink_Client_rcv();
                    }
                    break;
                default:
                    System.err.println("INVALID MEHTHOD");
                    break;
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
            byte[] snd_buf = new byte[Constants.BLOCKSIZE];

            dataOut.writeInt(num_blocks);
            dataOut.flush();
            System.out.println("\n uplink_Client_snd with " + "Number Blocks=" + num_blocks);
            for (int i = 0; i < num_blocks; i++) {
                RTout.write(snd_buf);
                RTout.writeTimeVector.add(System.currentTimeMillis());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            System.err.println("uplink_DONE");
        }
    }

    private boolean uplink_Client_sndInSeconds() {
        boolean keepRunning = true;
        try {
            byte[] snd_buf = new byte[Constants.BLOCKSIZE];
            while (keepRunning) {
                RTout.write(snd_buf);
                dataMeasurement.aux_writeTimeVector.add(System.currentTimeMillis());
            }
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            keepRunning = false;
        }
    }

    private boolean downlink_Client_rcvInSeconds(long _end) {
        try {
            byte[] rcv_buf = new byte[Constants.BLOCKSIZE];
            int n = 0;
            System.out.println("\n downlink_Client_rcvInSeconds");
            //Initialize Timer
            if (METHOD.equalsIgnoreCase("MV_Downlink")) {
                reminderClient = new ReminderClient(1, this.dataMeasurement);
            }
            long now = System.currentTimeMillis();
            while (System.currentTimeMillis() < _end) {

                byteCnt = 0;
                //Cycle to read each block
                do {
                    n = RTin.read(rcv_buf, byteCnt, Constants.BLOCKSIZE - byteCnt);

                    if (n > 0) {
                        byteCnt += n;
                        byteSecond += n;
                        if ((System.currentTimeMillis() >= (now + 1000)) && METHOD.equalsIgnoreCase("MV_readVectorDOWN")) {
                            System.out.println("ENTROU");
                            now = System.currentTimeMillis();
                            dataMeasurement.add_SampleSecond_down(byteSecond, System.currentTimeMillis());
                            byteSecond = 0;
                        }
                    } else {
                        System.out.println("Read n<0");
                        break;
                    }

                    if (byteCnt < Constants.BLOCKSIZE) {
                        System.err.println("Read " + n + " bytes");
                        //Keep reading MTU
                    } else {
                        //MTU is finished
                        if (METHOD.equalsIgnoreCase("MV_Downlink")) {
                            dataMeasurement.add_SampleSecond_down(byteCnt, System.currentTimeMillis());
                        }
                        break;
                    }

                } while ((n > 0) && (byteCnt < Constants.BLOCKSIZE));

                if (n < 0) {
                    System.out.println("Exited with n=-1");
                    break;
                }
            }
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            if (METHOD.equalsIgnoreCase("MV_Downlink")) {
                reminderClient.timer.cancel();
            }
        }
    }

    private void downlink_Client_rcv() {
        try {
            byte[] rcv_buf = new byte[Constants.BLOCKSIZE];
            int num_blocks = 0, n = 0;
            num_blocks = dataIn.readInt();
            System.out.println("\n downlink_Client_rcv with " + "Number Blocks=" + num_blocks);

            for (int i = 0; i < num_blocks; i++) {
                byteCnt = 0;
                //Cycle to read each block
                do {
                    n = RTin.read(rcv_buf, byteCnt, Constants.BLOCKSIZE - byteCnt);

                    if (n > 0) {
                        byteCnt += n;
                    }

                    if (byteCnt < Constants.BLOCKSIZE) {
                        //Keep reading MTU
                    } else {
                        RTin.readTimeVector.add(System.currentTimeMillis());
                        System.out.println("Reach the end of the block " + i + " with " + n + " bytes read & byteCount=" + byteCnt);
                        break;
                    }
                } while ((n > 0) && (byteCnt < Constants.BLOCKSIZE));

                if (n == -1) {
                    System.out.println("Exited with n=-1");
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private double PacketTrain() {
        AvaBW = 0;
        int length = RTin.readTimeVector.size() - 1;
        double deltaN = RTin.readTimeVector.get(length) - RTin.readTimeVector.get(0);
        int N = Constants.NUMBER_BLOCKS;
        int L = Constants.BLOCKSIZE;
        AvaBW = (((N - 1) * L) / deltaN);
        System.err.println("AvaBW: " + AvaBW);
        System.out.println("PTprocess is DONE");
        return AvaBW;
    }

    private void Method_PGM() {
        //Parameters
        Constants.SOCKET_RCVBUF = 2920;
        Constants.SOCKET_RCVBUF = 2920;
        Constants.NUMBER_BLOCKS = 100;
        //Measurements
        try {
            //Uplink
            dataIn.readByte();
            uplink_Client_snd();
            //Downlink
            dataIn.readByte();
            downlink_Client_rcv();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Calculate Deltas
            for (int i = 3; i < RTin.readTimeVector.size(); i++) {
                if (i % 2 == 1) {
                    dataMeasurement.deltaINVector_uplink.add(RTout.writeTimeVector.get(i) - RTout.writeTimeVector.get(i - 1));
                    dataMeasurement.deltaOUTVector_downlink.add(RTin.readTimeVector.get(i) - RTin.readTimeVector.get(i - 1));
                }
            }
        }
        //Report Measurements
        try {
            dataOut.writeByte(1);
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
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            RTin.readTimeVector.clear();
            RTout.writeTimeVector.clear();
            System.err.println("Method_PGM along with Report is done!");
        }

    }

    private void Method_PT() {
        //Parameters
        Constants.SOCKET_RCVBUF = 14600;
        Constants.SOCKET_RCVBUF = 14600;
        Constants.NUMBER_BLOCKS = 10;

        //Measurements
        try {
            //Uplink
            dataIn.readByte();
            for (int p = 0; p < 10; p++) {
                uplink_Client_snd();
            }
            //Downlink
            AvailableBW.clear();
            dataIn.readByte();
            for (int p = 0; p < 10; p++) {
                downlink_Client_rcv();
                AvailableBW.add(PacketTrain());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        //Report Measurements - AvailableBW_down Vector
        try {
            AvailableBW.clear();
            dataOut.writeByte(2);
            dataOut.writeInt(AvailableBW.size());
            for (int k = 0; k < AvailableBW.size(); k++) {
                dataOut.writeDouble(AvailableBW.get(k));
                dataOut.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            RTin.readTimeVector.clear();
            RTout.writeTimeVector.clear();
            System.err.println("Method_PT along with report is done!");
        }

    }

    private void Method_MV_Uplink_Client() {
        //Parameters
        Constants.SOCKET_RCVBUF = 14600;
        Constants.SOCKET_RCVBUF = 14600;

        //Measurements
        try {
            //Uplink
            dataIn.readByte();
            uplink_Client_sndInSeconds();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    private void Method_MV_Downlink_Client() {
        //Parameters
        Constants.SOCKET_RCVBUF = 14600;
        Constants.SOCKET_RCVBUF = 14600;

        //Measurements
        try {
            //Downlink
            dataIn.readByte();
            long end = System.currentTimeMillis() + runningTime;
            downlink_Client_rcvInSeconds(end);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void Method_MV_Report_Client() {
        //Report 1secBytes Vector, sending size first 
        try {
            dataOut.writeByte(3);
            dataOut.writeInt(dataMeasurement.SampleSecond_down.size());
            for (int k = 0; k < dataMeasurement.SampleSecond_down.size(); k++) {
                dataOut.writeInt(dataMeasurement.SampleSecond_down.get(k).bytesRead);
                dataOut.flush();
                dataOut.writeLong(dataMeasurement.SampleSecond_down.get(k).sampleTime);
                dataOut.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            RTin.readTimeVector.clear();
            RTout.writeTimeVector.clear();
            dataMeasurement.SampleSecond_down.clear();
            System.err.println("Method_MV_Client along with Report is done!");
        }
    }

    private void Method_MV_UP_readVector_Client() {
        //Parameters
        Constants.SOCKET_RCVBUF = 14600;
        Constants.SOCKET_RCVBUF = 14600;

        //Measurements
        try {
            //Uplink
            dataIn.readByte();
            uplink_Client_sndInSeconds();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void Method_MV_DOWN_readVector_Client() {
        //Parameters
        Constants.SOCKET_RCVBUF = 14600;
        Constants.SOCKET_RCVBUF = 14600;

        //Measurements
        try {
            //Downlink
            dataIn.readByte();
            long end = System.currentTimeMillis() + runningTime;
            downlink_Client_rcvInSeconds(end);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void Method_MV_Report_readVector_Client() {
        //Report 1secBytes Vector, sending size first 
        try {
            dataOut.writeByte(3);
            dataOut.writeInt(dataMeasurement.SampleSecond_down.size());
            for (int k = 0; k < dataMeasurement.SampleSecond_down.size(); k++) {
                dataOut.writeInt(dataMeasurement.SampleSecond_down.get(k).bytesRead);
                dataOut.flush();
                dataOut.writeLong(dataMeasurement.SampleSecond_down.get(k).sampleTime);
                dataOut.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            RTin.readTimeVector.clear();
            RTout.writeTimeVector.clear();
            dataMeasurement.SampleSecond_down.clear();
            System.err.println("Method_MV_readVector_Client along with Report is done!");
        }
    }

    private void Method_ACKTimingUP_Client() {
        //Parameters
        Constants.SOCKET_RCVBUF = 14600;
        Constants.SOCKET_RCVBUF = 14600;

        //Measurements
        try {
            //Uplink
            dataIn.readByte();
            uplink_Client_sndInSeconds();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void Method_ACKTimingDOWN_Client() {
        //Parameters
        Constants.SOCKET_RCVBUF = 14600;
        Constants.SOCKET_RCVBUF = 14600;

        //Measurements
        try {
            //Downlink
            dataIn.readByte();
            long end = System.currentTimeMillis() + runningTime;
            downlink_Client_rcvInSeconds(end);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void Method_ACKTiming_Report_Client() {
        //Report ACKTiming Vector, sending size first 
        try {
            dataOut.writeByte(4);
            dataOut.writeInt(dataMeasurement.aux_writeTimeVector.size());
            for (int k = 0; k < dataMeasurement.aux_writeTimeVector.size(); k++) {
                dataOut.writeLong(dataMeasurement.aux_writeTimeVector.get(k));
                dataOut.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            RTin.readTimeVector.clear();
            RTout.writeTimeVector.clear();
            dataMeasurement.aux_writeTimeVector.clear();
            System.err.println("Method_ACKTiming_Client along with Report is done!");
        }
    }
}
