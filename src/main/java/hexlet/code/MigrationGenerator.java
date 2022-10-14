package hexlet.code;

import io.ebean.annotation.Platform;
import io.ebean.dbmigration.DbMigration;
import java.io.IOException;

public class MigrationGenerator {
    public static void main(String[] args) throws IOException {
        DbMigration dbMigration = DbMigration.create();

        dbMigration.addPlatform(Platform.POSTGRES, "postgres");
        dbMigration.addPlatform(Platform.H2, "h2");

        dbMigration.generateMigration();
    }
}
