package cn.edu.pku.pkurunner.Permission;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import cn.edu.pku.pkurunner.Permission.PermissionDialog;
import cn.edu.pku.pkurunner.R;
import java.util.ArrayList;

public class PermissionDialog extends DialogFragment {

    /* renamed from: t, reason: collision with root package name */
    private View f6990t;

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void s(DialogInterface dialogInterface, int i2) {
    }

    @Override // androidx.fragment.app.DialogFragment
    @NonNull
    public Dialog onCreateDialog(Bundle bundle) {
        int checkSelfPermission;
        int checkSelfPermission2;
        int checkSelfPermission3;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View inflate = getActivity().getLayoutInflater().inflate(R.layout.fragment_permission_dialog, (ViewGroup) null);
        this.f6990t = inflate;
        builder.setView(inflate);
        TextView textView = (TextView) this.f6990t.findViewById(R.id.f_perm_phone);
        TextView textView2 = (TextView) this.f6990t.findViewById(R.id.f_perm_sd);
        TextView textView3 = (TextView) this.f6990t.findViewById(R.id.f_perm_location);
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
                builder.setNegativeButton(R.string.f_permission_not_auth, new DialogInterface.OnClickListener() { // from class: u.b
                    @Override // android.content.DialogInterface.OnClickListener
                    public final void onClick(DialogInterface dialogInterface, int i2) {
                        PermissionDialog.this.q(dialogInterface, i2);
                    }
                });
                builder.setPositiveButton(R.string.f_permission_auth_now, new DialogInterface.OnClickListener() { // from class: u.c
                    @Override // android.content.DialogInterface.OnClickListener
                    public final void onClick(DialogInterface dialogInterface, int i2) {
                        PermissionDialog.this.r(arrayList, dialogInterface, i2);
                    }
                });
                return builder.create();
            }
        } else {
            textView.setVisibility(8);
            textView2.setVisibility(8);
            textView3.setVisibility(8);
        }
        builder.setPositiveButton(R.string.f_permission_btn_ok, new DialogInterface.OnClickListener() { // from class: u.d
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                PermissionDialog.s(dialogInterface, i2);
            }
        });
        return builder.create();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void q(DialogInterface dialogInterface, int i2) {
        dismiss();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void r(ArrayList arrayList, DialogInterface dialogInterface, int i2) {
        requestPermissions((String[]) arrayList.toArray(new String[arrayList.size()]), 101);
    }

    private void t(TextView textView, int i2, Boolean bool) {
        int i3;
        if (bool.booleanValue()) {
            i3 = R.string.f_permission_status_positive;
        } else {
            i3 = R.string.f_permission_status_negative;
        }
        textView.setText(Html.fromHtml(getString(i2, getString(i3))));
    }
}
