package cn.edu.pku.pkurunner.Permission;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import cn.edu.pku.pkurunner.Permission.PermissionDialog;
import cn.edu.pku.pkurunner.R;
import java.util.ArrayList;
import java.util.Map;

public class PermissionDialog extends DialogFragment {

    private View rootView;

    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> map) {
                dismissAllowingStateLoss();
            }
        });
    }

    public static /* synthetic */ void s(DialogInterface dialogInterface, int index) {
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle bundle) {
        int checkSelfPermission;
        int checkSelfPermission2;
        int checkSelfPermission3;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        View inflate = getActivity().getLayoutInflater().inflate(R.layout.fragment_permission_dialog, (ViewGroup) null);
        this.rootView = inflate;
        builder.setView(inflate);
        TextView textView = (TextView) this.rootView.findViewById(R.id.f_perm_phone);
        TextView textView2 = (TextView) this.rootView.findViewById(R.id.f_perm_sd);
        TextView textView3 = (TextView) this.rootView.findViewById(R.id.f_perm_location);
        if (Build.VERSION.SDK_INT >= 23) {
            checkSelfPermission = getActivity().checkSelfPermission("android.permission.READ_PHONE_STATE");
            Boolean valueOf = Boolean.valueOf(checkSelfPermission == 0);
            checkSelfPermission2 = getActivity().checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE");
            Boolean valueOf2 = Boolean.valueOf(checkSelfPermission2 == 0);
            checkSelfPermission3 = getActivity().checkSelfPermission("android.permission.ACCESS_FINE_LOCATION");
            Boolean valueOf3 = Boolean.valueOf(checkSelfPermission3 == 0);
            t(textView, R.string.f_permission_phone, valueOf);
            t(textView2, R.string.f_permission_sd, valueOf2);
            t(textView3, R.string.f_permission_location, valueOf3);
            final ArrayList arrayList = new ArrayList();
            if (!valueOf.booleanValue()) {
                arrayList.add("android.permission.READ_PHONE_STATE");
            }
            if (!valueOf2.booleanValue()) {
                arrayList.add("android.permission.WRITE_EXTERNAL_STORAGE");
            }
            if (!valueOf3.booleanValue()) {
                arrayList.add("android.permission.ACCESS_FINE_LOCATION");
            }
            if (arrayList.size() != 0) {
                builder.setNegativeButton(R.string.f_permission_not_auth, new DialogInterface.OnClickListener() {
                    @Override
                    public final void onClick(DialogInterface dialogInterface, int index) {
                        PermissionDialog.this.q(dialogInterface, index);
                    }
                });
                builder.setPositiveButton(R.string.f_permission_auth_now, new DialogInterface.OnClickListener() {
                    @Override
                    public final void onClick(DialogInterface dialogInterface, int index) {
                        PermissionDialog.this.r(arrayList, dialogInterface, index);
                    }
                });
                return builder.create();
            }
        } else {
            textView.setVisibility(8);
            textView2.setVisibility(8);
            textView3.setVisibility(8);
        }
        builder.setPositiveButton(R.string.f_permission_btn_ok, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int index) {
                PermissionDialog.s(dialogInterface, index);
            }
        });
        return builder.create();
    }

    public /* synthetic */ void q(DialogInterface dialogInterface, int index) {
        dismiss();
    }

    public /* synthetic */ void r(ArrayList arrayList, DialogInterface dialogInterface, int index) {
        this.permissionLauncher.launch((String[]) arrayList.toArray(new String[arrayList.size()]));
    }

    private void t(TextView textView, int index, Boolean bool) {
        int index2;
        if (bool.booleanValue()) {
            index2 = R.string.f_permission_status_positive;
        } else {
            index2 = R.string.f_permission_status_negative;
        }
        textView.setText(Html.fromHtml(getString(index, getString(index2))));
    }
}
