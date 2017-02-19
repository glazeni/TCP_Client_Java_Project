/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

/**
 *
 * @author glazen
 */
public class RunShellCommands extends Thread {

    private String cmd = null;
    private BufferedReader buffReader = null;
    private String line = null;
    private String[] parts = null;
    private String part1 = null;
    private String part2 = null;
    private DataMeasurement dataMeasurement = null;
    private int ID = 0;
    private int multiplier = 0;
    private boolean isUplink;

    public RunShellCommands(DataMeasurement _dataMeasurement, String _cmd) {
        this.dataMeasurement = _dataMeasurement;
        this.cmd = _cmd;
        this.start();
    }

    @Override
    public void run() {
        try {
            if(cmd.contains("-R")){
                isUplink = false;
            }else{
                isUplink = true;
            }
            
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
                    part2 = parts[1].substring(0, parts[1].length() - 5);
                    if (part2.contains("G")) {
                        multiplier = 1000000000;
                    } else if (part2.contains("M")) {
                        multiplier = 1000000;
                    } else if (part2.contains("K")) {
                        multiplier = 1000;
                    } else {
                        multiplier = 1;
                    }
                    part2 = parts[1].substring(0, parts[1].length() - 6);
                    int value = (int) Math.round(Float.parseFloat(part2) * multiplier);
                    if(isUplink){
                        dataMeasurement.ByteSecondShell_up.add(value);
                    }else{
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

    public Vector<Integer> getByteSecondShellVector_up() {
        return dataMeasurement.ByteSecondShell_up;
    }
    
    public Vector<Integer> getByteSecondShellVector_down() {
        return dataMeasurement.ByteSecondShell_down;
    }
}
