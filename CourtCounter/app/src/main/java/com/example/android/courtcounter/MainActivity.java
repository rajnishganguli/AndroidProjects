package com.example.android.courtcounter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    final int POINTS_FOR_FREE_THROW = 1;
    int scoreTeamA = 0;
    int scoreTeamB = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        displayForTeamA(scoreTeamA);
        displayForTeamB(scoreTeamB);
    }

    public void free_throw_for_teamA(View view){
        scoreTeamA += POINTS_FOR_FREE_THROW;
        displayForTeamA(scoreTeamA);
    }

    public void add_2_for_teamA(View view){
        scoreTeamA += 2;
        displayForTeamA(scoreTeamA);
    }

    public void add_3_for_teamA(View view){
        scoreTeamA += 3;
        displayForTeamA(scoreTeamA);
    }

    // Displays the given score for Team A.
    public void displayForTeamA(int score) {
        TextView scoreView = (TextView) findViewById(R.id.team_a_score);
        scoreView.setText(String.valueOf(score));
    }

    public void free_throw_for_teamB(View view){
        scoreTeamB += POINTS_FOR_FREE_THROW;
        displayForTeamB(scoreTeamB);
    }

    public void add_2_for_teamB(View view){
        scoreTeamB += 2;
        displayForTeamB(scoreTeamB);
    }

    public void add_3_for_teamB(View view){
        scoreTeamB += 3;
        displayForTeamB(scoreTeamB);
    }

    // Displays the given score for Team A.
    public void displayForTeamB(int score) {
        TextView scoreView = (TextView) findViewById(R.id.team_b_score);
        scoreView.setText(String.valueOf(score));
    }

    public void resetScores(View view){
        scoreTeamA = 0;
        scoreTeamB = 0;
        displayForTeamA(scoreTeamA);
        displayForTeamB(scoreTeamB);
    }
}
