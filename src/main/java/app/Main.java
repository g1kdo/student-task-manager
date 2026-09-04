package app;

/**
 * Entry point. Everything the application does lives in {@link ConsoleApp},
 * which takes its streams as arguments and is therefore testable; this class
 * only supplies the real console.
 */
public class Main {

    public static void main(String[] args) {
        new ConsoleApp(System.in, System.out).run();
    }
}
