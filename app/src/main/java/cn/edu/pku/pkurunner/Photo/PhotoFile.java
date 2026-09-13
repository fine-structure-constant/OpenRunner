package cn.edu.pku.pkurunner.Photo;

import android.os.Environment;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public abstract class PhotoFile {
    public static final String PicutreType = Environment.DIRECTORY_PICTURES;

    public static File getCompressedPhotoDir(File file) {
        return new File(file, "/compressed");
    }

    public static File getPhotoDir(File file) {
        return new File(file, "/photo");
    }

    public static String newPhotoName() {
        return "JPEG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(new Date()) + UUID.randomUUID();
    }

    public static File createPhotoFile(File file) throws IOException {
        String newPhotoName = newPhotoName();
        File photoDir = getPhotoDir(file);
        if (!photoDir.exists()) {
            photoDir.mkdir();
        }
        return File.createTempFile(newPhotoName, ".jpg", photoDir);
    }
}
