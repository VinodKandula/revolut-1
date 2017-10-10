package service;

import db.tables.Account;
import db.tables.records.TransferRecord;
import model.Transfer;
import model.TransferRequest;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.RecordMapper;

import java.util.List;
import java.util.Optional;

import static db.tables.Account.ACCOUNT;
import static db.tables.Transfer.TRANSFER;

public class TransfersService {
    private final DSLContext ctx;

    private final Account fromAcc = ACCOUNT.as("fromAcc");
    private final Account toAcc = ACCOUNT.as("toAcc");

    public TransfersService(DSLContext ctx) {
        this.ctx = ctx;
    }

    public List<Transfer> getAllTransfers() {
        return ctx.selectFrom(TRANSFER
                .join(fromAcc).onKey(TRANSFER.FROM_ACC)
                .join(toAcc).onKey(TRANSFER.TO_ACC))
                .orderBy(TRANSFER.DATE.desc())
                .fetch(new TransferRecordMapper());
    }

    public Optional<Transfer> getTransfer(long transferId) {
        Optional<Transfer> transfer = ctx.selectFrom(TRANSFER
                .join(fromAcc).onKey(TRANSFER.FROM_ACC)
                .join(toAcc).onKey(TRANSFER.TO_ACC))
                .where(TRANSFER.ID.eq(transferId))
                .fetchOptional(new TransferRecordMapper());

        return transfer;
    }

    //TODO Add transactions
    public Transfer transferAmount(TransferRequest transferRequest) {
        TransferRecord transferRecord = ctx.insertInto(TRANSFER, TRANSFER.FROM_ACC, TRANSFER.TO_ACC, TRANSFER.AMOUNT)
                .values(transferRequest.fromAcc, transferRequest.toAcc, transferRequest.amount)
                .returning(TRANSFER.ID).fetchOne();

        Transfer transfer = ctx.selectFrom(TRANSFER.join(fromAcc).onKey(TRANSFER.FROM_ACC).join(toAcc).onKey(TRANSFER.TO_ACC))
                .where(TRANSFER.ID.eq(transferRecord.getId()))
                .fetchOne(new TransferRecordMapper());

        return transfer;
    }

    private class TransferRecordMapper implements RecordMapper<Record, Transfer> {
        @Override
        public Transfer map(Record record) {
            Transfer t = new Transfer();
            t.fromAcc = new model.Account();
            t.toAcc = new model.Account();

            t.id = record.get(TRANSFER.ID);
            t.amount = record.get(TRANSFER.AMOUNT);
            t.timestamp = record.get(TRANSFER.DATE);

            t.fromAcc.id = record.get(fromAcc.ID);
            t.fromAcc.name = record.get(fromAcc.NAME);

            t.toAcc.id = record.get(toAcc.ID);
            t.toAcc.name = record.get(toAcc.NAME);

            return t;
        }
    }
}
