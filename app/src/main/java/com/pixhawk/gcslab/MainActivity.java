package com.pixhawk.gcslab;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView status;
    private Button toggle;
    private boolean running = false;

    private final Runnable poller = new Runnable() {
        @Override public void run() {
            if (!running) return;
            String stats = NativeBridge.getStats();
            String batch = NativeBridge.getLatestBatch(5);
            status.setText("Stats:\n" + stats + "\nLatest:\n" + batch);
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        status = new TextView(this);
        toggle = new Button(this);
        toggle.setText(getString(R.string.start));
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!running) {
                    NativeBridge.startTelemetry();
                    running = true;
                    toggle.setText(getString(R.string.stop));
                    handler.post(poller);
                } else {
                    NativeBridge.stopTelemetry();
                    running = false;
                    toggle.setText(getString(R.string.start));
                    status.append("\nStopped.");
                }
            }
        });
        // Simple vertical layout substitute
        androidx.appcompat.widget.LinearLayoutCompat layout = new androidx.appcompat.widget.LinearLayoutCompat(this);
        layout.setOrientation(androidx.appcompat.widget.LinearLayoutCompat.VERTICAL);
        layout.addView(toggle);
        layout.addView(status);
        setContentView(layout);
    }
}
