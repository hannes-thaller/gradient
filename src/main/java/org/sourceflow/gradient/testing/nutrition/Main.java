package org.sourceflow.gradient.testing.nutrition;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final String CSV_SYMBOL = ",";

    private static class Query {
        private final List<String> name = new ArrayList<>();
        private final List<String> gender = new ArrayList<>();
        private final List<Integer> age = new ArrayList<>();
        private final List<Float> weight = new ArrayList<>();
        private final List<Float> height = new ArrayList<>();
    }


    private static Query readFile(File csvFile) throws FileNotFoundException {
        if (csvFile == null || !csvFile.exists()) {
            throw new IllegalArgumentException("Expected non-null file and existing file.");
        }

        final Query query = new Query();

        try (final Scanner scanner = new Scanner(csvFile)) {
            scanner.nextLine();
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                final String[] parts = line.split(CSV_SYMBOL);
                if (parts.length == 5) {
                    query.gender.add(parts[0]);
                    query.name.add(parts[1]);
                    query.age.add(Integer.parseInt(parts[2]));
                    query.height.add(Float.parseFloat(parts[3]));
                    query.weight.add(Float.parseFloat(parts[4]));
                } else {
                    System.err.printf("Error reading a query line: %s%n", line);
                }
            }
        }

        assert query.name.size() == query.age.size()
                && query.age.size() == query.weight.size()
                && query.weight.size() == query.height.size()
                && query.height.size() == query.gender.size();

        return query;
    }


    private static void run(File csvFile) throws FileNotFoundException {
        final Query query = readFile(csvFile);

        final Servlet servlet = new Servlet();

        for (int i = 0; i < query.name.size(); i++) {
            servlet.handleRequest(query.name.get(i), query.gender.get(i), query.age.get(i),
                    query.height.get(i), query.weight.get(i));
        }
    }


    public static void main(String[] args) throws FileNotFoundException, URISyntaxException {
        URL resource = Thread.currentThread().getContextClassLoader().getResource("person.csv");
        assert resource != null : "Query file not found in resources";

        final File queryFile = Paths.get(resource.toURI()).toFile();
        Main.run(queryFile);
        System.out.println("Done");
    }
}
