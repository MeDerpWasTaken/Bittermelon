package com.site21.bittermelon.common.systems.fluid.substance;

import com.site21.bittermelon.common.systems.substance.SubstanceMixture;

public interface MixtureOwner {
    SubstanceMixture getMixture();

    default int getVolume() {
        return getMixture().getVolume();
    }

    default float getTemperature() {
        return getMixture().getTemperature();
    }

    default int getColor() {
        return getMixture().getColor();
    }

    default int getViscosity() {
        return getMixture().getViscosity();
    }
}
