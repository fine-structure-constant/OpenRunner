package cn.edu.pku.pkurunner.Permission;

import android.app.Dialog;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import cn.edu.pku.pkurunner.R;
import java.util.Map;

/** Shows the single runtime permission needed to record a run. */
public class PermissionDialog extends DialogFragment {

    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    public void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        this.permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                (ActivityResultCallback<Map<String, Boolean>>) result -> dismissAllowingStateLoss());
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(android.os.Bundle bundle) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View rootView = requireActivity().getLayoutInflater().inflate(R.layout.fragment_permission_dialog, (ViewGroup) null);
        builder.setView(rootView);

        boolean locationGranted = Build.VERSION.SDK_INT < 23
                || requireActivity().checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") == 0
                || requireActivity().checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") == 0;
        TextView location = rootView.findViewById(R.id.f_perm_location);
        location.setText(getString(R.string.f_permission_location,
                getString(locationGranted ? R.string.f_permission_status_positive : R.string.f_permission_status_negative)));

        if (!locationGranted) {
            builder.setNegativeButton(R.string.f_permission_not_auth, null);
            builder.setPositiveButton(R.string.f_permission_auth_now, (dialog, which) -> permissionLauncher.launch(new String[]{
                    "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.ACCESS_COARSE_LOCATION"
            }));
        } else {
            builder.setPositiveButton(R.string.f_permission_btn_ok, null);
        }
        return builder.create();
    }
}
