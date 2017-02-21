/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author glazen
 */
public class RunShellCommandsClient {

    private String cmd = null;
    private BufferedReader buffReader = null;
    private String line = null;
    private String[] parts = null;
    private String part1 = null;
    private String part2 = null;
    private DataMeasurement dataMeasurement = null;
    private int multiplier = 0;
    private boolean isUplinkTest;

    public RunShellCommandsClient(DataMeasurement _dataMeasurement, String _cmd, boolean _isUplinkTest) {
        this.dataMeasurement = _dataMeasurement;
        this.cmd = _cmd;
        this.isUplinkTest = _isUplinkTest;
    }

    public void run() {
        try {

            Process proc = Runtime.getRuntime().exec(cmd);

            // Read the output
            buffReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            while ((line = buffReader.readLine()) != null) {
                if (line.contains("sender")) {
                    break;
                }
                System.out.print("Line " + line + "\n");
                part1 = line.replaceAll("\\s+", "");
                if (part1.contains("sec")) {
                    parts = part1.split("sec");
                    parts = parts[1].split("Bytes");
                    part2 = parts[0].substring(0, parts[0].length());
                    if (part2.contains("G")) {
                        multiplier = 1024*1024*1024;
                    } else if (part2.contains("M")) {
                        multiplier = 1024*1024;
                    } else if (part2.contains("K")) {
                        multiplier = 1024;
                    } else {
                        multiplier = 1;
                    }
                    part2 = parts[0].substring(0, parts[0].length() - 1);
                    int value = (int) Math.round(Float.parseFloat(part2) * multiplier)*8;
                    if (isUplinkTest) {
                        dataMeasurement.ByteSecondShell_up.add(value);
                    } else {
                        dataMeasurement.ByteSecondShell_down.add(value);
                    }
                    System.out.print("Value " + value + "\n");
                }
            }
            try {
                proc.waitFor();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
}
