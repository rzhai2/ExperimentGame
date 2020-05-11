package com.jhu.chenyuzhang.experimentgame.Questions;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.jhu.chenyuzhang.experimentgame.Bluetooth;
import com.jhu.chenyuzhang.experimentgame.EndDemoActivity;
import com.jhu.chenyuzhang.experimentgame.R;
import com.jhu.chenyuzhang.experimentgame.ResultActivity;
import com.jhu.chenyuzhang.experimentgame.TimeDbHelper;
import com.jhu.chenyuzhang.experimentgame.Trial;
import com.jhu.chenyuzhang.experimentgame.TrialDbHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

// Vertical display of two options; either 2 or 4 attributes in each option
// attributes not present are set to be 'gone'
public class QuestionActivity extends AppCompatActivity {
    private static final String TAG = "bluetooth";

    public static double totalAmountWon;
    public static final String KEY_TOTAL_AMOUNT = "keyTotalAmount";

    private boolean isDemo;
    private static final String KEY_DO_DEMO = "keyDoDemo";
    private SharedPreferences demo_prefs;

    private CountDownTimer countDownTimer;

    private TrialDbHelper trialInfoDb;
    private Trial currentTrial;
    private static int trialCounter;
    private SharedPreferences counter_prefs;
    public static final String KEY_TRIAL_COUNTER = "keyTrialCounter";
    private double amountWon;
    private double p1;
    private double p2;
    private double a1;
    private double a2;

    private TimeDbHelper timeRecordDb;

    private TextView textViewDollar1;
    private TextView textViewProbability1;
    private TextView textViewDollar2;
    private TextView textViewProbability2;
    private TextView textViewTest;

    private ViewAnimator viewAnimatorDollar1;
    private ViewAnimator viewAnimatorDollar2;
    private ViewAnimator viewAnimatorProbability1;
    private ViewAnimator viewAnimatorProbability2;

    private Choreographer.FrameCallback frameCallbackA1 = null;
    private boolean frameCallbackPendingA1 = false;
    /*
    private Choreographer.FrameCallback frameCallbackP1 = null;
    private boolean frameCallbackPendingP1 = false;
    private Choreographer.FrameCallback frameCallbackA2 = null;
    private boolean frameCallbackPendingA2 = false;
    private Choreographer.FrameCallback frameCallbackP2 = null;
    private boolean frameCallbackPendingP2 = false;
    */

    //private LocalTime startTimeWorld;

    private String eventClick = "Clicked, Displayed";
    private String eventTimeOut = "TimeOut, Covered";

    private long backPressedTime;

    Bluetooth bluetooth;

    // identifers maps the id of a attribute view to the code sent when it is uncovered
    // for each attribute, contains two codes before and after the uncover; third code is its alias in the database
    private HashMap<Integer, String[]> identifiers = new HashMap<>();

    // the code sent when an attribute view is covered after 1s
    private String identifier_cover = "16";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

        demo_prefs = getSharedPreferences("doDemo", MODE_PRIVATE);
        isDemo = demo_prefs.getBoolean(KEY_DO_DEMO, true);   // get shared preference of whether this is a training session

        identifiers.put(R.id.view_animator_dollar1, new String[] {"3", "7", "A1"});
        identifiers.put(R.id.view_animator_dollar2, new String[] {"5", "9", "A2"});
        identifiers.put(R.id.view_animator_probability1, new String[] {"4", "8", "P1"});
        identifiers.put(R.id.view_animator_probability2, new String[] {"6", "10", "P2"});

        textViewDollar1 = findViewById(R.id.text_view_dollar1);
        textViewDollar2 = findViewById(R.id.text_view_dollar2);
        textViewProbability1 = findViewById(R.id.text_view_probability1);
        textViewProbability2 = findViewById(R.id.text_view_probability2);

        textViewTest = findViewById(R.id.text_view_test);

        Button buttonSelect1 = findViewById(R.id.button_select1);
        Button buttonSelect2 = findViewById(R.id.button_select2);

        viewAnimatorDollar1 = findViewById(R.id.view_animator_dollar1);
        viewAnimatorDollar2 = findViewById(R.id.view_animator_dollar2);
        viewAnimatorProbability1 = findViewById(R.id.view_animator_probability1);
        viewAnimatorProbability2 = findViewById(R.id.view_animator_probability2);

