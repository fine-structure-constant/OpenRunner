package cn.edu.pku.pkurunner.Exception;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, application = android.app.Application.class)
public class ServerExceptionTest {

    @Test
    public void setResources_enablesLocalizedMessageById() {
        ServerException.setResources(RuntimeEnvironment.getApplication().getResources());

        assertThat(ServerException.getLocalizedMessage(1)).isNotEmpty();
    }

    @Test
    public void setResources_enablesLocalizedMessageOnInstance() {
        ServerException.setResources(RuntimeEnvironment.getApplication().getResources());
        ServerException exception = new ServerException(1, "detail");

        assertThat(exception.getLocalizedMessage()).isEqualTo(ServerException.getLocalizedMessage(1));
    }
}
