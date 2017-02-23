/*
 * Class that create the connection and that handles the sending of random bytes
 */
package Client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Vector;

public class Connection extends Thread {

    private Socket s_down = null;
    private Socket s_report = null;
    private Socket s = null;
    private RTInputStream RTin = null;
    private RTOutputStream RTout = null;
    private DataInputStream dataIn = null;
    private DataOutputStream dataOut = null;
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
    private boolean isIperfSettings;
    private boolean isNagleDisable;
    private long firstPacket = 0;
    private long lastPacket = 0;

    public Connection(int _ID, Socket _s, DataMeasurement _dataMeasurement, boolean _isIperfSettings, boolean _isNagleDisable) {
        try {
            this.ID = _ID;
            this.s = _s;
            this.dataMeasurement = _dataMeasurement;
            this.isIperfSettings = _isIperfSettings;
            this.isNagleDisable = _isNagleDisable;
            RTin = new RTInputStream(s.getInputStream());
            RTout = new RTOutputStream(s.getOutputStream());
            dataIn = new DataInputStream(RTin);
            dataOut = new DataOutputStream(RTout);
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
                case "PGM":
                    Method_PGM();
                    break;
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
                case "ACKTiming_UP":
                    Method_ACKTimingUP_Client();
                    break;
                case "ACKTiming_DOWN":
                    Method_ACKTimingDOWN_Client();
                    break;
                case "ACKTiming_Report":
                    Method_ACKTiming_Report_Client();
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
                            dataMeasurement.add_SampleReadTime(byteCnt, System.currentTimeMillis());
                        }
                    } else {
                        System.err.println("Read n<0");
                        break;
                    }

