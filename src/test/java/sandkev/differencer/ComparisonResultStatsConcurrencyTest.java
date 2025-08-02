package sandkev.differencer;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.*;

class ComparisonResultStatsConcurrencyTest {

    private static final int NUM_THREADS       = 20;
    private static final int CALLS_PER_THREAD  = 500;
    private static final int EXPECTED_TOTAL    = NUM_THREADS * CALLS_PER_THREAD;

    @Test
    void testThreadSafetyUnderConcurrentUpdatesWithAwaitility() {
        ComparisonResultStats stats = new ComparisonResultStats();

        ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
        CyclicBarrier startBarrier = new CyclicBarrier(NUM_THREADS);

        // Kick off all workers
        for (int t = 0; t < NUM_THREADS; t++) {
            final int threadId = t;
            exec.submit(() -> {
                try {
                    startBarrier.await();
                    for (int i = 0; i < CALLS_PER_THREAD; i++) {
                        stats.onEqual("T" + threadId + "_E"  + i);
                        stats.onApproximatelyEqual("T" + threadId + "_A" + i, null);
                        stats.onAdded("T" + threadId + "_AD" + i, new Object());
                        stats.onDropped("T" + threadId + "_DR" + i, new Object());
                        stats.onChanged("T" + threadId + "_CH" + i, null);
                    }
                } catch (Exception ex) {
                    fail("Worker thread failed: " + ex);
                }
            });
        }

        // Shut down and wait (via Awaitility) for all tasks to complete
        exec.shutdown();
        await().atMost(Duration.ofSeconds(30))
               .until(exec::isTerminated);

        // Now wait until all counts and sets hit the expected total
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertEquals(EXPECTED_TOTAL, stats.getEqualCount().get());
            assertEquals(EXPECTED_TOTAL, stats.getApproximatelyEqualCount().get());
            assertEquals(EXPECTED_TOTAL, stats.getAddedCount().get());
            assertEquals(EXPECTED_TOTAL, stats.getDroppedCount().get());
            assertEquals(EXPECTED_TOTAL, stats.getChangedCount().get());

            Set<Object> addedKeys   = stats.getAddedKeys();
            Set<Object> droppedKeys = stats.getDroppedKeys();
            Set<Object> changedKeys = stats.getChangedKeys();

            assertEquals(EXPECTED_TOTAL, addedKeys.size());
            assertEquals(EXPECTED_TOTAL, droppedKeys.size());
            assertEquals(EXPECTED_TOTAL, changedKeys.size());

            // spot–check a few IDs
            IntStream.range(0, 3).forEach(i -> {
                assertTrue(addedKeys.contains("T0_AD" + i));
                assertTrue(droppedKeys.contains("T1_DR" + i));
                assertTrue(changedKeys.contains("T2_CH" + i));
            });
        });
    }

    @Test
    void canReset() {
        ComparisonResultStats stats = new ComparisonResultStats();
        stats.onAdded(1, "a");
        assertEquals(1, stats.getAddedCount());
        stats.reset();
        assertEquals(0, stats.getAddedCount());
    }


}
