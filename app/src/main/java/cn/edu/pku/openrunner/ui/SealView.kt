package cn.edu.pku.openrunner.ui

import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import androidx.appcompat.widget.AppCompatTextView

/**
 * 页面印章。除了长得像印章，入场时也盖一下。
 *
 * 做成自定义 View 而不是「每个 Fragment 各写一遍」：印章一共出现在八个布局里
 * （六个页面 + 抽屉头部 + 登录页），写进 View 之后布局里只要把 `TextView` 换成
 * `SealView`，入场就自动有了，新增页面也不会漏。
 *
 * **盖章的三段**
 *
 * ```
 *  1.5×            ← 起手：放大、透明，像印章举在空中
 *    ↓  压下（约 130ms）
 *  0.94×           ← 落纸：略微过冲，这一下是「盖」的实感
 *    ↓  回弹（约 110ms）
 *  1.0×            ← 停稳
 * ```
 *
 * 同时叠一个极轻的倾角（[STAMP_TILT_DEGREES]）在落纸前收回 —— 手盖章不会端得
 * 笔直，这一点偏斜是「盖章」与「放大淡入」的分界。不想要就把它改成 `0f`。
 *
 * **为什么有 [STAMP_START_DELAY_MILLIS]**
 *
 * 页面转场是 fade through，进入动画自带 90ms 的 startOffset（见
 * `anim/or_page_enter.xml`）。那 90ms 里本页的 alpha 还是 0，盖章最用力的
 * 一段会白演。所以把盖章推迟同样的 90ms，正好接在页面开始显形的那一刻。
 * 没有转场的页面（冷启动首屏、地图页）多等 90ms，察觉不到。
 *
 * 注意延时期间必须先把初始状态落下去（见 [playStamp] 开头），否则印章会先以
 * 正常样子显示 90ms，再猛地跳成 1.5×，比不做动画还难看。
 *
 * **每枚只盖一次**（见 [stamped]）：抽屉头部那枚会随列表项被回收复用，不设守卫
 * 就会在拉开抽屉后的某个随机时刻重播。页面上的印章不受影响 —— 导航走 `replace()`，
 * 每次进入页面都是新视图、新实例，照旧每进一次盖一次。
 *
 * **动画被跳过时不能留下坏状态**
 *
 * 系统关掉动画（无障碍里的「移除动画」）时 ValueAnimator 的 duration 与
 * startDelay 都会被缩放成 0，动画直接落到终值 —— 终值是 `scale 1 / alpha 1`，
 * 也就是印章的正常样子，所以不需要额外分支。中途被 detach 则显式复位，
 * 见 [cancelStamp]。
 */
class SealView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var stampAnimator: ValueAnimator? = null

    /**
     * 这一枚是否已经盖过章。
     *
     * 只认「实例」而不是「每次 attach」，是因为抽屉头部那枚印章会随列表项一起被
     * 回收再复用：Material 把头部放进 NavigationMenuPresenter 里一个**共享的**
     * headerLayout，由它充当那个列表项的 ViewHolder（反编译
     * NavigationMenuPresenter$NavigationMenuAdapter 确认）。菜单一旦滚动到头部
     * 被回收、再经 onCreateViewHolder 复用同一个容器，头部里的印章就会 detach
     * 又 attach 一次 —— 不设守卫的话，用户会在拉开抽屉后某个随机时刻看到它
     * 突然盖一下。
     *
     * 页面上的印章不受影响：导航走 replace()，每次进入页面都是新视图、新实例，
     * 所以照旧每进一次盖一次。
     */
    private var stamped = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 布局预览器里也会 attach，那里不该跑动画。
        if (isInEditMode || stamped) return
        stamped = true
        playStamp()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelStamp()
    }

    private fun playStamp() {
        cancelStamp()
        // 先落初始状态：startDelay 期间动画还没开始写值，不先摆好的话
        // 印章会以正常样子亮 90ms 再跳一下。
        scaleX = STAMP_START_SCALE
        scaleY = STAMP_START_SCALE
        rotation = STAMP_TILT_DEGREES
        alpha = 0f

        // 分段缓动交给关键帧，外层用线性，各段的时间分配才准。
        val press = PathInterpolator(0.2f, 0f, 0.3f, 1f)
        val settle = PathInterpolator(0.25f, 0f, 0.4f, 1f)

        val scaleXHolder = PropertyValuesHolder.ofKeyframe(
            "scaleX",
            Keyframe.ofFloat(0f, STAMP_START_SCALE),
            Keyframe.ofFloat(PRESS_FRACTION, STAMP_PRESS_SCALE).apply { interpolator = press },
            Keyframe.ofFloat(1f, 1f).apply { interpolator = settle }
        )
        val scaleYHolder = PropertyValuesHolder.ofKeyframe(
            "scaleY",
            Keyframe.ofFloat(0f, STAMP_START_SCALE),
            Keyframe.ofFloat(PRESS_FRACTION, STAMP_PRESS_SCALE).apply { interpolator = press },
            Keyframe.ofFloat(1f, 1f).apply { interpolator = settle }
        )
        // 墨色比尺寸早一点显形：落纸那一下墨已经压上了，不是边压边浮出来。
        val alphaHolder = PropertyValuesHolder.ofKeyframe(
            "alpha",
            Keyframe.ofFloat(0f, 0f),
            Keyframe.ofFloat(INK_FRACTION, 1f),
            Keyframe.ofFloat(1f, 1f)
        )
        // 倾角在落纸前就收回，回弹阶段已经是正的。
        val rotationHolder = PropertyValuesHolder.ofKeyframe(
            "rotation",
            Keyframe.ofFloat(0f, STAMP_TILT_DEGREES),
            Keyframe.ofFloat(PRESS_FRACTION, 0f),
            Keyframe.ofFloat(1f, 0f)
        )

        stampAnimator = ObjectAnimator.ofPropertyValuesHolder(
            this, scaleXHolder, scaleYHolder, alphaHolder, rotationHolder
        ).apply {
            duration = STAMP_DURATION_MILLIS
            startDelay = STAMP_START_DELAY_MILLIS
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun cancelStamp() {
        stampAnimator?.cancel()
        stampAnimator = null
        // 复位到「盖完了」的样子。动画被 detach 打断时留下的可能是 1.5×/透明。
        scaleX = 1f
        scaleY = 1f
        rotation = 0f
        alpha = 1f
    }

    private companion object {
        const val STAMP_DURATION_MILLIS = 240L

        /** 见类注释：与 `anim/or_page_enter.xml` 的 startOffset 对齐。 */
        const val STAMP_START_DELAY_MILLIS = 90L

        const val STAMP_START_SCALE = 1.5f

        /** 落纸时的过冲。1.0 就成了普通缩放，看不出「盖」。 */
        const val STAMP_PRESS_SCALE = 0.94f

        /** 过冲出现在整段动画的这个位置，剩下的是回弹。 */
        const val PRESS_FRACTION = 0.55f

        /** 墨色显形的位置，比落纸早。 */
        const val INK_FRACTION = 0.3f

        /** 起手时的一点偏斜。改成 0f 就退化成纯「压下」。 */
        const val STAMP_TILT_DEGREES = -3f
    }
}
