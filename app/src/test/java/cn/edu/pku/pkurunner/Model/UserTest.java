package cn.edu.pku.pkurunner.Model;

import static com.google.common.truth.Truth.assertThat;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, application = android.app.Application.class)
public class UserTest {

    private static Record recordWithId(int id) {
        Record record = new Record("u", 100, 20, new Date(0L), 30, "check");
        record.setId(id);
        return record;
    }

    @Test
    public void createOfflineUser_marksOfflineAndGeneratesUniqueIdentity() {
        User first = User.createOfflineUser(User.FEMALE, "张三");
        User second = User.createOfflineUser(User.FEMALE, "张三");

        assertThat(first.isOffline()).isTrue();
        assertThat(first.getDepartment()).isEqualTo("离线");
        assertThat(first.getName()).isEqualTo("张三");
        assertThat(first.getGender()).isEqualTo(User.FEMALE);
        assertThat(first.isPESpecialty()).isFalse();
        assertThat(first.getId()).isNotEmpty();
        assertThat(first.getToken()).isNotEmpty();
        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(first.getToken()).isNotEqualTo(second.getToken());
    }

    @Test
    public void addRecord_returnsIdAndStoresRecord() {
        User user = new User();
        Record record = recordWithId(5);

        assertThat(user.addRecord(record)).isEqualTo(5);
        assertThat(user.getRecords()).containsExactly(record);
    }

    @Test
    public void deleteRecord_removesOnlyWhenPresent() {
        User user = new User();
        Record kept = recordWithId(1);
        Record removed = recordWithId(2);
        Record stranger = recordWithId(3);
        user.addRecord(kept);
        user.addRecord(removed);

        assertThat(user.deleteRecord(removed)).isTrue();
        assertThat(user.getRecords()).containsExactly(kept);
        assertThat(user.deleteRecord(stranger)).isFalse();
        assertThat(user.getRecords()).containsExactly(kept);
    }

    @Test
    public void getRecordById_findsStoredRecordAndNullWhenMissing() {
        User user = new User();
        Record record = recordWithId(9);
        user.addRecord(record);

        assertThat(user.getRecordById(9)).isSameInstanceAs(record);
        assertThat(user.getRecordById(404)).isNull();
    }

    @Test
    public void addGymRecord_returnsIdAndStoresRecord() {
        User user = new User();
        GymRecord gymRecord = new GymRecord("u", 1);
        gymRecord.setId(4);

        assertThat(user.addGymRecord(gymRecord)).isEqualTo(4);
        assertThat(user.getGymRecords()).containsExactly(gymRecord);
    }

    @Test
    public void deleteGymRecordById_removesWhenNotUploaded() {
        User user = new User();
        GymRecord gymRecord = new GymRecord("u", 1);
        gymRecord.setId(6);
        gymRecord.setUploaded(Boolean.FALSE);
        user.addGymRecord(gymRecord);

        assertThat(user.deleteGymRecordById(6)).isTrue();
        assertThat(user.getGymRecords()).isEmpty();
    }

    @Test
    public void deleteGymRecordById_refusesWhenUploaded() {
        User user = new User();
        GymRecord gymRecord = new GymRecord("u", 1);
        gymRecord.setId(6);
        gymRecord.setUploaded(Boolean.TRUE);
        user.addGymRecord(gymRecord);

        assertThat(user.deleteGymRecordById(6)).isFalse();
        assertThat(user.getGymRecords()).containsExactly(gymRecord);
    }

    @Test
    public void deleteGymRecordById_returnsFalseWhenAbsent() {
        assertThat(new User().deleteGymRecordById(123)).isFalse();
    }

    @Test
    public void getGymRecordById_findsStoredRecordAndNullWhenMissing() {
        User user = new User();
        GymRecord gymRecord = new GymRecord("u", 2);
        gymRecord.setId(8);
        user.addGymRecord(gymRecord);

        assertThat(user.getGymRecordById(8)).isSameInstanceAs(gymRecord);
        assertThat(user.getGymRecordById(404)).isNull();
    }

    @Test
    public void newUser_startsWithEmptyCollections() {
        User user = new User();

        assertThat(user.getRecords()).isEmpty();
        assertThat(user.getGymRecords()).isEmpty();
        assertThat(user.isOffline()).isFalse();
    }
}
