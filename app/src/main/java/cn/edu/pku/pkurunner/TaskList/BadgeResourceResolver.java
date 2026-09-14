package cn.edu.pku.pkurunner.TaskList;

import cn.edu.pku.pkurunner.R;
import java.util.HashMap;

public abstract class BadgeResourceResolver {
    public static final int NULL_RESOURCE = R.drawable.badge_null_template;
    public static final int UNACHIEVED_RESOURCE = R.drawable.badge_null_template;

    private static final HashMap badgeResourceMap;

    static class UnknownBadgeException extends Exception {
        UnknownBadgeException() {
        }
    }

    static {
        HashMap hashMap = new HashMap();
        badgeResourceMap = hashMap;
        hashMap.put("daily", Integer.valueOf(R.drawable.badge_daily_template));
    }

    public static int resolve(String str) throws UnknownBadgeException {
        if (str != null) {
            HashMap hashMap = badgeResourceMap;
            if (hashMap.containsKey(str)) {
                return ((Integer) hashMap.get(str)).intValue();
            }
        }
        throw new UnknownBadgeException();
    }
}
