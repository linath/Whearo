package net.skix.whearo;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preview.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import orbotix.robot.base.OrbBasicControl;
import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotProvider;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.Sphero;
import orbotix.view.connection.SpheroConnectionView;


public class SpheroActivity extends Activity {
    private static final String TAG = "WHEARO-";


    private SpheroConnectionView mSpheroConnectionView;
    private Sphero mRobot = null;

    int red = 0;
    int green = 10;
    int blue = 200;

    static boolean shouldStop = false;
    static boolean shouldBlink = true;

    private final static String BLINK = "do:blink";
    private final static String STOPBLINK = "do:stop";
    private final static String DRIVE = "do:driveEight";
    private final static String STOP = "do:stopDrive";

    private SeekBar speedSlider;
    private TextView speedView;
    private static float speed = 0.5f;

    private SeekBar durationSlider;
    private TextView durationView;
    private static int duration;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sphero);

        speedView = (TextView) findViewById(R.id.speedView);
        speedSlider = (SeekBar) findViewById(R.id.speedSlider);

        durationView = (TextView) findViewById(R.id.durationView);
        durationSlider = (SeekBar) findViewById(R.id.durationSlider);


        duration = durationSlider.getProgress();

        speedSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if (progress < 1) {
                    speedSlider.setProgress(1);
                } else {
                    // TODO Auto-generated method stub
                    Log.i(TAG, "changed speed seekbar to value: " + progress);
                    speedView.setText(Float.toString(progress/10.f));
                    speed = progress/10.f;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

        durationSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if (progress < 100) {
                    durationSlider.setProgress(100);
                } else {
                    // TODO Auto-generated method stub
                    Log.i(TAG, "changed seekbar to value: " + progress);
                    durationView.setText(Integer.toString(progress));
                    duration = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });



        mSpheroConnectionView = (SpheroConnectionView) findViewById(R.id.sphero_connection_view);
        mSpheroConnectionView.setSingleSpheroMode(true); // todo - allow connection of more than one.


    }

    /**
     * Called when the user comes back to this app
     */
    @Override
    protected void onResume() {
        super.onResume();

        RobotProvider.getDefaultProvider().addConnectionListener(new ConnectionListener() {

            @Override
            public void onConnected(Robot robot) {
                mRobot = (Sphero) robot;

                Log.i(TAG, "connected to version:" + mRobot.getVersion());

                createNotification();
                startBlink(null);

            }

            @Override
            public void onConnectionFailed(Robot sphero) {
                // let the SpheroConnectionView handle or hide it and do something here...
                Log.i(TAG,"connection failed");
            }

            @Override
            public void onDisconnected(Robot sphero) {
                Log.i(TAG,"connection disconnected");
                mRobot = null;

                if (!sphero.isConnected()) {
                    mSpheroConnectionView.startDiscovery();
                }
            }

        });

//        if (mRobot == null) {
//            mSpheroConnectionView.startDiscovery();
//        }
    }

    /**
     * Called when the user presses the back or home button
     */
    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();
        stopBlink(null);
        // Disconnect Robot properly
        RobotProvider.getDefaultProvider().disconnectControlledRobots();
        RobotProvider.getDefaultProvider().removeDiscoveryListeners();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if(intent.getDataString() != null) {
            Log.i(TAG, intent.getDataString());

            if (intent.getDataString().equals(BLINK)) {
                startBlink(null);
            } else if (intent.getDataString().equals(DRIVE)) {
                doDrive(null);
            } else if (intent.getDataString().equals(STOP)) {
                stopDrive(null);
            }
        }
    }


    public void startBlink(View view) {
        shouldBlink = true;
        setBlueColor(null);
        blink(false);
    }

    public void stopBlink(View view) {
        shouldBlink = false;
    }

    private void blink(final boolean lit) {

        if (mRobot != null) {

            //If not lit, send command to show blue light, or else, send command to show no light
            if (lit && shouldBlink) {
                mRobot.setColor(0, 0, 0);                               // 1
            } else {
                mRobot.setColor(red, green, blue);                             // 2
            }


            final Handler handler = new Handler();                       // 3
            handler.postDelayed(new Runnable() {
                public void run() {
                    blink(!lit);
                }
            }, 300);

        }
    }

    public void doDrive(View view) {
        shouldStop = false;
        stopBlink(null);
        setOrangeColor(null);
        driveEight(mRobot, 0f, true);
    }

    public void stopDrive(View view){
        shouldStop = true;
        if(mRobot != null) {
            mRobot.stop();
            startBlink(null);
        }
    }

    public static void driveEight(final Sphero sphero, final float rotation, final boolean isFirstEight) {

        if (sphero != null) {


            final Handler handler = new Handler();                               // 2
            handler.postDelayed(new MyRunnable(sphero, rotation, isFirstEight), duration);

        }
    }

    public void setBlueColor(View view) {
        red = 0;
        blue = 190;
        green = 20;
    }

    public void setOrangeColor(View view) {
        red = 255;
        green = 165;
        blue = 0;
    }

    public void setRedColor(View view) {
        red = 255;
        blue = 0;
        green = 0;
    }


    private void createNotification() {
        int notificationId = 001;
        // Build intent for notification content


        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.sphero);


        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.sphero)
                        .setLargeIcon(bm)
                        .setContentTitle("Sphero")
                        .setContentText("Connected")

                        .addAction(R.drawable.eight,
                                "Draw an 8", createIntentWithUrl(DRIVE))

                        .addAction(R.drawable.stop,
                                "Stop drive", createIntentWithUrl(STOP));

// Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

// Build the notification and issues it with notification manager.
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private PendingIntent createIntentWithUrl(String action) {
        Intent viewIntent = new Intent(this, SpheroActivity.class);
        Uri wtfUri = Uri.parse(action);
        viewIntent.setData(wtfUri);

        return PendingIntent.getActivity(this, 0, viewIntent, 0);
    }

    private static class MyRunnable implements Runnable {
        float rotation = 0.f;
        boolean isFirstEight = true;
        Sphero mRobot;

        MyRunnable(Sphero robot, final float rotation, final boolean isFirstEight) {
            this.mRobot = robot;
            this.rotation = rotation;
            this.isFirstEight = isFirstEight;
        }

        public void run() {
            if(shouldStop) {
                return;
            }

            if (isFirstEight) {

                rotation += 10.f;
                if (rotation >= 360.f) {
                    isFirstEight = false;
                }

            } else {

                rotation -= 10.f;
                if (rotation <= 0.f) {
                    isFirstEight = true;
                }
            }

            if(mRobot != null && mRobot.isConnected()) {
                mRobot.drive(rotation, speed);
            } else {
                shouldStop = true;
            }

            SpheroActivity.driveEight(mRobot, rotation, isFirstEight);

        }
    }

}
