package model;

import java.math.BigDecimal;
import java.util.Date;

public class Transfer {
    public long id;
    public Date timestamp;
    public Account fromAcc;
    public Account toAcc;
    public BigDecimal amount;
}
