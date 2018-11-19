package accounts.dto;


/**
 * @author Jakub Kalinowski-Zajdak
 */
public class AccountHistoryDTO {

    public String accountId;
    public String operationType;
    public String fromTo;
    public double beforeBalance;
    public double afterBalance;

    public  AccountHistoryDTO() {

    }
    public AccountHistoryDTO(String pAccountId, String pOperationType, String pFromTo, double pBeforeBalance, double pAfterBalance) {
        accountId = pAccountId;
        operationType = pOperationType;
        fromTo = pFromTo;
        beforeBalance = pBeforeBalance;
        afterBalance = pAfterBalance;
    }

}
