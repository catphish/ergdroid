package catphish.indoorrowinganalyser;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.text.format.Time;

import java.util.ArrayList;

public class SignalReceiverService extends Service {
    // Useful data from analysis
    double drag = 0;
    double speed = 0;
    Time mStartTime = new Time();
    Time mFinishTime = new Time();
    boolean mRunning = false;

    // DSP Settings
    private static final int RECORDER_SOURCE = MediaRecorder.AudioSource.DEFAULT;
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    AudioRecord recorder;
    Thread recordingThread;

    public double getDrag() {
        return drag;
    }

    public double getSpeed() {
        return speed;
    }

    public boolean isRunning() {
        return mRunning;
    }

    public Time getStartTime() {
        return mStartTime;
    }

    public Time getFinishTime() {
        return mFinishTime;
    }

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        SignalReceiverService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SignalReceiverService.this;
        }
    }

    public boolean stopSession() {
        recordingThread.interrupt();
        mRunning = false;
        return true;
    }

    public boolean startSession() {
        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        recorder = new AudioRecord(RECORDER_SOURCE, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize * 20);

        if((recorder.getState() != AudioRecord.STATE_INITIALIZED)) return false;

        recordingThread = new Thread(new SignalReceiver());
        recordingThread.start();
        mRunning = true;

        mStartTime.setToNow();

        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class SignalReceiver implements Runnable {
        SignalAnalyser mSignalAnalyser;

        public SignalReceiver() {}

        @Override
        public void run() {
            mSignalAnalyser = new SignalAnalyser();
            recorder.startRecording();

            short sData[] = new short[100];

            while (!Thread.currentThread().isInterrupted()) {
                recorder.read(sData, 0, 100);
                for (int n = 0; n < 100; n++) {
                    mSignalAnalyser.processDataItem(sData[n]);
                }
                mFinishTime.setToNow();
            }
            recorder.stop();
        }

    }

    public class SignalAnalyser {
        double MOMENT = 0.1001;

        int previous_value=0;
        int recent_samples = 0;
        int all_samples = 0;
        int all_measurements = 0;
        boolean peak_trough;

        Measurement previous_start_of_recovery = null;
        Measurement start_of_stroke = null;
        Measurement end_of_stroke = null;
        Measurement start_of_recovery = null;
        Measurement end_of_recovery = null;

        ArrayList<Measurement> measurementHistory;

        public SignalAnalyser() {
            measurementHistory = new ArrayList<Measurement>();
        }

        public void processDataItem(int current_value) {
            recent_samples += 1;
            all_samples += 1;
            if(current_value < -5000 && previous_value >= -5000 && recent_samples > 20) {
                if (recent_samples < 2000) {
                    all_measurements += 1;
                    Measurement measurement = new Measurement(recent_samples, all_samples, all_measurements);
                    measurementHistory.add(measurement);
                    if(measurementHistory.size() > 7) {
                        measurementHistory.remove(0);
                        if(measurementHistory.get(0).getLengthInSamples() < measurementHistory.get(3).getLengthInSamples() && measurementHistory.get(6).getLengthInSamples() < measurementHistory.get(3).getLengthInSamples()) {
                            if(!peak_trough) {
                                end_of_recovery = measurementHistory.get(0);
                                start_of_stroke = measurement;
                                if(start_of_recovery != null) {
                                    calculateDragFactor();
                                }
                                peak_trough = true;
                            }
                        }
                        if(measurementHistory.get(0).getLengthInSamples() > measurementHistory.get(3).getLengthInSamples() && measurementHistory.get(6).getLengthInSamples() > measurementHistory.get(3).getLengthInSamples()) {
                            if(peak_trough) {
                                previous_start_of_recovery = start_of_recovery;
                                start_of_recovery = measurement;
                                if (previous_start_of_recovery != null) {
                                    calculateSpeed();
                                }
                                end_of_stroke = measurementHistory.get(0);
                                peak_trough = false;
                            }
                        }
                    }
                }
                recent_samples = 0;
            }
            previous_value = current_value;
        }

        public void calculateDragFactor() {
            drag = MOMENT * (1.0/start_of_recovery.velocity() - 1.0/end_of_recovery.velocity()) / (end_of_recovery.time_since_start() - start_of_recovery.time_since_start()) * -1;
        }

        public void calculateSpeed() {
            double measurements = start_of_recovery.getMeasurementsSinceStart() - previous_start_of_recovery.getMeasurementsSinceStart();
            double radians = measurements * 2.094;
            double time = (start_of_recovery.time_since_start() - previous_start_of_recovery.time_since_start());
            double rotational_velocity = radians / time;
            speed = Math.pow(drag / 2.8, 0.3333) * rotational_velocity;
        }

        //public void calculatePower() {
        //    // I ( dω / dt ) dθ + k ω2 dθ
        //    double dw = start_of_stroke.getMeasurementsSinceStart() - previous_start_of_recovery.getMeasurementsSinceStart();
        //    //power = MOMENT *
        //}

    }

}