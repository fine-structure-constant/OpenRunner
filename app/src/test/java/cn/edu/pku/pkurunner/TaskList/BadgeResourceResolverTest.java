package cn.edu.pku.pkurunner.TaskList;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, application = android.app.Application.class)
public class BadgeResourceResolverTest {

    @Test
    public void resolve_knownSeries_returnsRealDrawable() throws Exception {
        int resource = BadgeResourceResolver.resolve("daily");
        assertThat(resource).isNotEqualTo(0);
        assertThat(resource).isNotEqualTo(BadgeResourceResolver.NULL_RESOURCE);
    }

    @Test
    public void resolve_nullSeries_throwsUnknownBadgeException() {
        assertUnknownBadge(null);
    }

    @Test
    public void resolve_unknownSeries_throwsUnknownBadgeException() {
        assertUnknownBadge("no-such-series");
    }

    private void assertUnknownBadge(String series) {
        try {
            BadgeResourceResolver.resolve(series);
            fail("expected UnknownBadgeException");
        } catch (BadgeResourceResolver.UnknownBadgeException expected) {
            assertThat(expected).isInstanceOf(Exception.class);
        }
    }
}
