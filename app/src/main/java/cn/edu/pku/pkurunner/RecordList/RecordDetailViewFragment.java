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
import cn.edu.pku.pkurunner.StackTracePrintingConsumer;
import cn.edu.pku.pkurunner.Exception.ServerException;
import com.bumptech.glide.Glide;
import cn.edu.pku.pkurunner.Model.Point;
import cn.edu.pku.pkurunner.Model.Record;
import cn.edu.pku.pkurunner.Network.Network;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.RecordList.RecordDetailViewFragment;
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

    private MapView mapView;

    private TextView placeText;

    private TextView distanceText;

    private TextView statusText;

    private TextView dateText;

    private TextView userIdText;

    class BottomSheetStateCallback extends BottomSheetBehavior.BottomSheetCallback {
        @Override
        public void onStateChanged(View view, int index) {
            if (index != 5) {
                return;
            }
            RecordDetailViewFragment.this.dismiss();
        }

        BottomSheetStateCallback() {
        }

        @Override
        public void onSlide(View view, float value4) {
            Guideline guideline = (Guideline) view.findViewById(R.id.f_record_detail_guideline_detail_top);
            Guideline guideline2 = (Guideline) view.findViewById(R.id.f_record_detail_guideline_detail_mid);
            Guideline guideline3 = (Guideline) view.findViewById(R.id.f_record_detail_guideline_detail_bottom);
            Guideline guideline4 = (Guideline) view.findViewById(R.id.f_record_detail_guideline_above_map);
            ImageView imageView = (ImageView) view.findViewById(R.id.f_record_detail_img_photo);
            float x2 = RecordDetailViewFragment.this.x(value4, 200.0f, 72.0f);
            guideline.setGuidelineBegin((int) (RecordDetailViewFragment.this.I * x2));
            guideline2.setGuidelineBegin((int) ((56.0f + x2) * RecordDetailViewFragment.this.I));
            float value5 = x2 + 112.0f;
            guideline3.setGuidelineBegin((int) (RecordDetailViewFragment.this.I * value5));
            guideline4.setGuidelineBegin((int) ((value5 + 8.0f) * RecordDetailViewFragment.this.I));
            imageView.setTranslationY(RecordDetailViewFragment.this.x(value4, BitmapDescriptorFactory.HUE_RED, -200.0f) * RecordDetailViewFragment.this.I);
        }
    }

    private static class TrackWindow {

        private List window = new LinkedList();

        private int windowSize;

        void add(Point point) {
            if (this.window.size() > this.windowSize) {
                this.window.remove(0);
            }
            this.window.add(point);
        }

        void clear() {
            this.window.clear();
        }

        double[] direction() {
            if (this.window.size() < 2) {
                return new double[]{Math.random(), Math.random()};
            }
            Point point = (Point) this.window.get(0);
            List list = this.window;
            Point point2 = (Point) list.get(list.size() - 1);
            return new double[]{point2.getLongitude() - point.getLongitude(), point2.getLatitude() - point.getLatitude()};
        }

        Point first() {
            if (this.window.size() == 0) {
                LogUtil.e("Critical error in simplifying track!");
            }
            return (Point) this.window.get(0);
        }

        TrackWindow(int index) {
            this.windowSize = index;
        }
    }

    private ArrayList A(ArrayList arrayList) {
        double[] dArr = {1.0d, 0.0d};
        ArrayList arrayList2 = new ArrayList();
        TrackWindow trackWindow = new TrackWindow(5);
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            Point point = (Point) it.next();
            int status = point.getStatus();
            if (status == 0) {
                trackWindow.add(point);
                double[] character = trackWindow.direction();
                if (z(dArr, character) < 0.94d) {
                    arrayList2.add(trackWindow.first());
                    dArr = character;
                }
            } else if (status == 1) {
                arrayList2.add(point);
                trackWindow.add(point);
            } else if (status == 2) {
                arrayList2.add(point);
                trackWindow.clear();
            } else if (status == 3) {
                arrayList2.add(point);
            }
        }
        arrayList2.add((Point) arrayList.get(arrayList.size() - 1));
        return arrayList2;
    }

    public float x(float value4, float value5, float f4) {
        return value5 + (Math.min(Math.max(value4, BitmapDescriptorFactory.HUE_RED), 1.0f) * (f4 - value5));
    }

    private double z(double[] dArr, double[] dArr2) {
        double value = dArr[0];
        double value2 = dArr2[0] * value;
        double value3 = dArr[1];
        return (value2 + (dArr2[1] * value3)) / (Math.hypot(value, value3) * Math.hypot(dArr2[0], dArr2[1]));
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
            from.setBottomSheetCallback(new BottomSheetStateCallback());
        }
    }

    private void w(ArrayList arrayList) {
        if (arrayList == null || arrayList.size() == 0) {
            return;
        }
        ArrayList A = A(arrayList);
        Iterator it = A.iterator();
        double value = 0.0d;
        double value2 = 90.0d;
        double value3 = 180.0d;
        double d5 = 0.0d;
        while (it.hasNext()) {
            Point point = (Point) it.next();
            if (point.getStatus() != 3) {
                double latitude = point.getLatitude();
                double longitude = point.getLongitude();
                if (latitude > value) {
                    value = latitude;
                }
                if (latitude < value2) {
                    value2 = latitude;
                }
                if (longitude > d5) {
                    d5 = longitude;
                }
                if (longitude < value3) {
                    value3 = longitude;
                }
            }
        }
        double d6 = (value + value2) / 2.0d;
        double d7 = (d5 + value3) / 2.0d;
        int index = 1;
        while (true) {
            double d8 = d7;
            if (index >= A.size()) {
                double d9 = d6;
                double log = 9.0d - (Math.log(value - value2) / Math.log(2.0d));
                double log2 = 9.0d - (Math.log((d5 - value3) * Math.cos((3.141592653589793d * d9) / 180.0d)) / Math.log(2.0d));
                this.G.addMarker(new MarkerOptions().position(((Point) A.get(0)).toLatLng()).title(getString(R.string.f_record_detail_track_start)));
                this.G.addMarker(new MarkerOptions().position(((Point) A.get(A.size() - 1)).toLatLng()).title(getString(R.string.f_record_detail_track_end)));
                this.G.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(d9, d8)));
                this.G.moveCamera(CameraUpdateFactory.zoomTo((float) Math.min(log, log2)));
                return;
            }
            Point point2 = (Point) A.get(index - 1);
            Point point3 = (Point) A.get(index);
            double d10 = d6;
            if (point3.getStatus() == 3) {
                this.G.addMarker(new MarkerOptions().position(point2.toLatLng()).title(getString(R.string.f_record_detail_track_8am)));
            } else {
                if (point2.getStatus() == 3) {
                    if (index > 1) {
                        point2 = (Point) A.get(index - 2);
                    }
                }
                if (point2.getStatus() == 2 && point3.getStatus() == 1) {
                    this.G.addMarker(new MarkerOptions().position(point2.toLatLng()).title(getString(R.string.f_record_detail_track_pause)));
                    this.G.addMarker(new MarkerOptions().position(point3.toLatLng()).title(getString(R.string.f_record_detail_track_resume)));
                } else {
                this.G.addPolyline(new PolylineOptions().add(point2.toLatLng(), point3.toLatLng()).color(getResources().getColor(R.color.map_route)));
                }
            }
            index++;
            d7 = d8;
            d6 = d10;
        }
    }

    public /* synthetic */ void y(String str) {
        this.placeText.setText(str);
        this.B.setText(str);
    }

    private void u(Record record) {
        int index;
        int index2;
        int index3;
        if (Data.getUser().isOffline().booleanValue()) {
            Data.getRecordPlaceHintForOfflineUser(record).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    RecordDetailViewFragment.this.y((String) obj);
                }
            }, new StackTracePrintingConsumer());
        } else {
            Record.RecordPlace place = record.getPlace();
            String placeString = Record.getPlaceString(place);
            Drawable drawable = this.E.getDrawable();
            Resources resources = getResources();
            if (place == Record.RecordPlace.UNKNOWN) {
                index = R.color.blue_grey_700;
            } else {
                index = R.color.teal_700;
            }
            drawable.setTint(resources.getColor(index));
            this.placeText.setText(placeString);
            this.B.setText(placeString);
        }
        this.distanceText.setText(getString(R.string.v_record_card_distance_format, Double.valueOf(record.getDistance() / 1000.0d)));
        if (Data.getUser().isOffline().booleanValue()) {
            this.statusText.setText(R.string.f_record_detail_status_offline);
            this.statusText.setTextColor(getResources().getColor(R.color.or_warning));
        } else if (!record.isUploaded()) {
            this.statusText.setText(R.string.f_record_detail_status_unuploaded);
        } else if (!record.isVerified()) {
            this.statusText.setText(getString(R.string.f_record_detail_status_verified_fail, ServerException.getLocalizedMessage(record.getInvalidReason())));
        } else {
            TextView textView = this.statusText;
            if (record.getInvalidReason() == 0) {
                index2 = R.string.f_record_detail_status_ok;
            } else {
                index2 = R.string.f_record_detail_status_ok_bonus;
            }
            textView.setText(index2);
            this.statusText.setTextColor(getResources().getColor(R.color.or_success));
        }
        this.dateText.setText(N.format(record.getDate()));
        this.userIdText.setText(record.getUserId());
        int abs = Math.abs((int) (record.getAccumulateBearing() / 6.283185307179586d));
        if (abs == 0) {
            this.A.setText(R.string.f_record_detail_lap_zero);
        } else {
            TextView textView2 = this.A;
            Object[] objArr = new Object[2];
            if (abs > 0) {
                index3 = R.string.f_record_detail_lap_anticlockwise;
            } else {
                index3 = R.string.f_record_detail_lap_clockwise;
            }
            objArr[0] = getString(index3);
            objArr[1] = Integer.valueOf(abs);
            textView2.setText(getString(R.string.f_record_detail_lap_count, objArr));
        }
        this.C.setText(getString(R.string.f_record_detail_step_count, Integer.valueOf(record.getStep())));
        this.D.setText(getString(R.string.f_record_detail_speed, Double.valueOf((record.getDistance() * 1.0d) / record.getDuration())));
        w(record.getTrack());
        String photoRemotePath = record.getPhotoRemotePath();
        if (photoRemotePath != null && !"".equals(photoRemotePath)) {
            Glide.with(this).load(Network.photoBaseUrl + record.getPhotoRemotePath()).placeholder(R.color.or_surface_container_high).error(R.drawable.ic_cloud_off_black_24dp).centerCrop().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).transition((TransitionOptions<?, ? super Drawable>) DrawableTransitionOptions.withCrossFade()).into(this.F);
            return;
        }
        this.F.setImageResource(R.drawable.clip_runner_default);
        this.F.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle bundle) {
        Dialog onCreateDialog = super.onCreateDialog(bundle);
        this.H = onCreateDialog.getWindow();
        return onCreateDialog;
    }

    @Override
    @Nullable
    public View onCreateView(LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
        View inflate = layoutInflater.inflate(R.layout.fragment_record_detail, viewGroup, false);
        this.mapView = (MapView) inflate.findViewById(R.id.f_record_detail_map);
        this.E = (ImageView) inflate.findViewById(R.id.f_record_detail_circle_bg);
        this.placeText = (TextView) inflate.findViewById(R.id.f_record_detail_txt_place);
        this.distanceText = (TextView) inflate.findViewById(R.id.f_record_detail_txt_distance);
        this.statusText = (TextView) inflate.findViewById(R.id.f_record_detail_txt_status);
        this.dateText = (TextView) inflate.findViewById(R.id.f_record_detail_txt_date);
        this.userIdText = (TextView) inflate.findViewById(R.id.f_record_detail_txt_uid);
        this.A = (TextView) inflate.findViewById(R.id.f_record_detail_txt_laps);
        this.B = (TextView) inflate.findViewById(R.id.f_record_detail_txt_location);
        this.C = (TextView) inflate.findViewById(R.id.f_record_detail_txt_steps);
        this.D = (TextView) inflate.findViewById(R.id.f_record_detail_txt_speed);
        this.F = (ImageView) inflate.findViewById(R.id.f_record_detail_img_photo);
        this.mapView.onCreate(bundle);
        AMap map = this.mapView.getMap();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mapView.onDestroy();
        this.mapView = null;
        this.G = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mapView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mapView.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        this.mapView.onSaveInstanceState(bundle);
    }

    @Override
    public void onStart() {
        super.onStart();
        v();
    }
}
