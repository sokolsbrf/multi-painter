package ru.dimasokol.learning.multipainter;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String SELECTED_COLOR = "selectedColor";

    private View[] mColorChoosers = new View[3];
    private MultiPaintingView mPaintingView;
    private View mModeView;

    private View.OnClickListener mColorClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            for (View colorChooser : mColorChoosers) {
                colorChooser.setSelected(v == colorChooser);
            }

            if (mModeView.isSelected()) {
                mModeView.callOnClick();
            }

            switch (v.getId()) {
                case R.id.color_blue:
                    mPaintingView.setCurrentColor(getResources().getColor(R.color.blue));
                    break;
                case R.id.color_red:
                    mPaintingView.setCurrentColor(getResources().getColor(R.color.red));
                    break;
                default:
                    mPaintingView.setCurrentColor(getResources().getColor(R.color.green));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mColorChoosers[0] = findViewById(R.id.color_red);
        mColorChoosers[1] = findViewById(R.id.color_green);
        mColorChoosers[2] = findViewById(R.id.color_blue);

        mPaintingView = findViewById(R.id.painting_view);

        mModeView = findViewById(R.id.tool);

        mModeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPaintingView.setPaintingMode(v.isSelected());
                v.setSelected(!v.isSelected());
            }
        });

        for (View colorChooser : mColorChoosers) {
            colorChooser.setOnClickListener(mColorClickListener);
        }

        if (savedInstanceState == null) {
            mColorChoosers[0].callOnClick();
        } else {
            int color = savedInstanceState.getInt(SELECTED_COLOR, 0);
            mColorChoosers[color].callOnClick();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int color = 0;
        for (int i = 0; i < mColorChoosers.length; i++) {
            if (mColorChoosers[i].isSelected()) {
                color = i;
                break;
            }
        }

        outState.putInt(SELECTED_COLOR, color);
    }
}
