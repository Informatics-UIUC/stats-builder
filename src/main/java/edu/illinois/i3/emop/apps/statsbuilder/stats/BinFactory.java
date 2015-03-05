package edu.illinois.i3.emop.apps.statsbuilder.stats;

/**
 * @author capitanu
 */
public interface BinFactory<T> {

    Bin<T> createBin(Double min, Double max);

}
