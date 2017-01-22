package com.daysailsoftware.simelevator;

import android.graphics.Color;
import android.graphics.Typeface;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

enum elevator_state {
    stopped,
    moving
}

enum elevator_stopped {
    arriving,
    door_open,
    door_closed
}

enum elevator_direction {
    none,
    ascending,
    descending
}

public class MainActivity extends AppCompatActivity {

    private TextView txtDisplayFloor;
    private TextView txtDisplayMessage;
    private ImageView imgDisplayUp;
    private ImageView imgDisplayDown;


    private Button btnFloors[];
    private Button btnAlarm;
    private int floor_current;
    private String floor_display[];
    private String welcome_messages[];
    private Boolean floor_stop[];
    private int state_counter;
    private SoundPool sounds;
    private int soundIdsWelcome[];
    private int soundIdsOpen;
    private int soundIdsClose;
    private int soundIdsDing;
    private int soundIdsAlarm;
    private int soundIdsFloors[];
    private int soundIdsGoingUp;
    private int soundIdsGoingDown;
    private boolean alarm;


    private elevator_state current_state;
    private elevator_direction current_direction;
    private elevator_direction last_direction;
    private elevator_stopped current_stop_phase;

    private SoundTask soundTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the LED display attributes including custom font
        Typeface ledDisplayFont = Typeface.createFromAsset(getAssets(), "fonts/DSEG14Classic-Regular.ttf");

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int width = metrics.widthPixels;

        txtDisplayFloor = (TextView) findViewById(R.id.txtDisplayFloor);
        txtDisplayFloor.setTypeface(ledDisplayFont);
        txtDisplayFloor.setTextColor(Color.BLUE);
        txtDisplayFloor.setTextSize(TypedValue.COMPLEX_UNIT_PX, ((float)width/(float)5.0)-(float)50.0);
        txtDisplayFloor.setWidth((width - 32) / 5 * 3);

