package model;

import java.math.BigDecimal;
import java.util.Date;

public class Transfer {
    public long id;
    public BigDecimal amount;
    public long sender;
    public long recipient;
    public Date date;
}
