package ru.majordomo.hms.personmgr.common;

public class Count implements Comparable<Count> {
    private Long count = 0L;

    public Count() {
    }

    public Count(Long count) {
        this.count = count;
    }
    public Count(int count) {
        this.count = (long) count;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    @Override
    public int compareTo(Count o) {
        long compareCount = o.getCount();

        return this.count.compareTo(compareCount);
    }

    @Override
    public String toString() {
        return "Count{" +
                "count=" + count +
                '}';
    }
}
