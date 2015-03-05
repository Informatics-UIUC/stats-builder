package edu.illinois.i3.emop.apps.statsbuilder.stats;

/**
 * @author capitanu
 */
public class Bin<T> {
    private final T _min;
    private final T _max;

    public Bin(T min, T max) {
        _min = min;
        _max = max;
    }

    public T getMin() {
        return _min;
    }

    public T getMax() {
        return _max;
    }

    public String getName() {
        String min = _min == null ? "*" : "" + _min;
        String max = _max == null ? "*" : "" + _max;

        return String.format("%s_to_%s", min, max);
    }

    @Override
    public String toString() {
        return getName();
    }
}
