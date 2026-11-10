import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Runner {

    enum Status { PASSED, FAILED }

    record TestResult(String name, Status status, long durationMs) { }

    static Callable<TestResult> fakeTest(String name, long durationMs, Status status) {
        return () -> {
            Thread.sleep(durationMs);
            return new TestResult(name, status, durationMs);
        };
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        List<Callable<TestResult>> tests = List.of(
                fakeTest("test1", 300, Status.PASSED),
                fakeTest("test2", 350, Status.PASSED),
                fakeTest("test3", 400, Status.FAILED),
                fakeTest("test4", 450, Status.PASSED),
                fakeTest("test5", 500, Status.PASSED),
                fakeTest("test6", 320, Status.FAILED),
                fakeTest("test7", 380, Status.PASSED),
                fakeTest("test8", 420, Status.PASSED)
        );

        long start = System.nanoTime();

        List<TestResult> results;
        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            List<Future<TestResult>> futures = pool.invokeAll(tests); // ждёт всех
            results = new ArrayList<>(futures.size());
            for (Future<TestResult> f : futures) {
                results.add(f.get());
            }
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        long passed = results.stream().filter(r -> r.status() == Status.PASSED).count();
        long failed = results.size() - passed;

        long sumMs = results.stream().mapToLong(TestResult::durationMs).sum();
        long savedMs = sumMs - elapsedMs;

        System.out.println("Прошло: " + passed + ", упало: " + failed);
        System.out.println("Суммарное время тестов: " + sumMs + " ms");
        System.out.println("Реальное время прогона: " + elapsedMs + " ms");
        System.out.println("Параллельность сэкономила: " + savedMs + " ms");
    }
}