package cn.edu.pku.pkurunner.Network.Model;

import java.util.Date;

public class UserStatus {
    private Date beginDate;
    private int bonus;
    private int current;
    private Date endDate;
    private boolean isPassed;
    private int target;
    private int validCount;

    public Date getBeginDate() {
        return this.beginDate;
    }

    public int getBonus() {
        return this.bonus;
    }

    public int getCurrent() {
        return this.current;
    }

    public Date getEndDate() {
        return this.endDate;
    }

    public int getTarget() {
        return this.target;
    }

    public int getValidCount() {
        return this.validCount;
    }

    public boolean isPassed() {
        return this.isPassed;
    }
}
