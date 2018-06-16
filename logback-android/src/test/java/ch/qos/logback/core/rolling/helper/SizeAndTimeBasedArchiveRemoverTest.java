package ch.qos.logback.core.rolling.helper;

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.util.CoreTestConstants;

public class SizeAndTimeBasedArchiveRemoverTest {

    Context context = new ContextBase();

    @Test
    public void smoke() {
        FileNamePattern fileNamePattern = new FileNamePattern("smoke-%d-%i.gz", context);
        SizeAndTimeBasedArchiveRemover remover = new SizeAndTimeBasedArchiveRemover(fileNamePattern, null);
        File[] fileArray = new File[2];
        File[] expected = new File[2];

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date today = new Date();
        String baseFilename = "smoke-" + dateFormatter.format(today);
        fileArray[0] = expected[1] = new File(CoreTestConstants.OUTPUT_DIR_PREFIX, baseFilename + "-0.gz");
        fileArray[1] = expected[0] = new File(CoreTestConstants.OUTPUT_DIR_PREFIX, baseFilename + "-1.gz");

        remover.descendingSort(fileArray, today);

        assertArrayEquals(expected, fileArray);
    }

    @Test
    public void badFilenames() {
        FileNamePattern fileNamePattern = new FileNamePattern("smoke-%d-%i.gz", context);
        SizeAndTimeBasedArchiveRemover remover = new SizeAndTimeBasedArchiveRemover(fileNamePattern, null);
        File[] fileArray = new File[2];
        File[] expected = new File[2];

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date today = new Date();
        String baseFilename = "smoke-" + dateFormatter.format(today);
        fileArray[0] = expected[0] = new File(CoreTestConstants.OUTPUT_DIR_PREFIX, baseFilename + "-b.gz");
        fileArray[1] = expected[1] = new File(CoreTestConstants.OUTPUT_DIR_PREFIX, baseFilename + "-c.gz");

        remover.descendingSort(fileArray, today);

        assertArrayEquals(expected, fileArray);
    }
}
