package estu.ceng.edu;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.IntStream;

import static java.lang.System.out;

public class Main {
    @Option(name = "-numThreads", aliases = "--numThreads", usage = "Sets the number of threads (must be greater than or equal to 1)", required = true)
    private int threads = 0; // number of threads CLI argument
    @Option(name = "-numBins", aliases = "--numBins", usage = "Sets the number of bins (must be even and greater than or equal to 2)", required = true)
    private int bins = 0; // number of bins CLI argument
    @Option(name = "-awaitTime", aliases = "--awaitTime", usage = "How many seconds the program will wait for all the threads to finish (default 30 seconds)")
    private int awaitTime = 30; // await time CLI argument
    // atomic integer array variable binArray to increment according random indexes properly
    AtomicIntegerArray binArray;

    public static void main(String[] args) {
        final Main instance = new Main(); // creating an instance of main class
        try {
            instance.getArgs(args); // getting and controlling the arguments
            instance.generateBins(); // generating binArray with respect to given 'bins' argument
            instance.fillBins(); // filling the bins using multithreading
            instance.printBins(); // printing and summing every bin cell
        } catch (IOException ex) {
            out.println("An unexpected I/O Exception has been occurred: " + ex);
        }
    }

    private void getArgs(final String[] args) throws IOException {
        final CmdLineParser parser = new CmdLineParser(this);
        if (args.length < 1) {
            parser.printUsage(out);
            System.exit(-1);
        }
        try {
            parser.parseArgument(args);
            if (threads < 1 || bins < 1 || bins % 2 != 0) { // threads and bins must be greater than or equal to 1
                parser.printUsage(out);
                System.exit(-1);
            }
        } catch (CmdLineException ex) {
            out.println("Unable to parse command-line options: " + ex);
        }
    }

    private void generateBins() {
        // initiating atomic integer array with size of given 'bins'
        binArray = new AtomicIntegerArray(bins);
    }

    private void fillBins() {
        // in order to increase general efficiency of the whole program
        // getting the core number of the CPU
        int cores = Runtime.getRuntime().availableProcessors();
        // and creating newFixedThreadPool with core number to work as much efficient as it could be
        // FixedThreadPool executor executes at most 'fixed' number of threads on the other hand other threads waits to be executed by enqueueing
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(cores);
        // using streams to loop (int i = 0; i < threads; i++) but in parallel to make operation quicker
        IntStream.range(0, threads).parallel().forEach(iteration -> { // for each iteration
            executor.execute(() -> { // instantiating and executing new anonymous Runnable interface which simply acts like a ball
                Random random = new Random(); // random class to be used
                int index = bins - 1; // index can be at most (bins - 1)
                for (int j = 0; j < bins - 1; j++) { // mathematical statistic of galton board is not that too hard to understand
                    // each ball does (N-1) movements which consist of left or right in N binned Galton Board
                    // since index is N-1
                    // if random.nextInt() comes 0 -> that means left -> index must be decreased one
                    // else if random.nextInt() comes 1 -> nothing needs to be done since initial value of index is N-1
                    if (random.nextInt(2) == 0) --index;
                }
                // atomic variables synchronizes the whole reading and writing process
                // on other words this method that lies below is 'synchronized'
                // the same functionality can be done with a method which has 'synchronized' keyword
                // when a thread is incrementing the index the rest of the threads cannot interrupt or interact with array or/and index
                // tl;dr - the area below can be considered as 'critical section' and it is synchronized
                binArray.incrementAndGet(index);
            });
        });
        try {
            executor.shutdown();
            // executor will not accept furthermore threads since shutdown method has been called
            boolean isFinished = executor.awaitTermination(awaitTime, TimeUnit.SECONDS);
            // if the executor didn't terminate in the given await time then program aborts
            if (!isFinished) {
                out.println("Mission aborted due to the whole process has been not finished since " + awaitTime + " seconds!\n" +
                        "You can change awaitTime by using '-awaitTime <seconds>' argument.");
                System.exit(-1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void printBins() {
        int sum = 0; // initial sum value
        for (int i = 0; i < bins; i++) {
            sum += binArray.get(i); // getting the value of bin cell
            out.println(i + "\t" + binArray.get(i)); // printing the value of cell
        }
        out.printf("Number of requested thread: %s%n", threads);
        out.printf("Sum of bin values: %s", sum);
    }
}
