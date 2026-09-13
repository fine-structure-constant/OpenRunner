package cn.edu.pku.pkurunner.Storage;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class StorageUtilTest {

    @Test
    public void sizeToReadableString_bytes() {
        assertThat(StorageUtil.sizeToReadableString(0L)).isEqualTo("0.0 B");
        assertThat(StorageUtil.sizeToReadableString(512L)).isEqualTo("512.0 B");
        assertThat(StorageUtil.sizeToReadableString(1023L)).isEqualTo("1023.0 B");
    }

    @Test
    public void sizeToReadableString_kilobytes() {
        assertThat(StorageUtil.sizeToReadableString(1024L)).isEqualTo("1.0 K");
        assertThat(StorageUtil.sizeToReadableString(1536L)).isEqualTo("1.5 K");
        assertThat(StorageUtil.sizeToReadableString(1048575L)).isEqualTo("1024.0 K");
    }

    @Test
    public void sizeToReadableString_megabytes() {
        assertThat(StorageUtil.sizeToReadableString(1048576L)).isEqualTo("1.0 M");
        assertThat(StorageUtil.sizeToReadableString(1048576L * 12)).isEqualTo("12.0 M");
    }

    @Test
    public void sizeToReadableString_gigabytes() {
        assertThat(StorageUtil.sizeToReadableString(1073741824L)).isEqualTo("1.0 G");
        assertThat(StorageUtil.sizeToReadableString(1073741824L * 2 + 1048576L * 512)).isEqualTo("2.5 G");
    }

    @Test
    public void sizeToReadableString_capsAtTera() {
        long oneTerabyte = 1L;
        for (int i = 0; i < 6; i++) {
            oneTerabyte *= 1024L;
        }
        String s = StorageUtil.sizeToReadableString(oneTerabyte);
        assertThat(s).endsWith(" T");
    }

    @Test
    public void sizeToReadableString_negativeTreatsAsUnsignedWrap() {
        String s = StorageUtil.sizeToReadableString(-1L);
        assertThat(s).isNotEmpty();
    }
}
