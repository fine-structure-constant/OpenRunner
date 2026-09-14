package cn.edu.pku.pkurunner.View;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import cn.edu.pku.pkurunner.R;

/**
 * Material 3 replacement for the legacy {@link android.app.ProgressDialog}.
 *
 * <p>Renders a rounded surface holding an indeterminate {@link CircularProgressIndicator} plus a
 * single-line message. The legacy {@code setProgressStyle} / {@code setIndeterminate} /
 * {@code setMax} / {@code setProgress} methods are kept as no-op-ish shims so existing call sites
 * keep compiling while still switching to determinate mode when a max is supplied.
 */
public class OrLoadingDialog extends Dialog {

    @Nullable
    private final CircularProgressIndicator indicator;

    @Nullable
    private final android.widget.TextView messageView;

    public OrLoadingDialog(@NonNull Context context) {
        super(context, R.style.Theme_OpenRunner_LoadingDialog);
        // Inflate against the *dialog's* themed context, not the host activity's. Dialog wraps the
        // context it was given in a ContextThemeWrapper holding our Material 3 dialog theme; using
        // the raw activity context would blow up on any screen whose theme is not a Material one
        // (MaterialCardView enforces Theme.MaterialComponents via ThemeEnforcement).
        View content = LayoutInflater.from(getContext()).inflate(R.layout.dialog_loading, null, false);
        setContentView(content);
        this.indicator = content.findViewById(R.id.d_loading_indicator);
        this.messageView = content.findViewById(R.id.d_loading_txt_message);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    /** Replaces {@code ProgressDialog.setMessage}. */
    public void setMessage(@Nullable CharSequence message) {
        if (this.messageView != null) {
            this.messageView.setText(message == null ? "" : message);
        }
    }

    /** Kept for call-site compatibility. M3 always uses a circular indicator. */
    @SuppressWarnings("unused")
    public void setProgressStyle(int style) {
        // no-op
    }

    /** Kept for call-site compatibility; determinate mode is driven by {@link #setMax(int)}. */
    @SuppressWarnings("unused")
    public void setIndeterminate(boolean indeterminate) {
        if (indeterminate && this.indicator != null) {
            this.indicator.setIndeterminate(true);
        }
    }

    /** Switches the indicator to determinate mode when a positive max is supplied. */
    public void setMax(int max) {
        if (this.indicator != null && max > 0) {
            this.indicator.setIndeterminate(false);
            this.indicator.setMax(max);
        }
    }

    /** Kept for call-site compatibility. */
    @SuppressWarnings("unused")
    public void setProgress(int value) {
        if (this.indicator != null && !this.indicator.isIndeterminate()) {
            this.indicator.setProgressCompat(value, true);
        }
    }
}
