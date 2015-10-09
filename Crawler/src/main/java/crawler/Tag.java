package crawler;

public class Tag {

    private String name;
    private Integer uses;

    public Tag() {
        name = "";
        uses = 0;
    }

    public Tag(String title, Integer uses) {
        this.name = title;
        this.uses = uses;
    }

    public String getTag() {
        return name;
    }

    public Integer getUses() {
        return uses;
    }

    public void setTag(String title) {
        this.name = title;
    }

    public void setUses(Integer uses) {
        this.uses = uses;
    }

    @Override
    public String toString() {
        return name;
    }
}
