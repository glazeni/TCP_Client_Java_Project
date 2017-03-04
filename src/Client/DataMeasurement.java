/*
 * Thread that performs measurements on ClientThread
 */
package Client;

import java.util.Vector;

class DataSample {

    int bytes = 0;
    long sendTime = 0;
    long arrivalTime = 0;

    DataSample(int _bytesRead, long _sendTime, long _arrivalTime) {
        bytes = _bytesRead;
        sendTime = _sendTime;
        arrivalTime = _arrivalTime;
    }
}

class DataSecond {

    int bytesRead = 0;
    long sampleTime = 0;

    DataSecond(int _bytesRead, long _TimeStamp) {
        bytesRead = _bytesRead;
        sampleTime = _TimeStamp;
    }
}

public class DataMeasurement {

    protected Vector<Integer> ByteSecondShell_up = null;
    protected Vector<Integer> ByteSecondShell_down = null;
    protected Vector<DataSecond> SampleReadTime = null;
    protected Vector<DataSecond> SampleWriteTime = null;
    protected Vector<DataSample> SamplesBlock = null;
    protected Vector<Integer> SampleSecond_up = null;
    protected Vector<Integer> SampleSecond_down = null;
    protected Vector<Double> AvailableBW_Down=null;

    public DataMeasurement() {
        try {
            ByteSecondShell_up = new Vector<Integer>();
            ByteSecondShell_down = new Vector<Integer>();
            SampleReadTime = new Vector<DataSecond>();
            SampleWriteTime = new Vector<DataSecond>();
            SamplesBlock = new Vector<DataSample>();
            SampleSecond_up = new Vector<Integer>();
            SampleSecond_down = new Vector<Integer>();
            AvailableBW_Down = new Vector<Double>();
        } catch (Exception ex) {
            ex.getStackTrace();
        }

    }

    public long TimeThread(long _startThread, long _endThread) {
        long elpasedThread = _endThread - _startThread;
        return elpasedThread;
    }

    public void add_SampleBlock(int MTUsize, long start_time, long end_time) {
        SamplesBlock.add(new DataSample(MTUsize, start_time, end_time));
    }

    public void add_SampleSecond_up(int _bytes) {
        SampleSecond_up.add(_bytes);
    }

    public void add_SampleSecond_down(int _bytes) {
        SampleSecond_down.add(_bytes);
    }

    public void add_SampleReadTime(int _bytes, long _sampleTime) {
        SampleReadTime.add(new DataSecond(_bytes, _sampleTime));
    }
}
