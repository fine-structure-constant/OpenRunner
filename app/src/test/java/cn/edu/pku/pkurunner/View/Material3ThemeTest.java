package cn.edu.pku.pkurunner.View;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.view.ContextThemeWrapper;
import cn.edu.pku.pkurunner.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, application = android.app.Application.class)
public class Material3ThemeTest {

    private Context themed() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_OpenRunner);
    }

    private int attr(int attrId) {
        TypedValue value = new TypedValue();
        assertThat(themed().getTheme().resolveAttribute(attrId, value, true)).isTrue();
        return value.data;
    }

    private View inflate(int layoutId) {
        return LayoutInflater.from(themed()).inflate(layoutId, null);
    }

    private static View find(View root, int id) {
        return root.findViewById(id);
    }

    @Test
    public void lightThemeResolvesBrandTokens() {
        assertThat(attr(com.google.android.material.R.attr.colorPrimary)).isEqualTo(0xFFF1454D);
        assertThat(attr(com.google.android.material.R.attr.colorOnPrimary)).isEqualTo(0xFFFFFFFF);
        assertThat(attr(com.google.android.material.R.attr.colorSurface)).isEqualTo(0xFFFEFEFE);
        assertThat(attr(com.google.android.material.R.attr.colorOnSurface)).isEqualTo(0xFF12151B);
        assertThat(attr(com.google.android.material.R.attr.colorOnSurfaceVariant)).isEqualTo(0xFF6B6E78);
        assertThat(attr(com.google.android.material.R.attr.colorSurfaceContainer)).isEqualTo(0xFFF3F5F9);
    }

    @Test
    @Config(sdk = 33, application = android.app.Application.class, qualifiers = "night")
    public void darkThemeResolvesBrandTokens() {
        assertThat(attr(com.google.android.material.R.attr.colorPrimary)).isEqualTo(0xFFF34C4F);
        assertThat(attr(com.google.android.material.R.attr.colorSurface)).isEqualTo(0xFF12151B);
        assertThat(attr(com.google.android.material.R.attr.colorOnSurface)).isEqualTo(0xFFE6E8EE);
        assertThat(attr(com.google.android.material.R.attr.colorOnSurfaceVariant)).isEqualTo(0xFF8E95A4);
        assertThat(attr(com.google.android.material.R.attr.colorSurfaceContainer)).isEqualTo(0xFF1A1C24);
    }

    @Test
    public void baseThemeFamilyAllResolveToMaterial3() {
        Context light = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.BaseTheme_Light);
        Context dark = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.BaseTheme_Dark);
        TypedValue value = new TypedValue();
        assertThat(light.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, value, true)).isTrue();
        assertThat(value.data).isEqualTo(0xFFF1454D);
        assertThat(dark.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, value, true)).isTrue();
        assertThat(value.data).isEqualTo(0xFFF1454D);
    }

    @Test
    public void mainLayoutExposesAppBarHeroAndFab() {
        View root = inflate(R.layout.view_main);

        assertThat(find(root, R.id.v_main_appbar)).isNotNull();
        assertThat(find(root, R.id.v_main_toolbar)).isNotNull();
        assertThat(find(root, R.id.v_main_frame)).isNotNull();
        assertThat(find(root, R.id.v_main_status_container)).isNotNull();
        assertThat(find(root, R.id.v_main_fab_switch)).isInstanceOf(RunningFabView.class);
        assertThat(find(root, R.id.v_status_progress)).isInstanceOf(SimpleProgressView.class);
    }

    @Test
    public void heroCardRendersValueUnitCaptionAndPercent() {
        View root = inflate(R.layout.view_record_user_status);
        SimpleProgressView hero = (SimpleProgressView) find(root, R.id.v_status_progress);

        assertThat(hero).isInstanceOf(MaterialCardView.class);
        hero.setActiveMode(true);
        hero.setCollapseMode(false);
        hero.setMainValue("0.00");
        hero.setMainUnit("/ 85 km");
        hero.setMainCaption("完成 0 次");
        hero.setMainProgress(0.0f);

        assertThat(text(hero, R.id.v_progress_txt_distance)).isEqualTo("0.00");
        assertThat(text(hero, R.id.v_progress_txt_unit)).isEqualTo("/ 85 km");
        assertThat(text(hero, R.id.v_progress_txt_caption)).isEqualTo("完成 0 次");
        assertThat(text(hero, R.id.v_progress_txt_percent)).isEqualTo("0%");
        assertThat(find(hero, R.id.v_progress_progress_distance)).isInstanceOf(LinearProgressIndicator.class);
    }

    @Test
    public void heroProgressIsClampedToPercentRange() {
        SimpleProgressView hero = (SimpleProgressView) find(inflate(R.layout.view_record_user_status), R.id.v_status_progress);

        hero.setMainProgress(1.4f);
        assertThat(((LinearProgressIndicator) find(hero, R.id.v_progress_progress_distance)).getProgress()).isEqualTo(100);
        assertThat(text(hero, R.id.v_progress_txt_percent)).isEqualTo("100%");

        hero.setMainProgress(-0.2f);
        assertThat(((LinearProgressIndicator) find(hero, R.id.v_progress_progress_distance)).getProgress()).isEqualTo(0);
    }

    @Test
    public void collapseModeHidesProgressAndDayRows() {
        SimpleProgressView hero = (SimpleProgressView) find(inflate(R.layout.view_record_user_status), R.id.v_status_progress);

        hero.setActiveMode(true);
        hero.setCollapseMode(false);
        assertThat(find(hero, R.id.v_progress_progress_group).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(find(hero, R.id.v_progress_day_group).getVisibility()).isEqualTo(View.VISIBLE);

        hero.setCollapseMode(true);
        assertThat(find(hero, R.id.v_progress_progress_group).getVisibility()).isEqualTo(View.GONE);
        assertThat(find(hero, R.id.v_progress_day_group).getVisibility()).isEqualTo(View.GONE);

        hero.setActiveMode(false);
        assertThat(find(hero, R.id.v_progress_sleeping_group).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void recordListExposesSectionHeaderAndList() {
        View root = inflate(R.layout.fragment_recordlist);

        assertThat(find(root, R.id.f_recordlist_swipeRefreshLayout)).isNotNull();
        assertThat(find(root, R.id.f_recordlist_recyclerview)).isNotNull();
        assertThat(find(root, R.id.f_recordlist_txt_section)).isNotNull();
        assertThat(find(root, R.id.f_recordlist_txt_section_action)).isNotNull();
    }

    @Test
    public void uploadedRecordCardExposesEveryIdTheAdapterBinds() {
        View root = inflate(R.layout.view_record_card_uploaded);

        assertThat(find(root, R.id.view_card_record_fg)).isInstanceOf(MaterialCardView.class);
        assertThat(find(root, R.id.v_record_card_circle_bg)).isNotNull();
        assertThat(find(root, R.id.v_record_card_txt_place)).isNotNull();
        assertThat(find(root, R.id.v_record_card_txt_distance)).isNotNull();
        assertThat(find(root, R.id.v_record_card_txt_time)).isNotNull();
        assertThat(find(root, R.id.v_record_card_txt_date)).isNotNull();
        assertThat(find(root, R.id.v_record_card_img_status)).isNotNull();
        assertThat(find(root, R.id.v_record_card_img_chevron)).isNotNull();
    }

    @Test
    public void localRecordCardExposesEveryIdTheAdapterBinds() {
        View root = inflate(R.layout.view_record_card);

        assertThat(find(root, R.id.view_card_record_fg)).isInstanceOf(MaterialCardView.class);
        assertThat(find(root, R.id.v_record_card_img_icon_bg)).isNotNull();
        assertThat(find(root, R.id.v_record_card_txt_distance)).isNotNull();
        assertThat(find(root, R.id.v_record_card_txt_time)).isNotNull();
        assertThat(find(root, R.id.v_record_card_txt_date)).isNotNull();
        assertThat(find(root, R.id.v_record_card_img_status)).isNotNull();
        assertThat(find(root, R.id.v_record_card_img_chevron)).isNotNull();
    }

    @Test
    public void runningFabInflatesWithLabelAndIcon() {
        View fab = find(inflate(R.layout.view_main), R.id.v_main_fab_switch);

        assertThat(((ViewGroup) fab).getChildCount()).isEqualTo(1);
        TextView label = findFirstTextView((ViewGroup) fab);
        assertThat(label).isNotNull();
        assertThat(label.getText().toString()).isEqualTo(RuntimeEnvironment.getApplication().getString(R.string.or_start_running));
    }

    @Test
    public void runningFabHideAndShowToggleVisibility() {
        RunningFabView fab = (RunningFabView) find(inflate(R.layout.view_main), R.id.v_main_fab_switch);

        fab.hide();
        assertThat(fab.isHidden()).isTrue();
        fab.show();
        assertThat(fab.isHidden()).isFalse();
        assertThat(fab.getVisibility()).isEqualTo(View.VISIBLE);
    }

    private static String text(View parent, int id) {
        return ((TextView) find(parent, id)).getText().toString();
    }

    // ---------------------------------------------------------------------------------------
    // Regression: the login screen used to crash on "登录" because two things assumed the host
    // activity was themed with Material.
    // ---------------------------------------------------------------------------------------

    /**
     * {@code IAAA_Authen} is themed with {@code AppTheme.NoActionBar}. If that theme is not a
     * Material descendant, {@code MaterialAlertDialogBuilder} throws in its constructor and
     * {@code MaterialCardView} throws while inflating -- both before any network call is made.
     */
    @Test
    public void appThemeNoActionBarIsMaterialCompatible() {
        Context host = new ContextThemeWrapper(
                RuntimeEnvironment.getApplication(), R.style.AppTheme_NoActionBar);
        // Throws IllegalArgumentException if the theme is not a Theme.MaterialComponents descendant.
        new MaterialAlertDialogBuilder(host).setTitle(R.string.error).setMessage("boom");
        // Throws while inflating if MaterialCardView cannot resolve a Material theme.
        LayoutInflater.from(host).inflate(R.layout.dialog_loading, null);
    }

    /**
     * The loading dialog must inflate against its own theme, not the caller's. {@code IAAA_Authen}
     * passes a non-Material activity context.
     */
    @Test
    public void loadingDialogSurvivesANonMaterialHostContext() {
        Context host = new ContextThemeWrapper(
                RuntimeEnvironment.getApplication(), R.style.AppTheme_NoActionBar);
        OrLoadingDialog dialog = new OrLoadingDialog(host);
        View content = dialog.findViewById(android.R.id.content);
        assertThat(find(content, R.id.d_loading_indicator)).isNotNull();
        assertThat(find(content, R.id.d_loading_txt_message)).isNotNull();
    }

    /**
     * The loading dialog theme must inherit the app theme rather than sit on a bare
     * {@code Theme.Material3.DayNight.Dialog.Alert}. A bare dialog theme leaves
     * {@code materialCardViewStyle} at Material's default, whose {@code android:stateListAnimator}
     * ({@code @animator/m3_card_state_list_anim}) references theme attributes that
     * {@code AnimatorInflater} cannot resolve, which blows up with NumberFormatException on a real
     * device.
     *
     * <p>Note: Robolectric cannot resolve {@code ?attr} references inside animator XML at all, so
     * the animator itself is not loaded here -- only the theme wiring that keeps it off.
     */
    @Test
    public void loadingDialogThemeInheritsTheAppCardStyle() {
        Context host = new ContextThemeWrapper(
                RuntimeEnvironment.getApplication(), R.style.Theme_OpenRunner_LoadingDialog);
        TypedValue value = new TypedValue();
        assertThat(host.getTheme().resolveAttribute(
                com.google.android.material.R.attr.materialCardViewStyle, value, true)).isTrue();
        assertThat(value.resourceId).isEqualTo(R.style.Widget_OpenRunner_DefaultCard);
    }

    private static TextView findFirstTextView(ViewGroup group) {        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                return (TextView) child;
            }
            if (child instanceof ViewGroup) {
                TextView nested = findFirstTextView((ViewGroup) child);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }
}
