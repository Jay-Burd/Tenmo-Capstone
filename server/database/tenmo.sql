BEGIN TRANSACTION;
DROP TABLE IF EXISTS tenmo_user, account, transfer;
DROP SEQUENCE IF EXISTS seq_user_id, seq_account_id, seq_transfer_id;
-- Sequence to start user_id values at 1001 instead of 1
CREATE SEQUENCE seq_user_id
  INCREMENT BY 1
  START WITH 1001
  NO MAXVALUE;
CREATE TABLE tenmo_user (
	user_id int NOT NULL DEFAULT nextval('seq_user_id'),
	username varchar(50) NOT NULL,
	password_hash varchar(200) NOT NULL,
	CONSTRAINT PK_tenmo_user PRIMARY KEY (user_id),
	CONSTRAINT UQ_username UNIQUE (username)
);
-- Sequence to start account_id values at 2001 instead of 1
-- Note: Use similar sequences with unique starting values for additional tables
CREATE SEQUENCE seq_account_id
  INCREMENT BY 1
  START WITH 2001
  NO MAXVALUE;
CREATE TABLE account (
	account_id int NOT NULL DEFAULT nextval('seq_account_id'),
	user_id int NOT NULL,
	balance numeric(13, 2) NOT NULL,
	CONSTRAINT PK_account PRIMARY KEY (account_id),
	CONSTRAINT FK_account_tenmo_user FOREIGN KEY (user_id) REFERENCES tenmo_user (user_id)
);
CREATE SEQUENCE seq_transfer_id
  INCREMENT BY 1
  START WITH 3001
  NO MAXVALUE;

Create Table transfer (
	transfer_id int Not NULL Default nextval('seq_transfer_id'),
	sender_id int Not NULL,
	receiver_id int Not NULL,
	amount numeric(14,2) Not NULL,
	status varchar(40) Not NULL Default ('Approved'),
	Constraint PK_transfer PRIMARY KEY (transfer_id),
	Constraint FK_transfer_tenmo_user FOREIGN KEY (sender_id) References tenmo_user (user_id),
	Constraint FK_transfer_tenmo_user2 FOREIGN KEY (receiver_id) References tenmo_user (user_id)
);
COMMIT;