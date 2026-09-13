package cn.edu.pku.pkurunner.Settings;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import cn.edu.pku.pkurunner.BuildConfig;
import cn.edu.pku.pkurunner.Data;
import cn.edu.pku.pkurunner.GuidePage.IntroActivity;
import cn.edu.pku.pkurunner.Map.SpeedHelper;
import cn.edu.pku.pkurunner.Photo.PhotoFile;
import cn.edu.pku.pkurunner.Photo.UselessPhotoCleaner;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.Storage.Dropbox;
import cn.edu.pku.pkurunner.Storage.OperationCancelException;
import cn.edu.pku.pkurunner.Storage.StorageUtil;
import cn.edu.pku.pkurunner.i1;
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
import org.xutils.common.util.LogUtil;

public class SettingsActivity extends PreferenceActivity {

    /* renamed from: a, reason: collision with root package name */
    private ProgressDialog f7051a;

    /* renamed from: b, reason: collision with root package name */
    private int f7052b = 0;

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void U(Preference preference, Pair pair) {
        preference.setSummary(getString(R.string.p_setting_storage_user, ((FullAccount) pair.first).getName().getDisplayName(), StorageUtil.sizeToReadableString(((SpaceUsage) pair.second).getUsed()), StorageUtil.sizeToReadableString(((SpaceUsage) pair.second).getAllocation().getIndividualValue().getAllocated())));
    }

