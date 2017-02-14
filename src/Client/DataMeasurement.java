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

    protected Vector<DataSecond> SampleReadTime = null;
    protected Vector<DataSecond> SampleWriteTime = null;
    protected Vector<DataSample> SamplesBlock = null;
    protected Vector<DataSecond> SampleSecond_up = null;
    protected Vector<DataSecond> SampleSecond_down = null;
    protected Vector<Long> deltaINVector_uplink = null; //Sending time uplink vector 
    protected Vector<Long> deltaOUTVector_uplink = null; //Arrival time uplink vector
    protected Vector<Long> deltaINVector_downlink = null; //Sending time downlink vector
    protected Vector<Long> deltaOUTVector_downlink = null; //Arrival time downlink vector
    protected Vector<Long> aux_writeTimeVector = null;
    protected Vector<Long> ACKTimingVector = null;

    public DataMeasurement() {
        try {
            SampleReadTime = new Vector<DataSecond>();
            SampleWriteTime = new Vector<DataSecond>();
            SamplesBlock = new Vector<DataSample>();
            SampleSecond_up = new Vector<DataSecond>();
            SampleSecond_down = new Vector<DataSecond>();
            deltaINVector_uplink = new Vector<Long>();
            deltaOUTVector_uplink = new Vector<Long>();
            deltaINVector_downlink = new Vector<Long>();
            deltaOUTVector_downlink = new Vector<Long>();
            aux_writeTimeVector = new Vector<Long>();
            ACKTimingVector = new Vector<Long>();
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
    
    public void add_SampleSecond_up(int _bytes, long _sampleTime) {
        SampleSecond_up.add(new DataSecond(_bytes, _sampleTime)); 
    }
    public void add_SampleSecond_down(int _bytes, long _sampleTime) {
        SampleSecond_down.add(new DataSecond(_bytes, _sampleTime)); 
    }
    
    public void add_SampleReadTime(int _bytes, long _sampleTime) {
        SampleReadTime.add(new DataSecond(_bytes, _sampleTime));
    }
}
