package com.kitepromiss.desubtitle.video;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VideoStorageFileNamesTest {

    @Test
    void acceptsUuidWithOptionalExt() {
        assertTrue(VideoStorageFileNames.isSafeStoredOrOutputFileName("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));
        assertTrue(
                VideoStorageFileNames.isSafeStoredOrOutputFileName("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11.mp4"));
    }

    @Test
    void rejectsPathSegmentsAndTraversal() {
        assertFalse(VideoStorageFileNames.isSafeStoredOrOutputFileName("../x.mp4"));
        assertFalse(VideoStorageFileNames.isSafeStoredOrOutputFileName("a/b.mp4"));
        assertFalse(VideoStorageFileNames.isSafeStoredOrOutputFileName("..\\evil"));
    }

    @Test
    void safeResolveRejectsEscapeFromBase(@TempDir Path base) {
        String evil = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11";
        assertTrue(VideoStorageFileNames.isSafeStoredOrOutputFileName(evil));
        assertTrue(VideoStorageFileNames.safeResolve(base, evil).isPresent());

        assertFalse(VideoStorageFileNames.safeResolve(base, "../outside").isPresent());
    }
}
