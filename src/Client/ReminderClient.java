/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author glazen
 */
public class ReminderClient extends Thread {

    private Timer timer = null;
    private DataMeasurement dataMeasurement = null;
    private RTInputStream RTin = null;
    private int i = 0;

    public ReminderClient(int seconds, DataMeasurement _dataMeasurement, RTInputStream _RTin) {
        this.dataMeasurement = _dataMeasurement;
        this.RTin = _RTin;
        timer = new Timer();
        //timer.schedule(new RemindTask(), 0, seconds);
        timer.scheduleAtFixedRate(new RemindTask(this.RTin), 0, seconds*1000);

    }

    public synchronized void cancelTimer() {
        timer.cancel();
    }

    class RemindTask extends TimerTask{
        private RTInputStream RTinput=null;
        public RemindTask(RTInputStream _RTInput) {
            this.RTinput = _RTInput;
        }

        @Override
        public void run() {
            try {
                dataMeasurement.add_SampleSecond_down(this.RTinput.getBytes2Bits());
                System.out.println("REMINDER CLIENT" + i + " with " + "bits=" + this.RTinput.getBytes2Bits());
                i++;
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                this.RTinput.clearBytes();
            }
        }
    }
}
