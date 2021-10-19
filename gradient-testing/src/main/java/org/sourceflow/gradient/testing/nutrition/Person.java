package org.sourceflow.gradient.testing.nutrition;

public class Person {

    String name;
    int age;
    float weight;
    float height;
    String gender;

    public Person(String name, String gender, int age, float height, float weight) {
        assert name != null;
        assert gender != null;
        assert age > 0;
        assert weight > 0;
        assert height > 0;

        this.name = name;
        this.gender = gender;
        this.age = age;
        this.weight = weight;
        this.height = height;
    }
}
