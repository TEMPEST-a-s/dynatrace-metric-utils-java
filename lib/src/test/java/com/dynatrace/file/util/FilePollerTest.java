package com.dynatrace.file.util;

import com.dynatrace.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.*;

class FilePollerTest {
    @Test
    void filePollerConstructorThrowsOnNonExistentFile() {
        final String nonExistentFile = TestUtils.generateNonExistentFilename();

        assertThrows(IllegalArgumentException.class, () -> {
            new FilePoller(nonExistentFile);
        });
    }

    @Test
    void filePollerConstructorThrowsOnPassingAFolder() throws IOException {
        final Path tempDir = Files.createTempDirectory("temp");

        assertThrows(IllegalArgumentException.class, () -> {
            new FilePoller(tempDir.toString());
        });
    }

    @Test
    void filePollerUpdatesOnChange() throws IOException, InterruptedException {
        final Path tempFile = Files.createTempFile("tempfile", ".txt");

        final FilePoller poller = new FilePoller(tempFile.toString());
        assertFalse(poller.fileContentsUpdated());

        Files.writeString(tempFile, "test file content");
        // wait for non-blocking IO
        Thread.sleep(1);

        assertTrue(poller.fileContentsUpdated());
        assertFalse(poller.fileContentsUpdated());
    }

    @Test
    void filePollerUpdatesOnFileMove() throws IOException, InterruptedException {
        final Path tempFile1 = Files.createTempFile("tempfile", ".txt");
        final Path tempFile2 = Files.createTempFile("tempfile", ".txt");

        final FilePoller poller = new FilePoller(tempFile1.toString());
        // set up the second file
        Files.writeString(tempFile2, "test file content for tempFile2");

        // no changes to the first file yet
        assertFalse(poller.fileContentsUpdated());

        // move the second file into position.
        Files.move(tempFile2, tempFile1, REPLACE_EXISTING);
        // wait for non-blocking io to be finished.
        Thread.sleep(1);

        assertTrue(poller.fileContentsUpdated());
        assertFalse(poller.fileContentsUpdated());
    }

    @Test
    void filePollerUpdatesOnFileCopy() throws IOException, InterruptedException {
        final Path tempFile1 = Files.createTempFile("tempfile", ".txt");
        final Path tempFile2 = Files.createTempFile("tempfile", ".txt");

        final FilePoller poller = new FilePoller(tempFile1.toString());
        Files.writeString(tempFile2, "test file content for tempFile2");

        assertFalse(poller.fileContentsUpdated());

        Files.copy(tempFile2, tempFile1, REPLACE_EXISTING);
        // wait for non-blocking io
        Thread.sleep(1);

        assertTrue(poller.fileContentsUpdated());
        assertFalse(poller.fileContentsUpdated());
    }
}