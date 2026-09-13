package cn.edu.pku.pkurunner.Model;

public class Task {
    public static final int ACQUIRED = 2;
    public static final int HIDDEN = 0;
    public static final int NOT_ACQUIRED = 1;
    private int activityId;
    private String description;

    private int id;
    private String name;
    private String requirement;
    private int status;

    public @interface Status {
    }

    public int getActivityId() {
        return this.activityId;
    }

    public String getDescription() {
        return this.description;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getRequirement() {
        return this.requirement;
    }

    public int getStatus() {
        return this.status;
    }

    public void setActivityId(int i2) {
        this.activityId = i2;
    }

    public void setDescription(String str) {
        this.description = str;
    }

    public void setId(int i2) {
        this.id = i2;
    }

    public void setName(String str) {
        this.name = str;
    }

    public void setRequirement(String str) {
        this.requirement = str;
    }

    public void setStatus(int i2) {
        this.status = i2;
    }
}
