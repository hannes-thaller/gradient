package org.sourceflow.gradient.testing.nutrition;

public class Servlet {
    private final NutritionAdvisor advisor = new NutritionAdvisor();

    public void handleRequest(String name, String gender, int age, float height, float weight) {
        final Person person = new Person(name, gender, age, height, weight);
        final String advice = advisor.advice(person);
        System.out.println(advice);
    }
}
