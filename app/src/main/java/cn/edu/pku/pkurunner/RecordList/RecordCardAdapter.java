package cn.edu.pku.pkurunner.RecordList;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;
import cn.edu.pku.pkurunner.Data;
import cn.edu.pku.pkurunner.Model.Record;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.RecordList.ItemTouchHelperCallback;
import cn.edu.pku.pkurunner.RecordList.RecordCardAdapter;
import cn.edu.pku.pkurunner.RecordList.RecordListContract;
import cn.edu.pku.pkurunner.i1;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class RecordCardAdapter extends RecyclerView.Adapter<RecordCardAdapter.b> implements ItemTouchHelperCallback.b {

    /* renamed from: h, reason: collision with root package name */
    private static final SimpleDateFormat f6997h = new SimpleDateFormat("yy-MM-dd");

    /* renamed from: i, reason: collision with root package name */
    private static final SimpleDateFormat f6998i = new SimpleDateFormat("HH:mm");

    /* renamed from: c, reason: collision with root package name */
    private ArrayList f6999c;

    /* renamed from: d, reason: collision with root package name */
    private Boolean f7000d;

    /* renamed from: e, reason: collision with root package name */
    private Boolean f7001e;

    /* renamed from: f, reason: collision with root package name */
    private RecordListContract.Presenter f7002f;

    /* renamed from: g, reason: collision with root package name */
    private c f7003g;

    interface a {
        void onClick(View view);
    }

    static class b extends RecyclerView.ViewHolder {

        /* renamed from: s, reason: collision with root package name */
        protected TextView f7004s;

        /* renamed from: t, reason: collision with root package name */
        protected TextView f7005t;

        /* renamed from: u, reason: collision with root package name */
        protected TextView f7006u;

        /* renamed from: v, reason: collision with root package name */
        protected TextView f7007v;

        /* renamed from: w, reason: collision with root package name */
        protected a f7008w;

        /* renamed from: x, reason: collision with root package name */
        protected ImageView f7009x;

        /* renamed from: y, reason: collision with root package name */
        protected ImageView f7010y;

        /* renamed from: z, reason: collision with root package name */
        protected CardView f7011z;

        public View H() {
            return this.f7006u;
        }

        public View I() {
            return this.f7004s;
        }

        public View J() {
            return this.f7009x;
        }

        void L(a aVar) {
            this.f7008w = aVar;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void K(View view) {
            a aVar = this.f7008w;
            if (aVar != null) {
                aVar.onClick(view);
            }
        }

        void M() {
            float cardElevation = this.f7011z.getCardElevation();
            float f2 = 4.0f * cardElevation;
            ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this.f7011z, "cardElevation", cardElevation, f2);
            ofFloat.setInterpolator(new LinearOutSlowInInterpolator());
            ofFloat.setDuration(250L);
            ofFloat.setStartDelay(1000L);
            ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this.f7011z, "cardElevation", f2, cardElevation);
            ofFloat2.setInterpolator(new FastOutSlowInInterpolator());
            ofFloat2.setDuration(250L);
            ofFloat2.setStartDelay(1000L);
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playSequentially(ofFloat, ofFloat2);
            animatorSet.start();
        }

        b(View view) {
            super(view);
            this.f7004s = (TextView) view.findViewById(R.id.v_record_card_txt_distance);
            this.f7005t = (TextView) view.findViewById(R.id.v_record_card_txt_time);
            this.f7006u = (TextView) view.findViewById(R.id.v_record_card_txt_date);
            this.f7009x = (ImageView) view.findViewById(R.id.v_record_card_img_status);
            this.f7011z = (CardView) view.findViewById(R.id.view_card_record_fg);
            this.f7007v = (TextView) view.findViewById(R.id.v_record_card_txt_place);
            this.f7010y = (ImageView) view.findViewById(R.id.v_record_card_circle_bg);
            view.setOnClickListener(new View.OnClickListener() { // from class: cn.edu.pku.pkurunner.RecordList.d
                @Override // android.view.View.OnClickListener
                public final void onClick(View view2) {
                    RecordCardAdapter.b.this.K(view2);
                }
            });
        }
    }

    interface c {
        Resources a();
    }

    public void notifyDataInvalid() {
        this.f7000d = Boolean.FALSE;
    }

    public void notifyFirstElementElevation() {
        this.f7001e = Boolean.TRUE;
    }

    public void setPresenter(RecordListContract.Presenter presenter, c cVar) {
        if (presenter != null) {
            this.f7002f = presenter;
        }
        this.f7003g = cVar;
    }

    static class d extends b {
        d(View view) {
            super(view);
        }
    }

    private ArrayList e() {
        if (!this.f7000d.booleanValue()) {
            if (Data.isValid()) {
                ArrayList<Record> records = Data.getRecords();
                this.f6999c = records;
                Collections.sort(records, new Comparator() { // from class: v.a
                    @Override // java.util.Comparator
                    public final int compare(Object obj, Object obj2) {
                        int f2;
                        f2 = RecordCardAdapter.f((Record) obj, (Record) obj2);
                        return f2;
                    }
                });
            } else {
                this.f6999c = new ArrayList();
            }
            this.f7000d = Boolean.TRUE;
        }
        return this.f6999c;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void i(b bVar, String str) {
        bVar.f7007v.setText(str);
    }

    private boolean j() {
        if (!this.f7001e.booleanValue()) {
            return false;
        }
        this.f7001e = Boolean.FALSE;
        return true;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(final b bVar, int i2) {
        Record record = (Record) e().get(i2);
        bVar.f7004s.setText(this.f7003g.a().getString(R.string.v_record_card_distance_format, Double.valueOf(record.getDistance() / 1000.0d)));
        Date date = record.getDate();
        String format = f6997h.format(date);
        SimpleDateFormat simpleDateFormat = f6998i;
        String format2 = simpleDateFormat.format(new Date(date.getTime() - (record.getDuration() * 1000)));
        String format3 = simpleDateFormat.format(date);
        bVar.f7006u.setText(format);
        bVar.f7005t.setText(this.f7003g.a().getString(R.string.v_record_card_duration_format, format2, format3));
        bVar.L(new a() { // from class: cn.edu.pku.pkurunner.RecordList.a
            @Override // cn.edu.pku.pkurunner.RecordList.RecordCardAdapter.a
            public final void onClick(View view) {
                RecordCardAdapter.this.g(bVar, view);
            }
        });
        if (bVar instanceof d) {
            ((d) bVar).f7009x.setOnClickListener(new View.OnClickListener() { // from class: cn.edu.pku.pkurunner.RecordList.b
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    RecordCardAdapter.this.h(bVar, view);
                }
            });
        } else {
            if (record.isVerified()) {
                TextView textView = bVar.f7004s;
                textView.setPaintFlags(textView.getPaintFlags() & (-17));
                bVar.f7009x.setImageResource(R.drawable.ic_done_black_24dp);
                bVar.f7009x.getDrawable().setTint(this.f7003g.a().getColor(R.color.green_A700));
            } else {
                TextView textView2 = bVar.f7004s;
                textView2.setPaintFlags(textView2.getPaintFlags() | 16);
                bVar.f7009x.setImageResource(R.drawable.ic_error_outline_black_24dp);
                bVar.f7009x.getDrawable().setTint(this.f7003g.a().getColor(R.color.orange_A400));
            }
            if (Data.getUser().isOffline().booleanValue()) {
                Data.getRecordPlaceHintForOfflineUser(record).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() { // from class: cn.edu.pku.pkurunner.RecordList.c
                    @Override // io.reactivex.functions.Consumer
                    public final void accept(Object obj) {
                        RecordCardAdapter.i(bVar, (String) obj);
                    }
                }, new i1());
            } else {
                Record.RecordPlace place = record.getPlace();
                bVar.f7007v.setText(Record.getPlaceString(place));
                bVar.f7010y.getDrawable().setTint(this.f7003g.a().getColor(place == Record.RecordPlace.UNKNOWN ? R.color.blue_grey_700 : R.color.teal_700));
            }
        }
        if (i2 == 0 && j()) {
            bVar.M();
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public b onCreateViewHolder(ViewGroup viewGroup, int i2) {
        if (i2 == 0) {
            return new b(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_record_card_uploaded, viewGroup, false));
        }
        if (i2 != 1) {
            return null;
        }
        return new d(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_record_card, viewGroup, false));
    }

    @Override // cn.edu.pku.pkurunner.RecordList.ItemTouchHelperCallback.b
    public void onItemSwiped(int i2) {
        this.f7002f.deleteRecord(((Record) e().get(i2)).getId(), i2);
    }

    public RecordCardAdapter() {
        Boolean bool = Boolean.FALSE;
        this.f7000d = bool;
        this.f7001e = bool;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ int f(Record record, Record record2) {
        return -record.getDate().compareTo(record2.getDate());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void g(b bVar, View view) {
        this.f7002f.showRecordDetail(((Record) e().get(bVar.getAdapterPosition())).getId());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void h(b bVar, View view) {
        int adapterPosition = bVar.getAdapterPosition();
        this.f7002f.uploadVerifyRecord(((Record) e().get(adapterPosition)).getId(), adapterPosition);
    }

    @Override // cn.edu.pku.pkurunner.RecordList.ItemTouchHelperCallback.b
    public boolean canBeSwiped(int i2) {
        return !((Record) e().get(i2)).isUploaded();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return e().size();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemViewType(int i2) {
        return !((Record) e().get(i2)).isUploaded() ? 1 : 0;
    }
}
