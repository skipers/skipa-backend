package com.skipers.skipa.domain.patent.infra;

import io.minio.CopyObjectArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MinioPatentOriginalPdfStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @Test
    void copyUsesConfiguredBucketAndSourceObject() throws Exception {
        MinioPatentOriginalPdfStorageService storageService = new MinioPatentOriginalPdfStorageService(minioClient);
        ReflectionTestUtils.setField(storageService, "bucket", "skipa");
        ReflectionTestUtils.setField(storageService, "region", "us-east-1");

        storageService.copy(
                "tmp/patent-extract-jobs/7/original.pdf",
                "patents/1/original.pdf"
        );

        ArgumentCaptor<CopyObjectArgs> argsCaptor = ArgumentCaptor.forClass(CopyObjectArgs.class);
        verify(minioClient).copyObject(argsCaptor.capture());

        CopyObjectArgs args = argsCaptor.getValue();
        assertThat(args.bucket()).isEqualTo("skipa");
        assertThat(args.region()).isEqualTo("us-east-1");
        assertThat(args.object()).isEqualTo("patents/1/original.pdf");
        assertThat(args.source().bucket()).isEqualTo("skipa");
        assertThat(args.source().region()).isEqualTo("us-east-1");
        assertThat(args.source().object()).isEqualTo("tmp/patent-extract-jobs/7/original.pdf");
    }
}
