package apoc.load;

import apoc.util.MySQLContainerExtension;
import apoc.util.TestUtil;
import apoc.util.Util;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.test.rule.DbmsRule;
import org.neo4j.test.rule.ImpermanentDbmsRule;

import java.util.Map;

import static apoc.util.TestUtil.testCall;
import static org.junit.Assert.assertEquals;

public class MySQLJdbcTest extends AbstractJdbcTest {

    @ClassRule
    public static MySQLContainerExtension mysql = new MySQLContainerExtension();

    @ClassRule
    public static DbmsRule db = new ImpermanentDbmsRule();

    @BeforeClass
    public static void setUpContainer() {
        mysql.start();
        TestUtil.registerProcedure(db, Jdbc.class);
        db.executeTransactionally("CALL apoc.load.driver($driver)", Util.map("driver", mysql.getDriverClassName()));
    }

    @AfterClass
    public static void tearDown() {
        mysql.stop();
        db.shutdown();
    }

    @Test
    public void testLoadJdbc() {
        testCall(db, "CALL apoc.load.jdbc($url, $table, [])",
            Util.map(
                "url", mysql.getJdbcUrl(),
                "table", "countrylanguage"),
            row -> {
                Map<String, Object> expected = Util.map(
                        "ID", 8,
                        "Name", "Utrecht",
                        "CountryCode", "NLD",
                        "District", "Utrecht",
                        "Population", 234323);
                assertEquals(expected, row.get("row"));
            });
    }
}
