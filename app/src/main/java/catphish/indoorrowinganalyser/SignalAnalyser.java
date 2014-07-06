package catphish.indoorrowinganalyser;

import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by charlie on 26/06/14.
 */
public class SignalAnalyser {
    Activity activity;

    double MOMENT = 0.1001;

    int previous_value=0;
    int recent_samples = 0;
    int all_samples = 0;
    int all_measurements = 0;
    double drag = 0;
    double speed = 0;
    double power = 0;
    boolean peak_trough;

    Measurement previous_start_of_recovery = null;
    Measurement start_of_stroke = null;
    Measurement end_of_stroke = null;
    Measurement start_of_recovery = null;
    Measurement end_of_recovery = null;

    ArrayList<Measurement> measurementHistory;

    public SignalAnalyser(Activity activity) {
        this.activity = activity;
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
                        if(peak_trough == false) {
                            end_of_recovery = measurementHistory.get(0);
                            start_of_stroke = measurement;
                            if(start_of_recovery != null) {
                                calculateDragFactor();
                            }
                            peak_trough = true;
                        }
                    }
                    if(measurementHistory.get(0).getLengthInSamples() > measurementHistory.get(3).getLengthInSamples() && measurementHistory.get(6).getLengthInSamples() > measurementHistory.get(3).getLengthInSamples()) {
                        if(peak_trough == true) {
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
                activity.runOnUiThread(new UpdateViewRunnable(activity, measurement, speed));
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
