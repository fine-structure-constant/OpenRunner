package cn.edu.pku.pkurunner.Utils;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import cn.edu.pku.pkurunner.R;

/** Checks and requests only the location permission needed to record a run. */
public class RunPermissionUtil {

    public static boolean check(final FragmentActivity activity) {
        if (android.os.Build.VERSION.SDK_INT < 23
                || ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.p_run_permission_title)
                .setMessage(R.string.p_run_permission_content)
                .setNegativeButton(R.string.p_run_permission_cancel, null)
                .setPositiveButton(R.string.p_run_permission_action, (dialog, which) ->
                        ActivityCompat.requestPermissions(activity, new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        }, 1001))
                .setCancelable(true)
                .show();
        return false;
    }
}
