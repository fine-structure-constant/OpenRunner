package cn.edu.pku.pkurunner;

import static com.google.common.truth.Truth.assertThat;

import cn.edu.pku.pkurunner.Network.Model.UserStatus;
import org.junit.Test;

public class DataTest {

    @Test
    public void setUserStatus_storesTheInstance() {
        UserStatus status = new UserStatus();

        Data.setUserStatus(status);

        assertThat(Data.getUserStatus()).isSameInstanceAs(status);
    }

    @Test
    public void refreshUserStatusLambdaStoresTheInstance() {
        UserStatus status = new UserStatus();

        Data.W(status);

        assertThat(Data.getUserStatus()).isSameInstanceAs(status);
    }
}
