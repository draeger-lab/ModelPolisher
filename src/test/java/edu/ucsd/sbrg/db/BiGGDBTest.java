package edu.ucsd.sbrg.db;

import edu.ucsd.sbrg.annotation.BiGGDBContainerTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class BiGGDBTest extends BiGGDBContainerTest {


    @Test
    public void getSubsystems() {
        var org = BiGGDB.getOrganism("iJO1366");
        assertTrue(org.isPresent());
        assertEquals("Escherichia coli str. K-12 substr. MG1655", org.get());
    }

}
