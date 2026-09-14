package cn.edu.pku.pkurunner.Photo;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, application = android.app.Application.class)
public class PhotoFileTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void getPhotoDir_usesPhotoSubDirectory() {
        File root = new File(folder.getRoot(), "pictures");
        File dir = PhotoFile.getPhotoDir(root);
        assertThat(dir.getName()).isEqualTo("photo");
        assertThat(dir.getParentFile()).isEqualTo(root);
    }

    @Test
    public void getCompressedPhotoDir_usesCompressedSubDirectory() {
        File root = new File(folder.getRoot(), "pictures");
        File dir = PhotoFile.getCompressedPhotoDir(root);
        assertThat(dir.getName()).isEqualTo("compressed");
        assertThat(dir.getParentFile()).isEqualTo(root);
    }

    @Test
    public void newPhotoName_startsWithJpegPrefixAndDate() {
        String name = PhotoFile.newPhotoName();
        assertThat(name).startsWith("JPEG_");
        assertThat(name.length()).isGreaterThan("JPEG_yyyyMMdd_HHmmss".length());
    }

    @Test
    public void newPhotoName_isUniqueOnEveryCall() {
        assertThat(PhotoFile.newPhotoName()).isNotEqualTo(PhotoFile.newPhotoName());
    }

    @Test
    public void createPhotoFile_createsFileInsidePhotoDir() throws IOException {
        File root = folder.newFolder("pictures");
        File file = PhotoFile.createPhotoFile(root);
        assertThat(file.exists()).isTrue();
        assertThat(file.getName()).endsWith(".jpg");
        assertThat(file.getParentFile()).isEqualTo(PhotoFile.getPhotoDir(root));
    }

    @Test
    public void createPhotoFile_requiresParentDirectoryToAlreadyExist() {
        File root = new File(folder.getRoot(), "missing");
        try {
            PhotoFile.createPhotoFile(root);
            fail("expected IOException when the parent directory does not exist");
        } catch (IOException expected) {
            assertThat(PhotoFile.getPhotoDir(root).exists()).isFalse();
        }
    }
}
