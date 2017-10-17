package server;

import db.MemoryDatabase;
import model.TransferRequest;
import org.hsqldb.jdbc.JDBCPool;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static db.tables.Account.ACCOUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Not actually unit or functional test.
 *
 * This class tests only approach used for correct implementation of concurrent transfers: MVCC engine + exclusive locks.
 */
public class ConcurrentTransfersTest {
    private MemoryDatabase db;

    @BeforeEach
    void setUp() throws Exception {
        db = new MemoryDatabase();
    }

    @AfterEach
    void tearDown() {
       db.ctx().execute("SHUTDOWN");
    }

    @Test
    void testConcurrentTransfers_FinalBalancesAsForSerialTransfers() throws Exception {
        //acc1 = 300
        //acc2 = 400

        //tx1: acc1 ==100=> acc2
        //tx2: acc2 ==200=> acc1

        //acc1 = 400
        //acc2 = 300

        BigDecimal b1 = db.ctx().select(ACCOUNT.BALANCE).from(ACCOUNT).where(ACCOUNT.ID.eq(1L)).fetchOne(ACCOUNT.BALANCE);
        BigDecimal b2 = db.ctx().select(ACCOUNT.BALANCE).from(ACCOUNT).where(ACCOUNT.ID.eq(2L)).fetchOne(ACCOUNT.BALANCE);
        assertEquals(BigDecimal.valueOf(30000, 2), b1);
        assertEquals(BigDecimal.valueOf(40000, 2), b2);

        Thread t1 = new Thread(() -> {
            TransferRequest tr = new TransferRequest();

            tr.fromAcc = 1;
            tr.toAcc = 2;
            tr.amount = BigDecimal.valueOf(100);

            transferAmount("t1", tr);
        });

        Thread t2 = new Thread(() -> {
            TransferRequest tr = new TransferRequest();

            tr.fromAcc = 2;
            tr.toAcc = 1;
            tr.amount = BigDecimal.valueOf(200);

            transferAmount("t2", tr);
        });

        t1.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        t2.start();

        t1.join();
        t2.join();

        b1 = db.ctx().select(ACCOUNT.BALANCE).from(ACCOUNT).where(ACCOUNT.ID.eq(1L)).fetchOne(ACCOUNT.BALANCE);
        b2 = db.ctx().select(ACCOUNT.BALANCE).from(ACCOUNT).where(ACCOUNT.ID.eq(2L)).fetchOne(ACCOUNT.BALANCE);
        assertEquals(BigDecimal.valueOf(40000, 2), b1);
        assertEquals(BigDecimal.valueOf(30000, 2), b2);
    }

    private void transferAmount(String thread, TransferRequest trReq) {
        System.out.println(thread + " " + System.currentTimeMillis() + " : tx started");

        db.ctx().transaction(configuration -> {
            System.out.println(thread + " " + System.currentTimeMillis() + ": transfer " + trReq.amount + " " +
                    "from = " + DSL.using(configuration).select(ACCOUNT.BALANCE).from(ACCOUNT).where(ACCOUNT.ID.eq(trReq.fromAcc)).fetchOne(ACCOUNT.BALANCE) + " " +
                    "to = " + DSL.using(configuration).select(ACCOUNT.BALANCE).from(ACCOUNT).where(ACCOUNT.ID.eq(trReq.toAcc)).fetchOne(ACCOUNT.BALANCE));

            DSL.using(configuration)
                    .update(ACCOUNT)
                    .set(ACCOUNT.BALANCE, ACCOUNT.BALANCE.minus(trReq.amount))
                    .where(ACCOUNT.ID.eq(trReq.fromAcc))
                    .execute();
            System.out.println(thread + " " + System.currentTimeMillis() + " : after updated from = " +
                    DSL.using(configuration).select(ACCOUNT.BALANCE).from(ACCOUNT).where(ACCOUNT.ID.eq(trReq.fromAcc)).fetchOne(ACCOUNT.BALANCE));

            Thread.sleep(2000);

            DSL.using(configuration)
                    .update(ACCOUNT)
                    .set(ACCOUNT.BALANCE, ACCOUNT.BALANCE.plus(trReq.amount))
                    .where(ACCOUNT.ID.eq(trReq.toAcc))
                    .execute();
            System.out.println(thread + " " + System.currentTimeMillis() + " : after updated to = " +
                    DSL.using(configuration).select(ACCOUNT.BALANCE).from(ACCOUNT).where(ACCOUNT.ID.eq(trReq.toAcc)).fetchOne(ACCOUNT.BALANCE));


            System.out.println(thread + " " + System.currentTimeMillis() + ": final balances " +
                    "from = " + DSL.using(configuration).select(ACCOUNT.BALANCE).from(ACCOUNT).where(ACCOUNT.ID.eq(trReq.fromAcc)).fetchOne(ACCOUNT.BALANCE) + " " +
                    "to = " + DSL.using(configuration).select(ACCOUNT.BALANCE).from(ACCOUNT).where(ACCOUNT.ID.eq(trReq.toAcc)).fetchOne(ACCOUNT.BALANCE));
        });

        System.out.println(thread + " " + System.currentTimeMillis() + " : tx finished");
    }

