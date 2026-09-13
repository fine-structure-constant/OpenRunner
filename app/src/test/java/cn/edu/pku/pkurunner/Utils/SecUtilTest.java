package cn.edu.pku.pkurunner.Utils;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Date;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, application = android.app.Application.class)
public class SecUtilTest {

    private static final long FIXED_MILLIS = 1757761200000L;
    private static final String USER_ID = "1234567890";
    private static final String OTHER_USER_ID = "1234567891";

    @Test
    public void generateCheckField_isStable() {
        Date d = new Date(FIXED_MILLIS);
        assertThat(SecUtil.generateCheckField(USER_ID, d)).isNotNull();
        assertThat(SecUtil.generateCheckField(USER_ID, d))
                .isEqualTo(SecUtil.generateCheckField(USER_ID, d));
    }

    @Test
    public void generateCheckField_differsByPayload() {
        Date d = new Date(FIXED_MILLIS);
        assertThat(SecUtil.generateCheckField(USER_ID, d))
                .isNotEqualTo(SecUtil.generateCheckField(OTHER_USER_ID, d));
    }

    @Test
    public void generateCheckField_differsByDate() {
        assertThat(SecUtil.generateCheckField(USER_ID, new Date(FIXED_MILLIS)))
                .isNotEqualTo(SecUtil.generateCheckField(USER_ID, new Date(FIXED_MILLIS + 1000L)));
    }

    @Test
    public void generateCheckField_returnsNullWhenKeyIsNotAValidAesLength() {
        assertThat(SecUtil.generateCheckField("short", new Date(FIXED_MILLIS))).isNull();
    }

    @Test
    public void verifyCheckField_roundTrip() {
        Date d = new Date(FIXED_MILLIS);
        String field = SecUtil.generateCheckField(USER_ID, d);
        assertThat(SecUtil.verifyCheckField(USER_ID, d, field)).isTrue();
    }

    @Test
    public void verifyCheckField_rejectsTampered() {
        Date d = new Date(FIXED_MILLIS);
        String field = SecUtil.generateCheckField(USER_ID, d);
        assertThat(SecUtil.verifyCheckField(OTHER_USER_ID, d, field)).isFalse();
    }

    @Test
    public void verifyCheckField_rejectsNullField() {
        assertThat(SecUtil.verifyCheckField(USER_ID, new Date(FIXED_MILLIS), null)).isFalse();
    }

    @Test
    public void getAbstract_isStable() {
        String a = SecUtil.getAbstract(USER_ID, "token-abc");
        assertThat(a).isEqualTo(SecUtil.getAbstract(USER_ID, "token-abc"));
        assertThat(a).hasLength(32);
    }

    @Test
    public void getAbstract_differsByInputs() {
        assertThat(SecUtil.getAbstract(USER_ID, "token-abc"))
                .isNotEqualTo(SecUtil.getAbstract(USER_ID, "token-xyz"));
    }
}
