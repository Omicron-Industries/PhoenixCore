package net.phoenix.core.growth.tendril;

import net.phoenix.core.growth.GrowthMultiblockMachine;

@FunctionalInterface
public interface TendrilWeightCondition {

    int weight(GrowthMultiblockMachine machine, TendrilShape shape);

    static TendrilWeightCondition constant(int weight) {
        return (machine, shape) -> weight;
    }
}