    @Disabled("Test case for possible bug in H2 or its docs")
    @Test
    void testForStackOverflow() throws Exception {
        //acc1 = 300
        //acc2 = 400

        //tx1: acc1 ==50=> acc2
        //tx2: acc2 ==200=> acc1

        //acc1 = 450
        //acc2 = 250

        final String url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MULTI_THREADED=1;" +
                "INIT=RUNSCRIPT FROM 'classpath:/h2/schema.sql'\\;RUNSCRIPT FROM 'classpath:/h2/test-data.sql'";

        //JdbcConnectionPool pool = JdbcConnectionPool.create(url, "sa", "");

        JDBCPool pool = new JDBCPool();
        pool.setUrl("jdbc:hsqldb:res:/hsqldb/transfers");

        assertEquals(BigDecimal.valueOf(30000, 2), readData(pool, 1));
        assertEquals(BigDecimal.valueOf(40000, 2), readData(pool, 2));

        Thread t1 = new Thread(() -> updateData("t1", pool, 1, 2, BigDecimal.valueOf(50)));
        Thread t2 = new Thread(() -> updateData("t2", pool, 2, 1, BigDecimal.valueOf(200)));

        t1.start();
        Thread.sleep(200);
        t2.start();

        t1.join();
        t2.join();

//        assertEquals(BigDecimal.valueOf(45000, 2), readData(pool, 1));
//        assertEquals(BigDecimal.valueOf(25000, 2), readData(pool, 2));

        System.out.println(readData(pool, 1));
        System.out.println(readData(pool, 2));

        try (Connection c = pool.getConnection(); Statement st = c.createStatement()) {
            st.execute("SHUTDOWN IMMEDIATELY");
        }
    }

    private BigDecimal readData(DataSource ds, long id) {
        try (Connection connection = ds.getConnection();
             PreparedStatement select = connection.prepareStatement("SELECT BALANCE FROM ACCOUNT WHERE ID = ?")) {
            return readFromPreparedStatment(select, id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private BigDecimal readFromPreparedStatment(PreparedStatement select, long id) throws Exception {
        select.setLong(1, id);
        ResultSet rs = select.executeQuery();
        rs.next();
        BigDecimal b = rs.getBigDecimal(1);
        rs.close();

        return b;
    }

    private void updateData(String thread, DataSource ds, long from, long to, BigDecimal amount) {
        try {
            try (Connection connection = ds.getConnection();
                 PreparedStatement lock = connection.prepareStatement("SELECT * FROM ACCOUNT WHERE ID = ? OR ID = ? FOR UPDATE");
                 PreparedStatement minus = connection.prepareStatement("UPDATE ACCOUNT SET BALANCE = BALANCE - ? WHERE ID = ?");
                 PreparedStatement plus = connection.prepareStatement("UPDATE ACCOUNT SET BALANCE = BALANCE + ? WHERE ID = ?");
                 PreparedStatement select = connection.prepareStatement("SELECT BALANCE FROM ACCOUNT WHERE ID = ?")
            ) {
                connection.setAutoCommit(false);

                System.out.println(System.currentTimeMillis() + " " + thread + ": transfer " + amount + " " +
                                "from = " + readFromPreparedStatment(select, from) + " to = " + readFromPreparedStatment(select, to));

                minus.setBigDecimal(1, amount);
                minus.setLong(2, from);
                System.out.println(System.currentTimeMillis() + " " + thread + ": from minus " + amount);
                minus.executeUpdate();

                Thread.sleep(2000);

                plus.setBigDecimal(1, amount);
                plus.setLong(2, to);
                System.out.println(System.currentTimeMillis() + " " + thread + ": to plus " + amount);
                plus.executeUpdate();

                System.out.println(System.currentTimeMillis() + " " + thread + ": before commit from = " + readFromPreparedStatment(select, from));
                System.out.println(System.currentTimeMillis() + " " + thread + ": before commit to = " + readFromPreparedStatment(select, to));

                connection.commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
