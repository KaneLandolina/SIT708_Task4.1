package com.example.task41;

/* --------------- STUDENT DETAILS ----------------
Name: Kane Landolina
ID: 218692411
UNIT Code: SIT708
 */

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import com.example.task41.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    private CountDownTimer workoutTimer;

    private CountDownTimer restTimer;

    private boolean workoutTimerRunning = false;
    private boolean restTimerRunning = false;

    private static boolean cancel = false;

    int i = 0;

    private NotificationManagerCompat notificationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationManager = NotificationManagerCompat.from(this);

        //Creating binding for using items from view
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //when start button is pressed, save inputs from view to variables to control how long timers run for and how many times to repeat
                long workoutDuration = Long.parseLong(binding.editTextWorkDuration.getText().toString()) * 1000;
                long restDuration = Long.parseLong(binding.editTextRestDuration.getText().toString()) * 1000;
                long rounds = Long.parseLong(binding.editTextRounds.getText().toString());

                //create countdown timer for workout, added extra milliseconds for processing due to issues with on screen time skipping numbers
                workoutTimer = new CountDownTimer(workoutDuration + 25, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long minutes = (millisUntilFinished / 60000) % 60;
                        long seconds = (millisUntilFinished / 1000) % 60;

                        //block start button from being pressed while timer is running
                        binding.buttonStart.setClickable(false);
                        binding.buttonStart.setBackgroundColor(Color.YELLOW);

                        workoutTimerRunning = true;

                        //show running image and text on screen during workout
                        binding.imageView.setImageResource(R.drawable.runicon);
                        binding.activityTextView.setText("WORKOUT");

                        //update on screen countdown timer with up-to-date minutes and seconds
                        binding.countDownTextView.setText(String.format("%02d:%02d", minutes, seconds));

                        //update progress bar at same time as timer
                        int progress = (int) (100 - (millisUntilFinished * 100) / (workoutDuration));
                        binding.progressBar.setProgress(progress);

                        //if user input 'cancel' on notification pop-up, both timers are cancelled and start button can be pressed again
                        if(cancel == true){
                            restTimer.cancel();
                            workoutTimer.cancel();
                            workoutTimerRunning = false;
                            restTimerRunning = false;
                            cancel = false;
                            binding.buttonStart.setClickable(true);
                            binding.buttonStart.setBackgroundColor(Color.parseColor("#6200ee"));
                            notificationManager.cancel(1);
                        }


                    }

                    @Override
                    public void onFinish() {
                        //when workout timer is finished, play a sound and show rest icon and message
                        workoutTimerRunning = false;
                        MediaPlayer.create(MainActivity.this, R.raw.beeptone).start();
                        binding.imageView.setImageResource(R.drawable.resticon2);
                        binding.activityTextView.setText("REST");

                        long minutes = (restDuration / 60000) % 60;
                        long seconds = (restDuration / 1000) % 60;

                        //create intent for notification to return to MainActivity is notification is pressed
                        Intent intent = new Intent(MainActivity.this, MainActivity.class);
                        intent.setAction(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_LAUNCHER);

                        //create intent to allow user to cancel the timer from the notification
                        Intent cancelIntent = new Intent(MainActivity.this, NotificationActionService.class)
                                .setAction("cancel");
                        cancelIntent.putExtra("action", "continue");

                        //create pendingIntent variables for both cancel and return intents for use with the notification
                        //cancel intent has a listener "NotificationActionService" which will set cancel to true if cancel is pressed
                        PendingIntent pendingIntentCancel = PendingIntent.getService(MainActivity.this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE);
                        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

                        //create the notification and notify the user
                        Notification notification = new NotificationCompat.Builder(MainActivity.this, ChannelBuild.CHANNEL_1_ID)
                                .setSmallIcon(R.drawable.pngegg)
                                .setContentTitle("Begin Rest")
                                .setContentText("Total Rest Time: " + String.format("%02d:%02d", minutes, seconds))
                                .setPriority(Notification.PRIORITY_HIGH)
                                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                .setContentIntent(pendingIntent)
                                .addAction(R.drawable.pngegg,"Cancel Timer", pendingIntentCancel)
                                .build();

                        notificationManager.notify(1,notification);

                        //create the rest timer
                        //added extra milliseconds to countdown timer total to avoid issues with skipped seconds
                        restTimer = new CountDownTimer(restDuration + 50, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                long minutes = (millisUntilFinished / 60000) % 60;
                                long seconds = (millisUntilFinished / 1000) % 60;

                                restTimerRunning = true;

                                //update user view for countdown timer and progress bar
                                binding.countDownTextView.setText(String.format("%02d:%02d", minutes, seconds));

                                int progress = (int) (100 - (millisUntilFinished * 100) / (restDuration));
                                binding.progressBar.setProgress(progress);

                                //if cancel is pressed from the notification, cancel the timers and allow start to be pressed again
                                if(cancel == true){
                                    restTimer.cancel();
                                    workoutTimer.cancel();
                                    workoutTimerRunning = false;
                                    restTimerRunning = false;
                                    cancel = false;
                                    binding.buttonStart.setClickable(true);
                                    binding.buttonStart.setBackgroundColor(Color.parseColor("#6200ee"));
                                    notificationManager.cancel(1);
                                }
                            }

                            @Override
                            public void onFinish() {
                                //when resttimer is finished, play a sound
                                i++;
                                MediaPlayer.create(MainActivity.this, R.raw.whistletone).start();
                                //if the amount of rounds is less the 'i' restart the workoutTimer and send a notification
                                if (i < rounds) {
                                    workoutTimer.start();
                                        long minutes = (workoutDuration / 60000) % 60;
                                        long seconds = (workoutDuration / 1000) % 60;
                                        Notification notification = new NotificationCompat.Builder(MainActivity.this, ChannelBuild.CHANNEL_1_ID)
                                                .setSmallIcon(R.drawable.pngegg)
                                                .setContentTitle("Begin Workout")
                                                .setContentText("Total Workout Time: " + String.format("%02d:%02d", minutes, seconds))
                                                .setPriority(Notification.PRIORITY_HIGH)
                                                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                                .setContentIntent(pendingIntent)
                                                .addAction(R.drawable.pngegg,"Cancel Timer", pendingIntentCancel)
                                                .build();

                                        notificationManager.notify(1,notification);
                                } else {
                                    //if the total rounds has been met, allow the start button to be pressed again
                                    i = 0;
                                    restTimerRunning = false;
                                    binding.buttonStart.setClickable(true);
                                    binding.buttonStart.setBackgroundColor(Color.parseColor("#6200ee"));
                                }
                            }
                        };
                        restTimer.start();
                    }
                };
                workoutTimer.start();
            }
        });

        binding.buttonStop.setOnClickListener(new View.OnClickListener() {
            //if the stop button is pressed, checkc which timer is running and cancel the timer
            //allow the start button to be pressed again
            @Override
            public void onClick(View view) {
                if(workoutTimerRunning == true) {
                    workoutTimer.cancel();
                    workoutTimerRunning = false;
                } else if (restTimerRunning == true) {
                    restTimer.cancel();
                    restTimerRunning = false;
                }
                binding.buttonStart.setClickable(true);
                binding.buttonStart.setBackgroundColor(Color.parseColor("#6200ee"));
            }
        });


    }

    //an intent listener to change cancel to true when user presses cancel on the notification
    public static class NotificationActionService extends IntentService {
        public NotificationActionService() {
            super(NotificationActionService.class.getSimpleName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            String action = intent.getAction();
            if (action == "cancel") {
                cancel = true;

            }
        }
    }

}


