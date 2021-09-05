package org.sourceflow.gradient.sensor.test;

import org.sourceflow.gradient.annotation.InModelingUniverse;

@InModelingUniverse
public class BmiService {
    float bmi(float height, float weight) {
        assert height != 0;

        float heightInMeters = height / 100;

        return weight / (heightInMeters * heightInMeters);
    }
}