                    if (byteCnt < Constants.BLOCKSIZE) {
                        //System.out.println("Read " + n + " bytes");
                        //Keep reading MTU
                    } else {
                        //MTU is finished
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
            System.out.println("\n downlink_Client_rcv with " + "Number Blocks=" + num_blocks);
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

                    if (byteCnt < Constants.BLOCKSIZE) {
                        //Keep reading MTU
                    } else {
                        RTin.readTimeVector.add(System.currentTimeMillis());
                        break;
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
        System.out.println("AvaBW: " + AvaBW);
        return AvaBW.intValue();
    }

    private void Method_PGM() {
        //Parameters
        Constants.SOCKET_RCVBUF = 2920;
        Constants.SOCKET_SNDBUF = 2920;
        Constants.BLOCKSIZE = 1460;
        Constants.NUMBER_BLOCKS = 1000;
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
            //Send Delta Vectorss
            for (int j = 0; j < dataMeasurement.deltaINVector_uplink.size(); j++) {
                dataOut.writeLong(dataMeasurement.deltaINVector_uplink.get(j));
                dataOut.writeLong(dataMeasurement.deltaOUTVector_downlink.get(j));
                dataOut.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
//            String cmd = "iperf3 -p 11008 -i 1 -N -w 14600 -l 1460 -c 193.136.127.218";
//            RunShellCommandFromJava(cmd);
            System.err.println("Method_PGM along with Report is done!");
        }

    }

    private void Method_PT() {
        //Parameters
        Constants.SOCKET_RCVBUF = 146000;
        Constants.SOCKET_SNDBUF = 146000;
        Constants.BLOCKSIZE = 146000;
        Constants.NUMBER_BLOCKS = 1;

        //Measurements
        try {
            //Uplink
            dataIn.readByte();
            for (int p = 0; p < 10; p++) {
                dataIn.readByte();
                uplink_Client_snd();
            }
            //Run Iperf
            if (isNagleDisable) {
                String cmd = "iperf3 -p 11010 -N -t 10 -w 146000 -l 146000 -c 193.136.127.218";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
                runShell.run();
            } else {
                String cmd = "iperf3 -p 11010 -t 10 -w 146000 -l 146000 -c 193.136.127.218";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
                runShell.run();
            }
            //Downlink App
            AvailableBW.clear();
            dataIn.readByte();
            for (int p = 0; p < 10; p++) {
                dataOut.writeByte(2);
                downlink_Client_rcv();
                AvailableBW.add(PacketTrain());

            }
            //Run Iperf
            if (isNagleDisable) {
                String cmd = "iperf3 -p 11010 -N -t 10 -w 146000 -l 146000 -c 193.136.127.218 -R";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
                runShell.run();
            } else {
                String cmd = "iperf3 -p 11010 -t 10 -w 146000 -l 146000 -c 193.136.127.218 -R";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
                runShell.run();
            }
        } catch (IOException ex) {
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
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            System.err.println("Method_PT along with report is done!");
        }

    }

    private void Method_MV_Uplink_Client() throws InterruptedException {
        //Parameters
        if (isIperfSettings) {
            Constants.SOCKET_RCVBUF = 64000;
            Constants.SOCKET_SNDBUF = 64000;
            Constants.BLOCKSIZE = 8000;
        } else {
            Constants.SOCKET_RCVBUF = 14600;
            Constants.SOCKET_SNDBUF = 14600;
            Constants.BLOCKSIZE = 1460;
        }

        //Measurements
        dataMeasurement.ByteSecondShell_up.clear();
        try {
            //Uplink
            dataIn.readByte();
            //Run Both Tests
            if (isIperfSettings && isNagleDisable) {
                uplink_Client_sndInSeconds();
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -N -w 64000 -l 8000 -c 193.136.127.218";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
                runShell.run();
            } else if (isIperfSettings && !isNagleDisable) {
                uplink_Client_sndInSeconds();
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -w 64000 -l 8000 -c 193.136.127.218";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
                runShell.run();
            } else if (!isIperfSettings && !isNagleDisable) {
                uplink_Client_sndInSeconds();
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -w 14600 -l 1460 -c 193.136.127.218";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
                runShell.run();
            } else if (!isIperfSettings && isNagleDisable) {
                uplink_Client_sndInSeconds();
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -N -w 14600 -l 1460 -c 193.136.127.218";
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
                Thread c = new Connection(this.ID, s_down, this.dataMeasurement, isIperfSettings, isNagleDisable);
                c.start();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

    private void Method_MV_Downlink_Client() throws InterruptedException {
        //Parameters
        if (isIperfSettings) {
            Constants.SOCKET_RCVBUF = 64000;
            Constants.SOCKET_SNDBUF = 64000;
            Constants.BLOCKSIZE = 8000;
        } else {
            Constants.SOCKET_RCVBUF = 14600;
            Constants.SOCKET_SNDBUF = 14600;
            Constants.BLOCKSIZE = 1460;
        }

        //Measurements
        dataMeasurement.SampleSecond_down.clear();
        dataMeasurement.ByteSecondShell_down.clear();
        try {
            //Downlink
            dataIn.readByte();
            //Run Both Tests
            if (isIperfSettings && isNagleDisable) {
                long end = System.currentTimeMillis() + runningTime;
                downlink_Client_rcvInSeconds(end);
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -N -w 64000 -l 8000 -c 193.136.127.218 -R";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
                runShell.run();
            } else if (isIperfSettings && !isNagleDisable) {
                long end = System.currentTimeMillis() + runningTime;
                downlink_Client_rcvInSeconds(end);
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -w 64000 -l 8000 -c 193.136.127.218 -R";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
                runShell.run();
            } else if (!isIperfSettings && !isNagleDisable) {
                long end = System.currentTimeMillis() + runningTime;
                downlink_Client_rcvInSeconds(end);
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -w 14600 -l 1460 -c 193.136.127.218 -R";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
                runShell.run();
            } else if (!isIperfSettings && isNagleDisable) {
                long end = System.currentTimeMillis() + runningTime;
                downlink_Client_rcvInSeconds(end);
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -N -w 14600 -l 1460 -c 193.136.127.218 -R";
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
                Thread c = new Connection(this.ID, s_report, this.dataMeasurement, isIperfSettings, isNagleDisable);
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
        //Parameters
        if (isIperfSettings) {
            Constants.SOCKET_RCVBUF = 64000;
            Constants.SOCKET_SNDBUF = 64000;
            Constants.BLOCKSIZE = 8000;
        } else {
            Constants.SOCKET_RCVBUF = 14600;
            Constants.SOCKET_SNDBUF = 14600;
            Constants.BLOCKSIZE = 1460;
        }

        //Measurements
        try {
            //Uplink
            dataIn.readByte();
            //Run Both Tests
            if (isIperfSettings && isNagleDisable) {
                uplink_Client_sndInSeconds();
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -N -w 64000 -l 8000 -c 193.136.127.218";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
                runShell.run();
            } else if (isIperfSettings && !isNagleDisable) {
                uplink_Client_sndInSeconds();
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -w 64000 -l 8000 -c 193.136.127.218";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
                runShell.run();
            } else if (!isIperfSettings && !isNagleDisable) {
                uplink_Client_sndInSeconds();
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -w 14600 -l 1460 -c 193.136.127.218";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, true);
                runShell.run();
            } else if (!isIperfSettings && isNagleDisable) {
                uplink_Client_sndInSeconds();
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -N -w 14600 -l 1460 -c 193.136.127.218";
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
                Thread c = new Connection(this.ID, s_down, this.dataMeasurement, isIperfSettings, isNagleDisable);
                c.start();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void Method_MV_DOWN_readVector_Client() throws InterruptedException {
        //Parameters
        if (isIperfSettings) {
            Constants.SOCKET_RCVBUF = 64000;
            Constants.SOCKET_SNDBUF = 64000;
            Constants.BLOCKSIZE = 8000;
        } else {
            Constants.SOCKET_RCVBUF = 14600;
            Constants.SOCKET_SNDBUF = 14600;
            Constants.BLOCKSIZE = 1460;
        }

        //Measurements
        dataMeasurement.SampleReadTime.clear();
        try {
            //Downlink
            dataIn.readByte();
            //Run Iperf
            if (isIperfSettings && isNagleDisable) {
                long end = System.currentTimeMillis() + runningTime;
                downlink_Client_rcvInSeconds(end);
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -N -w 64000 -l 8000 -c 193.136.127.218 -R";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
                runShell.run();
            } else if (isIperfSettings && !isNagleDisable) {
                long end = System.currentTimeMillis() + runningTime;
                downlink_Client_rcvInSeconds(end);
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -w 64000 -l 8000 -c 193.136.127.218 -R";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
                runShell.run();
            } else if (!isIperfSettings && !isNagleDisable) {
                long end = System.currentTimeMillis() + runningTime;
                downlink_Client_rcvInSeconds(end);
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -w 14600 -l 1460 -c 193.136.127.218 -R";
                runShell = new RunShellCommandsClient(this.dataMeasurement, cmd, false);
                runShell.run();
            } else if (!isIperfSettings && isNagleDisable) {
                long end = System.currentTimeMillis() + runningTime;
                downlink_Client_rcvInSeconds(end);
                String cmd = "iperf3 -p 11010 -t 30 -i 1 -M -N -w 14600 -l 1460 -c 193.136.127.218 -R";
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
                Thread c = new Connection(this.ID, s_report, this.dataMeasurement, isIperfSettings, isNagleDisable);
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

    private void Method_ACKTimingUP_Client() {
        //Parameters
        if (isIperfSettings) {
            Constants.SOCKET_RCVBUF = 64000;
            Constants.SOCKET_SNDBUF = 64000;
            Constants.BLOCKSIZE = 8000;
        } else {
            Constants.SOCKET_RCVBUF = 14600;
            Constants.SOCKET_SNDBUF = 14600;
            Constants.BLOCKSIZE = 1460;
        }

        //Measurements
        try {
            //Uplink
            dataIn.readByte();
            uplink_Client_sndInSeconds();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                //Socket + Connection Downlink
                s_down = new Socket(Constants.SERVER_IP, Constants.SERVERPORT);
                TCP_param = new TCP_Properties(s_down, isNagleDisable);
                dataOut = new DataOutputStream(s_down.getOutputStream());
                dataOut.writeInt(this.ID);
                Thread c = new Connection(this.ID, s_down, this.dataMeasurement, isIperfSettings, isNagleDisable);
                c.start();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void Method_ACKTimingDOWN_Client() {
        //Parameters
        if (isIperfSettings) {
            Constants.SOCKET_RCVBUF = 64000;
            Constants.SOCKET_SNDBUF = 64000;
            Constants.BLOCKSIZE = 8000;
        } else {
            Constants.SOCKET_RCVBUF = 14600;
            Constants.SOCKET_SNDBUF = 14600;
            Constants.BLOCKSIZE = 1460;
        }

        //Measurements
        try {
            //Downlink
            dataIn.readByte();
            long end = System.currentTimeMillis() + runningTime;
            downlink_Client_rcvInSeconds(end);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                //Socket + Connection Report
                s_report = new Socket(Constants.SERVER_IP, Constants.SERVERPORT);
                TCP_param = new TCP_Properties(s_report, isNagleDisable);
                dataOut = new DataOutputStream(s_report.getOutputStream());
                dataOut.writeInt(this.ID);
                Thread c = new Connection(this.ID, s_report, this.dataMeasurement, isIperfSettings, isNagleDisable);
                c.start();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
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
            dataMeasurement.aux_writeTimeVector.clear();
            System.err.println("Method_ACKTiming_Client along with Report is done!");
        }
    }
}
