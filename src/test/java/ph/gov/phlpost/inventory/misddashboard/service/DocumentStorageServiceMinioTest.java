package ph.gov.phlpost.inventory.misddashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.MockMultipartFile;

import ph.gov.phlpost.inventory.misddashboard.config.MinioConfig;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Round-trips a document through a real MinIO (storage.mode=minio) using the
 * production client configuration from {@link MinioConfig}.
 *
 * <p>
 * This is the compatibility check to run before changing the AWS SDK version:
 * SDK 2.30+ sends integrity checksums by default, which some S3-compatible
 * servers reject. It runs only when a MinIO is supplied, and is reported as
 * skipped otherwise:
 *
 * <pre>
 * MISD_TEST_MINIO_ENDPOINT=http://localhost:9000
 * MISD_TEST_MINIO_ACCESS_KEY=...
 * MISD_TEST_MINIO_SECRET_KEY=...
 * ./mvnw test -Dtest=DocumentStorageServiceMinioTest
 * </pre>
 *
 * Point it at the same MinIO version production runs. It creates (and leaves)
 * a bucket named misd-storage-test and removes every object it writes.
 */
class DocumentStorageServiceMinioTest {

    private static final String BUCKET = "misd-storage-test";

    private static S3Client client;
    private static DocumentStorageService storage;

    @BeforeAll
    static void connect() {
        String endpoint = System.getenv("MISD_TEST_MINIO_ENDPOINT");
        assumeTrue(endpoint != null && !endpoint.isBlank(),
                "MISD_TEST_MINIO_ENDPOINT not set; skipping the MinIO compatibility test");

        client = MinioConfig.buildClient(endpoint, System.getenv("MISD_TEST_MINIO_ACCESS_KEY"),
                System.getenv("MISD_TEST_MINIO_SECRET_KEY"));
        try {
            client.createBucket(request -> request.bucket(BUCKET));
        } catch (BucketAlreadyOwnedByYouException alreadyThere) {
            // Re-run against the same server.
        }

        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("s3Client", client);
        storage = new DocumentStorageService(beans.getBeanProvider(S3Client.class), BUCKET, "minio", "unused");
    }

    @AfterAll
    static void disconnect() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void uploadReadAndDeleteRoundTrip() throws Exception {
        byte[] content = "Official receipt OR-123456".getBytes(StandardCharsets.UTF_8);
        var file = new MockMultipartFile("documentFiles", "receipt scan.pdf", "application/pdf", content);

        String key = storage.uploadDocument(file, "it-equipment", "PPC-2026-10-28-00001");

        assertThat(key).startsWith("it-equipment/PPC-2026-10-28-00001/").endsWith("_receipt_scan.pdf");
        try (InputStream stored = storage.readDocument(key)) {
            assertThat(stored.readAllBytes()).isEqualTo(content);
        }

        storage.deleteDocument(key);

        assertThatThrownBy(() -> storage.readDocument(key).close()).isInstanceOf(NoSuchKeyException.class);
    }

    @Test
    void emptyUploadIsRefusedBeforeReachingTheServer() {
        var empty = new MockMultipartFile("documentFiles", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> storage.uploadDocument(empty, "vehicles", "ABC-1234"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
