package cn.edu.pku.pkurunner.Photo;

import cn.edu.pku.pkurunner.Data;
import cn.edu.pku.pkurunner.Model.Record;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import org.xutils.common.util.LogUtil;

public abstract class UselessPhotoCleaner {
    public static void cleanAllUnused(File file) {
        File photoDir = PhotoFile.getPhotoDir(file);
        if (photoDir.exists() && photoDir.isDirectory()) {
            File[] listFiles = photoDir.listFiles();
            LogUtil.d(listFiles.length + " photos found.");
            for (File file2 : listFiles) {
                file2.delete();
            }
        }
        HashSet hashSet = new HashSet();
        Iterator<Record> it = Data.getRecords().iterator();
        while (it.hasNext()) {
            Record next = it.next();
            if (!next.isUploaded()) {
                hashSet.add(next.getPhotoName());
            }
        }
        String lastUsedPhoto = Data.getLastUsedPhoto();
        if (!"".equals(lastUsedPhoto)) {
            hashSet.add(lastUsedPhoto);
        }
        File compressedPhotoDir = PhotoFile.getCompressedPhotoDir(file);
        if (compressedPhotoDir.exists() && compressedPhotoDir.isDirectory()) {
            File[] listFiles2 = compressedPhotoDir.listFiles();
            LogUtil.d(listFiles2.length + " compressed photos found.");
            for (File file3 : listFiles2) {
                if (!hashSet.contains(file3.getName())) {
                    LogUtil.d("ready to delete " + file3.getName());
                    file3.delete();
                }
            }
        }
    }
}
