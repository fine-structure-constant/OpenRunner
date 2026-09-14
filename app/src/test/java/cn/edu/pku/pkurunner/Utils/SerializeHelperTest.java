package cn.edu.pku.pkurunner.Utils;

import static com.google.common.truth.Truth.assertThat;

import java.io.Serializable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, application = android.app.Application.class)
public class SerializeHelperTest {

    static class Payload implements Serializable {
        final String name;
        final int count;

        Payload(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    @Test
    public void objectToString_thenStringToObject_roundTripsString() {
        String encoded = SerializeHelper.objectToString("hello");
        assertThat(encoded).isNotNull();
        assertThat(SerializeHelper.stringToObject(encoded).blockingFirst()).isEqualTo("hello");
    }

    @Test
    public void objectToString_thenStringToObject_roundTripsSerializable() {
        String encoded = SerializeHelper.objectToString(new Payload("run", 12));
        Payload restored = (Payload) SerializeHelper.stringToObject(encoded).blockingFirst();
        assertThat(restored.name).isEqualTo("run");
        assertThat(restored.count).isEqualTo(12);
    }

    @Test
    public void stringToObject_onGarbage_emitsErrorInsteadOfThrowing() {
        Throwable error = null;
        try {
            SerializeHelper.stringToObject("!!!not-base64!!!").blockingFirst();
        } catch (Throwable thrown) {
            error = thrown;
        }
        assertThat(error).isNotNull();
    }
}
