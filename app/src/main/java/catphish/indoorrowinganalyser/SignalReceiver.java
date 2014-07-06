package catphish.indoorrowinganalyser;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by charlie on 29/06/14.
 */
public class SignalReceiver implements Runnable {
    private static final int RECORDER_SOURCE = MediaRecorder.AudioSource.DEFAULT;
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final boolean TEST_MODE = false;
    Activity activity;

    SignalAnalyser mSignalAnalyser;

    public SignalReceiver(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void run() {
        mSignalAnalyser = new SignalAnalyser(activity);

        if(TEST_MODE) {
            try {
                File sdcard = Environment.getExternalStorageDirectory();

                File file = new File(sdcard, "rowing2.txt");
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(file));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                while (!Thread.currentThread().isInterrupted()) {
                    String line;
                    line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    ;
                    mSignalAnalyser.processDataItem(Integer.valueOf(line));
                }
                br.close();
            } catch (Exception ex) {}
        } else {
            short sData[] = new short[100];

            int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                    RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
            AudioRecord recorder = new AudioRecord(RECORDER_SOURCE, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize * 20);

            recorder.startRecording();

            while (!Thread.currentThread().isInterrupted()) {
                recorder.read(sData, 0, 100);
                for (int n = 0; n < 100; n++) {
                    mSignalAnalyser.processDataItem(sData[n]);
                }
            }
            recorder.stop();
        }
    }

}
