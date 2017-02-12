/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import org.jfree.data.time.Second;

/**
 *
 * @author glazen
 */
public class ReminderClient extends Thread {

    public volatile boolean isRunning;
    public Timer timer = null;
    private DataMeasurement dataMeasurement = null;
    int i = 0;

    public ReminderClient(int seconds, DataMeasurement _dataMeasurement) {
        this.dataMeasurement = _dataMeasurement;
        timer = new Timer();
        //timer.schedule(new RemindTask(), 0, seconds);
        timer.scheduleAtFixedRate(new RemindTask(), 0, (seconds * 1000));

    }

    class RemindTask extends TimerTask {

        public RemindTask() {
            //Do nothihng in constructor
        }

        public void run() {
            try {
                System.err.println("REMINDER CLIENT" + i);
                i++;
                dataMeasurement.add_SampleSecond_down(RTInputStream.bytesTotal, System.currentTimeMillis());

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                RTInputStream.bytesTotal = 0;
            }
        }
    }
}
