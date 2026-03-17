-- ============================================================
-- RevPay Oracle Database Setup Script
-- Run this in SQL*Plus or SQL Developer as SYSTEM user
-- ============================================================

-- ============================================================
-- CLEANUP (Drop existing tables/sequences if re-running)
-- ============================================================
BEGIN
    FOR t IN (SELECT table_name FROM user_tables WHERE table_name IN (
        'INVOICE_ITEMS','INVOICES','LOAN_APPLICATIONS','BUSINESS_PROFILES',
        'NOTIFICATION_PREFS','NOTIFICATIONS','MONEY_REQUESTS',
        'TRANSACTIONS','PAYMENT_METHODS','SECURITY_QUESTIONS','USERS'
    )) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS';
    END LOOP;
END;
/

BEGIN
    FOR s IN (SELECT sequence_name FROM user_sequences WHERE sequence_name IN (
        'SEQ_USER_ID','SEQ_TXN_ID','SEQ_METHOD_ID','SEQ_REQUEST_ID',
        'SEQ_NOTIF_ID','SEQ_PREF_ID','SEQ_INVOICE_ID','SEQ_ITEM_ID',
        'SEQ_LOAN_ID','SEQ_PROFILE_ID','SEQ_QUESTION_ID'
    )) LOOP
        EXECUTE IMMEDIATE 'DROP SEQUENCE ' || s.sequence_name;
    END LOOP;
END;
/

