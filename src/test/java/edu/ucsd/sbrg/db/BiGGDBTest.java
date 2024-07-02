package edu.ucsd.sbrg.db;

import edu.ucsd.sbrg.bigg.annotation.BiGGDBContainerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class BiGGDBTest extends BiGGDBContainerTest {

    @BeforeEach
    public void setup() {
        BiGGDB.init(bigg.getHost(), bigg.getFirstMappedPort().toString(), "postgres", "postgres", "bigg");
    }

    @Container
    public GenericContainer bigg = new GenericContainer(DockerImageName.parse("schmirgel/bigg_db:1.6"))
            .withExposedPorts(5432)
            .withEnv("POSTGRES_PASSWORD", "postgres")
            .withStartupTimeout(Duration.ofMinutes(5));

    @Test
    public void getSubsystems() {
        var org = BiGGDB.getOrganism("iJO1366");
        assertTrue(org.isPresent());
        assertEquals("Escherichia coli str. K-12 substr. MG1655", org.get());
    }

}
