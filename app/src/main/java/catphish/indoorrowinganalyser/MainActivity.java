package catphish.indoorrowinganalyser;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView speed_data;
    private TextView time_data;
    private TextView distance_data;
    private TextView drag_data;

    private Handler handler = new Handler();

    SignalReceiverService mService;
    boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, SignalReceiverService.class);
        startService(intent);
        setContentView(R.layout.activity_main);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (mBound) {
                drag_data.setText(Long.toString(Math.round(mService.getDrag() * 1000000)));
                double fivehundred = 500 / mService.getSpeed();
                int mins = (int) Math.floor(fivehundred / 60.0);
                int secs = (int) Math.floor(fivehundred % 60.0);
                speed_data.setText(String.format("%02d:%02d", mins, secs));

                long runtime = mService.getFinishTime().toMillis(true) - mService.getStartTime().toMillis(true);
                runtime = runtime / 1000;
                mins = (int) Math.floor(runtime / 60.0);
                secs = (int) Math.floor(runtime % 60.0);
                time_data.setText(String.format("%02d:%02d", mins, secs));
            }
            handler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to LocalService
        Intent intent = new Intent(this, SignalReceiverService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        speed_data = (TextView)findViewById(R.id.speed_data);
        time_data = (TextView)findViewById(R.id.time_data);
        distance_data = (TextView)findViewById(R.id.distance_data);
        drag_data = (TextView)findViewById(R.id.drag_data);

        handler.postDelayed(runnable, 100);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    /** Called when a button is clicked (the button in the layout file attaches to
     * this method with the android:onClick attribute) */
    public void startSession(View v) {
        if (mBound) {
            if(mService.startSession()) {
                Toast.makeText(this, "Started!", Toast.LENGTH_SHORT).show();
                findViewById(R.id.start_button).setEnabled(false);
                findViewById(R.id.stop_button).setEnabled(true);
            }
        }
    }

    public void stopSession(View v) {
        if (mBound) {
            if(mService.stopSession()) {
                Toast.makeText(this, "Stopped!", Toast.LENGTH_SHORT).show();
                findViewById(R.id.start_button).setEnabled(true);
                findViewById(R.id.stop_button).setEnabled(false);
            }
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            SignalReceiverService.LocalBinder binder = (SignalReceiverService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


}
