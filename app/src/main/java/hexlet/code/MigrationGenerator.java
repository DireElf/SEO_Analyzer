package hexlet.code;

import io.ebean.annotation.Platform;
import io.ebean.dbmigration.DbMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MigrationGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationGenerator.class);

    public static void main(String[] args) throws IOException {
        LOGGER.debug("Initialise DB migration");
        DbMigration dbMigration = DbMigration.create();

        dbMigration.addPlatform(Platform.H2, "h2");
        dbMigration.addPlatform(Platform.POSTGRES, "postgres");

        dbMigration.generateMigration();
    }
}
