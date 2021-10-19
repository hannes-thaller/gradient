package org.sourceflow.gradient.testing.nutrition;


public class NutritionAdvisor {

    private final BmiService bmiService = new BmiService();

    public String advice(Person person) {
        assert person != null;

        float bmi = bmiService.bmi(person.height, person.weight);

        String advice;
        if (bmi < 18.5) {
            advice = "Do not stop eating!";
        } else if (18.5 <= bmi && bmi < 25) {
            advice = "Your are good, eat if you want.";
        } else if (25 <= bmi && bmi < 30) {
            advice = "Consider skipping the meal.";
        } else if (30 <= bmi && bmi < 35) {
            advice = "Hungry again?";
        } else if (35 <= bmi && bmi < 39) {
            advice = "Do not eat!";
        } else {
            advice = "Please do not eat me!";
        }

        return advice;
    }
}
