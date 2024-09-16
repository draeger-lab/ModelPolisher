package de.uni_halle.informatik.biodata.mp.annotation.adb;

import de.uni_halle.informatik.biodata.mp.db.adb.AnnotateDB;
import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGDB;
import org.junit.AfterClass;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@Testcontainers
public abstract class ADBDBContainerTest {

    @SuppressWarnings("resource")
    static final GenericContainer<?> adbContainer =
            new GenericContainer<>(DockerImageName.parse("schmirgel/annotatedb:0.1.1"))
                    .withExposedPorts(5432)
                    .withReuse(true)
                    .withEnv("POSTGRES_PASSWORD", "postgres")
                    .withStartupTimeout(Duration.ofMinutes(5));

    protected static final AnnotateDB adb;

    static {
        adbContainer.start();
        AnnotateDB.init(
                adbContainer.getHost(),
                adbContainer.getFirstMappedPort(),
                "postgres",
                "postgres",
                "adb");
        adb = new AnnotateDB();
    }

    @AfterClass
    public static void tearDown() {
        if (adbContainer != null) {
            adbContainer.close();
        }
    }
}
