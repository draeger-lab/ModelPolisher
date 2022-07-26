package edu.ucsd.sbrg.bigg.annotation;

import edu.ucsd.sbrg.db.BiGGDB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@Testcontainers
public abstract class BiGGDBTest {


    @BeforeEach
    public void setup() {
        BiGGDB.init(bigg.getHost(), bigg.getFirstMappedPort().toString(), "postgres", "postgres", "bigg");
    }

    @AfterEach
    public void cleanUp() {
        if (BiGGDB.inUse())
            BiGGDB.close();
    }

    @Container
    public GenericContainer bigg = new GenericContainer(DockerImageName.parse("preloaded_bigg:latest"))
            .withExposedPorts(5432)
            .withEnv("POSTGRES_PASSWORD", "postgres")
            .withStartupTimeout(Duration.ofMinutes(5));

}
