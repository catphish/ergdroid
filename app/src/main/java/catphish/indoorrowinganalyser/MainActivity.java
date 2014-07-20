package catphish.indoorrowinganalyser;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView speed_data;
    private TextView time_data;
    private TextView distance_data;
    private TextView drag_data;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        speed_data = (TextView)findViewById(R.id.speed_data);
        time_data = (TextView)findViewById(R.id.time_data);
        distance_data = (TextView)findViewById(R.id.distance_data);
        drag_data = (TextView)findViewById(R.id.drag_data);
        if(isMyServiceRunning()) {
            findViewById(R.id.stop_button).setEnabled(true);
            findViewById(R.id.start_button).setEnabled(false);
        } else {
            findViewById(R.id.stop_button).setEnabled(false);
            findViewById(R.id.start_button).setEnabled(true);
        }
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("catphish.indoorrowinganalyser.SignalReceiverService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(dataReceiver, new IntentFilter("data"));
        LocalBroadcastManager.getInstance(this).registerReceiver(statusReceiver, new IntentFilter("status"));
    }

    private BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            drag_data.setText(Long.toString(Math.round(intent.getDoubleExtra("drag", 0) * 1000000)));
            double fivehundred = 500 / intent.getDoubleExtra("speed", 0);
            int mins = (int) Math.floor(fivehundred / 60.0);
            int secs = (int) Math.floor(fivehundred % 60.0);
            speed_data.setText(String.format("%02d:%02d", mins, secs));
            distance_data.setText(Double.toString(intent.getDoubleExtra("distance", 0)));

            long runtime = intent.getLongExtra("time", 0);
            runtime = runtime / 1000;
            mins = (int) Math.floor(runtime / 60.0);
            secs = (int) Math.floor(runtime % 60.0);
            time_data.setText(String.format("%02d:%02d", mins, secs));
        }
    };

    private BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("status").equals("started")) {
                findViewById(R.id.stop_button).setEnabled(true);
                findViewById(R.id.start_button).setEnabled(false);
            }
            if(intent.getStringExtra("status").equals("stopped")) {
                findViewById(R.id.stop_button).setEnabled(false);
                findViewById(R.id.start_button).setEnabled(true);
            }
        }
    };

    @Override
    protected void onStop() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver);
    }

    public void startSession(View v) {
        Intent intent = new Intent(this, SignalReceiverService.class);
        findViewById(R.id.start_button).setEnabled(false);
        startService(intent);
    }

    public void stopSession(View v) {
        Intent intent = new Intent("shutdown-service");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

}
