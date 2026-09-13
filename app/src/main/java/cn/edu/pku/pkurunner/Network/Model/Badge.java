package cn.edu.pku.pkurunner.Network.Model;

public class Badge {
    private String description;

    private int id;
    private String name;
    private String requirement;
    private int status;

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
