package edu.ucsd.sbrg.bigg.annotation;

import edu.ucsd.sbrg.db.BiGGDB;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@Testcontainers
public abstract class BiGGDBContainerTest {

    static final GenericContainer bigg = new GenericContainer(DockerImageName.parse("preloaded_bigg:latest"))
            .withExposedPorts(5432)
            .withEnv("POSTGRES_PASSWORD", "postgres")
            .withStartupTimeout(Duration.ofMinutes(5));

    static {
        bigg.start();
        BiGGDB.init(bigg.getHost(), bigg.getFirstMappedPort().toString(), "postgres", "postgres", "bigg");
    }

}
