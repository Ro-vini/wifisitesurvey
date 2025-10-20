package com.example.wifisitesurvey.utils;

import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EdgeToEdgeUtils {

    public static void setupEdgeToEdge(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int tappableBottomInset = insets.getInsets(WindowInsetsCompat.Type.tappableElement()).bottom;

            if (tappableBottomInset > 0) {
                Log.d("NavigationMode", "Button navigation detected. Applying padding.");

                int paddingBottomPx = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 50, v.getResources().getDisplayMetrics());

                v.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        paddingBottomPx
                );
            } else {
                Log.d("NavigationMode", "Gesture navigation detected. No extra padding applied.");
            }

            return insets;
        });
    }
}
