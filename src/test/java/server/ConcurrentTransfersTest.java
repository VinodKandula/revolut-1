package server;

import model.TransferRequest;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static db.tables.Account.ACCOUNT;

/**
 * Not actually unit or functional test.
 *
 * This class tests only approach used for correct implementation of concurrent transfers: MVCC engine + exclusive locks.
 */
public class ConcurrentTransfersTest {
    private DSLContext ctx;

    @BeforeEach
    void setUp() {
        String dbUrl = "jdbc:h2:mem:transfers;DB_CLOSE_DELAY=-1;MULTI_THREADED=1;" +
                "INIT=RUNSCRIPT FROM 'classpath:/h2/schema.sql'\\;RUNSCRIPT FROM 'classpath:/h2/test-data.sql';";
        JdbcConnectionPool pool = JdbcConnectionPool.create(dbUrl, "sa", "");

        ctx = DSL.using(pool, SQLDialect.H2);
    }

    @AfterEach
    void tearDown() {
        ctx.execute("SHUTDOWN IMMEDIATELY");
    }

    @Test
    void testConcurrentTransfers_FinalBalancesAsForSerialTransfers() throws Exception {
        Thread t1 = new Thread(() -> {
            TransferRequest tr = new TransferRequest();

            tr.fromAcc = 1;
            tr.toAcc = 2;
            tr.amount = BigDecimal.valueOf(100);

            transferAmount(tr);
        });

        Thread t2 = new Thread(() -> {
            try {
                Thread.currentThread().sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            TransferRequest tr = new TransferRequest();

            tr.fromAcc = 2;
            tr.toAcc = 1;
            tr.amount = BigDecimal.valueOf(200);

            transferAmount(tr);
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        BigDecimal b1 = ctx.select(ACCOUNT.BALANCE).from(ACCOUNT).where(ACCOUNT.ID.eq(1L)).fetchOne(ACCOUNT.BALANCE);
        BigDecimal b2 = ctx.select(ACCOUNT.BALANCE).from(ACCOUNT).where(ACCOUNT.ID.eq(2L)).fetchOne(ACCOUNT.BALANCE);

        Assertions.assertEquals(b1, BigDecimal.valueOf(40000, 2));
        Assertions.assertEquals(b2, BigDecimal.valueOf(30000, 2));
    }

    private void transferAmount(TransferRequest trReq) {
        ctx.transaction(configuration -> {
            DSL.using(configuration)
                    .selectFrom(ACCOUNT).where(ACCOUNT.ID.eq(trReq.fromAcc).or(ACCOUNT.ID.eq(trReq.toAcc)))
                    .forUpdate().fetchInto(model.Account.class);

            DSL.using(configuration)
                    .update(ACCOUNT)
                    .set(ACCOUNT.BALANCE, ACCOUNT.BALANCE.minus(trReq.amount))
                    .where(ACCOUNT.ID.eq(trReq.fromAcc))
                    .execute();

            Thread.sleep(1000);

            DSL.using(configuration)
                    .update(ACCOUNT)
                    .set(ACCOUNT.BALANCE, ACCOUNT.BALANCE.plus(trReq.amount))
                    .where(ACCOUNT.ID.eq(trReq.toAcc))
                    .execute();
        });
    }
}
