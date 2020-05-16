package com.jhu.chenyuzhang.experimentgame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.jhu.chenyuzhang.experimentgame.Questions.Question2Att4OpActivity;
import com.jhu.chenyuzhang.experimentgame.Questions.Question2Att4OpHorizontal;
import com.jhu.chenyuzhang.experimentgame.Questions.Question4Activity;
import com.jhu.chenyuzhang.experimentgame.Questions.Question4ActivityHorizontal;
import com.jhu.chenyuzhang.experimentgame.Questions.Question4Att2OpActivity;
import com.jhu.chenyuzhang.experimentgame.Questions.Question4Att2OpHorizontal;
import com.jhu.chenyuzhang.experimentgame.Questions.QuestionActivity;
import com.jhu.chenyuzhang.experimentgame.Questions.QuestionActivityHorizontal;

import java.util.Random;

public class ResultActivity extends AppCompatActivity {
    private double amountWon;
    private ImageView imageViewCongrats;
    private TextView textViewSorry;
    private TextView textViewAmount;
    private Button buttonNextTrial;

    private boolean isDemo;
    private static final String KEY_DO_DEMO = "keyDoDemo";

    TrialDbHelper trialInfoDb;

    public static int trialCounter;
    public static final String KEY_TRIAL_COUNTER = "keyTrialCounter";

    public static final String KEY_TOTAL_AMOUNT = "keyTotalAmount";
    private TextView tvTotal;
    private double totalAmountWon;

    TimeDbHelper timeRecordDb;
    Bluetooth bluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        amountWon = getIntent().getDoubleExtra("EXTRA_AMOUNT_WON", 0);

        imageViewCongrats = findViewById(R.id.image_view_congrats);
        textViewSorry = findViewById(R.id.text_view_sorry);
        textViewAmount = findViewById(R.id.text_view_result_amount);
        buttonNextTrial = findViewById(R.id.button_next_trial);

        tvTotal = findViewById(R.id.text_view_total);
        SharedPreferences prefs = getSharedPreferences("totalAmountWon", MODE_PRIVATE);
        totalAmountWon = prefs.getFloat(KEY_TOTAL_AMOUNT, 0);

        timeRecordDb = new TimeDbHelper(this);

        SharedPreferences demo_prefs = getSharedPreferences("doDemo", MODE_PRIVATE);
        isDemo = demo_prefs.getBoolean(KEY_DO_DEMO, true);   // get whether to initiate a training trial

        trialInfoDb = new TrialDbHelper(this);

        //bluetooth = new Bluetooth(timeRecordDb);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                /*
                try {
                    // send identifier and timestamp
                    bluetooth.timeStamper( "14", MainActivity.getCurrentTime());
                    bluetooth.sendData(String.format ("%.2f",amountWon));
                } catch (IOException e) {}
                */
                buttonNextTrial.setVisibility(View.VISIBLE);
                if (amountWon <= 0) {
                    imageViewCongrats.setVisibility(View.GONE);
                    textViewSorry.setVisibility(View.VISIBLE);
                    if (amountWon == 0) {
                        textViewAmount.setText("You didn't win / lose any money.");
                    } else {
                        textViewAmount.setText("You lost $" + String.format("%.2f",-amountWon) + ".");
                    }
                } else {
                    imageViewCongrats.setVisibility(View.VISIBLE);
                    textViewSorry.setVisibility(View.GONE);
                    textViewAmount.setText("You won $" + String.format("%.2f",amountWon) + "!");
                }
                tvTotal.setText("Total Amount Won: $" + String.format("%.2f", totalAmountWon));

            }
        }, 1000);

        buttonNextTrial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getNextIntent();
                startActivity(intent);
                finish();
            }
        });
    }

    private Trial getNextTrial() {
        SharedPreferences counter_prefs = getSharedPreferences("trialCounter", MODE_PRIVATE);
        trialCounter = counter_prefs.getInt(KEY_TRIAL_COUNTER, 1);

        if (isDemo) {   // if is in training, randomly choose a trial; otherwise, pick the next one
            //int trial_num = new Random().nextInt((int)trialInfoDb.getNumRows()); // random integer in [0, table_size)
            int trial_num = new Random().nextInt(160);  // get one of the first 160 trials
            trial_num++;    // need to be in [1, size]

            counter_prefs.edit().putInt(KEY_TRIAL_COUNTER, trial_num).apply();  // set shared trialCounter to trial_num

            return trialInfoDb.getTrial(trial_num);
        }

        return trialInfoDb.getTrial(trialCounter);
    }

    private Intent getNextIntent() {
        // Random int decides the orientation. 0: vertical, 1: horizontal
        int random = new Random().nextInt(2);
        Intent intent;

        Trial currentTrial = getNextTrial();
        if (currentTrial.getType().equals("1")) {   // 2Opt2Attr
            if (random == 0) {
                intent = new Intent(ResultActivity.this, QuestionActivity.class);
            } else {
                intent = new Intent(ResultActivity.this, QuestionActivityHorizontal.class);
            }
        } else if (currentTrial.getType().equals("2")) {    // 2Opt4Attr
            if (random == 0) {
                intent = new Intent(ResultActivity.this, Question4Att2OpActivity.class);
            } else {
                intent = new Intent(ResultActivity.this, Question4Att2OpHorizontal.class);
            }
        } else if (currentTrial.getType().equals("3")) {    // 4Opt2Attr
            if (random == 0) {
                intent = new Intent(ResultActivity.this, Question2Att4OpActivity.class);
            } else {
                intent = new Intent(ResultActivity.this, Question2Att4OpHorizontal.class);
            }
        } else {   // 4Opt4Attr
            if (random == 0) {
                intent = new Intent(ResultActivity.this, Question4Activity.class);
            } else {
                intent = new Intent(ResultActivity.this, Question4ActivityHorizontal.class);
            }
        }
        return intent;
    }
}
