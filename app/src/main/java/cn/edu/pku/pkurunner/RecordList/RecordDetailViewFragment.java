package cn.edu.pku.pkurunner.RecordList;

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Guideline;
import androidx.recyclerview.widget.ItemTouchHelper;
import cn.edu.pku.pkurunner.Data;
import cn.edu.pku.pkurunner.Exception.ServerException;
import com.bumptech.glide.Glide;
import cn.edu.pku.pkurunner.Model.Point;
import cn.edu.pku.pkurunner.Model.Record;
import cn.edu.pku.pkurunner.Network.Network;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.RecordList.RecordDetailViewFragment;
import cn.edu.pku.pkurunner.i1;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.PolylineOptions;
import com.bumptech.glide.TransitionOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.xutils.common.util.LogUtil;

public class RecordDetailViewFragment extends BottomSheetDialogFragment {
    private static SimpleDateFormat N = new SimpleDateFormat("yy-MM-dd\nHH:mm");
    private TextView A;
    private TextView B;
    private TextView C;
    private TextView D;
    private ImageView E;
    private ImageView F;
    private AMap G;
    private Window H;
    private float I;
    private final int J = ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION;
    private final int K = 112;
    private final int L = 72;
    private final int M = 8;

    /* renamed from: u, reason: collision with root package name */
    private MapView f7012u;

    /* renamed from: v, reason: collision with root package name */
    private TextView f7013v;

    /* renamed from: w, reason: collision with root package name */
    private TextView f7014w;

    /* renamed from: x, reason: collision with root package name */
    private TextView f7015x;

    /* renamed from: y, reason: collision with root package name */
    private TextView f7016y;

    /* renamed from: z, reason: collision with root package name */
    private TextView f7017z;

    class a extends BottomSheetBehavior.BottomSheetCallback {
        @Override // com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
        public void onStateChanged(View view, int i2) {
            if (i2 != 5) {
                return;
            }
            RecordDetailViewFragment.this.dismiss();
        }

        a() {
        }

        @Override // com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
        public void onSlide(View view, float f2) {
            Guideline guideline = (Guideline) view.findViewById(R.id.f_record_detail_guideline_detail_top);
            Guideline guideline2 = (Guideline) view.findViewById(R.id.f_record_detail_guideline_detail_mid);
            Guideline guideline3 = (Guideline) view.findViewById(R.id.f_record_detail_guideline_detail_bottom);
            Guideline guideline4 = (Guideline) view.findViewById(R.id.f_record_detail_guideline_above_map);
            ImageView imageView = (ImageView) view.findViewById(R.id.f_record_detail_img_photo);
            float x2 = RecordDetailViewFragment.this.x(f2, 200.0f, 72.0f);
            guideline.setGuidelineBegin((int) (RecordDetailViewFragment.this.I * x2));
            guideline2.setGuidelineBegin((int) ((56.0f + x2) * RecordDetailViewFragment.this.I));
            float f3 = x2 + 112.0f;
            guideline3.setGuidelineBegin((int) (RecordDetailViewFragment.this.I * f3));
            guideline4.setGuidelineBegin((int) ((f3 + 8.0f) * RecordDetailViewFragment.this.I));
            imageView.setTranslationY(RecordDetailViewFragment.this.x(f2, BitmapDescriptorFactory.HUE_RED, -200.0f) * RecordDetailViewFragment.this.I);
        }
    }

    private static class b {

        /* renamed from: a, reason: collision with root package name */
        private List f7019a = new LinkedList();

        /* renamed from: b, reason: collision with root package name */
        private int f7020b;

        void a(Point point) {
            if (this.f7019a.size() > this.f7020b) {
                this.f7019a.remove(0);
            }
            this.f7019a.add(point);
        }

        void b() {
            this.f7019a.clear();
        }

        double[] c() {
            if (this.f7019a.size() < 2) {
                return new double[]{Math.random(), Math.random()};
            }
            Point point = (Point) this.f7019a.get(0);
            List list = this.f7019a;
            Point point2 = (Point) list.get(list.size() - 1);
            return new double[]{point2.getLongitude() - point.getLongitude(), point2.getLatitude() - point.getLatitude()};
        }

        Point d() {
            if (this.f7019a.size() == 0) {
                LogUtil.e("Critical error in simplifying track!");
            }
            return (Point) this.f7019a.get(0);
        }

        b(int i2) {
            this.f7020b = i2;
        }
    }

    private ArrayList A(ArrayList arrayList) {
        double[] dArr = {1.0d, 0.0d};
        ArrayList arrayList2 = new ArrayList();
        b bVar = new b(5);
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            Point point = (Point) it.next();
            int status = point.getStatus();
            if (status == 0) {
                bVar.a(point);
                double[] c2 = bVar.c();
                if (z(dArr, c2) < 0.94d) {
                    arrayList2.add(bVar.d());
                    dArr = c2;
                }
            } else if (status == 1) {
                arrayList2.add(point);
                bVar.a(point);
            } else if (status == 2) {
                arrayList2.add(point);
                bVar.b();
            } else if (status == 3) {
                arrayList2.add(point);
            }
        }
        arrayList2.add((Point) arrayList.get(arrayList.size() - 1));
        return arrayList2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float x(float f2, float f3, float f4) {
        return f3 + (Math.min(Math.max(f2, BitmapDescriptorFactory.HUE_RED), 1.0f) * (f4 - f3));
    }

