package net.skix.whearo;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preview.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import orbotix.robot.base.OrbBasicControl;
import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotProvider;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.Sphero;
import orbotix.view.calibration.CalibrationButtonView;
import orbotix.view.connection.SpheroConnectionView;


public class SpheroActivity extends Activity {
    private static final String TAG = "WHEARO-";

    /**
     * Sphero Connection View
     */
    private SpheroConnectionView mSpheroConnectionView;
    private CalibrationButtonView mCalibrationButtonViewAbove;

    int red = 0;
    int green = 10;
    int blue = 200;

    boolean shouldStop = false;

    private final static String BLINK = "do:blink";
    private final static String STOPBLINK = "do:stop";
    private final static String DRIVE = "do:driveEight";

    private final static float speed = 0.5f;

    EditText durationView;
    private static int duration;

    /**
     * Robot to from which we are running OrbBasic programs on
     */
    private Sphero mRobot = null;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sphero);

        durationView = (EditText) findViewById(R.id.duration);
        durationView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            public void afterTextChanged(Editable s) {
                if(s.length() > 0) {
                    Log.i(TAG, "duration changed to " + s.toString());
                    duration = Integer.parseInt(s.toString());
                }
            }});

                duration = Integer.parseInt(durationView.getText().toString());

                mSpheroConnectionView = (SpheroConnectionView) findViewById(R.id.sphero_connection_view);
                mSpheroConnectionView.setSingleSpheroMode(true); // todo - allow connection of more than one.

                RobotProvider.getDefaultProvider().addConnectionListener(new ConnectionListener() {

                    @Override
                    public void onConnected(Robot robot) {
                        mRobot = (Sphero) robot;
                        Log.d(TAG, "Version:" + mRobot.getVersion());
                        mRobot.getOrbBasicControl().addEventListener(new OrbBasicControl.EventListener() {
                            @Override
                            public void onEraseCompleted(boolean success) {
                                String successStr = (success) ? "Success" : "Failure";

                            }

                            @Override
                            public void onLoadProgramComplete(boolean success) {
                                String successStr = (success) ? "Success" : "Failure";

                            }

                            @Override
                            public void onPrintMessage(String message) {

                            }

                            @Override
                            public void onErrorMessage(String message) {
                                Log.d("ERROR", message);

                            }

                            @Override
                            public void onErrorByteArray(byte[] bytes) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }
                        });

                        createNotification();

                    }

                    @Override
                    public void onConnectionFailed(Robot sphero) {
                        // let the SpheroConnectionView handle or hide it and do something here...
                    }

                    @Override
                    public void onDisconnected(Robot sphero) {
                        if (!sphero.isConnected()) {
                            mSpheroConnectionView.startDiscovery();
                        }
                    }

                });
            }

            /**
             * Called when the user comes back to this app
             */
            @Override
            protected void onResume() {
                super.onResume();
                if (mRobot == null) {
                    mSpheroConnectionView.startDiscovery();
                }
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

                // Disconnect Robot properly
                RobotProvider.getDefaultProvider().disconnectControlledRobots();
                RobotProvider.getDefaultProvider().removeDiscoveryListeners();
            }

            @Override
            protected void onNewIntent(Intent intent) {
                Log.i(TAG, intent.getDataString());

                if (intent.getDataString().equals(BLINK)) {
                    startBlink(null);
                } else if (intent.getDataString().equals(STOPBLINK)) {
                    stopBlink(null);
                } else if (intent.getDataString().equals(DRIVE)) {
                    doDrive(null);
                }
            }


            public void startBlink(View view) {
                shouldStop = false;
                blink(false);
            }

            public void stopBlink(View view) {
                shouldStop = true;
            }

            private void blink(final boolean lit) {

                if (mRobot != null) {

                    //If not lit, send command to show blue light, or else, send command to show no light
                    if (lit) {
                        mRobot.setColor(0, 0, 0);                               // 1
                    } else {
                        mRobot.setColor(red, green, blue);                             // 2
                    }

                    //Send delayed message on a handler to run blink again
                    if (!shouldStop) {
                        final Handler handler = new Handler();                       // 3
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                blink(!lit);
                            }
                        }, 300);
                    }
                }
            }

            public void doDrive(View view) {
                driveEight(mRobot, 0f, true);
            }

            public static void driveEight(final Sphero sphero, final float rotation, final boolean isFirstEight) {

                if (sphero != null) {


                    final Handler handler = new Handler();                               // 2
                    handler.postDelayed(new MyRunnable(sphero, rotation, isFirstEight), duration);

                }
            }

            public void setBlueColor(View view) {
                red = 0;
                blue = 200;
                green = 10;
            }

            public void setRedColor(View view) {
                red = 255;
                blue = 0;
                green = 0;
            }


            private void createNotification() {
                int notificationId = 001;
                // Build intent for notification content


                NotificationCompat.Builder notificationBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setContentTitle("Sphero")
                                .setContentText("Connected")

                                .addAction(R.drawable.play,
                                        "Blink", createIntentWithUrl(BLINK))

                                .addAction(R.drawable.stop,
                                        "Stop Blink", createIntentWithUrl(STOPBLINK))

                                .addAction(R.drawable.eight,
                                        "Drive a 8", createIntentWithUrl(DRIVE));

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
                    mRobot.drive(rotation, speed);

                    if (isFirstEight) {

                        rotation += 10.f;
                        if (rotation > 360.f) {
                            isFirstEight = false;
                            mRobot.stop();
                        }

                    } else {

                        rotation -= 10.f;
                        if (rotation <= 0.f) {
                            isFirstEight = true;
                            mRobot.stop();
                            return;
                        }
                    }
                    SpheroActivity.driveEight(mRobot, rotation, isFirstEight);
                }
            }

        }
