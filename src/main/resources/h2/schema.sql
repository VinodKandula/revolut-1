DROP TABLE IF EXISTS ACCOUNT;
CREATE TABLE ACCOUNT (
  ID BIGINT PRIMARY KEY AUTO_INCREMENT,
  NUMBER VARCHAR(25) NOT NULL,
  BALANCE DECIMAL(19,2) DEFAULT 0 CHECK (BALANCE >= 0)
);

DROP TABLE IF EXISTS TRANSFER;
CREATE TABLE TRANSFER (
  ID BIGINT PRIMARY KEY AUTO_INCREMENT,
  AMOUNT DECIMAL(19,2) CHECK (AMOUNT >= 0),
  FROM_ACC BIGINT,
  TO_ACC BIGINT,
  DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),

  FOREIGN KEY (FROM_ACC) REFERENCES ACCOUNT(ID),
  FOREIGN KEY (TO_ACC) REFERENCES ACCOUNT(ID),

  CONSTRAINT NOT_EQUAL_ACCS CHECK (FROM_ACC != TO_ACC)
);
