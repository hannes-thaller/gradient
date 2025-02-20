package org.sourceflow.gradient.sensor.test;

import org.sourceflow.gradient.annotation.InModelingUniverse;

@InModelingUniverse
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }
}
