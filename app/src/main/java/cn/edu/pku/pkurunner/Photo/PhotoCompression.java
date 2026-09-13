package cn.edu.pku.pkurunner.Photo;

import android.graphics.Bitmap;
import id.zelory.compressor.Compressor;
import java.io.File;
import java.io.IOException;

public abstract class PhotoCompression {
    public static String compressPhoto(File file, Compressor compressor, String str) throws IOException {
        return compressor.setMaxWidth(640).setMaxHeight(480).setQuality(50).setCompressFormat(Bitmap.CompressFormat.JPEG).setDestinationDirectoryPath(PhotoFile.getCompressedPhotoDir(file).getPath()).compressToFile(new File(PhotoFile.getPhotoDir(file), str)).getName();
    }
}
