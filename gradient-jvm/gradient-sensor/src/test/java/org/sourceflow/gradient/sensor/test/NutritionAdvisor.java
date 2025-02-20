package org.sourceflow.gradient.sensor.test;

import org.sourceflow.gradient.annotation.InModelingUniverse;
import org.sourceflow.gradient.annotation.NotInModelingUniverse;

@InModelingUniverse
public class NutritionAdvisor {

    @NotInModelingUniverse
    private final BmiService bmiService = new BmiService();

    public String advice(Person person) {
        assert person != null;

        float bmi = bmiService.bmi(person.height, person.weight);

        String advice;
        if (bmi < 18.5) {
            advice = "Do not stop eating!";
        } else if (18.5 < bmi && bmi < 24.9) {
            advice = "Your are good, eat if you want.";
        } else if (25 < bmi && bmi < 29.9) {
            advice = "Consider skipping the meal.";
        } else if (30 < bmi && bmi < 34.9) {
            advice = "Hungry again?";
        } else if (35 < bmi && bmi < 38.9) {
            advice = "Do not eat!";
        } else {
            advice = "Please do not eat me!";
        }

        return advice;
    }
}
