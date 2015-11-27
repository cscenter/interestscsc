package data;

public class NGram {

    private final String text;
    private final String usesStr;
    private final Integer usesCnt;

    public NGram(String text, String usesStr, Integer usesCnt) {
        this.text = text;
        this.usesStr = usesStr;
        this.usesCnt = usesCnt;
    }

    public String getText() {
        return text;
    }

    public String getUsesStr() {
        return usesStr;
    }

    public Integer getUsesCnt() {
        return usesCnt;
    }

    @Override
    public String toString() {
        return text;
    }

}