    private double z(double[] dArr, double[] dArr2) {
        double d2 = dArr[0];
        double d3 = dArr2[0] * d2;
        double d4 = dArr[1];
        return (d3 + (dArr2[1] * d4)) / (Math.hypot(d2, d4) * Math.hypot(dArr2[0], dArr2[1]));
    }

    private void v() {
        View bottomSheet = this.H == null ? null
                : this.H.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }
        BottomSheetBehavior from = BottomSheetBehavior.from(bottomSheet);
        this.I = getResources().getDisplayMetrics().density;
        if (from != null) {
            from.setBottomSheetCallback(new a());
        }
    }

    private void w(ArrayList arrayList) {
        if (arrayList == null || arrayList.size() == 0) {
            return;
        }
        ArrayList A = A(arrayList);
        Iterator it = A.iterator();
        double d2 = 0.0d;
        double d3 = 90.0d;
        double d4 = 180.0d;
        double d5 = 0.0d;
        while (it.hasNext()) {
            Point point = (Point) it.next();
            if (point.getStatus() != 3) {
                double latitude = point.getLatitude();
                double longitude = point.getLongitude();
                if (latitude > d2) {
                    d2 = latitude;
                }
                if (latitude < d3) {
                    d3 = latitude;
                }
                if (longitude > d5) {
                    d5 = longitude;
                }
                if (longitude < d4) {
                    d4 = longitude;
                }
            }
        }
        double d6 = (d2 + d3) / 2.0d;
        double d7 = (d5 + d4) / 2.0d;
        int i2 = 1;
        while (true) {
            double d8 = d7;
            if (i2 >= A.size()) {
                double d9 = d6;
                double log = 9.0d - (Math.log(d2 - d3) / Math.log(2.0d));
                double log2 = 9.0d - (Math.log((d5 - d4) * Math.cos((3.141592653589793d * d9) / 180.0d)) / Math.log(2.0d));
                this.G.addMarker(new MarkerOptions().position(((Point) A.get(0)).toLatLng()).title(getString(R.string.f_record_detail_track_start)));
                this.G.addMarker(new MarkerOptions().position(((Point) A.get(A.size() - 1)).toLatLng()).title(getString(R.string.f_record_detail_track_end)));
                this.G.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(d9, d8)));
                this.G.moveCamera(CameraUpdateFactory.zoomTo((float) Math.min(log, log2)));
                return;
            }
            Point point2 = (Point) A.get(i2 - 1);
            Point point3 = (Point) A.get(i2);
            double d10 = d6;
            if (point3.getStatus() == 3) {
                this.G.addMarker(new MarkerOptions().position(point2.toLatLng()).title(getString(R.string.f_record_detail_track_8am)));
            } else {
                if (point2.getStatus() == 3) {
                    if (i2 > 1) {
                        point2 = (Point) A.get(i2 - 2);
                    }
                }
                if (point2.getStatus() == 2 && point3.getStatus() == 1) {
                    this.G.addMarker(new MarkerOptions().position(point2.toLatLng()).title(getString(R.string.f_record_detail_track_pause)));
                    this.G.addMarker(new MarkerOptions().position(point3.toLatLng()).title(getString(R.string.f_record_detail_track_resume)));
                } else {
                    this.G.addPolyline(new PolylineOptions().add(point2.toLatLng(), point3.toLatLng()).color(getResources().getColor(R.color.red_500)));
                }
            }
            i2++;
            d7 = d8;
            d6 = d10;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void y(String str) {
        this.f7013v.setText(str);
        this.B.setText(str);
    }

    private void u(Record record) {
        int i2;
        int i3;
        int i4;
        if (Data.getUser().isOffline().booleanValue()) {
            Data.getRecordPlaceHintForOfflineUser(record).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: v.b
                @Override // io.reactivex.functions.Consumer
                public final void accept(Object obj) {
                    RecordDetailViewFragment.this.y((String) obj);
                }
            }, new i1());
        } else {
            Record.RecordPlace place = record.getPlace();
            String placeString = Record.getPlaceString(place);
            Drawable drawable = this.E.getDrawable();
            Resources resources = getResources();
            if (place == Record.RecordPlace.UNKNOWN) {
                i2 = R.color.blue_grey_700;
            } else {
                i2 = R.color.teal_700;
            }
            drawable.setTint(resources.getColor(i2));
            this.f7013v.setText(placeString);
            this.B.setText(placeString);
        }
        this.f7014w.setText(getString(R.string.v_record_card_distance_format, Double.valueOf(record.getDistance() / 1000.0d)));
        if (Data.getUser().isOffline().booleanValue()) {
            this.f7015x.setText(R.string.f_record_detail_status_offline);
            this.f7015x.setTextColor(getResources().getColor(R.color.orange_A700));
        } else if (!record.isUploaded()) {
            this.f7015x.setText(R.string.f_record_detail_status_unuploaded);
        } else if (!record.isVerified()) {
            this.f7015x.setText(getString(R.string.f_record_detail_status_verified_fail, ServerException.getLocalizedMessage(record.getInvalidReason())));
        } else {
            TextView textView = this.f7015x;
            if (record.getInvalidReason() == 0) {
                i3 = R.string.f_record_detail_status_ok;
            } else {
                i3 = R.string.f_record_detail_status_ok_bonus;
            }
            textView.setText(i3);
            this.f7015x.setTextColor(getResources().getColor(R.color.green_A700));
        }
        this.f7016y.setText(N.format(record.getDate()));
        this.f7017z.setText(record.getUserId());
        int abs = Math.abs((int) (record.getAccumulateBearing() / 6.283185307179586d));
        if (abs == 0) {
            this.A.setText(R.string.f_record_detail_lap_zero);
        } else {
            TextView textView2 = this.A;
            Object[] objArr = new Object[2];
            if (abs > 0) {
                i4 = R.string.f_record_detail_lap_anticlockwise;
            } else {
                i4 = R.string.f_record_detail_lap_clockwise;
            }
            objArr[0] = getString(i4);
            objArr[1] = Integer.valueOf(abs);
            textView2.setText(getString(R.string.f_record_detail_lap_count, objArr));
        }
        this.C.setText(getString(R.string.f_record_detail_step_count, Integer.valueOf(record.getStep())));
        this.D.setText(getString(R.string.f_record_detail_speed, Double.valueOf((record.getDistance() * 1.0d) / record.getDuration())));
        w(record.getTrack());
        String photoRemotePath = record.getPhotoRemotePath();
        if (photoRemotePath != null && !"".equals(photoRemotePath)) {
            Glide.with(this).load(Network.photoBaseUrl + record.getPhotoRemotePath()).placeholder(R.color.grey_200).error(R.drawable.ic_cloud_off_black_24dp).centerCrop().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).transition((TransitionOptions<?, ? super Drawable>) DrawableTransitionOptions.withCrossFade()).into(this.F);
            return;
        }
        this.F.setImageResource(R.drawable.clip_runner_default);
        this.F.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    @Override // com.google.android.material.bottomsheet.BottomSheetDialogFragment, androidx.appcompat.app.AppCompatDialogFragment, androidx.fragment.app.DialogFragment
    @NonNull
    public Dialog onCreateDialog(Bundle bundle) {
        Dialog onCreateDialog = super.onCreateDialog(bundle);
        this.H = onCreateDialog.getWindow();
        return onCreateDialog;
    }

    @Override // androidx.fragment.app.Fragment
    @Nullable
    public View onCreateView(LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
        View inflate = layoutInflater.inflate(R.layout.fragment_record_detail, viewGroup, false);
        this.f7012u = (MapView) inflate.findViewById(R.id.f_record_detail_map);
        this.E = (ImageView) inflate.findViewById(R.id.f_record_detail_circle_bg);
        this.f7013v = (TextView) inflate.findViewById(R.id.f_record_detail_txt_place);
        this.f7014w = (TextView) inflate.findViewById(R.id.f_record_detail_txt_distance);
        this.f7015x = (TextView) inflate.findViewById(R.id.f_record_detail_txt_status);
        this.f7016y = (TextView) inflate.findViewById(R.id.f_record_detail_txt_date);
        this.f7017z = (TextView) inflate.findViewById(R.id.f_record_detail_txt_uid);
        this.A = (TextView) inflate.findViewById(R.id.f_record_detail_txt_laps);
        this.B = (TextView) inflate.findViewById(R.id.f_record_detail_txt_location);
        this.C = (TextView) inflate.findViewById(R.id.f_record_detail_txt_steps);
        this.D = (TextView) inflate.findViewById(R.id.f_record_detail_txt_speed);
        this.F = (ImageView) inflate.findViewById(R.id.f_record_detail_img_photo);
        this.f7012u.onCreate(bundle);
        AMap map = this.f7012u.getMap();
        this.G = map;
        map.getUiSettings().setMyLocationButtonEnabled(false);
        this.G.getUiSettings().setScaleControlsEnabled(false);
        this.G.getUiSettings().setZoomControlsEnabled(false);
        this.G.getUiSettings().setCompassEnabled(false);
        this.G.getUiSettings().setScrollGesturesEnabled(false);
        this.G.getUiSettings().setZoomGesturesEnabled(false);
        this.G.setMyLocationEnabled(false);
        u((Record) getArguments().getParcelable("record"));
        return inflate;
    }

    @Override // androidx.fragment.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        this.f7012u.onDestroy();
        this.f7012u = null;
        this.G = null;
    }

    @Override // androidx.fragment.app.Fragment
    public void onPause() {
        super.onPause();
        this.f7012u.onPause();
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        this.f7012u.onResume();
    }

    @Override // androidx.fragment.app.DialogFragment, androidx.fragment.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        this.f7012u.onSaveInstanceState(bundle);
    }

    @Override // androidx.fragment.app.DialogFragment, androidx.fragment.app.Fragment
    public void onStart() {
        super.onStart();
        v();
    }
}
