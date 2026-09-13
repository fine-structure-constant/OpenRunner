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

    /* renamed from: d, reason: collision with root package name */
    private b f6991d;

    /* renamed from: e, reason: collision with root package name */
    private a f6992e;

    /* renamed from: f, reason: collision with root package name */
    private Drawable f6993f;

    /* renamed from: g, reason: collision with root package name */
    private int f6994g;

    /* renamed from: h, reason: collision with root package name */
    private int f6995h;

    /* renamed from: i, reason: collision with root package name */
    private Drawable f6996i;

    interface a {
        void a(boolean z2);
    }

    interface b {
        boolean canBeSwiped(int i2);

        void notifyItemChanged(int i2);

        void onItemSwiped(int i2);
    }

    @Override // androidx.recyclerview.widget.ItemTouchHelper.Callback
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return ItemTouchHelper.Callback.makeMovementFlags(0, 4);
    }

    @Override // androidx.recyclerview.widget.ItemTouchHelper.Callback
    public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float f2, float f3, int i2, boolean z2) {
        float f4;
        CardView cardView = ((RecordCardAdapter.b) viewHolder).f7011z;
        if (viewHolder instanceof RecordCardAdapter.d) {
            View view = viewHolder.itemView;
            int bottom = view.getBottom() - view.getTop();
            this.f6996i.setBounds(view.getRight() + ((int) f2), view.getTop(), view.getRight(), view.getBottom());
            this.f6996i.draw(canvas);
            int top = view.getTop();
            int i3 = this.f6995h;
            int i4 = top + ((bottom - i3) / 2);
            int i5 = (bottom - i3) / 2;
            this.f6993f.setBounds((view.getRight() - i5) - this.f6994g, i4, view.getRight() - i5, this.f6995h + i4);
            this.f6993f.draw(canvas);
            f4 = f2;
        } else {
            float width = f2 / canvas.getWidth();
            f4 = canvas.getWidth() * 0.1f * width * (2.0f - width);
        }
        ItemTouchHelper.Callback.getDefaultUIUtil().onDraw(canvas, recyclerView, cardView, f4, f3, i2, z2);
    }

    @Override // androidx.recyclerview.widget.ItemTouchHelper.Callback
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2) {
        return false;
    }

    @Override // androidx.recyclerview.widget.ItemTouchHelper.Callback
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        ItemTouchHelper.Callback.getDefaultUIUtil().clearView(((RecordCardAdapter.b) viewHolder).f7011z);
    }

    @Override // androidx.recyclerview.widget.ItemTouchHelper.Callback
    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
        return viewHolder instanceof RecordCardAdapter.d ? 0.5f : 10.0f;
    }

    @Override // androidx.recyclerview.widget.ItemTouchHelper.Callback
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int i2) {
        if (viewHolder != null) {
            ItemTouchHelper.Callback.getDefaultUIUtil().onSelected(((RecordCardAdapter.b) viewHolder).f7011z);
        }
        if (i2 == 0) {
            this.f6992e.a(true);
        } else {
            this.f6992e.a(false);
        }
    }

    @Override // androidx.recyclerview.widget.ItemTouchHelper.Callback
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int i2) {
        if (this.f6991d.canBeSwiped(viewHolder.getAdapterPosition())) {
            this.f6991d.onItemSwiped(viewHolder.getAdapterPosition());
        } else {
            this.f6991d.notifyItemChanged(viewHolder.getAdapterPosition());
        }
    }

    public ItemTouchHelperCallback(b bVar, a aVar, RecordCardAdapter.c cVar) {
        this.f6991d = bVar;
        this.f6992e = aVar;
        Drawable drawable = cVar.a().getDrawable(R.drawable.ic_delete_black_24dp);
        this.f6993f = drawable;
        drawable.setTint(cVar.a().getColor(R.color.white));
        this.f6994g = this.f6993f.getIntrinsicWidth();
        this.f6995h = this.f6993f.getIntrinsicHeight();
        this.f6996i = new ColorDrawable(cVar.a().getColor(R.color.primaryDark));
    }
}