        // if is in training part, display "training" and the "end training" button
        if (isDemo) {
            Button buttonEndDemo = findViewById(R.id.button_end_demo);
            TextView tvDemo = findViewById(R.id.text_view_demo);
            buttonEndDemo.setVisibility(View.VISIBLE);
            tvDemo.setVisibility(View.VISIBLE);

            buttonEndDemo.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    recordEvent("Training ended");
                    // end the training; go to EndDemoActivity
                    endDemo();
                }
            });
        }

        //startTime = System.nanoTime();    // get relative start time in nanoseconds; not used for now

        //finish activity after 1 minute of inactivity
        countDownTimer = new CountDownTimer(60000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                finish();
            }
        }.start();


        String position = "A1P1,A2P2";      // NOTE: dis-enabled random positions
        /*
        int random_position = new Random().nextInt(2);
        Log.d("Random QH", Integer.toString(random_position));
        if (random_position==1){
            Log.d("Random QH", Integer.toString(random_position));
            position = "P1A1,P2A2";
            exchangeA1P1();
            exchangeA2P2();
        }
        */

        timeRecordDb = new TimeDbHelper(this);
        trialInfoDb = new TrialDbHelper(this);

        setupTrial();

        if (isDemo) {
            timeRecordDb.insertData(getCurrentTime(), "startTrainingTrial" + trialCounter + "; Option1: A1=" + a1 + " P1=" + p1 + ", Option2: A2=" + a2 + " P2=" + p2 + "; Orientation: vertical; " + position);

        } else {
            incrementTrialCounter();   // increment the counter to indicate the next trial
            timeRecordDb.insertData(getCurrentTime(), "startTrial" + trialCounter + "; Option1: A1=" + a1 + " P1=" + p1 + ", Option2: A2=" + a2 + " P2=" + p2 + "; Orientation: vertical; " + position);
        }

        //bluetooth = new Bluetooth(timeRecordDb);

        /*
        try {
            // send trial number
            bluetooth.timeStamper(Integer.toString(trialCounter +200),getCurrentTime());
            // send attribute magnitudes
            bluetooth.timeStamper(Integer.toString(16),String.format ("%.0f",a1*100));
            bluetooth.timeStamper(Integer.toString(18),String.format ("%.0f",p1));
            bluetooth.timeStamper(Integer.toString(17),String.format ("%.0f",a2*100));
            bluetooth.timeStamper(Integer.toString(19),String.format ("%.0f",p2));

            bluetooth.timeStamper( "20", Integer.toString(random_position));

        } catch (IOException e) {
            e.printStackTrace();
        }
        */

        viewAnimatorDollar1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                attributeOnClick(viewAnimatorDollar1,
                        new ViewAnimator[] {viewAnimatorDollar2, viewAnimatorProbability1, viewAnimatorProbability2});
            }
        });

        viewAnimatorDollar2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                attributeOnClick(viewAnimatorDollar2,
                        new ViewAnimator[] {viewAnimatorDollar1, viewAnimatorProbability1, viewAnimatorProbability2});

            }
        });

        viewAnimatorProbability1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                attributeOnClick(viewAnimatorProbability1,
                        new ViewAnimator[] {viewAnimatorDollar1, viewAnimatorDollar2, viewAnimatorProbability2});
            }
        });

        viewAnimatorProbability2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                attributeOnClick(viewAnimatorProbability2,
                        new ViewAnimator[] {viewAnimatorDollar1, viewAnimatorDollar2, viewAnimatorProbability1});
            }
        });

        buttonSelect1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View V) {
                /*
                try {
                    // send identifier and timestamp
                    bluetooth.timeStamper( "12", getCurrentTime());
                } catch (IOException e) {}
                */
                showResult(p1, a1,"Option1");
            }
        });

        buttonSelect2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View V) {
                /*
                try {
                    // send identifier and timestamp
                    bluetooth.timeStamper( "13", getCurrentTime());
                } catch (IOException e) {}
                */
                showResult(p2, a2,"Option2");
            }
        });

        //textViewTest.setText(trialCounter+"; "+totalAmountWon);
    }

    // called when each attribute is clicked
    private void attributeOnClick(final ViewAnimator tappedView, ViewAnimator[] otherViews) {
        //for testing purposes
        if (textViewTest.getVisibility() == View.VISIBLE) {
            textViewTest.setVisibility(View.GONE);
        } else {
            textViewTest.setVisibility(View.VISIBLE);
        }

        /* on tap, if the attribute view is covered, uncover it for 1s and cover other attributes */
        if (tappedView.getDisplayedChild() == 0) {
            final String[] codes = identifiers.get(tappedView.getId()); // get the corresponding identifiers for the clicked attribute

            /*
            try {
                // send identifier and timestamp
                bluetooth.timeStamper( codes[0], getCurrentTime());
            } catch (IOException e) {}
            */
            //armVSyncHandlerA1();

            tappedView.showNext();  /* uncover */
            /*
            try {
                bluetooth.timeStamper( codes[1], getCurrentTime());
            } catch (IOException e) {}
            */
            recordEvent(codes[2] + " " + eventClick);

            /* automatically re-cover after 1000ms */
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (tappedView.getDisplayedChild() == 1) {
                        /*
                        try {
                            bluetooth.timeStamper( identifier_cover, getCurrentTime());
                        } catch (IOException e) {}
                        */

                        tappedView.showNext();
                        recordEvent(codes[2] + " " + eventTimeOut);
                    }
                }
            }, 1000);

            /* if other attributes are uncovered, cover them */
            for (ViewAnimator v: otherViews) {
                if (v.getDisplayedChild() == 1) {
                    /*
                    try {
                        bluetooth.timeStamper( identifier_cover, getCurrentTime());
                    } catch (IOException e) {}
                    */
                    v.showNext();
                }
            }
        }

        countDownTimer.cancel();
        countDownTimer.start();
    }

    private void endDemo(){
        demo_prefs.edit().putBoolean(KEY_DO_DEMO, false).apply();    // change shared "prefs" for do_demo to false

        // set trialCounter back to 1
        counter_prefs.edit().putInt(KEY_TRIAL_COUNTER, 1).apply();

        Intent intent = new Intent(QuestionActivity.this, EndDemoActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupTrial() {
        // load trialCounter from shared preference
        counter_prefs = getSharedPreferences("trialCounter", MODE_PRIVATE);
        trialCounter = counter_prefs.getInt(KEY_TRIAL_COUNTER, 1);

        Log.d("Q Test", Integer.toString(trialCounter));

        // get current trial
        currentTrial = trialInfoDb.getTrial(trialCounter);
        getAttributes();
    }

    private void getAttributes(){
        ArrayList<String> attributes = currentTrial.getAttributes();
        a1 = Double.parseDouble(attributes.get(2));
        p1 = Double.parseDouble(attributes.get(3))*100;
        a2 = Double.parseDouble(attributes.get(4));
        p2 = Double.parseDouble(attributes.get(5))*100;
        textViewProbability1.setText((int) p1 + "%");
        textViewProbability2.setText((int) p2 + "%");
        textViewDollar1.setText("$" + String.format("%.2f", a1));
        textViewDollar2.setText("$" + String.format("%.2f", a2));

        if (a1 < 0) {   // if the two dollar amounts are negative, set icons to losing
            ImageView img_dollar1 = findViewById(R.id.image_view_dollar1);
            ImageView img_prob1 = findViewById(R.id.image_view_probability1);
            ImageView img_dollar2 = findViewById(R.id.image_view_dollar2);
            ImageView img_prob2 = findViewById(R.id.image_view_probability2);
            img_dollar1.setImageResource(R.drawable.dollar_lose);
            img_dollar2.setImageResource(R.drawable.dollar_lose);
            img_prob1.setImageResource(R.drawable.probability_lose);
            img_prob2.setImageResource(R.drawable.probability_lose);
        }
    }

    private void incrementTrialCounter() {
        if (trialCounter == trialInfoDb.getNumRows()){  // increment trial counter
            trialCounter = 1;       // wrap around if reaches the end
        } else {
            trialCounter++;
        }

        counter_prefs.edit().putInt(KEY_TRIAL_COUNTER, trialCounter).apply();
    }


    //get current time in milliseconds
    private String getCurrentTime() {
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd:HH:mm:ss:SSS");
        String formattedDate= dateFormat.format(date);
        return formattedDate;
    }

    private void recordEvent(String event) {
        //long timeSpan = System.nanoTime() - startTime;
        //String timeString = String.format("%d", timeSpan / 1000);
        String timeString = getCurrentTime();

        timeRecordDb.insertData(timeString, event);
    }

    private void showResult(double p, double a, String option){
        double random = new Random().nextDouble()*100;
        if (random < p) {
            amountWon = a;
        } else {
            amountWon = 0;
        }

        SharedPreferences prefs = getSharedPreferences("totalAmountWon", MODE_PRIVATE);
        totalAmountWon = prefs.getFloat(KEY_TOTAL_AMOUNT, 0);

        if (!isDemo) {      // only change totalAmountWon if is not in training
            totalAmountWon = totalAmountWon + amountWon;
            prefs.edit().putFloat(KEY_TOTAL_AMOUNT, (float) totalAmountWon).apply();
        }

        recordEvent(option+" selected, $"+amountWon+" won; total amount won: $"+totalAmountWon);

        timeRecordDb.close();

        Intent intent = new Intent(QuestionActivity.this, ResultActivity.class);
        intent.putExtra("EXTRA_AMOUNT_WON", amountWon);
        startActivity(intent);
        finish();
    }

    private void exchangeA1P1(){
        ViewGroup parent = (ViewGroup) viewAnimatorDollar1.getParent();
        int indexDollar1 = parent.indexOfChild(viewAnimatorDollar1);
        int indexProbability1 = parent.indexOfChild(viewAnimatorProbability1);
        parent.removeView(viewAnimatorDollar1);
        parent.addView(viewAnimatorDollar1, indexProbability1);
        parent.removeView(viewAnimatorProbability1);
        parent.addView(viewAnimatorProbability1, indexDollar1);
    }

    private void exchangeA2P2(){
        ViewGroup parent = (ViewGroup) viewAnimatorDollar2.getParent();
        int indexDollar2 = parent.indexOfChild(viewAnimatorDollar2);
        int indexProbability2 = parent.indexOfChild(viewAnimatorProbability2);
        parent.removeView(viewAnimatorDollar2);
        parent.addView(viewAnimatorDollar2,indexProbability2);
        parent.removeView(viewAnimatorProbability2);
        parent.addView(viewAnimatorProbability2,indexDollar2);
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            finish();
        } else {
            Toast.makeText(this, "Press back again to finish", Toast.LENGTH_SHORT).show();
        }

        backPressedTime = System.currentTimeMillis();
    }
}