package org.sourceflow.gradient.testing.nutrition;

public class BmiService {
    float bmi(float height, float weight) {
        assert height != 0;

        float heightInMeters = height / 100;

        return weight / (heightInMeters * heightInMeters);
    }
}
