package com.techelevator.dao;


import com.techelevator.tenmo.dao.JdbcUserDao;
import com.techelevator.tenmo.model.Account;
import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;

public class JdbcUserDaoTests extends BaseDaoTests{

    private JdbcUserDao sut;

    private Account account1;
    private Account account2;
    private Transfer transfer1;
    private Transfer transfer2;

    @Before
    public void setup() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        sut = new JdbcUserDao(jdbcTemplate);
        account1 = new Account();
        account2 = new Account();
        transfer1 = new Transfer();
        transfer2 = new Transfer();
        account1.setUsername("Adam");
        account1.setBalance(new BigDecimal("1500"));
        account2.setUsername("Brandon");
        account2.setBalance(new BigDecimal("5000"));
    }

    @Test
    public void createNewUser() {
        boolean userCreated = sut.create("TEST_USER","test_password");
        Assert.assertTrue(userCreated);
        User user = sut.findByUsername("TEST_USER");
        Assert.assertEquals("TEST_USER", user.getUsername());
    }




    @org.junit.jupiter.api.Test
    void retrieveBalances() {

    }

    @org.junit.jupiter.api.Test
    void listUsersForTransfer() {
    }

    @org.junit.jupiter.api.Test
    void transfer() {
    }

    @org.junit.jupiter.api.Test
    void recordTransfer() {
    }

    @org.junit.jupiter.api.Test
    void listTransfers() {
    }

    @org.junit.jupiter.api.Test
    void getTransferById() {
    }

    @org.junit.jupiter.api.Test
    void requestTransfer() {
    }

    @org.junit.jupiter.api.Test
    void getPendingRequests() {
    }

    @org.junit.jupiter.api.Test
    void acceptRequest() {
    }

    @org.junit.jupiter.api.Test
    void rejectRequest() {
    }

    @org.junit.jupiter.api.Test
    void depositMoney() {
    }


}
