package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.db.BiGGDB;

import org.junit.AfterClass;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@Testcontainers
public abstract class BiGGDBContainerTest {

    @SuppressWarnings("resource")
    static final GenericContainer<?> bigg = new GenericContainer<>(DockerImageName.parse("schmirgel/bigg_db:1.6"))
            .withExposedPorts(5432)
            .withEnv("POSTGRES_PASSWORD", "postgres")
            .withStartupTimeout(Duration.ofMinutes(5));

    static {
        bigg.start();
        BiGGDB.init(bigg.getHost(), bigg.getFirstMappedPort().toString(), "postgres", "postgres", "bigg");
    }

    @AfterClass
    public static void tearDown() {
        if (bigg != null) {
                bigg.close();
        }
    }
}
