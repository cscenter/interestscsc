package crawler;

public class Tag {

    private String name;
    private Integer uses;

    public Tag(String title, Integer uses) {
        this.name = title;
        this.uses = uses;
    }

    public String getName() {
        return name;
    }

    public Integer getUses() {
        return uses;
    }

    public String writeToFile() {
        return name + " : " + uses + "\n";
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;

        Tag tag = (Tag) o;

        return !(name != null ? !name.equals(tag.name) : tag.name != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (uses != null ? uses.hashCode() : 0);
        return result;
    }
}