-- ============================================================
-- SEQUENCES
-- ============================================================
CREATE SEQUENCE SEQ_USER_ID     START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_QUESTION_ID START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_METHOD_ID   START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_TXN_ID      START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_REQUEST_ID  START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_NOTIF_ID    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_PREF_ID     START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_INVOICE_ID  START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_ITEM_ID     START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_LOAN_ID     START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_PROFILE_ID  START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ============================================================
-- USERS TABLE
-- ============================================================
CREATE TABLE USERS (
    user_id         NUMBER PRIMARY KEY,
    full_name       VARCHAR2(100) NOT NULL,
    email           VARCHAR2(150) UNIQUE NOT NULL,
    phone           VARCHAR2(20) UNIQUE,
    username        VARCHAR2(50) UNIQUE NOT NULL,
    password_hash   VARCHAR2(255) NOT NULL,
    role            VARCHAR2(20) DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    account_type    VARCHAR2(10) DEFAULT 'PERSONAL' CHECK (account_type IN ('PERSONAL', 'BUSINESS')),
    wallet_balance  NUMBER(15,2) DEFAULT 0.00,
    is_active       NUMBER(1) DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE TRIGGER TRG_USER_ID
    BEFORE INSERT ON USERS
    FOR EACH ROW
BEGIN
    IF :NEW.user_id IS NULL THEN
        SELECT SEQ_USER_ID.NEXTVAL INTO :NEW.user_id FROM DUAL;
    END IF;
END;
/

-- ============================================================
-- SECURITY QUESTIONS TABLE
-- ============================================================
CREATE TABLE SECURITY_QUESTIONS (
    question_id NUMBER PRIMARY KEY,
    user_id     NUMBER NOT NULL REFERENCES USERS(user_id) ON DELETE CASCADE,
    question    VARCHAR2(500) NOT NULL,
    answer_hash VARCHAR2(255) NOT NULL
);

CREATE OR REPLACE TRIGGER TRG_QUESTION_ID
    BEFORE INSERT ON SECURITY_QUESTIONS
    FOR EACH ROW
BEGIN
    IF :NEW.question_id IS NULL THEN
        SELECT SEQ_QUESTION_ID.NEXTVAL INTO :NEW.question_id FROM DUAL;
    END IF;
END;
/

-- ============================================================
-- BUSINESS PROFILES TABLE
-- ============================================================
CREATE TABLE BUSINESS_PROFILES (
    profile_id      NUMBER PRIMARY KEY,
    user_id         NUMBER UNIQUE NOT NULL REFERENCES USERS(user_id) ON DELETE CASCADE,
    business_name   VARCHAR2(200) NOT NULL,
    business_type   VARCHAR2(100),
    tax_id          VARCHAR2(50),
    gst_number      VARCHAR2(50),
    address         VARCHAR2(500),
    status          VARCHAR2(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE TRIGGER TRG_PROFILE_ID
    BEFORE INSERT ON BUSINESS_PROFILES
    FOR EACH ROW
BEGIN
    IF :NEW.profile_id IS NULL THEN
        SELECT SEQ_PROFILE_ID.NEXTVAL INTO :NEW.profile_id FROM DUAL;
    END IF;
END;
/

-- ============================================================
-- PAYMENT METHODS TABLE
-- ============================================================
CREATE TABLE PAYMENT_METHODS (
    method_id       NUMBER PRIMARY KEY,
    user_id         NUMBER NOT NULL REFERENCES USERS(user_id) ON DELETE CASCADE,
    card_type       VARCHAR2(20) CHECK (card_type IN ('CREDIT','DEBIT','BANK_ACCOUNT')),
    card_number_enc VARCHAR2(255) NOT NULL,
    last_four       VARCHAR2(4) NOT NULL,
    expiry_month    NUMBER(2),
    expiry_year     NUMBER(4),
    cardholder_name VARCHAR2(100),
    billing_address VARCHAR2(500),
    pin_hash        VARCHAR2(255),
    is_default      NUMBER(1) DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    balance         NUMBER(15,2) DEFAULT 10000.00
);

CREATE OR REPLACE TRIGGER TRG_METHOD_ID
    BEFORE INSERT ON PAYMENT_METHODS
    FOR EACH ROW
BEGIN
    IF :NEW.method_id IS NULL THEN
        SELECT SEQ_METHOD_ID.NEXTVAL INTO :NEW.method_id FROM DUAL;
    END IF;
END;
/

-- ============================================================
-- TRANSACTIONS TABLE
-- ============================================================
CREATE TABLE TRANSACTIONS (
    txn_id          NUMBER PRIMARY KEY,
    sender_id       NUMBER REFERENCES USERS(user_id),
    receiver_id     NUMBER REFERENCES USERS(user_id),
    amount          NUMBER(15,2) NOT NULL,
    txn_type        VARCHAR2(20) NOT NULL CHECK (txn_type IN ('SEND','RECEIVE','ADD_FUNDS','WITHDRAWAL','PAYMENT','LOAN_REPAYMENT')),
    status          VARCHAR2(20) DEFAULT 'COMPLETED' CHECK (status IN ('PENDING','COMPLETED','FAILED','CANCELLED')),
    note            VARCHAR2(500),
    reference_id    VARCHAR2(50),
    txn_timestamp   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE TRIGGER TRG_TXN_ID
    BEFORE INSERT ON TRANSACTIONS
    FOR EACH ROW
BEGIN
    IF :NEW.txn_id IS NULL THEN
        SELECT SEQ_TXN_ID.NEXTVAL INTO :NEW.txn_id FROM DUAL;
    END IF;
END;
/

-- ============================================================
-- MONEY REQUESTS TABLE
-- ============================================================
CREATE TABLE MONEY_REQUESTS (
    request_id      NUMBER PRIMARY KEY,
    requester_id    NUMBER NOT NULL REFERENCES USERS(user_id),
    requestee_id    NUMBER NOT NULL REFERENCES USERS(user_id),
    amount          NUMBER(15,2) NOT NULL,
    purpose         VARCHAR2(500),
    status          VARCHAR2(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACCEPTED','DECLINED','CANCELLED')),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE TRIGGER TRG_REQUEST_ID
    BEFORE INSERT ON MONEY_REQUESTS
    FOR EACH ROW
BEGIN
    IF :NEW.request_id IS NULL THEN
        SELECT SEQ_REQUEST_ID.NEXTVAL INTO :NEW.request_id FROM DUAL;
    END IF;
END;
/

-- ============================================================
-- NOTIFICATIONS TABLE
-- ============================================================
CREATE TABLE NOTIFICATIONS (
    notif_id    NUMBER PRIMARY KEY,
    user_id     NUMBER NOT NULL REFERENCES USERS(user_id) ON DELETE CASCADE,
    title       VARCHAR2(200) NOT NULL,
    message     VARCHAR2(1000) NOT NULL,
    notif_type  VARCHAR2(50) DEFAULT 'GENERAL',
    is_read     NUMBER(1) DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE TRIGGER TRG_NOTIF_ID
    BEFORE INSERT ON NOTIFICATIONS
    FOR EACH ROW
BEGIN
    IF :NEW.notif_id IS NULL THEN
        SELECT SEQ_NOTIF_ID.NEXTVAL INTO :NEW.notif_id FROM DUAL;
    END IF;
END;
/

-- ============================================================
-- NOTIFICATION PREFERENCES TABLE
-- ============================================================
CREATE TABLE NOTIFICATION_PREFS (
    pref_id     NUMBER PRIMARY KEY,
    user_id     NUMBER NOT NULL REFERENCES USERS(user_id) ON DELETE CASCADE,
    notif_type  VARCHAR2(50) NOT NULL,
    is_enabled  NUMBER(1) DEFAULT 1,
    CONSTRAINT uq_notif_pref UNIQUE (user_id, notif_type)
);

CREATE OR REPLACE TRIGGER TRG_PREF_ID
    BEFORE INSERT ON NOTIFICATION_PREFS
    FOR EACH ROW
BEGIN
    IF :NEW.pref_id IS NULL THEN
        SELECT SEQ_PREF_ID.NEXTVAL INTO :NEW.pref_id FROM DUAL;
    END IF;
END;
/

-- ============================================================
-- INVOICES TABLE
-- ============================================================
CREATE TABLE INVOICES (
    invoice_id      NUMBER PRIMARY KEY,
    business_user_id NUMBER NOT NULL REFERENCES USERS(user_id),
    invoice_number  VARCHAR2(50) UNIQUE,
    customer_name   VARCHAR2(200),
    customer_email  VARCHAR2(150),
    customer_address VARCHAR2(500),
    subtotal        NUMBER(15,2) DEFAULT 0,
    tax_total       NUMBER(15,2) DEFAULT 0,
    total_amount    NUMBER(15,2) DEFAULT 0,
    status          VARCHAR2(20) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','SENT','PAID','OVERDUE','CANCELLED')),
    payment_terms   VARCHAR2(100),
    due_date        DATE,
    notes           VARCHAR2(1000),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE TRIGGER TRG_INVOICE_ID
    BEFORE INSERT ON INVOICES
    FOR EACH ROW
DECLARE
    v_inv_id NUMBER;
BEGIN
    IF :NEW.invoice_id IS NULL THEN
        SELECT SEQ_INVOICE_ID.NEXTVAL INTO :NEW.invoice_id FROM DUAL;
    END IF;
    IF :NEW.invoice_number IS NULL THEN
        :NEW.invoice_number := 'INV-' || TO_CHAR(SYSDATE,'YYYYMM') || '-' || LPAD(:NEW.invoice_id, 4, '0');
    END IF;
END;
/

-- ============================================================
-- INVOICE ITEMS TABLE
-- ============================================================
CREATE TABLE INVOICE_ITEMS (
    item_id         NUMBER PRIMARY KEY,
    invoice_id      NUMBER NOT NULL REFERENCES INVOICES(invoice_id) ON DELETE CASCADE,
    description     VARCHAR2(500) NOT NULL,
    quantity        NUMBER(10,2) DEFAULT 1,
    unit_price      NUMBER(15,2) NOT NULL,
    tax_rate        NUMBER(5,2) DEFAULT 0,
    line_total      NUMBER(15,2)
);

CREATE OR REPLACE TRIGGER TRG_ITEM_ID
    BEFORE INSERT ON INVOICE_ITEMS
    FOR EACH ROW
BEGIN
    IF :NEW.item_id IS NULL THEN
        SELECT SEQ_ITEM_ID.NEXTVAL INTO :NEW.item_id FROM DUAL;
    END IF;
    :NEW.line_total := :NEW.quantity * :NEW.unit_price * (1 + :NEW.tax_rate / 100);
END;
/

-- ============================================================
-- LOAN APPLICATIONS TABLE
-- ============================================================
CREATE TABLE LOAN_APPLICATIONS (
    loan_id         NUMBER PRIMARY KEY,
    user_id         NUMBER NOT NULL REFERENCES USERS(user_id),
    loan_amount     NUMBER(15,2) NOT NULL,
    purpose         VARCHAR2(500),
    tenure_months   NUMBER(3) NOT NULL,
    interest_rate   NUMBER(5,2) DEFAULT 12.00,
    monthly_emi     NUMBER(15,2),
    status          VARCHAR2(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED','ACTIVE','CLOSED')),
    amount_paid     NUMBER(15,2) DEFAULT 0,
    doc_path        VARCHAR2(500),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE TRIGGER TRG_LOAN_ID
    BEFORE INSERT ON LOAN_APPLICATIONS
    FOR EACH ROW
DECLARE
    r NUMBER;
    n NUMBER;
BEGIN
    IF :NEW.loan_id IS NULL THEN
        SELECT SEQ_LOAN_ID.NEXTVAL INTO :NEW.loan_id FROM DUAL;
    END IF;
    -- Calculate EMI: P * r * (1+r)^n / ((1+r)^n - 1)
    r := :NEW.interest_rate / 100 / 12;
    n := :NEW.tenure_months;
    IF r = 0 THEN
        :NEW.monthly_emi := :NEW.loan_amount / n;
    ELSE
        :NEW.monthly_emi := :NEW.loan_amount * r * POWER(1+r, n) / (POWER(1+r, n) - 1);
    END IF;
END;
/

-- ============================================================
-- STORED PROCEDURE: Transfer Money
-- ============================================================
CREATE OR REPLACE PROCEDURE SP_TRANSFER_MONEY(
    p_sender_id   IN NUMBER,
    p_receiver_id IN NUMBER,
    p_amount      IN NUMBER,
    p_note        IN VARCHAR2,
    p_result      OUT VARCHAR2
) AS
    v_balance NUMBER;
BEGIN
    -- Check sender balance
    SELECT wallet_balance INTO v_balance FROM USERS WHERE user_id = p_sender_id FOR UPDATE;

    IF v_balance < p_amount THEN
        p_result := 'INSUFFICIENT_FUNDS';
        ROLLBACK;
        RETURN;
    END IF;

    -- Deduct from sender
    UPDATE USERS SET wallet_balance = wallet_balance - p_amount,
                     updated_at = CURRENT_TIMESTAMP
    WHERE user_id = p_sender_id;

    -- Credit to receiver
    UPDATE USERS SET wallet_balance = wallet_balance + p_amount,
                     updated_at = CURRENT_TIMESTAMP
    WHERE user_id = p_receiver_id;

    -- Record transaction (SEND side)
    INSERT INTO TRANSACTIONS (sender_id, receiver_id, amount, txn_type, status, note)
    VALUES (p_sender_id, p_receiver_id, p_amount, 'SEND', 'COMPLETED', p_note);

    COMMIT;
    p_result := 'SUCCESS';
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        ROLLBACK;
        p_result := 'USER_NOT_FOUND';
    WHEN OTHERS THEN
        ROLLBACK;
        p_result := 'ERROR: ' || SQLERRM;
END SP_TRANSFER_MONEY;
/

-- ============================================================
-- DEFAULT NOTIFICATION TYPES (reference data)
-- Types: TRANSACTION, MONEY_REQUEST, CARD_CHANGE, LOW_BALANCE, LOAN, INVOICE
-- ============================================================
-- ============================================================
-- DEFAULT SYSTEM ADMIN
-- ============================================================
-- Note: 'admin123' as password_hash. Replace with proper BCrypt hash if application enforces BCrypt on login.
INSERT INTO USERS (user_id, full_name, email, username, password_hash, role, account_type)
VALUES (SEQ_USER_ID.NEXTVAL, 'System Admin', 'admin@revpay.com', 'admin', '$2a$10$XjJH0pb3lkkFR9VDjNx0NucZTwzLfVp/UZKhP8IcX8FFWe02ovUnK', 'ADMIN', 'PERSONAL');

COMMIT;