    private void A() {
        Preference findPreference = findPreference("pref_dropbox");
        findPreference.setSummary("Not connected");
        findPreference.setEnabled(true);
        findPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: cn.edu.pku.pkurunner.Settings.h
            @Override // android.preference.Preference.OnPreferenceClickListener
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
        findPreference("pref_guide").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: cn.edu.pku.pkurunner.Settings.r
            @Override // android.preference.Preference.OnPreferenceClickListener
            public final boolean onPreferenceClick(Preference preference) {
                boolean W;
                W = SettingsActivity.this.W(preference);
                return W;
            }
        });
        Preference findPreference = findPreference("pref_version");
        final String[] stringArray = getResources().getStringArray(R.array.p_about_easteregg);
        findPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: cn.edu.pku.pkurunner.Settings.s
            @Override // android.preference.Preference.OnPreferenceClickListener
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
        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: cn.edu.pku.pkurunner.Settings.a
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public final boolean onPreferenceChange(Preference preference, Object obj) {
                boolean Z;
                Z = SettingsActivity.this.Z(preference, obj);
                return Z;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void I(Throwable th) {
        if (th instanceof Dropbox.APIWrapper.DropboxException) {
            A();
        } else {
            th.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void J(Preference preference, Throwable th) {
        if (th instanceof NetworkIOException) {
            preference.setSummary(R.string.p_setting_storage_sockettimeout);
            return;
        }
        Toast.makeText(this, "Error in " + th, 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void K(ObservableEmitter observableEmitter, DialogInterface dialogInterface, int i2) {
        observableEmitter.onError(new OperationCancelException());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void L(ObservableEmitter observableEmitter, DialogInterface dialogInterface, int i2) {
        observableEmitter.onNext(Boolean.TRUE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void M(final ObservableEmitter observableEmitter) {
        new AlertDialog.Builder(this).setTitle("Really upload database (will overwrite remote file)?").setCancelable(false).setNegativeButton("Cancel", new DialogInterface.OnClickListener() { // from class: cn.edu.pku.pkurunner.Settings.o
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                SettingsActivity.K(observableEmitter, dialogInterface, i2);
            }
        }).setPositiveButton("Confirm", new DialogInterface.OnClickListener() { // from class: cn.edu.pku.pkurunner.Settings.p
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                SettingsActivity.L(observableEmitter, dialogInterface, i2);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ ObservableSource N(Boolean bool) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        this.f7051a = progressDialog;
        progressDialog.setProgressStyle(0);
        this.f7051a.setMessage("Uploading...");
        this.f7051a.setIndeterminate(false);
        this.f7051a.setCancelable(false);
        this.f7051a.show();
        return Observable.just(Boolean.TRUE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void P(FileMetadata fileMetadata) {
        this.f7051a.dismiss();
        this.f7051a = null;
        Toast.makeText(this, "Successfully updated (" + StorageUtil.sizeToReadableString(fileMetadata.getSize()) + ")!", 0).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void Q(Throwable th) {
        if (th instanceof OperationCancelException) {
            LogUtil.d("Operation dismissed.");
            return;
        }
        Toast.makeText(this, "Something went wrong " + th, 0).show();
        th.printStackTrace();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean R(final DbxClientV2 dbxClientV2, Preference preference) {
        Observable.create(new ObservableOnSubscribe() { // from class: cn.edu.pku.pkurunner.Settings.i
            @Override // io.reactivex.ObservableOnSubscribe
            public final void subscribe(ObservableEmitter observableEmitter) {
                SettingsActivity.this.M(observableEmitter);
            }
        }).subscribeOn(AndroidSchedulers.mainThread()).flatMap(new Function() { // from class: cn.edu.pku.pkurunner.Settings.j
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource N;
                N = SettingsActivity.this.N((Boolean) obj);
                return N;
            }
        }).observeOn(Schedulers.io()).flatMap(new Function() { // from class: cn.edu.pku.pkurunner.Settings.k
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource O;
                O = SettingsActivity.this.O(dbxClientV2, (Boolean) obj);
                return O;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.Settings.m
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                SettingsActivity.this.P((FileMetadata) obj);
            }
        }, new Consumer() { // from class: cn.edu.pku.pkurunner.Settings.n
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                SettingsActivity.this.Q((Throwable) obj);
            }
        });
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean V(Preference preference) {
        Auth.startOAuth2Authentication(this, BuildConfig.DROPBOX_KEY);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean W(Preference preference) {
        startActivity(new Intent(this, (Class<?>) IntroActivity.class));
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean X(String[] strArr, Preference preference) {
        int i2 = this.f7052b;
        if (i2 < strArr.length) {
            Toast.makeText(this, strArr[i2], 0).show();
            this.f7052b++;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void Y(Preference preference, Boolean bool) {
        Toast.makeText(this, "Successfully changed gender", 0).show();
        preference.setSummary(D(Data.getUser().getGender()));
        preference.setDefaultValue(E(Data.getUser().getGender()));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean Z(final Preference preference, Object obj) {
        Data.getUser().setGender(((String) obj).equals(getResources().getStringArray(R.array.p_setting_gender_value)[0]) ? 1 : 0);
        Data.saveUserToDatabase().observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.Settings.b
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj2) {
                SettingsActivity.this.Y(preference, (Boolean) obj2);
            }
        }, new i1());
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void a0(Preference preference, String str, Boolean bool) {
        Toast.makeText(this, "Successfully changed name", 0).show();
        preference.setSummary(str);
        preference.setDefaultValue(str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean b0(final Preference preference, Object obj) {
        final String str = (String) obj;
        Data.getUser().setName(str);
        Data.saveUserToDatabase().observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.Settings.x
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj2) {
                SettingsActivity.this.a0(preference, str, (Boolean) obj2);
            }
        }, new i1());
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean d0(Preference preference) {
        UselessPhotoCleaner.cleanAllUnused(getExternalFilesDir(PhotoFile.PicutreType));
        Toast.makeText(this, R.string.p_setting_photo_deleted, 0).show();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
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
        editTextPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: cn.edu.pku.pkurunner.Settings.w
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public final boolean onPreferenceChange(Preference preference, Object obj) {
                boolean b02;
                b02 = SettingsActivity.this.b0(preference, obj);
                return b02;
            }
        });
    }

    private void g0() {
        findPreference("pref_clean_photo").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: cn.edu.pku.pkurunner.Settings.v
            @Override // android.preference.Preference.OnPreferenceClickListener
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
        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: cn.edu.pku.pkurunner.Settings.u
            @Override // android.preference.Preference.OnPreferenceChangeListener
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
            Dropbox.APIWrapper.getToken(this).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.Settings.l
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    SettingsActivity.this.H((String) obj);
                }
            }, new Consumer() { // from class: cn.edu.pku.pkurunner.Settings.q
                @Override // io.reactivex.functions.Consumer
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
        StorageUtil.NetworkMethodWrapper(new StorageUtil.Producer() { // from class: cn.edu.pku.pkurunner.Settings.c
            @Override // cn.edu.pku.pkurunner.Storage.StorageUtil.Producer
            public final Object produce() {
                FullAccount S;
                    S = SettingsActivity.S(client);
                return S;
            }
        }).flatMap(new Function() { // from class: cn.edu.pku.pkurunner.Settings.d
            @Override // io.reactivex.functions.Function
            public final Object apply(Object obj) {
                ObservableSource T;
                T = SettingsActivity.T(client, (FullAccount) obj);
                return T;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.Settings.e
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                SettingsActivity.this.U(findPreference2, (Pair) obj);
            }
        }, new Consumer() { // from class: cn.edu.pku.pkurunner.Settings.f
            @Override // io.reactivex.functions.Consumer
            public final void accept(Object obj) {
                SettingsActivity.this.J(findPreference2, (Throwable) obj);
            }
        });
        findPreference("pref_dropbox_upload").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: cn.edu.pku.pkurunner.Settings.g
            @Override // android.preference.Preference.OnPreferenceClickListener
            public final boolean onPreferenceClick(Preference preference) {
                boolean R;
                R = SettingsActivity.this.R(client, preference);
                return R;
            }
        });
    }

    private String D(int i2) {
        String[] stringArray = getResources().getStringArray(R.array.p_setting_gender_display);
        char c2 = 1;
        if (i2 == 1) {
            c2 = 0;
        }
        return stringArray[c2];
    }

    private String E(int i2) {
        String[] stringArray = getResources().getStringArray(R.array.p_setting_gender_value);
        char c2 = 1;
        if (i2 == 1) {
            c2 = 0;
        }
        return stringArray[c2];
    }

    private String F(SpeedHelper.SPEED_UNIT speed_unit) {
        return getResources().getStringArray(R.array.p_setting_unit_display)[speed_unit.ordinal()];
    }

    private String G(SpeedHelper.SPEED_UNIT speed_unit) {
        return getResources().getStringArray(R.array.p_setting_unit_value)[speed_unit.ordinal()];
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void H(String str) {
        z();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ ObservableSource O(DbxClientV2 dbxClientV2, Boolean bool) {
        try {
            return Observable.just(Data.uploadDatabaseToDropbox(this, dbxClientV2));
        } catch (Exception e) {
            return Observable.error(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ FullAccount S(DbxClientV2 dbxClientV2) {
        try {
            return dbxClientV2.users().getCurrentAccount();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ ObservableSource T(DbxClientV2 dbxClientV2, FullAccount fullAccount) {
        try {
            return Observable.just(new Pair(fullAccount, dbxClientV2.users().getSpaceUsage()));
        } catch (Exception e) {
            return Observable.error(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
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

    @Override // android.preference.PreferenceActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        applySettingsTheme();
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.app_settings);
        f0();
        C();
        g0();
        B();
        i0();
        k0();
    }

    @Override // android.app.Activity
    protected void onPostCreate(@Nullable Bundle bundle) {
        super.onPostCreate(bundle);
        LinearLayout linearLayout = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.view_settings_toolbar, (ViewGroup) linearLayout, false);
        linearLayout.addView(toolbar, 0);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() { // from class: cn.edu.pku.pkurunner.Settings.t
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SettingsActivity.this.c0(view);
            }
        });
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        h0();
    }
}
