package com.shixianwen.certification;

import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.FileTypeDetector;
import com.shixianwen.storage.StoredFile;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CertificationMediaProcessorTest {
    @Test
    void extractsOnlySupportedMediaFromRedactedArchive() throws Exception {
        Path archive = Files.createTempFile("experience-proof-", ".zip");
        try (OutputStream output = Files.newOutputStream(archive);
             ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("说明.txt"));
            zip.write("not public media".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("录音.mp3"));
            zip.write(new byte[]{'I', 'D', '3', 4, 0, 0, 0, 0, 0, 0});
            zip.closeEntry();
        }

        CertificationMediaStateService state = mock(CertificationMediaStateService.class);
        FileStorage storage = mock(FileStorage.class);
        when(state.begin(9L)).thenReturn(new CertificationMediaStateService.SourceArchive(
            12L,
            "private/proof.zip",
            "proof.zip",
            "application/zip",
            "7996702",
            "NORMAL"
        ));
        org.mockito.Mockito.doAnswer(invocation -> {
            Files.copy(archive, invocation.getArgument(2), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return null;
        }).when(storage).copyTo(anyString(), any(), any(Path.class));
        when(storage.store(any(Path.class), anyString(), anyString(), any()))
            .thenReturn(new StoredFile("private/extracted.mp3", null, "audio/mpeg", 10));

        CertificationMediaProcessor processor = new CertificationMediaProcessor(
            state,
            storage,
            new FileTypeDetector()
        );
        processor.process(new CertificationMediaExtractionRequested(9L));

        verify(state).add(
            org.mockito.ArgumentMatchers.eq(9L),
            org.mockito.ArgumentMatchers.eq(12L),
            org.mockito.ArgumentMatchers.eq("AUDIO"),
            org.mockito.ArgumentMatchers.eq("音频1"),
            org.mockito.ArgumentMatchers.eq("录音.mp3"),
            any(StoredFile.class),
            org.mockito.ArgumentMatchers.eq(1)
        );
        verify(state).ready(9L);
        verify(state, never()).failed(anyLong(), anyString());
        Files.deleteIfExists(archive);
    }
}
