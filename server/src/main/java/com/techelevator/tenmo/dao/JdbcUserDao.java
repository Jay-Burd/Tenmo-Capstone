package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.model.*;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcUserDao implements UserDao {

    private JdbcTemplate jdbcTemplate;

    public JdbcUserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int findIdByUsername(String username) {
        String sql = "SELECT user_id FROM tenmo_user WHERE username ILIKE ?;";
        Integer id = jdbcTemplate.queryForObject(sql, Integer.class, username);
        if (id != null) {
            return id;
        } else {
            return -1;
        }
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username, password_hash FROM tenmo_user;";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql);
        while(results.next()) {
            User user = mapRowToUser(results);
            users.add(user);
        }
        return users;
    }

    @Override
    public User findByUsername(String username) throws UsernameNotFoundException {
        String sql = "SELECT user_id, username, password_hash FROM tenmo_user WHERE username ILIKE ?;";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, username);
        if (rowSet.next()){
            return mapRowToUser(rowSet);
        }
        throw new UsernameNotFoundException("User " + username + " was not found.");
    }

    @Override
    public boolean create(String username, String password) {

        // create user
        String sql = "INSERT INTO tenmo_user (username, password_hash) VALUES (?, ?) RETURNING user_id";
        String password_hash = new BCryptPasswordEncoder().encode(password);
        Integer newUserId;
        try {
            newUserId = jdbcTemplate.queryForObject(sql, Integer.class, username, password_hash);
            sql = "INSERT INTO account (user_id, balance) VALUES (?, ?);";
            int numberOfRows = jdbcTemplate.update(sql, newUserId, new BigDecimal(1000));
            if(numberOfRows!= 1){
                return false;
            }
        } catch (DataAccessException e) {
            return false;
        }

        return true;
    }

    public Account retrieveBalances(String userName){
        Account account = new Account();
        String sql = "SELECT username, balance FROM account JOIN tenmo_user ON account.user_id = tenmo_user.user_id " +
                "WHERE username = ?;";
        try{
            SqlRowSet rows = jdbcTemplate.queryForRowSet(sql, userName);
            while(rows.next()){

                account.setUsername(rows.getString("username"));
                account.setBalance(rows.getBigDecimal("balance"));

            }
        } catch (ResourceAccessException e){

        }

        return account;
    }

    public List<UserName> listUsersForTransfer(String userName) {
        List<UserName> users = new ArrayList<>();
        String sql = "Select username From tenmo_user Where username != ?;";

        try{
            SqlRowSet row = jdbcTemplate.queryForRowSet(sql, userName);
            while(row.next()) {
                UserName user = new UserName();
                user.setUserName(row.getString("username"));
                users.add(user);
            }
        } catch (ResourceAccessException e) {
            e.printStackTrace();
        }
        return users;
    }

    public boolean transfer(Account transferInfo, String userName){
        boolean success = false;
        if(transferInfo.getUsername().equals(userName)) {
            throw new ResourceAccessException("Cannot transfer to self");
        }
        String checkSql = "SELECT balance FROM account JOIN tenmo_user ON account.user_id = tenmo_user.user_id WHERE username = ?;";
        String addSql = "UPDATE account SET balance = balance + ? WHERE account_id = (SELECT account_id FROM account JOIN tenmo_user " +
                "ON account.user_id = tenmo_user.user_id WHERE username = ?);";
        String subtractSql = "UPDATE account SET balance = balance - ? WHERE account_id = (SELECT account_id FROM account JOIN tenmo_user " +
                "ON account.user_id = tenmo_user.user_id WHERE username = ?);";

        try {


            SqlRowSet row = jdbcTemplate.queryForRowSet(checkSql, userName);
            if(row.next()){
                if (row.getBigDecimal("balance").compareTo(transferInfo.getBalance()) < 0){
                    throw new DataIntegrityViolationException("Transfer amount more than account balance");
                }
            }

            int numberOfRows = jdbcTemplate.update(addSql, transferInfo.getBalance(), transferInfo.getUsername());
            if (numberOfRows == 0){
                System.out.println("Didn't update");

            }
            numberOfRows = jdbcTemplate.update(subtractSql, transferInfo.getBalance(), userName);
            if (numberOfRows == 0){
                System.out.println("Didn't update");

            }
            success = true;
        } catch (ResourceAccessException e){

        } catch (NullPointerException e){
            System.out.println("Couldn't find transfer info");
        }
        return success;
    }

    public Transfer recordTransfer(Account transferInfo, String userName) {
        Transfer transferAttempt = new Transfer();
        String transferSql = "INSERT INTO transfer (sender_id, receiver_id, amount) " +
                "VALUES ((SELECT user_id FROM tenmo_user WHERE username = ?), " +
                "(SELECT user_id FROM tenmo_user WHERE username = ?), " +
                "?) RETURNING transfer_id;";

        try {
            transferAttempt.setFrom(userName);

            transferAttempt.setTo(transferInfo.getUsername());

            transferAttempt.setTransferAmount(transferInfo.getBalance());

            transferAttempt.setStatus("Approved");

            int newTransferId = jdbcTemplate.queryForObject(transferSql, Integer.class, userName, transferInfo.getUsername(), transferInfo.getBalance());
            transferAttempt.setTransferId(newTransferId);
        }catch (ResourceAccessException e){
            e.printStackTrace();
        }
        return transferAttempt;
    }

    public List<Transfer> listTransfers(String username) {
        List <Transfer> transfers = new ArrayList<>();
        String sql = "Select transfer_id, username, amount, status From transfer Join tenmo_user ON transfer.receiver_id = tenmo_user.user_id Where sender_id = (Select user_id From tenmo_user Where username = ?);";
        String receivedSql = "Select transfer_id, username, amount, status From transfer Join tenmo_user ON transfer.sender_id = tenmo_user.user_id Where receiver_id = (Select user_id From tenmo_user Where username = ?);";
         try{
             SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, username);
             while(rowSet.next()) {
                 Transfer transfer = new Transfer();
                 transfer.setTransferId(rowSet.getInt("transfer_id"));
                 transfer.setTo(rowSet.getString("username"));
                 transfer.setTransferAmount(rowSet.getBigDecimal("amount"));
                 transfer.setStatus(rowSet.getString("status"));
                 transfer.setFrom(username);
                 transfers.add(transfer);
             }
             rowSet = jdbcTemplate.queryForRowSet(receivedSql, username);
             while(rowSet.next()) {
                 Transfer transfer = new Transfer();
                 transfer.setTransferId(rowSet.getInt("transfer_id"));
                 transfer.setTo(rowSet.getString("username"));
                 transfer.setTransferAmount(rowSet.getBigDecimal("amount"));
                 transfer.setStatus(rowSet.getString("status"));
                 transfer.setFrom(username);
                 transfers.add(transfer);
             }
         }catch (ResourceAccessException e) {

         }
         return transfers;
    }

    public Transfer getTransferById(int transferId, String username) {
        Transfer transfer = new Transfer();
        String sql = "Select transfer_id, username, sender_id, amount, status From transfer Join tenmo_user ON transfer.receiver_id = tenmo_user.user_id Where transfer_id = ?;";
        String receivedSql = "Select username From tenmo_user Where user_id = ?;";

        try{
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, transferId);
            int userId = -1;
            if(rowSet.next()) {
                transfer.setTransferId(transferId);
                transfer.setTo(rowSet.getString("username"));
                transfer.setTransferAmount(rowSet.getBigDecimal("amount"));
                transfer.setStatus(rowSet.getString("status"));
                userId = rowSet.getInt("sender_id");
            }
            rowSet = jdbcTemplate.queryForRowSet(receivedSql, userId);
            if(rowSet.next()) {
                transfer.setFrom(rowSet.getString("username"));
            }
        }catch(ResourceAccessException e) {

        }
        if(!username.equals(transfer.getFrom()) && !username.equals(transfer.getTo())){
            throw new ResourceAccessException("You do not have access to this transfer information.");
        }
        return transfer;
    }

    public Transfer requestTransfer(Account transferInfo, String username){

        Transfer transferAttempt = new Transfer();
        if(transferInfo.getUsername().equals(username)) {
            throw new ResourceAccessException("Cannot transfer to self");
        }

        String transferSql = "INSERT INTO transfer (sender_id, receiver_id, amount, status) " +
                "VALUES ((SELECT user_id FROM tenmo_user WHERE username = ?), " +
                "(SELECT user_id FROM tenmo_user WHERE username = ?), " +
                "?, 'Pending') RETURNING transfer_id;";

        try {

            transferAttempt.setTo(username);

            transferAttempt.setFrom(transferInfo.getUsername());

            transferAttempt.setTransferAmount(transferInfo.getBalance());

            transferAttempt.setStatus("Pending");

            int newTransferId = jdbcTemplate.queryForObject(transferSql, Integer.class, transferInfo.getUsername(), username, transferInfo.getBalance());
            transferAttempt.setTransferId(newTransferId);

        } catch (ResourceAccessException e){

        } catch (NullPointerException e){
            System.out.println("Couldn't find transfer info");
        }

        return transferAttempt;
    }

    public List<Transfer> getPendingRequests(String username){
        List <Transfer> transfers = new ArrayList<>();
        String sql = "Select transfer_id, username, amount, status From transfer Join tenmo_user ON transfer.receiver_id = tenmo_user.user_id Where sender_id = (Select user_id From tenmo_user Where username = ?) AND status = 'Pending';";
        String receivedSql = "Select transfer_id, username, amount, status From transfer Join tenmo_user ON transfer.sender_id = tenmo_user.user_id Where receiver_id = (Select user_id From tenmo_user Where username = ?) AND status = 'Pending';";
        try{
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, username);
            while(rowSet.next()) {
                Transfer transfer = new Transfer();
                transfer.setTransferId(rowSet.getInt("transfer_id"));
                transfer.setTo(rowSet.getString("username"));
                transfer.setTransferAmount(rowSet.getBigDecimal("amount"));
                transfer.setStatus(rowSet.getString("status"));
                transfer.setFrom(username);
                transfers.add(transfer);
            }
            rowSet = jdbcTemplate.queryForRowSet(receivedSql, username);
            while(rowSet.next()) {
                Transfer transfer = new Transfer();
                transfer.setTransferId(rowSet.getInt("transfer_id"));
                transfer.setTo(rowSet.getString("username"));
                transfer.setTransferAmount(rowSet.getBigDecimal("amount"));
                transfer.setStatus(rowSet.getString("status"));
                transfer.setFrom(username);
                transfers.add(transfer);
            }
        }catch (ResourceAccessException e) {

        }
        return transfers;
    }

    public Transfer acceptRequest(TransferStatusUpdate update, String username) {
        //check for: does receiver have enough TE, pending status required, make sure user accepting request match the from(sender_id)
        Transfer transfer = getTransferById(update.getTransferId(), username);
        Account transferInfo = new Account();
        transferInfo.setUsername(transfer.getTo());
        transferInfo.setBalance(transfer.getTransferAmount());
        String sql = "Update transfer Set status = 'Approved' Where transfer_id = ?;";
        if(!transfer.getStatus().equals("Pending")) {
            throw new ResourceAccessException("Request has already been processed.");
        }
        if(!transfer.getFrom().equals(username)) {
            throw new ResourceAccessException("You do not have permission to access this record.");
        }
        if(transfer(transferInfo, username)) {
            try{
                int numberOfRows = jdbcTemplate.update(sql, update.getTransferId());
                if(numberOfRows == 0) {
                    throw new ResourceAccessException("Did not update.");
                }
            }catch(ResourceAccessException e) {
                e.printStackTrace();
            }
        } else {
            throw new ResourceAccessException("Transfer Request Can Not Be Approved, Not Enough Funds");
        }
        return getTransferById(update.getTransferId(), username);
    }

    public Transfer rejectRequest(TransferStatusUpdate update, String username) {
        Transfer transfer = getTransferById(update.getTransferId(), username);
        String sql = "Update transfer Set status = 'Rejected' Where transfer_id = ?;";
        if(!transfer.getStatus().equals("Pending")) {
            throw new ResourceAccessException("Request has already been processed.");
        }
        if(!transfer.getFrom().equals(username)) {
            throw new ResourceAccessException("You do not have permission to access this record.");
        }
        try{
            int numberOfRows = jdbcTemplate.update(sql, update.getTransferId());
            if(numberOfRows == 0) {
                throw new ResourceAccessException("Did not update.");
            }
        }catch(ResourceAccessException e) {
            e.printStackTrace();
        }
        return getTransferById(update.getTransferId(), username);
    }

    public Account depositMoney(BigDecimal amount, String username) {
        String sql = "Update account Set balance = balance + ? Where user_id = ?;";
        try{
            int numberOfRows = jdbcTemplate.update(sql, amount, findIdByUsername(username));
            if(numberOfRows == 0) {
                throw new ResourceAccessException("Deposit failed, account not updated.");
            }
        }catch (ResourceAccessException e) {
            e.printStackTrace();
        }
        return retrieveBalances(username);
    }

    private User mapRowToUser(SqlRowSet rs) {
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password_hash"));
        user.setActivated(true);
        user.setAuthorities("USER");
        return user;
    }


}
