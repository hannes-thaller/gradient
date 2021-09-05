package org.sourceflow.gradient.sensor.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        List<String[]> data = null;
        try (InputStream stream = Objects.requireNonNull(Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("person.csv"))) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            reader.readLine(); // header
            data = reader.lines()
                    .map(it -> it.split(","))
                    .limit(1000)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (data != null) {
            Servlet servlet = new Servlet();
            data.forEach(it -> servlet.handleRequest(
                    it[1], it[0], Integer.parseInt(it[2]),
                    Float.parseFloat(it[3]), Float.parseFloat(it[4])
            ));
        }
    }
}
