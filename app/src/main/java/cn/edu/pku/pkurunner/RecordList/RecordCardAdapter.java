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
import cn.edu.pku.pkurunner.StackTracePrintingConsumer;
import cn.edu.pku.pkurunner.Model.Record;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.RecordList.ItemTouchHelperCallback;
import cn.edu.pku.pkurunner.RecordList.RecordCardAdapter;
import cn.edu.pku.pkurunner.RecordList.RecordListContract;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class RecordCardAdapter extends RecyclerView.Adapter<RecordCardAdapter.UploadedRecordViewHolder> implements ItemTouchHelperCallback.SwipeableItemCallback {

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yy-MM-dd");

    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm");

    private ArrayList records;

    private Boolean dataValid;

    private Boolean firstItemElevationPending;

    private RecordListContract.Presenter presenter;

    private ResourcesProvider resourcesProvider;

    interface CardClickListener {
        void onClick(View view);
    }

    static class UploadedRecordViewHolder extends RecyclerView.ViewHolder {

        protected TextView distanceText;

        protected TextView durationText;

        protected TextView dateText;

        protected TextView placeText;

        protected CardClickListener cardClickListener;

        protected ImageView statusImageView;

        protected ImageView circleBackgroundView;

        protected CardView cardView;

        public View H() {
            return this.dateText;
        }

        public View I() {
            return this.distanceText;
        }

        public View J() {
            return this.statusImageView;
        }

        void L(CardClickListener cardClickListener) {
            this.cardClickListener = cardClickListener;
        }

        public /* synthetic */ void K(View view) {
            CardClickListener cardClickListener = this.cardClickListener;
            if (cardClickListener != null) {
                cardClickListener.onClick(view);
            }
        }

        void M() {
            float cardElevation = this.cardView.getCardElevation();
            float value = 4.0f * cardElevation;
            ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this.cardView, "cardElevation", cardElevation, value);
            ofFloat.setInterpolator(new LinearOutSlowInInterpolator());
            ofFloat.setDuration(250L);
            ofFloat.setStartDelay(1000L);
            ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this.cardView, "cardElevation", value, cardElevation);
            ofFloat2.setInterpolator(new FastOutSlowInInterpolator());
            ofFloat2.setDuration(250L);
            ofFloat2.setStartDelay(1000L);
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playSequentially(ofFloat, ofFloat2);
            animatorSet.start();
        }

        UploadedRecordViewHolder(View view) {
            super(view);
            this.distanceText = (TextView) view.findViewById(R.id.v_record_card_txt_distance);
            this.durationText = (TextView) view.findViewById(R.id.v_record_card_txt_time);
            this.dateText = (TextView) view.findViewById(R.id.v_record_card_txt_date);
            this.statusImageView = (ImageView) view.findViewById(R.id.v_record_card_img_status);
            this.cardView = (CardView) view.findViewById(R.id.view_card_record_fg);
            this.placeText = (TextView) view.findViewById(R.id.v_record_card_txt_place);
            this.circleBackgroundView = (ImageView) view.findViewById(R.id.v_record_card_circle_bg);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view2) {
                    RecordCardAdapter.UploadedRecordViewHolder.this.K(view2);
                }
            });
        }
    }

    interface ResourcesProvider {
        Resources getResources();
    }

    public void notifyDataInvalid() {
        this.dataValid = Boolean.FALSE;
    }

    public void notifyFirstElementElevation() {
        this.firstItemElevationPending = Boolean.TRUE;
    }

    public void setPresenter(RecordListContract.Presenter presenter, ResourcesProvider resourcesProvider) {
        if (presenter != null) {
            this.presenter = presenter;
        }
        this.resourcesProvider = resourcesProvider;
    }

    static class RecordViewHolder extends UploadedRecordViewHolder {
        RecordViewHolder(View view) {
            super(view);
        }
    }

    private ArrayList e() {
        if (!this.dataValid.booleanValue()) {
            if (Data.isValid()) {
                ArrayList<Record> records = Data.getRecords();
                this.records = records;
                Collections.sort(records, new Comparator() {
                    @Override
                    public final int compare(Object obj, Object obj2) {
                        int value;
                        value = RecordCardAdapter.f((Record) obj, (Record) obj2);
                        return value;
                    }
                });
            } else {
                this.records = new ArrayList();
            }
            this.dataValid = Boolean.TRUE;
        }
        return this.records;
    }

    public static /* synthetic */ void i(UploadedRecordViewHolder holder, String str) {
        holder.placeText.setText(str);
    }

    private boolean j() {
        if (!this.firstItemElevationPending.booleanValue()) {
            return false;
        }
        this.firstItemElevationPending = Boolean.FALSE;
        return true;
    }

    @Override
    public void onBindViewHolder(final UploadedRecordViewHolder holder, int index) {
        Record record = (Record) e().get(index);
        holder.distanceText.setText(this.resourcesProvider.getResources().getString(R.string.v_record_card_distance_format, Double.valueOf(record.getDistance() / 1000.0d)));
        Date date = record.getDate();
        String format = DATE_FORMATTER.format(date);
        SimpleDateFormat simpleDateFormat = TIME_FORMATTER;
        String format2 = simpleDateFormat.format(new Date(date.getTime() - (record.getDuration() * 1000)));
        String format3 = simpleDateFormat.format(date);
        holder.dateText.setText(format);
        holder.durationText.setText(this.resourcesProvider.getResources().getString(R.string.v_record_card_duration_format, format2, format3));
        holder.L(new CardClickListener() {
            @Override
            public final void onClick(View view) {
                RecordCardAdapter.this.g(holder, view);
            }
        });
        if (holder instanceof RecordViewHolder) {
            ((RecordViewHolder) holder).statusImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    RecordCardAdapter.this.h(holder, view);
                }
            });
        } else {
            if (record.isVerified()) {
                TextView textView = holder.distanceText;
                textView.setPaintFlags(textView.getPaintFlags() & (-17));
                holder.statusImageView.setImageResource(R.drawable.ic_done_black_24dp);
                holder.statusImageView.getDrawable().setTint(this.resourcesProvider.getResources().getColor(R.color.green_A700));
            } else {
                TextView textView2 = holder.distanceText;
                textView2.setPaintFlags(textView2.getPaintFlags() | 16);
                holder.statusImageView.setImageResource(R.drawable.ic_error_outline_black_24dp);
                holder.statusImageView.getDrawable().setTint(this.resourcesProvider.getResources().getColor(R.color.orange_A400));
            }
            if (Data.getUser().isOffline().booleanValue()) {
                Data.getRecordPlaceHintForOfflineUser(record).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        RecordCardAdapter.i(holder, (String) obj);
                    }
                }, new StackTracePrintingConsumer());
            } else {
                Record.RecordPlace place = record.getPlace();
                holder.placeText.setText(Record.getPlaceString(place));
                holder.circleBackgroundView.getDrawable().setTint(this.resourcesProvider.getResources().getColor(place == Record.RecordPlace.UNKNOWN ? R.color.blue_grey_700 : R.color.teal_700));
            }
        }
        if (index == 0 && j()) {
            holder.M();
        }
    }

    @Override
    public UploadedRecordViewHolder onCreateViewHolder(ViewGroup viewGroup, int index) {
        if (index == 0) {
            return new UploadedRecordViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_record_card_uploaded, viewGroup, false));
        }
        if (index != 1) {
            return null;
        }
        return new RecordViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_record_card, viewGroup, false));
    }

    @Override
    public void onItemSwiped(int index) {
        this.presenter.deleteRecord(((Record) e().get(index)).getId(), index);
    }

    public RecordCardAdapter() {
        Boolean bool = Boolean.FALSE;
        this.dataValid = bool;
        this.firstItemElevationPending = bool;
    }

    public static /* synthetic */ int f(Record record, Record record2) {
        return -record.getDate().compareTo(record2.getDate());
    }

    public /* synthetic */ void g(UploadedRecordViewHolder holder, View view) {
        this.presenter.showRecordDetail(((Record) e().get(holder.getAdapterPosition())).getId());
    }

    public /* synthetic */ void h(UploadedRecordViewHolder holder, View view) {
        int adapterPosition = holder.getAdapterPosition();
        this.presenter.uploadVerifyRecord(((Record) e().get(adapterPosition)).getId(), adapterPosition);
    }

    @Override
    public boolean canBeSwiped(int index) {
        return !((Record) e().get(index)).isUploaded();
    }

    @Override
    public int getItemCount() {
        return e().size();
    }

    @Override
    public int getItemViewType(int index) {
        return !((Record) e().get(index)).isUploaded() ? 1 : 0;
    }
}