        txtDisplayMessage = (TextView) findViewById(R.id.txtDisplayMessage);
        txtDisplayMessage.setTypeface(ledDisplayFont);
        txtDisplayMessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)width/(float)24.0);
        txtDisplayMessage.setTextColor(Color.BLUE);

        int widthArrow = ( ( width - 32 ) / 5 );
        imgDisplayUp = (ImageView) findViewById(R.id.imgDisplayUp);
        imgDisplayUp.setBackgroundColor(Color.BLACK);
        imgDisplayUp.getLayoutParams().width = widthArrow;
        imgDisplayUp.getLayoutParams().height = widthArrow;
        imgDisplayUp.setVisibility(View.INVISIBLE);

        imgDisplayDown = (ImageView) findViewById(R.id.imgDisplayDown);
        imgDisplayDown.setBackgroundColor(Color.BLACK);
        imgDisplayDown.getLayoutParams().width = widthArrow;
        imgDisplayDown.getLayoutParams().height = widthArrow;
        imgDisplayDown.setVisibility(View.INVISIBLE);

        btnFloors = new Button[10];
        btnFloors[0] = (Button) findViewById(R.id.btnFloorBasement);
        btnFloors[1] = (Button) findViewById(R.id.btnFloor01);
        btnFloors[2] = (Button) findViewById(R.id.btnFloor02);
        btnFloors[3] = (Button) findViewById(R.id.btnFloor03);
        btnFloors[4] = (Button) findViewById(R.id.btnFloor04);
        btnFloors[5] = (Button) findViewById(R.id.btnFloor05);

        btnAlarm = (Button) findViewById(R.id.btnDoorAlarm);
        btnAlarm.setBackgroundColor(Color.BLUE);

        sounds = new SoundPool.Builder().build();

        soundIdsWelcome = new int[10];
        soundIdsWelcome[0] = sounds.load(this, R.raw.welcomefloor01, 1);
        soundIdsWelcome[1] = sounds.load(this, R.raw.welcomefloor02, 1);
        soundIdsWelcome[2] = sounds.load(this, R.raw.welcomefloor03, 1);
        soundIdsWelcome[3] = sounds.load(this, R.raw.welcomefloor04, 1);
        soundIdsWelcome[4] = sounds.load(this, R.raw.welcomefloor05, 1);
        soundIdsWelcome[5] = sounds.load(this, R.raw.welcomefloor06, 1);
        soundIdsWelcome[6] = sounds.load(this, R.raw.welcomefloor07, 1);
        soundIdsWelcome[7] = sounds.load(this, R.raw.welcomefloor08, 1);
        soundIdsWelcome[8] = sounds.load(this, R.raw.welcomefloor09, 1);
        soundIdsWelcome[9] = sounds.load(this, R.raw.welcomefloor10, 1);
        soundIdsOpen = sounds.load(this, R.raw.door_open, 1);
        soundIdsClose = sounds.load(this, R.raw.door_close, 1);
        soundIdsDing = sounds.load(this, R.raw.elevator_ding_soundbible_com_685385892, 1);
        soundIdsAlarm = sounds.load(this, R.raw.alarm, 1);
        soundIdsFloors = new int[10];
        soundIdsFloors[0] = sounds.load(this, R.raw.floor_basement, 1);
        soundIdsFloors[1] = sounds.load(this, R.raw.floor01, 1);
        soundIdsFloors[2] = sounds.load(this, R.raw.floor02, 1);
        soundIdsFloors[3] = sounds.load(this, R.raw.floor03, 1);
        soundIdsFloors[4] = sounds.load(this, R.raw.floor04, 1);
        soundIdsFloors[5] = sounds.load(this, R.raw.floor05, 1);
        soundIdsGoingUp = sounds.load(this, R.raw.going_up, 1);
        soundIdsGoingDown = sounds.load(this, R.raw.going_down, 1);

        soundTask = new SoundTask(sounds);


        floor_stop = new Boolean[] {false, false, false, false, false, false, false, false, false, false};
        floor_display = new String[] { "6HB", "1", "2", "3", "4", "5", "6", "7", "8", "9" };
        welcome_messages = new String[] {
                "Welcome to Haunted Basement",
                "Welcome to First Floor",
                "Welcome to Second Floor",
                "Welcome to Third Floor",
                "Welcome to Fourth Floor",
                "Welcome to Fifth Floor",
                "Welcome to Sixth Floor",
                "Welcome to Seventh Floor",
                "Welcome to Eighth Floor",
                "Welcome to Ninth Floor"
        };

        floor_current = 1;
        state_counter = 0;
        current_state = elevator_state.stopped;
        current_stop_phase = elevator_stopped.door_closed;
        current_direction = elevator_direction.none;
        last_direction = elevator_direction.none;
        alarm = false;

        SendFloorTask sendFloorTask = new SendFloorTask();
        sendFloorTask.execute(floor_current);

        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethod();
            }
        }, 0, 1000);
    }

    public void sendMessageFloorBasement(View view) {
        processButton(0);
    }

    public void sendMessageFloor01(View view) {
        processButton(1);
    }

    public void sendMessageFloor02(View view) {
        processButton(2);
    }

    public void sendMessageFloor03(View view) {
        processButton(3);
    }

    public void sendMessageFloor04(View view) {
        processButton(4);
    }

    public void sendMessageFloor05(View view) {
        processButton(5);
    }

    public void sendMessageDoorOpen(View view) {
        if ( elevator_state.stopped == current_state ) {
            state_counter = -1;
            current_stop_phase = elevator_stopped.door_open;
        }
    }

    public void sendMessageDoorClose(View view) {
        if ( elevator_state.stopped == current_state ) {
            alarm = false;
            state_counter = -1;
            current_stop_phase = elevator_stopped.door_closed;
        }
    }

    public void sendMessageAlarm(View view) {
        alarm = true;
        txtDisplayMessage.setTextColor(Color.RED);
        txtDisplayMessage.setText(R.string.display_alarm);
        SoundTask soundTask = new SoundTask(sounds);
        soundTask.execute(soundIdsAlarm);
    }


    private void processButton(int button) {
        if ( floor_current != button) {
            floor_stop[button] = true;
            btnFloors[button].setBackgroundColor(Color.BLUE);
        }
    }

    private void TimerMethod() {
        this.runOnUiThread(Timer_Tick);
    }

    private final Runnable Timer_Tick = new Runnable() {
        @Override
        public void run() {
            switch (current_state) {
                case stopped:
                    processStateStopped();
                    break;

                case moving:
                    processStateMoving();
                    break;
            }

            txtDisplayFloor.setText(floor_display[floor_current]);
        }
    };

    private void processStateStopped() {
        Log.d("state_counter", Integer.toString(state_counter));

        switch ( current_stop_phase ) {
            case arriving:
                processSubStateArriving();
                break;

            case door_open:
                processSubStateDoorOpen();
                break;

            case door_closed:
                processSubStateDoorClose();
                break;
        }

        switch (current_direction) {
            case ascending:
            case descending:
                // If elevator is already scheduled to move, decrement wait count
                state_counter --;
                break;
            default:
                // If elevator is not currently scheduled to move, determine  if it should
                if ( elevator_direction.ascending == last_direction ) {
                    if ( floorsPendingDescending() ) {
                        current_direction = elevator_direction.descending;
                        imgDisplayUp.setBackgroundColor(Color.BLACK);
                        imgDisplayDown.setBackgroundColor(Color.BLUE);
                        imgDisplayUp.setVisibility(View.INVISIBLE);
                        imgDisplayDown.setVisibility(View.VISIBLE);
                        state_counter = 3;
                    } else if ( floorsPendingAscending() ) {
                        current_direction = elevator_direction.ascending;
                        imgDisplayUp.setBackgroundColor(Color.BLUE);
                        imgDisplayDown.setBackgroundColor(Color.BLACK);
                        imgDisplayUp.setVisibility(View.VISIBLE);
                        imgDisplayDown.setVisibility(View.INVISIBLE);
                        state_counter = 3;
                    }
                } else {
                    if ( floorsPendingAscending() ) {
                        current_direction = elevator_direction.ascending;
                        imgDisplayUp.setBackgroundColor(Color.BLUE);
                        imgDisplayDown.setBackgroundColor(Color.BLACK);
                        imgDisplayUp.setVisibility(View.INVISIBLE);
                        imgDisplayDown.setVisibility(View.VISIBLE);
                        state_counter = 3;
                    } else if ( floorsPendingDescending() ) {
                        current_direction = elevator_direction.descending;
                        imgDisplayUp.setBackgroundColor(Color.BLACK);
                        imgDisplayDown.setBackgroundColor(Color.BLUE);
                        imgDisplayUp.setVisibility(View.VISIBLE);
                        imgDisplayDown.setVisibility(View.INVISIBLE);
                        state_counter = 3;
                    }
                }
                break;
        }

        if ( -1 == state_counter && elevator_direction.none != current_direction &&  elevator_stopped.door_closed == current_stop_phase ) {
            if ( elevator_direction.ascending == current_direction ) {
                SoundTask soundTask = new SoundTask(sounds);
                soundTask.execute(soundIdsGoingUp);
            } else if ( elevator_direction.descending == current_direction ) {
                SoundTask soundTask = new SoundTask(sounds);
                soundTask.execute(soundIdsGoingDown);
            }

            current_state = elevator_state.moving;
            state_counter = -1;
        }

        if ( state_counter > 0 ) state_counter --;
    }  // End processStateStopped

    private void processSubStateArriving() {
        Log.d("Arriving-state_counter", Integer.toString(state_counter));

        if ( state_counter < 0 ) {
            state_counter = 3;
        }

        switch (state_counter) {
            case 3:
                btnFloors[floor_current].setBackgroundColor(Color.GRAY);
                SoundTask soundTask = new SoundTask(sounds);
                soundTask.execute(soundIdsFloors[floor_current]);
                txtDisplayMessage.setTextColor(Color.BLUE);
                txtDisplayMessage.setText(welcome_messages[floor_current]);
                break;

            case 0:
                state_counter = -1;
                current_stop_phase = elevator_stopped.door_open;
                break;
        }
    }  // End processSubStateArriving

    private void processSubStateDoorOpen() {
        Log.d("DoorOpen-state_counter", Integer.toString(state_counter));

        if ( state_counter < 0 ) {
            state_counter = 15;
        }

        switch (state_counter) {
            case 15:
                SoundTask soundTask = new SoundTask(sounds);
                soundTask.execute(soundIdsOpen);
                txtDisplayMessage.setTextColor(Color.RED);
                txtDisplayMessage.setText(R.string.display_door_open1);
                break;
            case 14:
                txtDisplayMessage.setText(R.string.display_door_open2);
                break;
            case 13:
                txtDisplayMessage.setText(R.string.display_door_open3);
                break;
            case 12:
                txtDisplayMessage.setText(R.string.display_door_open4);
                break;
            case 11:
                txtDisplayMessage.setText(R.string.display_door_open5);
                break;
            case 10:
            case 8:
            case 6:
            case 4:
            case 2:
                txtDisplayMessage.setTextColor(Color.GREEN);
                txtDisplayMessage.setText(R.string.display_door_open6);
                break;
            case 9:
            case 7:
            case 5:
            case 3:
            case 1:
                txtDisplayMessage.setTextColor(Color.GREEN);
                txtDisplayMessage.setText(" ");
                break;
            case 0:
                txtDisplayMessage.setTextColor(Color.GREEN);
                txtDisplayMessage.setText(R.string.display_door_open6);
                current_stop_phase = elevator_stopped.door_closed;
                state_counter = -1;
                break;
        }
    }  // End processSubStateDoorOpen

    private void processSubStateDoorClose() {
        Log.d("DoorClose-state_counter", Integer.toString(state_counter));

        if ( state_counter < 0 ) {
            state_counter = 5;
        }

        switch (state_counter) {
            case 5:
                SoundTask soundTask = new SoundTask(sounds);
                soundTask.execute(soundIdsClose);
                txtDisplayMessage.setTextColor(Color.RED);
                txtDisplayMessage.setText(R.string.display_door_close1);
            case 4:
                txtDisplayMessage.setText(R.string.display_door_close2);
                break;
            case 3:
                txtDisplayMessage.setText(R.string.display_door_close3);
                break;
            case 2:
                txtDisplayMessage.setTextColor(Color.GREEN);
                txtDisplayMessage.setText(R.string.display_door_close4);
                break;
            case 0:
                txtDisplayMessage.setText(" ");
                break;
        }
    }  // End processSubStateDoorClose

    private void processStateMoving() {
        if ( state_counter < 0) {
            state_counter = 5;
        }

        switch ( state_counter ) {
            case 3:
                if ( elevator_direction.descending == current_direction ) {
                    floor_current --;
                } else if ( elevator_direction.ascending == current_direction ) {
                    floor_current++;
                }

                SoundTask soundTask = new SoundTask(sounds);
                soundTask.execute(soundIdsDing);
                SendFloorTask sendFloorTask = new SendFloorTask();
                sendFloorTask.execute(floor_current);
                txtDisplayFloor.setText(floor_display[floor_current]);
                break;

            case 0:
                if ( elevator_direction.descending == current_direction ) {
                    if ( floor_stop[floor_current] ) {
                        floor_stop[floor_current] = false;

                        if ( ! floorsPendingDescending() ) {
                            last_direction = elevator_direction.descending;
                            current_direction = elevator_direction.none;
                            imgDisplayUp.setBackgroundColor(Color.BLACK);
                            imgDisplayDown.setBackgroundColor(Color.BLACK);
                            imgDisplayUp.setVisibility(View.INVISIBLE);
                            imgDisplayDown.setVisibility(View.INVISIBLE);
                        }

                        current_stop_phase = elevator_stopped.arriving;
                        current_state = elevator_state.stopped;
                    }
                } else if ( elevator_direction.ascending == current_direction ) {
                    if ( floor_stop[floor_current] ) {
                        floor_stop[floor_current] = false;

                        if ( ! floorsPendingAscending() ) {
                            last_direction = elevator_direction.ascending;
                            current_direction = elevator_direction.none;
                            imgDisplayUp.setBackgroundColor(Color.BLACK);
                            imgDisplayDown.setBackgroundColor(Color.BLACK);
                            imgDisplayUp.setVisibility(View.INVISIBLE);
                            imgDisplayDown.setVisibility(View.INVISIBLE);
                        }

                        current_stop_phase = elevator_stopped.arriving;
                        current_state = elevator_state.stopped;
                    }
                }

                state_counter = -1;

                break;
        }

        Log.d("Moving-state_counter3", Integer.toString(state_counter));
        if ( state_counter > 0 ) state_counter--;
    }  // End processStateMoving

    private void readStream(InputStream in) {
    }

    private boolean floorsPendingAscending() {
        boolean result = false;

        for ( int n = floor_current ; n < 10 ; n ++ ) {
            if ( floor_stop[n] ) result = true;
        }

        return result;
    }  // End floorsPendingAscending

    private boolean floorsPendingDescending() {
        boolean result = false;

        for ( int n = floor_current ; n >= 0 ; n -- ) {
            if ( floor_stop[n] ) result = true;
        }

        return result;
    }  // End floorsPendingDescending

    class SoundTask extends AsyncTask<Integer, Void, Integer> {
        private final WeakReference<SoundPool> soundsReference;
        private int data = 0;

        public SoundTask(SoundPool sounds) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            soundsReference = new WeakReference<>(sounds);
        }

        // Play sound in background
        @Override
        protected Integer doInBackground(Integer... params) {
            soundsReference.get().play(params[0],1,1,1,0,1);
            return 0;
        }

        @Override
        protected void onPostExecute(Integer n) {
        }
    }

    class SendFloorTask extends AsyncTask<Integer, Void, Integer> {
        @Override
        protected Integer doInBackground(Integer... params) {
            try {
                URL url = new URL("http://notebook-ent.com/daysailsoftware/update.php?floor=" + floor_current);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    readStream(in);
                } finally {
                    urlConnection.disconnect();
                }
            } catch (java.net.MalformedURLException e) {

            } catch (java.io.IOException e) {

            }
            return null;
        }
    }
}
