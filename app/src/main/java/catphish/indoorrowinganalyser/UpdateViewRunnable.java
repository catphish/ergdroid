package catphish.indoorrowinganalyser;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;

/**
 * Created by charlie on 28/06/14.
 */
public class UpdateViewRunnable implements Runnable{
    Measurement measurement;
    MainActivity activity;
    double speed;

    public UpdateViewRunnable(Activity activity, Measurement measurement, double speed) {
        this.activity = (MainActivity)activity;
        this.measurement = measurement;
        this.speed = speed;
    }

    public void run() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                double flywheel_speed = 100000.0 / measurement.getLengthInSamples();
                activity.getSeriesOne().appendData(new GraphView.GraphViewData(measurement.getMeasurementsSinceStart(), flywheel_speed), true, 2000);
                if(speed > 1) {
                    double fivehundred = 500 / speed;
                    int mins = (int) Math.floor(fivehundred / 60.0);
                    int secs = (int) Math.floor(fivehundred % 60.0);
                    ((TextView) activity.findViewById(R.id.textView)).setText(String.format("%02d:%02d", mins, secs));
                }
            }
        });
    }

}
