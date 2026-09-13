package cn.edu.pku.pkurunner.TaskList;

import cn.edu.pku.pkurunner.R;
import java.util.HashMap;

public abstract class BadgeResourceResolver {
    public static final int NULL_RESOURCE = R.drawable.badge_null_template;
    public static final int UNACHIEVED_RESOURCE = R.drawable.badge_null_template;

    /* renamed from: a, reason: collision with root package name */
    private static final HashMap f7088a;

    static class a extends Exception {
        a() {
        }
    }

    static {
        HashMap hashMap = new HashMap();
        f7088a = hashMap;
        hashMap.put("daily", Integer.valueOf(R.drawable.badge_daily_template));
    }

    public static int resolve(String str) throws a {
        if (str != null) {
            HashMap hashMap = f7088a;
            if (hashMap.containsKey(str)) {
                return ((Integer) hashMap.get(str)).intValue();
            }
        }
        throw new a();
    }
}
