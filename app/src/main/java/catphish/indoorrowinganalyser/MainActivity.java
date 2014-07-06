package catphish.indoorrowinganalyser;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Window;
import android.widget.FrameLayout;

import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;


public class MainActivity extends ActionBarActivity {
    Thread dataThread;
    public GraphViewSeries seriesOne;
    GraphView graphView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        FrameLayout layout = (FrameLayout) findViewById(R.id.main_frame);

        seriesOne = new GraphViewSeries(new GraphView.GraphViewData[] { new GraphView.GraphViewData(1, 0) });

        // graph with custom labels and drawBackground
        graphView = new LineGraphView(
                this
                , "GraphViewDemo"
        );
        graphView.getGraphViewStyle().setHorizontalLabelsColor(Color.WHITE);
        ((LineGraphView) graphView).setDrawBackground(true);
        graphView.addSeries(seriesOne);
        graphView.setViewPort(1, 1000);
        graphView.setScalable(true);

        layout.addView(graphView);
        dataThread = new Thread(new SignalReceiver(this));
        dataThread.start();
    }

    @Override
    public void onBackPressed() {
        dataThread.interrupt();
        this.finish();
    }

    public GraphViewSeries getSeriesOne() {
        return seriesOne;
    }

    public GraphView getGraphView() {
        return graphView;
    }

}
