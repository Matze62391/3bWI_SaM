package appeng.util;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

/**
 * Matches a list of inputs to a list of tests (i.e. ingredients of a shapeless recipe), such that every input is
 * assigned to exactly one test. Used for ingredients that cannot be matched using vanilla's stacked contents.
 */
public final class RecipeMatcher {
    private RecipeMatcher() {
    }

    /**
     * @return An array mapping inputs to tests ({@code result[input] = test}), or null if no complete matching exists.
     */
    public static <T> int @Nullable [] findMatches(List<T> inputs, List<? extends Predicate<T>> tests) {
        int size = inputs.size();
        if (size != tests.size()) {
            return null;
        }

        var matches = new boolean[size][size];
        for (int test = 0; test < size; test++) {
            for (int input = 0; input < size; input++) {
                matches[test][input] = tests.get(test).test(inputs.get(input));
            }
        }

        // Classic augmenting path algorithm for bipartite matching
        var testForInput = new int[size];
        Arrays.fill(testForInput, -1);
        for (int test = 0; test < size; test++) {
            if (!augment(test, matches, testForInput, new boolean[size])) {
                return null;
            }
        }
        return testForInput;
    }

    private static boolean augment(int test, boolean[][] matches, int[] testForInput, boolean[] visited) {
        for (int input = 0; input < testForInput.length; input++) {
            if (matches[test][input] && !visited[input]) {
                visited[input] = true;
                if (testForInput[input] == -1 || augment(testForInput[input], matches, testForInput, visited)) {
                    testForInput[input] = test;
                    return true;
                }
            }
        }
        return false;
    }
}
