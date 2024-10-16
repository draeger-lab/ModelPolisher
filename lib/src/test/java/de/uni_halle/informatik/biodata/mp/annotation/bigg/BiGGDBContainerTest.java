package de.uni_halle.informatik.biodata.mp.annotation.bigg;

import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGDB;

import org.junit.AfterClass;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@Testcontainers
public abstract class BiGGDBContainerTest {

    @SuppressWarnings("resource")
    static final GenericContainer<?> biggContainer =
            new GenericContainer<>(DockerImageName.parse("schmirgel/bigg_db:1.6"))
                    .withExposedPorts(5432)
                    .withReuse(true)
                    .withEnv("POSTGRES_PASSWORD", "postgres")
                    .withStartupTimeout(Duration.ofMinutes(5));

    protected static final BiGGDB bigg;

    static {
        biggContainer.start();
        BiGGDB.init(
                biggContainer.getHost(),
                biggContainer.getFirstMappedPort(),
                "postgres",
                "postgres",
                "bigg");
        bigg = new BiGGDB();
    }

    @AfterClass
    public static void tearDown() {
        if (biggContainer != null) {
            biggContainer.close();
        }
    }
}
