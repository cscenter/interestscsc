package com.interestscsc.data;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NGram nGram = (NGram) o;

        return text.equals(nGram.text);

    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

}
