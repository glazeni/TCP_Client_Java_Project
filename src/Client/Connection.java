/*
 * Class that create the connection and that handles the sending of random bytes
 */
package Client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.Vector;

public class Connection extends Thread {

    private Socket s_down = null;
    private Socket s_report = null;
    private Socket s = null;
    private RTInputStream RTin = null;
    private RTOutputStream RTout = null;
    private DataInputStream dataIn = null;
    private DataOutputStream dataOut = null;
    private PrintWriter outCtrl = null;
    private BufferedReader inCtrl = null;

    private DataMeasurement dataMeasurement = null;
    private ReminderClient reminderClient = null;
    private RunShellCommandsClient runShell = null;
    private int byteCnt = 0;
    private boolean isThreadMethod;
    private String METHOD = null;
    private Vector<Integer> AvailableBW = null;
    private TCP_Properties TCP_param = null;
    private long runningTime = 35000;
    private int ID = 0;
    private boolean isNagleDisable;
    private long firstPacket = 0;
    private long lastPacket = 0;

    public Connection(int _ID, Socket _s, DataMeasurement _dataMeasurement, boolean _isNagleDisable) {
        try {
            this.ID = _ID;
            this.s = _s;
            this.dataMeasurement = _dataMeasurement;
            this.isNagleDisable = _isNagleDisable;
            RTin = new RTInputStream(s.getInputStream());
            RTout = new RTOutputStream(s.getOutputStream());
            dataIn = new DataInputStream(RTin);
            dataOut = new DataOutputStream(RTout);
            outCtrl = new PrintWriter(RTout, true);
            inCtrl = new BufferedReader(new InputStreamReader(RTin));

            AvailableBW = new Vector<Integer>();
        } catch (Exception e) {
            System.out.println("Error in connection:" + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            METHOD = dataIn.readUTF();
            System.err.println("METHOD: " + METHOD);
            switch (METHOD) {
                case "PT":
                    Method_PT();
                    break;
                case "MV_Uplink":
                    isThreadMethod = true;
                    Method_MV_Uplink_Client();
                    break;
                case "MV_Downlink":
                    isThreadMethod = true;
                    Method_MV_Downlink_Client();
                    break;
                case "MV_Report":
                    Method_MV_Report_Client();
                    break;
                case "MV_readVectorUP":
                    isThreadMethod = false;
                    Method_MV_UP_readVector_Client();
                    break;
                case "MV_readVectorDOWN":
                    isThreadMethod = false;
                    Method_MV_DOWN_readVector_Client();
                    break;
                case "MV_Report_readVector":
                    Method_MV_Report_readVector_Client();
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
        int counter = 0;
        long beforeTime = 0;
        long afterTime = 0;
        double diffTime = 0;
        long myGapSize = 0;
        try {
            byte[] payload = new byte[500];
            Random rand = new Random();
            // Randomize the payload
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) ('A' + rand.nextInt(52));
            }
            while (counter < 50) {
                // start recording the first packet send time
                if (beforeTime == 0) {
                    beforeTime = System.currentTimeMillis();
                }
                // send packet with constant gap
                outCtrl.println(new String(payload));
                outCtrl.flush();

                // create train gap in nanoseconds
                try {
                    if (myGapSize > 0) {
                        Thread.sleep(myGapSize);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                counter++;
            }
            afterTime = System.currentTimeMillis();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            diffTime = afterTime - beforeTime;
            try {
                dataOut.writeDouble(diffTime);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.err.println("uplink_DONE");
        }
    }

    private boolean uplink_Client_sndInSeconds() {
        boolean keepRunning = true;
        try {
            byte[] snd_buf = new byte[Constants.BLOCKSIZE];
            new Random().nextBytes(snd_buf);
            while (keepRunning) {
                RTout.write(snd_buf);
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
            if (isThreadMethod) {
                reminderClient = new ReminderClient(1, this.dataMeasurement, this.RTin);
            }
            while (System.currentTimeMillis() < _end) {
                byteCnt = 0;
                //Cycle to read each block
                do {
                    n = RTin.read(rcv_buf, byteCnt, Constants.BLOCKSIZE - byteCnt);

                    if (n > 0) {
                        byteCnt += n;
                        if (!isThreadMethod) {
                            dataMeasurement.add_SampleReadTime(n, System.currentTimeMillis());
                        }
                    } else {
                        System.err.println("Read n<0");
                        break;
                    }

                } while ((n > 0) && (byteCnt < Constants.BLOCKSIZE));

                if (n == -1) {
                    System.out.println("Exited with n=-1");
                    break;
                }
            }
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            if (isThreadMethod) {
                reminderClient.cancelTimer();
            }
        }
    }

    private void downlink_Client_rcv() {
        try {
            byte[] rcv_buf = new byte[Constants.BLOCKSIZE];
            int num_blocks = 0, n = 0;
            boolean isFirstPacket = true;
            num_blocks = dataIn.readInt();
            System.out.println("downlink_Client_rcv with " + "Number Blocks=" + num_blocks);
            for (int i = 0; i < num_blocks; i++) {
                byteCnt = 0;
                //Cycle to read each block
                do {
                    n = RTin.read(rcv_buf, byteCnt, Constants.BLOCKSIZE - byteCnt);

                    if (n > 0) {
                        byteCnt += n;
                        if (byteCnt >= 1460 && isFirstPacket) {
                            firstPacket = System.currentTimeMillis();
                            isFirstPacket = false;
                        }
                    }

                } while ((n > 0) && (byteCnt < Constants.BLOCKSIZE));
                lastPacket = System.currentTimeMillis();
                if (n == -1) {
                    System.out.println("Exited with n=-1");
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private int PacketTrain() {
        Double AvaBW = null;
        double deltaN = lastPacket - firstPacket;
        int N = Constants.SOCKET_RCVBUF / 1460;
        int L = Constants.BLOCKSIZE;
        AvaBW = (((N - 1) * L) / deltaN) * 8;
        System.out.println("AvaBW: " + AvaBW + "\n");
        return AvaBW.intValue();
    }

    private void Method_PT() {
        //Measurements
        try {
            //Uplink App
            dataIn.readByte();
            uplink_Client_snd();
        
        //Run Iperf
        if (isNagleDisable) {
            String cmd = "iperf3 -p 11010 -M -N -t 10 -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BLOCKSIZE + " -c 193.136.127.218";
            runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
            runShell.run();
        } else {
            String cmd = "iperf3 -p 11010 -M -t 10 -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BLOCKSIZE + " -c 193.136.127.218";
            runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
            runShell.run();
        }
        //Downlink App
        AvailableBW.clear();
        dataOut.writeByte(2);
        for (int p = 0; p < 10; p++) {
            dataOut.writeByte(2);
            downlink_Client_rcv();
            AvailableBW.add(PacketTrain());

        }
        //Run Iperf
        if (isNagleDisable) {
            String cmd = "iperf3 -p 11010 -M -N -t 10 -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BLOCKSIZE + " -c 193.136.127.218 -R";
            runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
            runShell.run();
        } else {
            String cmd = "iperf3 -p 11010 -M -t 10 -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BLOCKSIZE + " -c 193.136.127.218 -R";
            runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
            runShell.run();
        }
    }
    catch (IOException ex

    
        ) {
            ex.printStackTrace();
    }
    //Report Measurements - AvailableBW_down Vector

    
        try {
            //Report AvailableBW_down 
            dataOut.writeByte(2);
        dataOut.writeInt(AvailableBW.size());
        for (int k = 0; k < AvailableBW.size(); k++) {
            dataOut.writeInt(AvailableBW.get(k));
            dataOut.flush();
        }
        //Report Shell Vector from terminal Uplink
        dataOut.writeInt(dataMeasurement.ByteSecondShell_up.size());
        for (int b = 0; b < dataMeasurement.ByteSecondShell_up.size(); b++) {
            dataOut.writeInt(dataMeasurement.ByteSecondShell_up.get(b));
            dataOut.flush();
        }
        //Report Shell Vector from terminal Downlink
        dataOut.writeInt(dataMeasurement.ByteSecondShell_down.size());
        for (int b = 0; b < dataMeasurement.ByteSecondShell_down.size(); b++) {
            dataOut.writeInt(dataMeasurement.ByteSecondShell_down.get(b));
            dataOut.flush();
        }
    }
    catch (IOException ex

    
        ) {
            ex.printStackTrace();
    }

    
        finally {
            System.err.println("Method_PT along with report is done!");
    }

}

private void Method_MV_Uplink_Client() throws InterruptedException {
        //Measurements
        dataMeasurement.ByteSecondShell_up.clear();
        try {
            //Uplink
            dataIn.readByte();
            //Run Both Tests
            if (isNagleDisable) {
                uplink_Client_sndInSeconds();
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -N -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BLOCKSIZE + " -c 193.136.127.218";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
                runShell.run();
            } else {
                uplink_Client_sndInSeconds();
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BLOCKSIZE + " -c 193.136.127.218";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
                runShell.run();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                //Create new ClientThread for Downlink
                s_down = new Socket(Constants.SERVER_IP, Constants.SERVERPORT);
                TCP_param = new TCP_Properties(s_down, isNagleDisable);
                dataOut = new DataOutputStream(s_down.getOutputStream());
                dataOut.writeInt(this.ID);
                Thread c = new Connection(this.ID, s_down, this.dataMeasurement, isNagleDisable);
                c.start();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

    private void Method_MV_Downlink_Client() throws InterruptedException {
        //Measurements
        dataMeasurement.SampleSecond_down.clear();
        dataMeasurement.ByteSecondShell_down.clear();
        try {
            //Downlink
            dataIn.readByte();
            //Run Both Tests
            if (isNagleDisable) {
                long end = System.currentTimeMillis() + runningTime;
                downlink_Client_rcvInSeconds(end);
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -N -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BLOCKSIZE + " -c 193.136.127.218 -R";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
                runShell.run();
            } else {
                long end = System.currentTimeMillis() + runningTime;
                downlink_Client_rcvInSeconds(end);
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BLOCKSIZE + " -c 193.136.127.218 -R";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
                runShell.run();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                //Create new ClientThread for Report
                s_report = new Socket(Constants.SERVER_IP, Constants.SERVERPORT);
                TCP_param = new TCP_Properties(s_report, isNagleDisable);
                dataOut = new DataOutputStream(s_report.getOutputStream());
                dataOut.writeInt(this.ID);
                Thread c = new Connection(this.ID, s_report, this.dataMeasurement, isNagleDisable);
                c.start();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void Method_MV_Report_Client() {
        //Report 1secBytes Vector, sending size first 
        try {
            //Report MV_Downlink 
            dataOut.writeByte(3);
            dataOut.writeInt(dataMeasurement.SampleSecond_down.size());
            for (int k = 0; k < dataMeasurement.SampleSecond_down.size(); k++) {
                dataOut.writeInt(dataMeasurement.SampleSecond_down.get(k));
                dataOut.flush();
            }
            //Report Shell Vector from terminal Uplink
            dataOut.writeInt(dataMeasurement.ByteSecondShell_up.size());
            for (int b = 0; b < dataMeasurement.ByteSecondShell_up.size(); b++) {
                dataOut.writeInt(dataMeasurement.ByteSecondShell_up.get(b));
                dataOut.flush();
            }
            //Report Shell Vector from terminal Downlink
            dataOut.writeInt(dataMeasurement.ByteSecondShell_down.size());
            for (int b = 0; b < dataMeasurement.ByteSecondShell_down.size(); b++) {
                dataOut.writeInt(dataMeasurement.ByteSecondShell_down.get(b));
                dataOut.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            System.err.println("Method_MV_Client along with Report is done!");
        }
    }

    private void Method_MV_UP_readVector_Client() throws InterruptedException {
        //Measurements
        try {
            //Uplink
            dataIn.readByte();
            //Run Both Tests
            if (isNagleDisable) {
                uplink_Client_sndInSeconds();
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -N -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BLOCKSIZE + " -c 193.136.127.218";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
                runShell.run();
            } else {
                uplink_Client_sndInSeconds();
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BLOCKSIZE + " -c 193.136.127.218";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
                runShell.run();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                //Create new ClientThread for Downlink
                s_down = new Socket(Constants.SERVER_IP, Constants.SERVERPORT);
                TCP_param = new TCP_Properties(s_down, isNagleDisable);
                dataOut = new DataOutputStream(s_down.getOutputStream());
                dataOut.writeInt(this.ID);
                Thread c = new Connection(this.ID, s_down, this.dataMeasurement, isNagleDisable);
                c.start();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void Method_MV_DOWN_readVector_Client() throws InterruptedException {
        //Measurements
        dataMeasurement.SampleReadTime.clear();
        try {
            //Downlink
            dataIn.readByte();
            //Run Iperf
            if (isNagleDisable) {
                long end = System.currentTimeMillis() + runningTime;
                downlink_Client_rcvInSeconds(end);
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -N -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BLOCKSIZE + " -c 193.136.127.218 -R";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
                runShell.run();
            } else {
                long end = System.currentTimeMillis() + runningTime;
                downlink_Client_rcvInSeconds(end);
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BLOCKSIZE + " -c 193.136.127.218 -R";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
                runShell.run();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                //Create new ClientThread for Report
                s_report = new Socket(Constants.SERVER_IP, Constants.SERVERPORT);
                TCP_param = new TCP_Properties(s_report, isNagleDisable);
                dataOut = new DataOutputStream(s_report.getOutputStream());
                dataOut.writeInt(this.ID);
                Thread c = new Connection(this.ID, s_report, this.dataMeasurement, isNagleDisable);
                c.start();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void Method_MV_Report_readVector_Client() {
        //Report 1secBytes Vector, sending size first 
        try {
            dataOut.writeByte(3);
            //Report MV_readVector_Downlink
            dataOut.writeInt(dataMeasurement.SampleReadTime.size());
            for (int k = 0; k < dataMeasurement.SampleReadTime.size(); k++) {
                dataOut.writeInt(dataMeasurement.SampleReadTime.get(k).bytesRead);
                dataOut.flush();
                dataOut.writeLong(dataMeasurement.SampleReadTime.get(k).sampleTime);
                dataOut.flush();
            }
            //Report Shell Vector from terminal Uplink
            dataOut.writeInt(dataMeasurement.ByteSecondShell_up.size());
            for (int b = 0; b < dataMeasurement.ByteSecondShell_up.size(); b++) {
                dataOut.writeInt(dataMeasurement.ByteSecondShell_up.get(b));
                dataOut.flush();
            }
            //Report Shell Vector from terminal Downlink
            dataOut.writeInt(dataMeasurement.ByteSecondShell_down.size());
            for (int b = 0; b < dataMeasurement.ByteSecondShell_down.size(); b++) {
                dataOut.writeInt(dataMeasurement.ByteSecondShell_down.get(b));
                dataOut.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            System.err.println("Method_MV_readVector_Client along with Report is done!");
        }
    }
}
