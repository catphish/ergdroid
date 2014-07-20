package catphish.indoorrowinganalyser;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class SignalReceiverService extends Service {
    // Useful data from analysis
    double drag;
    double distance;
    double speed;
    Time mStartTime;
    Time mFinishTime;
    Notification noti;

    // DSP Settings
    private static final int RECORDER_SOURCE = MediaRecorder.AudioSource.DEFAULT;
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    AudioRecord recorder;
    Thread recordingThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        return START_NOT_STICKY;
    }

    private void updateNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        noti = new Notification.Builder(this)
                .setContentTitle("Rowing")
                .setContentText(mFinishTime.toString())
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, noti);
    }

    @Override
    public void onCreate() {
        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        recorder = new AudioRecord(RECORDER_SOURCE, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize * 20);

        if((recorder.getState() != AudioRecord.STATE_INITIALIZED)) {
            stopSelf();
            return;
        }

        recordingThread = new Thread(new SignalReceiver());
        recordingThread.start();

        drag = 0;
        distance = 0;
        speed = 0;
        mStartTime = new Time();
        mStartTime.setToNow();
        mFinishTime = new Time();
        mFinishTime.setToNow();

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("shutdown-service"));
        updateNotification();

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            Log.d("receiver", "Got shutdown message");
            recordingThread.interrupt();
        }
    };

    public IBinder onBind(Intent intent) {
        return null;
    }

    public class SignalReceiver implements Runnable {
        SignalAnalyser mSignalAnalyser;

        public SignalReceiver() {}

        @Override
        public void run() {
            mSignalAnalyser = new SignalAnalyser();
            recorder.startRecording();

            short sData[] = new short[100];
            BufferedWriter buf = null;
            FileWriter writer = null;

            try {
                Long tsLong = System.currentTimeMillis()/1000;
                String ts = tsLong.toString();

                File file = new File(getExternalFilesDir(null), ts + ".row.raw");
                file.createNewFile();
                writer = new FileWriter(file);
                buf = new BufferedWriter(writer);
            } catch (IOException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            Intent intent = new Intent("status");
            intent.putExtra("status", "started");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

            while (!Thread.currentThread().isInterrupted()) {
                recorder.read(sData, 0, 100);
                for (int n = 0; n < 100; n++) {
                    mSignalAnalyser.processDataItem(sData[n]);
                    try {
                        buf.write(Short.toString(sData[n]));
                        buf.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                mFinishTime.setToNow();
            }
            try {
                buf.close();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            recorder.stop();
            intent = new Intent("status");
            intent.putExtra("status", "stopped");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

            stopSelf();
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
            if(current_value < -6000 && previous_value >= -6000 && recent_samples > 20) {
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
                    Intent intent = new Intent("data");
                    intent.putExtra("speed", speed);
                    intent.putExtra("distance", distance);
                    intent.putExtra("drag", drag);
                    intent.putExtra("time", mFinishTime.toMillis(true) - mStartTime.toMillis(true));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    updateNotification();
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
            distance += Math.pow(drag / 2.8, 0.3333) * radians;
        }

    }

}