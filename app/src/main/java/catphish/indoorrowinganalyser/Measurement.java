package catphish.indoorrowinganalyser;

/**
 * Created by charlie on 29/06/14.
 */
public class Measurement {
    int lengthInSamples;
    int samplesSinceStart;
    int measurementsSinceStart;

    public int getLengthInSamples() {
        return lengthInSamples;
    }

    public int getMeasurementsSinceStart() {
        return measurementsSinceStart;
    }

    public Measurement(int lengthInSamples, int samplesSinceStart, int measurementsSinceStart) {
        this.lengthInSamples = lengthInSamples;
        this.samplesSinceStart = samplesSinceStart;
        this.measurementsSinceStart = measurementsSinceStart;
    }

    public double velocity() {
        return(92362.8 / lengthInSamples);
    }

    public double time_since_start() {
        return(samplesSinceStart / 44100.0);
    }
}
