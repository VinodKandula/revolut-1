package server;

import db.MemoryDatabase;
import model.TransferRequest;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static db.tables.Account.ACCOUNT;

/**
 * Not actually unit or functional test.
 *
 * This class tests only approach used for correct implementation of concurrent transfers: MVCC engine + exclusive locks.
 */
public class ConcurrentTransfersTest {
    private MemoryDatabase db;

    @BeforeEach
    void setUp() throws Exception {
//        JdbcConnectionPool pool = JdbcConnectionPool.create("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MULTI_THREADED=1;", "sa", "");
//        ctx = DSL.using(pool, SQLDialect.H2);
//
//        ctx.execute("RUNSCRIPT FROM 'classpath:/h2/schema.sql'");
//        ctx.execute("RUNSCRIPT FROM 'classpath:/h2/test-data.sql'");

        db = new MemoryDatabase("/h2/test-data.sql");
    }

    @AfterEach
    void tearDown() {
        db.ctx().execute("SHUTDOWN IMMEDIATELY");
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

        BigDecimal b1 = db.ctx().select(ACCOUNT.BALANCE).from(ACCOUNT).where(ACCOUNT.ID.eq(1L)).fetchOne(ACCOUNT.BALANCE);
        BigDecimal b2 = db.ctx().select(ACCOUNT.BALANCE).from(ACCOUNT).where(ACCOUNT.ID.eq(2L)).fetchOne(ACCOUNT.BALANCE);

        Assertions.assertEquals(BigDecimal.valueOf(40000, 2), b1);
        Assertions.assertEquals(BigDecimal.valueOf(30000, 2), b2);
    }

    private void transferAmount(TransferRequest trReq) {
        db.ctx().transaction(configuration -> {
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

    //TODO Fix test
    @Test
    @Disabled
    void testTransactions_AutocommitIsTurnedOffForTxScope() throws Exception {
        Assertions.assertEquals(BigDecimal.valueOf(30000, 2), checkAutocommitRead(1), "Balance before tx");

        Thread t1 = new Thread(() -> {
            Assertions.assertEquals(BigDecimal.valueOf(20000, 2), checkAutocommitUpdate(1, BigDecimal.valueOf(100)), "Balance by tx");
        });

        t1.start();

        Thread.sleep(500);

        Assertions.assertEquals(BigDecimal.valueOf(30000, 2), checkAutocommitRead(1), "Balance in the middle of tx");

        t1.join();

        db.ctx().transaction(configuration -> {
            System.out.println("XXXXXX: " + DSL.using(configuration).select(ACCOUNT.BALANCE).from(ACCOUNT)
                    .where(ACCOUNT.ID.eq(1L))
                    .fetchOne(ACCOUNT.BALANCE));
        });

        Assertions.assertEquals(BigDecimal.valueOf(20000, 2), checkAutocommitRead(1), "Balance after tx");

    }

    private BigDecimal checkAutocommitUpdate(long accId, BigDecimal v) {
        return db.ctx().transactionResult(configuration -> {
            DSL.using(configuration)
                    .update(ACCOUNT)
                    .set(ACCOUNT.BALANCE, ACCOUNT.BALANCE.minus(v))
                    .where(ACCOUNT.ID.eq(accId))
                    .execute();

            Thread.sleep(2000);

            return DSL.using(configuration)
                    .select(ACCOUNT.BALANCE).from(ACCOUNT)
                    .where(ACCOUNT.ID.eq(accId))
                    .fetchOne(ACCOUNT.BALANCE);
        });
    }

    private BigDecimal checkAutocommitRead(long accId) {
        return db.ctx()
                .select(ACCOUNT.BALANCE).from(ACCOUNT)
                .where(ACCOUNT.ID.eq(accId))
                .fetchOne(ACCOUNT.BALANCE);
    }
}
