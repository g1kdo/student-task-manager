package app;
import java.util.Arrays;

public class StreamArrayExample {
    public static void main(String[] args) {
    String[] names = {"Alice", "Bob", "Charlie", "Alice", "David", "bob"};



    // Stream pipeline
        String[] result = Arrays.stream(names)
                .filter(name ->
                        name.length() > 3)// keep names longer than 3 chars
                .map(String::toLowerCase)// convert to lowercase
                .distinct() // remove duplicates
                .sorted()// sort alphabetically
                .toArray(String[]::new); // collect back into array


// Print results
        System.out.println("Original: " + Arrays.toString(names));
        System.out.println("Processed: " + Arrays.toString(result));

}
}