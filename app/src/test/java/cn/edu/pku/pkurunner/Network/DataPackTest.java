package cn.edu.pku.pkurunner.Network;

import static com.google.common.truth.Truth.assertThat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import org.junit.Test;

public class DataPackTest {

    static class Payload {
        String name;
        int count;
    }

    private final Gson gson = new Gson();

    private DataPack<Payload> parse(String json) {
        return gson.fromJson(json, new TypeToken<DataPack<Payload>>() {
        }.getType());
    }

    @Test
    public void deserializesEnvelopeFields() {
        DataPack<Payload> pack = parse("{\"success\":true,\"code\":0,\"message\":\"ok\",\"version\":3}");
        assertThat(pack.isSuccess()).isTrue();
        assertThat(pack.getCode()).isEqualTo(0);
        assertThat(pack.getMessage()).isEqualTo("ok");
        assertThat(pack.getVersion()).isEqualTo(3);
    }

    @Test
    public void deserializesTypedData() {
        DataPack<Payload> pack = parse("{\"success\":true,\"data\":{\"name\":\"run\",\"count\":7}}");
        assertThat(pack.getData()).isNotNull();
        assertThat(pack.getData().name).isEqualTo("run");
        assertThat(pack.getData().count).isEqualTo(7);
    }

    @Test
    public void isSuccessIsFalseWhenFieldAbsent() {
        assertThat(parse("{\"code\":0}").isSuccess()).isFalse();
    }

    @Test
    public void numericFieldsDefaultToZeroWhenAbsent() {
        DataPack<Payload> pack = parse("{}");
        assertThat(pack.getCode()).isEqualTo(0);
        assertThat(pack.getVersion()).isEqualTo(0);
        assertThat(pack.getData()).isNull();
        assertThat(pack.getMessage()).isNull();
    }

    @Test
    public void errorEnvelopeKeepsServerCodeAndMessage() {
        DataPack<Payload> pack = parse("{\"success\":false,\"code\":15,\"message\":\"版本过旧\"}");
        assertThat(pack.isSuccess()).isFalse();
        assertThat(pack.getCode()).isEqualTo(ErrorCode.VERSION_TOO_OLD);
        assertThat(pack.getMessage()).isEqualTo("版本过旧");
    }

    @Test
    public void deserializesListPayload() {
        DataPack<List<Payload>> pack = gson.fromJson(
                "{\"success\":true,\"data\":[{\"name\":\"a\",\"count\":1},{\"name\":\"b\",\"count\":2}]}",
                new TypeToken<DataPack<List<Payload>>>() {
                }.getType());
        assertThat(pack.getData()).hasSize(2);
        assertThat(pack.getData().get(1).name).isEqualTo("b");
    }
}
