package cn.edu.pku.pkurunner.RecordList;

import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import cn.edu.pku.pkurunner.R;
import cn.edu.pku.pkurunner.RecordList.RecordCardAdapter;

public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private SwipeableItemCallback swipeListener;

    private SwipeStateListener swipeStateListener;

    private Drawable deleteIcon;

    private int deleteIconWidth;

    private int deleteIconHeight;

    private Drawable swipeBackground;

    interface SwipeStateListener {
        void onSwipeStateChanged(boolean active);
    }

    interface SwipeableItemCallback {
        boolean canBeSwiped(int index);

        void notifyItemChanged(int index);

        void onItemSwiped(int index);
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return ItemTouchHelper.Callback.makeMovementFlags(0, 4);
    }

    @Override
    public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float value, float value2, int index, boolean z2) {
        float f4;
        CardView cardView = ((RecordCardAdapter.UploadedRecordViewHolder) viewHolder).cardView;
        if (viewHolder instanceof RecordCardAdapter.RecordViewHolder) {
            View view = viewHolder.itemView;
            int bottom = view.getBottom() - view.getTop();
            this.swipeBackground.setBounds(view.getRight() + ((int) value), view.getTop(), view.getRight(), view.getBottom());
            this.swipeBackground.draw(canvas);
            int top = view.getTop();
            int index2 = this.deleteIconHeight;
            int index3 = top + ((bottom - index2) / 2);
            int i5 = (bottom - index2) / 2;
            this.deleteIcon.setBounds((view.getRight() - i5) - this.deleteIconWidth, index3, view.getRight() - i5, this.deleteIconHeight + index3);
            this.deleteIcon.draw(canvas);
            f4 = value;
        } else {
            float width = value / canvas.getWidth();
            f4 = canvas.getWidth() * 0.1f * width * (2.0f - width);
        }
        ItemTouchHelper.Callback.getDefaultUIUtil().onDraw(canvas, recyclerView, cardView, f4, value2, index, z2);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2) {
        return false;
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        ItemTouchHelper.Callback.getDefaultUIUtil().clearView(((RecordCardAdapter.UploadedRecordViewHolder) viewHolder).cardView);
    }

    @Override
    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
        return viewHolder instanceof RecordCardAdapter.RecordViewHolder ? 0.5f : 10.0f;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int index) {
        if (viewHolder != null) {
            ItemTouchHelper.Callback.getDefaultUIUtil().onSelected(((RecordCardAdapter.UploadedRecordViewHolder) viewHolder).cardView);
        }
        if (index == 0) {
            this.swipeStateListener.onSwipeStateChanged(true);
        } else {
            this.swipeStateListener.onSwipeStateChanged(false);
        }
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int index) {
        if (this.swipeListener.canBeSwiped(viewHolder.getAdapterPosition())) {
            this.swipeListener.onItemSwiped(viewHolder.getAdapterPosition());
        } else {
            this.swipeListener.notifyItemChanged(viewHolder.getAdapterPosition());
        }
    }

    public ItemTouchHelperCallback(SwipeableItemCallback swipeListener, SwipeStateListener swipeStateListener, RecordCardAdapter.ResourcesProvider resourcesProvider) {
        this.swipeListener = swipeListener;
        this.swipeStateListener = swipeStateListener;
        Drawable drawable = resourcesProvider.getResources().getDrawable(R.drawable.ic_delete_black_24dp);
        this.deleteIcon = drawable;
        drawable.setTint(resourcesProvider.getResources().getColor(R.color.white));
        this.deleteIconWidth = this.deleteIcon.getIntrinsicWidth();
        this.deleteIconHeight = this.deleteIcon.getIntrinsicHeight();
        this.swipeBackground = new ColorDrawable(resourcesProvider.getResources().getColor(R.color.primaryDark));
    }
}
