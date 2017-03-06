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
import java.util.concurrent.locks.LockSupport;

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
    private TCP_Properties TCP_param = null;
    private long runningTime = 32000;
    private int ID = 0;
    private boolean isNagleDisable;

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
                case "PT_Uplink":
                    Method_PT_Uplink();
                    break;
                case "PT_Downlink":
                    Method_PT_Downlink();
                    break;
                case "PT_Report":
                    Method_PT_Report();
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
        try {
            System.out.println("uplink_Client_snd STARTED!");

            byte[] payload = new byte[Constants.PACKETSIZE_UPLINK];
            Random rand = new Random();
            // Randomize the payload with chars between 'a' to 'z' and 'A' to 'Z'  to assure there is no "\r\n"
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) ('A' + rand.nextInt(52));
            }
            //Send Packet Train
            while (counter < Constants.NUMBER_PACKETS) {
                // start recording the first packet send time
                if (beforeTime == 0) {
                    beforeTime = System.currentTimeMillis();
                }
                // send packet with constant gap
                outCtrl.println(new String(payload));
                outCtrl.flush();

                // create train gap
                try {
                    if (Constants.PACKET_GAP > 0) {
                        LockSupport.parkNanos(100);
                        //Thread.sleep(Constants.PACKET_GAP);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                counter++;
            }
            //afterTime = System.currentTimeMillis();
            diffTime = System.currentTimeMillis() - beforeTime;
            outCtrl.println("END:" + diffTime);
            outCtrl.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            System.err.println("uplink_Client_snd DONE");
        }
    }

    private boolean uplink_Client_sndInSeconds() {
        boolean keepRunning = true;
        try {
            byte[] snd_buf = new byte[Constants.BUFFERSIZE];
            new Random().nextBytes(snd_buf);
            long end = System.currentTimeMillis()+10000;
            while (System.currentTimeMillis()<end){//keepRunning) {
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
            byte[] rcv_buf = new byte[Constants.BUFFERSIZE];
            int n = 0;
            System.out.println("\n downlink_Client_rcvInSeconds");
            //Initialize Timer
            if (isThreadMethod) {
                reminderClient = new ReminderClient(1, this.dataMeasurement, this.RTin);
            }
            while (true){//System.currentTimeMillis() < _end) {
                byteCnt = 0;
                //Cycle to read each block
                do {
                    n = RTin.read(rcv_buf, byteCnt, Constants.BUFFERSIZE - byteCnt);

                    if (n > 0) {
                        byteCnt += n;
                        if (!isThreadMethod) {
                            dataMeasurement.add_SampleReadTime(n, System.currentTimeMillis());
                        }
                    } else {
                        System.err.println("Read n<0");
                        break;
                    }

                } while ((n > 0) && (byteCnt < Constants.BUFFERSIZE));

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

    private double downlink_Client_rcv() {
        int num_packets = 0;
        String inputLine = "";
        int counter = 0;
        int singlePktSize = 0;
        long startTime = 0;
        long endTime = 0;
        double gapTimeSrv = 0.0;
        double gapTimeClt = 0.0;
        double byteCounter = 0.0;
        double estTotalDownBandWidth = 0.0;
        double estAvailiableDownBandWidth = 0.0;
        double availableBWFraction = 1.0;
        try {
            System.out.println("downlink_Client_rcv STARTED!");
            //Receive Packet Train
            while ((inputLine = inCtrl.readLine()) != null) {
                if (startTime == 0) {
                    startTime = System.currentTimeMillis();
                    singlePktSize = inputLine.length();
                }

                byteCounter += inputLine.length();

                System.out.println("Received the " + (counter) + " message with size: " + inputLine.length());
                // increase the counter which is equal to the number of packets
                counter++;
                //read "END" msg
                if (inputLine.substring(0, Constants.FINAL_MSG.length()).equals(Constants.FINAL_MSG)) {
                    gapTimeClt = Double.parseDouble(inputLine.substring(Constants.FINAL_MSG.length() + 1));
                    System.out.println("Detect last downlink link message with GAP=" + gapTimeClt);
                    break;
                }
            }
            endTime = System.currentTimeMillis();

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            gapTimeSrv = endTime - startTime;
            // Bandwidth calculation
            // 1 Mbit/s = 125 Byte/ms 
            estTotalDownBandWidth = byteCounter / gapTimeSrv / 125.0;
            availableBWFraction = Math.min(gapTimeClt / gapTimeSrv, 1.0);
            estAvailiableDownBandWidth = estTotalDownBandWidth / availableBWFraction;

            // Display information at the server side
            System.out.println("Receive single Pkt size is " + singlePktSize + " Bytes.");
            System.out.println("Total receiving " + counter + " packets.");
            System.out.println("Client gap time is " + gapTimeClt + " ms.");
            System.out.println("Total package received " + byteCounter + " Bytes with " + gapTimeSrv + " ms total GAP.");
            System.out.println("Estimated Total download bandwidth is " + estTotalDownBandWidth + " Mbits/sec.");
            System.out.println("Availabe fraction is " + availableBWFraction);
            System.out.println("Estimated Available download bandwidth is " + estAvailiableDownBandWidth + " Mbits/sec.");
            System.err.println("downlink_Client_rcv DONE!");
        }
        return estAvailiableDownBandWidth;
    }

    private void Method_PT_Uplink() {
        //Measurements
        try {
            //Uplink App
            dataIn.readByte();
            for (int p = 0; p < 10; p++) {
                dataIn.readByte();
                uplink_Client_snd();
            }
            //Run Iperf
            if (isNagleDisable) {
                String cmd = "iperf3 -p 11010 -M -N -t 10 -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BUFFERSIZE + " -c 193.136.127.218";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
                runShell.run();
            } else {
                String cmd = "iperf3 -p 11010 -M -t 10 -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BUFFERSIZE + " -c 193.136.127.218";
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

    private void Method_PT_Downlink() {
        //Measurements
        try {
            //Downlink App
            dataMeasurement.AvailableBW_Down.clear();
            dataIn.readByte();
            double BW = 0;
            for (int p = 0; p < 10; p++) {
                BW = 0;
                dataOut.writeByte(2);
                BW = downlink_Client_rcv();
                dataMeasurement.AvailableBW_Down.add(BW);
            }
            //Run Iperf
            if (isNagleDisable) {
                String cmd = "iperf3 -p 11010 -M -N -t 10 -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BUFFERSIZE + " -c 193.136.127.218 -R";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
                runShell.run();
            } else {
                String cmd = "iperf3 -p 11010 -M -t 10 -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BUFFERSIZE + " -c 193.136.127.218 -R";
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

    private void Method_PT_Report() {
        //Report Measurements - AvailableBW_down Vector
        try {
            //Report AvailableBW_down 
            dataOut.writeByte(2);
            dataOut.writeInt(dataMeasurement.AvailableBW_Down.size());
            for (int k = 0; k < dataMeasurement.AvailableBW_Down.size(); k++) {
                dataOut.writeDouble(dataMeasurement.AvailableBW_Down.get(k));
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
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -N -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BUFFERSIZE + " -c 193.136.127.218";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
                runShell.run();
            } else {
                uplink_Client_sndInSeconds();
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BUFFERSIZE + " -c 193.136.127.218";
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
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -N -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BUFFERSIZE + " -c 193.136.127.218 -R";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
                runShell.run();
            } else {
                long end = System.currentTimeMillis() + runningTime;
                downlink_Client_rcvInSeconds(end);
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BUFFERSIZE + " -c 193.136.127.218 -R";
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
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -N -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BUFFERSIZE + " -c 193.136.127.218";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
                runShell.run();
            } else {
                uplink_Client_sndInSeconds();
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BUFFERSIZE + " -c 193.136.127.218";
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
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -N -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BUFFERSIZE + " -c 193.136.127.218 -R";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
                runShell.run();
            } else {
                long end = System.currentTimeMillis() + runningTime;
                downlink_Client_rcvInSeconds(end);
                String cmd = "iperf3 -p 11010 -t 35 -i 1 -M -w " + Constants.SOCKET_RCVBUF + " -l " + Constants.BUFFERSIZE + " -c 193.136.127.218 -R";
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
