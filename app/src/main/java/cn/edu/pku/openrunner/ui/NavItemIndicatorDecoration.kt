package cn.edu.pku.openrunner.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import cn.edu.pku.openrunner.R

/**
 * 抽屉菜单里选中项左侧那根竖装饰线。
 *
 * **为什么不在 drawable 里画**
 *
 * layer-list 的 `<item>` 想按固定宽度画一根 3dp 的线，只能靠 `android:width` +
 * `android:gravity`，而这两个属性是 API 23 才有的（本工程 minSdk 21，低版本上
 * 属性会被忽略，整块胶囊被线铺满）。绕开它们的办法是「底层整块胶囊染品牌色、
 * 上层底色左移 3dp」把线叠出来 —— 任何版本都成立，但线必然贴着胶囊左缘，
 * 两层之间没有第三个位置可以放间隙。
 *
 * 画在这里之后，线是一条独立的图元：
 *  - 位置、宽度、高度都是显式数值，不受 API 版本限制；
 *  - 线所在的横向区间在 item 背景里什么都没画，所以间隙是**真的透明**，
 *    不是拿背景色假装（换成非纯色抽屉背景也不会露馅）；
 *  - 胶囊的内缩量和线宽彻底解耦，各自单独调。
 *
 * 几何（见 `values/dimens.xml`）：
 * ```
 * | 10dp | 3dp 线 | 5dp 间隙 | ......胶囊...... | 10dp |
 * ```
 *
 * **怎么认选中项**
 *
 * `NavigationMenuItemView.onCreateDrawableState` 在菜单项可选中且已选中时把
 * `CHECKED_STATE_SET` 并进 drawable state（反编译 NavigationMenuItemView 确认），
 * 所以查 `state_checked` 就是查「这一行是不是当前选中项」。比去翻 NavigationView
 * 内部持有的 MenuItem 稳 —— 后者没有公开 API。
 *
 * 主题里的单选项（跟随系统 / 浅色 / 深色）也是 checkable，选中时同样会拿到
 * 胶囊与这根线，与它们已有的高亮表现一致。
 *
 * **为什么画在 onDraw 而不是 onDrawOver**
 *
 * 胶囊是 item 的背景，`onDraw` 先于子 View 绘制，所以胶囊会盖在线上。两者横向
 * 不重叠（线在 `or_nav_item_inset` 处、胶囊从 `or_nav_item_fill_inset` 开始），
 * 谁在上都行；但按压时的水波纹属于 item 背景，让它压在线上才对。
 */
class NavItemIndicatorDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val width = context.resources.getDimensionPixelSize(R.dimen.or_nav_indicator_width)
    private val height = context.resources.getDimensionPixelSize(R.dimen.or_nav_indicator_height)
    private val inset = context.resources.getDimensionPixelSize(R.dimen.or_nav_item_inset)

    /**
     * 圆头。取半宽即一根完整的胶囊，与设计系统里「一切转角都圆」一致；
     * 3dp 宽上这个差别很细，但方头会在深色底上留下两个硬角。
     */
    private val corner = width / 2f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        // 与抽屉头部那根 26dp 品牌色短横取同一个 token。不用 ?attr/colorPrimary：
        // 深色主题把它映射到 hud_primary，而抽屉自己的色板走 or_* 的 values-night 变体。
        color = ContextCompat.getColor(context, R.color.or_primary)
    }

    /** 复用同一个矩形，逐行绘制里不新建对象。 */
    private val bounds = RectF()

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val count = parent.childCount
        if (count == 0) return
        for (index in 0 until count) {
            val child = parent.getChildAt(index)
            if (!child.drawableState.contains(android.R.attr.state_checked)) continue
            val left = (child.left + inset).toFloat()
            // 线比整行矮，居中放 —— 读作「标记」，而不是「胶囊被切掉一条边」。
            val centerY = (child.top + child.bottom) / 2f
            val half = height / 2f
            bounds.set(left, centerY - half, left + width, centerY + half)
            canvas.drawRoundRect(bounds, corner, corner, paint)
        }
    }
}
