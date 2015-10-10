package crawler;

public class Tag {

    private String name;
    private int uses;

    public Tag(String title, int uses) {
        this.name = title;
        this.uses = uses;
    }

    public String getTag() {
        return name;
    }

    public void setTag(String title) {
        this.name = title;
    }

    public Integer getUses() {
        return uses;
    }

    public void setUses(int uses) {
        this.uses = uses;
    }

    @Override
    public String toString() {
        return name;
    }
}
