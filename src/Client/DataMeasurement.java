/*
 * Thread that performs measurements on ClientThread
 */
package Client;

import java.util.Vector;

//Define DataSample Vector
class Data_Sample {

    int byteCnt = 0;
    long startTime = 0;
    long endTime = 0;

    Data_Sample(int _byteCnt, long start_time, long end_time) {
        byteCnt = _byteCnt;
        startTime = start_time;
        endTime = end_time;

    }
}

class Data1secBytes {

    int bytesRead = 0;
    long sampleTime = 0;

    Data1secBytes(int _bytesRead, long _arrivalTime) {
        bytesRead = _bytesRead;
        sampleTime = _arrivalTime;
    }
}

class DataPacketTrain {

    int bytes = 0;
    long sendTime = 0;
    long arrivalTime = 0;

    DataPacketTrain(int _bytesRead, long _sendTime, long _arrivalTime) {
        bytes = _bytesRead;
        sendTime = _sendTime;
        arrivalTime = _arrivalTime;
    }
}

public class DataMeasurement {

    protected Vector<Data1secBytes> SampleSecond = null;
    protected static Vector<DataPacketTrain> packetTrains = null;
    protected static Vector<Data_Sample> SamplesBlock = null;
    protected Vector<Long> deltaINVector_uplink = null; //Sending time uplink vector 
    protected Vector<Long> deltaOUTVector_uplink = null; //Arrival time uplink vector
    protected Vector<Long> deltaINVector_downlink = null; //Sending time downlink vector
    protected Vector<Long> deltaOUTVector_downlink = null; //Arrival time downlink vector
    protected Vector<Integer> deltaByteCount_downlink = null;
    protected Vector<Integer> deltaByteCount_uplink = null;

    public DataMeasurement() {
        try {
            SamplesBlock = new Vector<Data_Sample>();
            SampleSecond = new Vector<Data1secBytes>();
            packetTrains = new Vector<DataPacketTrain>();
            deltaINVector_uplink = new Vector<Long>();
            deltaOUTVector_uplink = new Vector<Long>();
            deltaINVector_downlink = new Vector<Long>();
            deltaOUTVector_downlink = new Vector<Long>();
            deltaByteCount_downlink = new Vector<Integer>();
            deltaByteCount_uplink = new Vector<Integer>();
        } catch (Exception ex) {
            ex.getStackTrace();
        }

    }

    public static long TimeThread(long _startThread, long _endThread) {
        long elpasedThread = _endThread - _startThread;
        return elpasedThread;
    }

    public static void add_SampleBlock(int MTUsize, long start_time, long end_time) {
        SamplesBlock.add(new Data_Sample(MTUsize, start_time, end_time));

    }

    public void add_SampleSecond(int _bytesRead, long sampleTime) {
        SampleSecond.addElement(new Data1secBytes(_bytesRead, sampleTime));
    }

    public static void add_PacketTrain_Sample(int bytes, long send_time, long arrival_time) {
        packetTrains.add(new DataPacketTrain(bytes, send_time, arrival_time));
    }
}
