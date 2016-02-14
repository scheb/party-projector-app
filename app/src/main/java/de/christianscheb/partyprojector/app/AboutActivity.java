package de.christianscheb.partyprojector.app;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

public class AboutActivity extends AppCompatActivity {

    int clickNum = 0;
    boolean animationRunning = false;
    private MediaPlayer mp;
    private TranslateAnimation animation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
    }

    @Override
    protected void onStop() {
        stopAggi();
        super.onStop();
    }

    protected void stopAggi() {
        animationRunning = false;
        try {
            if (mp != null) {
                mp.release();
            }
        } catch (IllegalStateException ignored) {
        }
        if (animation != null) {
            animation.cancel();
        }
    }

    public void animateAggi() {
        animationRunning = true;
        ++clickNum;
        int sound = R.raw.aggi_start;
        if (clickNum >= 5) {
            sound = R.raw.aggi;
            clickNum = 0;
        }

        Log.d(getLocalClassName(), "animate");
        final ImageView image = (ImageView) findViewById(R.id.imageView);

        animation = new TranslateAnimation(-2, 2, 0, 0);
        animation.setDuration(20);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);

        mp = MediaPlayer.create(this, sound);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                animationRunning = false;
                animation.cancel();
                mp.release();
            }
        });
        mp.start();
        image.startAnimation(animation);
    }

    public void onImageClick(final View view) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (animationRunning) {
                    stopAggi();
                } else {
                    animateAggi();
                }
            }
        });
    }
}


