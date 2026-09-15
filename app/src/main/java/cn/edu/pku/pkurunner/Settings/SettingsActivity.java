package cn.edu.pku.pkurunner.Settings;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.app.AlertDialog;
import cn.edu.pku.pkurunner.View.OrLoadingDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import cn.edu.pku.pkurunner.BuildConfig;
import cn.edu.pku.pkurunner.StackTracePrintingConsumer;
import cn.edu.pku.pkurunner.Data;
import cn.edu.pku.pkurunner.Map.SpeedHelper;
import cn.edu.pku.pkurunner.Photo.PhotoFile;
import cn.edu.pku.pkurunner.Photo.UselessPhotoCleaner;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.Storage.Dropbox;
import cn.edu.pku.pkurunner.Storage.OperationCancelException;
import cn.edu.pku.pkurunner.Storage.StorageUtil;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.users.FullAccount;
import com.dropbox.core.v2.users.SpaceUsage;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import org.xutils.common.util.LogUtil;

public class SettingsActivity extends AppCompatActivity {

    private OrLoadingDialog progressDialog;

    private ActivityResultLauncher<Intent> exportDatabaseLauncher;

    private int uploadRetryCount = 0;

    private SettingsFragment settingsFragment;

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle bundle, String str) {
            setPreferencesFromResource(R.xml.app_settings, str);
        }
    }

    private Preference findPreference(CharSequence charSequence) {
        SettingsFragment settingsFragment = this.settingsFragment;
        if (settingsFragment == null) {
            return null;
        }
        return settingsFragment.findPreference(charSequence);
    }

    public /* synthetic */ void U(Preference preference, Pair pair) {
        preference.setSummary(getString(R.string.p_setting_storage_user, ((FullAccount) pair.first).getName().getDisplayName(), StorageUtil.sizeToReadableString(((SpaceUsage) pair.second).getUsed()), StorageUtil.sizeToReadableString(((SpaceUsage) pair.second).getAllocation().getIndividualValue().getAllocated())));
    }

    private void A() {
        Preference findPreference = findPreference("pref_dropbox");
        findPreference.setSummary("Not connected");
        findPreference.setEnabled(true);
        findPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public final boolean onPreferenceClick(Preference preference) {
                boolean V;
                V = SettingsActivity.this.V(preference);
                return V;
            }
        });
        findPreference("pref_dropbox_user").setEnabled(false);
        findPreference("pref_dropbox_upload").setEnabled(false);
    }

    private void B() {
        findPreference("pref_version").setSummary(BuildConfig.VERSION_NAME);
        Preference findPreference = findPreference("pref_version");
        final String[] stringArray = getResources().getStringArray(R.array.p_about_easteregg);
        findPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public final boolean onPreferenceClick(Preference preference) {
                boolean X;
                X = SettingsActivity.this.X(stringArray, preference);
                return X;
            }
        });
    }

    private void C() {
        ListPreference listPreference = (ListPreference) findPreference("pref_gender");
        listPreference.setSummary(D(Data.getUser().getGender()));
        listPreference.setDefaultValue(E(Data.getUser().getGender()));
        listPreference.setEnabled(Data.getUser().isOffline().booleanValue());
        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public final boolean onPreferenceChange(Preference preference, Object obj) {
                boolean Z;
                Z = SettingsActivity.this.Z(preference, obj);
                return Z;
            }
        });
    }

    public /* synthetic */ void I(Throwable th) {
        if (th instanceof Dropbox.APIWrapper.DropboxException) {
            A();
        } else {
            th.printStackTrace();
        }
    }

    public /* synthetic */ void J(Preference preference, Throwable th) {
        if (th instanceof NetworkIOException) {
            preference.setSummary(R.string.p_setting_storage_sockettimeout);
            return;
        }
        Toast.makeText(this, "Error in " + th, 0).show();
    }

    public static /* synthetic */ void K(ObservableEmitter observableEmitter, DialogInterface dialogInterface, int index) {
        observableEmitter.onError(new OperationCancelException());
    }

    public static /* synthetic */ void L(ObservableEmitter observableEmitter, DialogInterface dialogInterface, int index) {
        observableEmitter.onNext(Boolean.TRUE);
    }

    public /* synthetic */ void M(final ObservableEmitter observableEmitter) {
        new MaterialAlertDialogBuilder(this).setTitle("Really upload database (will overwrite remote file)?").setCancelable(false).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int index) {
                SettingsActivity.K(observableEmitter, dialogInterface, index);
            }
        }).setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int index) {
                SettingsActivity.L(observableEmitter, dialogInterface, index);
            }
        }).show();
    }

    public /* synthetic */ ObservableSource N(Boolean bool) {
        OrLoadingDialog progressDialog = new OrLoadingDialog(this);
        this.progressDialog = progressDialog;
        progressDialog.setProgressStyle(0);
        this.progressDialog.setMessage("Uploading...");
        this.progressDialog.setIndeterminate(false);
        this.progressDialog.setCancelable(false);
        this.progressDialog.show();
        return Observable.just(Boolean.TRUE);
    }

    public /* synthetic */ void P(FileMetadata fileMetadata) {
        this.progressDialog.dismiss();
        this.progressDialog = null;
        Toast.makeText(this, "Successfully updated (" + StorageUtil.sizeToReadableString(fileMetadata.getSize()) + ")!", 0).show();
    }

    public /* synthetic */ void Q(Throwable th) {
        if (th instanceof OperationCancelException) {
            LogUtil.d("Operation dismissed.");
            return;
        }
        Toast.makeText(this, "Something went wrong " + th, 0).show();
        th.printStackTrace();
    }

    public /* synthetic */ boolean R(final DbxClientV2 dbxClientV2, Preference preference) {
        Observable.create(new ObservableOnSubscribe() {
            @Override
            public final void subscribe(ObservableEmitter observableEmitter) {
                SettingsActivity.this.M(observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource N;
                N = SettingsActivity.this.N((Boolean) obj);
                return N;
            }
        }).observeOn(Schedulers.io()).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource O;
                O = SettingsActivity.this.O(dbxClientV2, (Boolean) obj);
                return O;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
            @Override
            public final void accept(Object obj) {
                SettingsActivity.this.P((FileMetadata) obj);
            }
        }, new Consumer() {
            @Override
            public final void accept(Object obj) {
                SettingsActivity.this.Q((Throwable) obj);
            }
        });
        return true;
    }

    public /* synthetic */ boolean V(Preference preference) {
        Auth.startOAuth2Authentication(this, BuildConfig.DROPBOX_KEY);
        return true;
    }

    public /* synthetic */ boolean X(String[] strArr, Preference preference) {
        int index = this.uploadRetryCount;
        if (index < strArr.length) {
            Toast.makeText(this, strArr[index], 0).show();
            this.uploadRetryCount++;
        }
        return true;
    }

    public /* synthetic */ void Y(Preference preference, Boolean bool) {
        Toast.makeText(this, "Successfully changed gender", 0).show();
        preference.setSummary(D(Data.getUser().getGender()));
        preference.setDefaultValue(E(Data.getUser().getGender()));
    }

    public /* synthetic */ boolean Z(final Preference preference, Object obj) {
        Data.getUser().setGender(((String) obj).equals(getResources().getStringArray(R.array.p_setting_gender_value)[0]) ? 1 : 0);
        Data.saveUserToDatabase().observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
            @Override
            public final void accept(Object obj2) {
                SettingsActivity.this.Y(preference, (Boolean) obj2);
            }
        }, new StackTracePrintingConsumer());
        return true;
    }

    public /* synthetic */ void a0(Preference preference, String str, Boolean bool) {
        Toast.makeText(this, "Successfully changed name", 0).show();
        preference.setSummary(str);
        preference.setDefaultValue(str);
    }

    public /* synthetic */ boolean b0(final Preference preference, Object obj) {
        final String str = (String) obj;
        Data.getUser().setName(str);
        Data.saveUserToDatabase().observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
            @Override
            public final void accept(Object obj2) {
                SettingsActivity.this.a0(preference, str, (Boolean) obj2);
            }
        }, new StackTracePrintingConsumer());
        return true;
    }

    public /* synthetic */ boolean d0(Preference preference) {
        UselessPhotoCleaner.cleanAllUnused(getExternalFilesDir(PhotoFile.PicutreType));
        Toast.makeText(this, R.string.p_setting_photo_deleted, 0).show();
        return true;
    }

    public /* synthetic */ boolean e0(ListPreference listPreference, Preference preference, Object obj) {
        Data.setSpeedUnitPreference(SpeedHelper.SPEED_UNIT.values()[Integer.valueOf((String) obj).intValue()]);
        listPreference.setSummary(F(Data.getSpeedUnitPreference()));
        return true;
    }

    private void f0() {
        EditTextPreference editTextPreference = (EditTextPreference) findPreference("pref_name");
        editTextPreference.setSummary(Data.getUser().getName());
        editTextPreference.setDefaultValue(Data.getUser().getName());
        editTextPreference.setEnabled(Data.getUser().isOffline().booleanValue());
        editTextPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public final boolean onPreferenceChange(Preference preference, Object obj) {
                boolean b02;
                b02 = SettingsActivity.this.b0(preference, obj);
                return b02;
            }
        });
    }

    private void g0() {
        findPreference("pref_clean_photo").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public final boolean onPreferenceClick(Preference preference) {
                boolean d02;
                d02 = SettingsActivity.this.d0(preference);
                return d02;
            }
        });
    }

    private void i0() {
        final ListPreference listPreference = (ListPreference) findPreference("pref_unit");
        listPreference.setSummary(F(Data.getSpeedUnitPreference()));
        listPreference.setDefaultValue(G(Data.getSpeedUnitPreference()));
        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public final boolean onPreferenceChange(Preference preference, Object obj) {
                boolean e02;
                e02 = SettingsActivity.this.e0(listPreference, preference, obj);
                return e02;
            }
        });
    }

    private void k0() {
        final ListPreference preference = (ListPreference) findPreference("pref_theme");
        String current = getSharedPreferences("appearance", MODE_PRIVATE).getString("theme", "system");
        preference.setValue(current);
        preference.setSummary(preference.getEntry());
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference ignored, Object value) {
                String theme = (String) value;
                getSharedPreferences("appearance", MODE_PRIVATE).edit().putString("theme", theme).apply();
                preference.setSummary(preference.getEntries()[preference.findIndexOfValue(theme)]);
                AppCompatDelegate.setDefaultNightMode("dark".equals(theme)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : "light".equals(theme) ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                recreate();
                return true;
            }
        });
    }

    private void y() {
        Preference findPreference = findPreference("pref_dropbox");
        if (!Data.getUser().isOffline().booleanValue()) {
            findPreference.setEnabled(false);
        } else {
            findPreference.setEnabled(true);
            Dropbox.APIWrapper.getToken(this).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    SettingsActivity.this.H((String) obj);
                }
            }, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    SettingsActivity.this.I((Throwable) obj);
                }
            });
        }
    }

    private void z() {
        Preference findPreference = findPreference("pref_dropbox_connection");
        findPreference.setSummary("Connected");
        findPreference.setEnabled(false);
        final DbxClientV2 client = Dropbox.ClientFactory.getClient();
        final Preference findPreference2 = findPreference("pref_dropbox_user");
        StorageUtil.NetworkMethodWrapper(new StorageUtil.Producer() {
            @Override
            public final Object produce() {
                FullAccount S;
                    S = SettingsActivity.S(client);
                return S;
            }
        }).flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                ObservableSource T;
                T = SettingsActivity.T(client, (FullAccount) obj);
                return T;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
            @Override
            public final void accept(Object obj) {
                SettingsActivity.this.U(findPreference2, (Pair) obj);
            }
        }, new Consumer() {
            @Override
            public final void accept(Object obj) {
                SettingsActivity.this.J(findPreference2, (Throwable) obj);
            }
        });
        findPreference("pref_dropbox_upload").setEnabled(true);
        findPreference("pref_dropbox_upload").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public final boolean onPreferenceClick(Preference preference) {
                boolean R;
                R = SettingsActivity.this.R(client, preference);
                return R;
            }
        });
    }

    private String D(int index) {
        String[] stringArray = getResources().getStringArray(R.array.p_setting_gender_display);
        char character = 1;
        if (index == 1) {
            character = 0;
        }
        return stringArray[character];
    }

    private String E(int index) {
        String[] stringArray = getResources().getStringArray(R.array.p_setting_gender_value);
        char character = 1;
        if (index == 1) {
            character = 0;
        }
        return stringArray[character];
    }

    private String F(SpeedHelper.SPEED_UNIT speed_unit) {
        return getResources().getStringArray(R.array.p_setting_unit_display)[speed_unit.ordinal()];
    }

    private String G(SpeedHelper.SPEED_UNIT speed_unit) {
        return getResources().getStringArray(R.array.p_setting_unit_value)[speed_unit.ordinal()];
    }

    public /* synthetic */ void H(String str) {
        z();
    }

    public /* synthetic */ ObservableSource O(DbxClientV2 dbxClientV2, Boolean bool) {
        try {
            return Observable.just(Data.uploadDatabaseToDropbox(this, dbxClientV2));
        } catch (Exception e) {
            return Observable.error(e);
        }
    }

    public static /* synthetic */ FullAccount S(DbxClientV2 dbxClientV2) {
        try {
            return dbxClientV2.users().getCurrentAccount();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static /* synthetic */ ObservableSource T(DbxClientV2 dbxClientV2, FullAccount fullAccount) {
        try {
            return Observable.just(new Pair(fullAccount, dbxClientV2.users().getSpaceUsage()));
        } catch (Exception e) {
            return Observable.error(e);
        }
    }

    public /* synthetic */ void c0(View view) {
        finish();
    }

    private void h0() {
        y();
    }

    private void applySettingsTheme() {
        String selected = getSharedPreferences("appearance", MODE_PRIVATE)
                .getString("theme", "system");
        boolean dark = "dark".equals(selected)
                || ("system".equals(selected)
                && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES);
        setTheme(dark ? R.style.BaseTheme_Dark : R.style.BaseTheme_Light);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        applySettingsTheme();
        super.onCreate(bundle);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.p_settings_toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                SettingsActivity.this.c0(view);
            }
        });
        this.settingsFragment = new SettingsFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.f_settings_container, this.settingsFragment).commit();
        getSupportFragmentManager().executePendingTransactions();
        f0();
        C();
        g0();
        B();
        setupLocalExport();
        i0();
        k0();
    }

    private void setupLocalExport() {
        exportDatabaseLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                Uri uri = result.getData().getData();
                if (uri != null) {
                    exportDatabaseTo(uri);
                }
            }
        });
        findPreference("pref_export_local").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/octet-stream");
                intent.putExtra(Intent.EXTRA_TITLE, "openrunner-data.db");
                exportDatabaseLauncher.launch(intent);
                return true;
            }
        });
    }

    private void exportDatabaseTo(final Uri uri) {
        Observable.create(new ObservableOnSubscribe<Long>() {
            @Override
            public void subscribe(ObservableEmitter<Long> observableEmitter) {
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    if (outputStream == null) {
                        observableEmitter.onError(new IOException("Cannot open target file"));
                        return;
                    }
                    try {
                        Data.exportDatabase(SettingsActivity.this, outputStream);
                    } finally {
                        outputStream.close();
                    }
                    observableEmitter.onNext(Long.valueOf(new File(getFilesDir(), "data.db").length()));
                    observableEmitter.onComplete();
                } catch (IOException e2) {
                    observableEmitter.onError(e2);
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long value) {
                Toast.makeText(SettingsActivity.this, getString(R.string.p_setting_export_success, StorageUtil.sizeToReadableString(value.longValue())), Toast.LENGTH_SHORT).show();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                Toast.makeText(SettingsActivity.this, getString(R.string.p_setting_export_failed, throwable.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        h0();
    }
}
