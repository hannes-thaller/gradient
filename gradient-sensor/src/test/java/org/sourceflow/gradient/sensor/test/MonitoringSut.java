package org.sourceflow.gradient.sensor.test;

import org.sourceflow.gradient.annotation.InModelingUniverse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@InModelingUniverse
public class MonitoringSut {
    private static int staticField;
    public int field;

    public MonitoringSut(int param) {
        this.field = param;
    }

    public int getField() {
        return field;
    }

    public int incField() {
        return ++field;
    }

    public int addToField(int a) {
        field += a;
        return getField();
    }

    public int local(int a) {
        int b = a + 1;
        return b;
    }

    public void externalExcluded(long a) {
        long b = a + 1;
        System.out.println(b);
    }

    public void externalIncluded(long a) {
        long b = a + 1 - 1;
        System.out.print(b);
    }

    public List<String> stringList(List<String> lst) {
        System.out.println(lst.size());
        return lst;
    }

    public String[] stringArray(String[] lst) {
        System.out.println(lst.length);
        return lst;
    }

    public List<Float> primitiveList(List<Float> lst) {
        System.out.println(lst.size());
        return lst;
    }

    public void inheritanceCall() {
        new IOException().printStackTrace();
    }

    public void exception(String fileName) {
        try (FileInputStream f = new FileInputStream(new File(fileName))) {
            f.available();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            System.out.print(fileName);
        }
    }

    public void throwing(String file){
        throw new IllegalArgumentException(file);
    }

    public static List<Integer> name() {
        List<Integer> list = new ArrayList<Integer>();
        list.add(staticField);
        return list;
    }
}
