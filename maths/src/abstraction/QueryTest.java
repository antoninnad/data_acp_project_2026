package abstraction;

import math.Vector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for the {@link Query} class.
 *
 * <p>Each test method covers one scenario (constructor defaults, squared norm,
 * best-match selection, empty database). All tests are run from {@link #main}
 * and throw {@link AssertionError} on failure.</p>
 *
 * @see Query
 */
public class QueryTest {

    /**
     * Runs all unit tests in sequence and prints a success message when they all pass.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        testDefaultConstructor();
        testConstructorWithThreshold();
        testSquaredNormeWithSameDimension();
        testSquaredNormeWithIdenticalVectors();
        testSquaredNormeWithDifferentDimensions();
        testFindBestMatchReturnsCorrectLabel();
        testFindBestMatchReturnsEmptyStringWhenNoMatch();
        testFindBestMatchWithEmptyDataBase();
        testFindBestMatchReturnsFirstMatchingLabel();

        System.out.println("\nTous les tests sont passés !");
    }

    private static void testDefaultConstructor() {
        Query query = new Query();

        if (query.threshold_similarity != 1.2) {
            throw new AssertionError("testDefaultConstructor échoué");
        }

        System.out.println("testDefaultConstructor réussi");
    }

    private static void testConstructorWithThreshold() {
        Query query = new Query(0.5);

        if (query.threshold_similarity != 0.5) {
            throw new AssertionError("testConstructorWithThreshold échoué");
        }

        System.out.println("testConstructorWithThreshold réussi");
    }

    private static void testSquaredNormeWithSameDimension() {
        Vector a = new Vector(new double[]{1.0, 2.0, 3.0});
        Vector b = new Vector(new double[]{4.0, 6.0, 3.0});

        double result = Query.squaredNorme(a, b);

        if (result != 25.0) {
            throw new AssertionError("testSquaredNormeWithSameDimension échoué : attendu 25.0, obtenu " + result);
        }

        System.out.println("testSquaredNormeWithSameDimension réussi");
    }

    private static void testSquaredNormeWithIdenticalVectors() {
        Vector a = new Vector(new double[]{1.0, 2.0, 3.0});
        Vector b = new Vector(new double[]{1.0, 2.0, 3.0});

        double result = Query.squaredNorme(a, b);

        if (result != 0.0) {
            throw new AssertionError("testSquaredNormeWithIdenticalVectors échoué : attendu 0.0, obtenu " + result);
        }

        System.out.println("testSquaredNormeWithIdenticalVectors réussi");
    }

    private static void testSquaredNormeWithDifferentDimensions() {
        Vector a = new Vector(new double[]{1.0, 2.0});
        Vector b = new Vector(new double[]{1.0, 2.0, 3.0});

        boolean exceptionLancee = false;

        try {
            Query.squaredNorme(a, b);
        } catch (DimensionVectorException e) {
            exceptionLancee = true;
        }

        if (!exceptionLancee) {
            throw new AssertionError("testSquaredNormeWithDifferentDimensions échoué : aucune exception lancée");
        }

        System.out.println("testSquaredNormeWithDifferentDimensions réussi");
    }

    private static void testFindBestMatchReturnsCorrectLabel() {
        Query query = new Query(2.0);

        Vector target = new Vector(new double[]{1.0, 1.0});

        Map<String, List<Vector>> dataBase = new LinkedHashMap<>();
        dataBase.put("Alice", List.of(
                new Vector(new double[]{10.0, 10.0})
        ));
        dataBase.put("Bob", List.of(
                new Vector(new double[]{1.5, 1.5})
        ));

        String result = query.findBestMatch(target, dataBase);

        if (!result.equals("Bob")) {
            throw new AssertionError("testFindBestMatchReturnsCorrectLabel échoué : attendu Bob, obtenu " + result);
        }

        System.out.println("testFindBestMatchReturnsCorrectLabel réussi");
    }

    private static void testFindBestMatchReturnsEmptyStringWhenNoMatch() {
        Query query = new Query(1.0);

        Vector target = new Vector(new double[]{1.0, 1.0});

        Map<String, List<Vector>> dataBase = new LinkedHashMap<>();
        dataBase.put("Alice", List.of(
                new Vector(new double[]{10.0, 10.0})
        ));
        dataBase.put("Bob", List.of(
                new Vector(new double[]{5.0, 5.0})
        ));

        String result = query.findBestMatch(target, dataBase);

        if (!result.equals("")) {
            throw new AssertionError("testFindBestMatchReturnsEmptyStringWhenNoMatch échoué : attendu chaîne vide, obtenu " + result);
        }

        System.out.println("testFindBestMatchReturnsEmptyStringWhenNoMatch réussi");
    }

    private static void testFindBestMatchWithEmptyDataBase() {
        Query query = new Query(1.0);

        Vector target = new Vector(new double[]{1.0, 1.0});
        Map<String, List<Vector>> dataBase = new LinkedHashMap<>();

        String result = query.findBestMatch(target, dataBase);

        if (!result.equals("")) {
            throw new AssertionError("testFindBestMatchWithEmptyDataBase échoué : attendu chaîne vide, obtenu " + result);
        }

        System.out.println("testFindBestMatchWithEmptyDataBase réussi");
    }

    private static void testFindBestMatchReturnsFirstMatchingLabel() {
        Query query = new Query(10.0);

        Vector target = new Vector(new double[]{1.0, 1.0});

        Map<String, List<Vector>> dataBase = new LinkedHashMap<>();
        dataBase.put("Alice", List.of(
                new Vector(new double[]{2.0, 2.0})
        ));
        dataBase.put("Bob", List.of(
                new Vector(new double[]{1.5, 1.5})
        ));

        String result = query.findBestMatch(target, dataBase);

        if (!result.equals("Alice")) {
            throw new AssertionError("testFindBestMatchReturnsFirstMatchingLabel échoué : attendu Alice, obtenu " + result);
        }

        System.out.println("testFindBestMatchReturnsFirstMatchingLabel réussi");
    }
}