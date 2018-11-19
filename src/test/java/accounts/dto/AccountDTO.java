package accounts.dto;


/**
 * @author Jakub Kalinowski-Zajdak
 */
public class AccountDTO {

	public String id;
	public String owner;
	public double singleWithdrawLimit;
	public double balance;
	
	public AccountDTO() {
		
	}
	
	public AccountDTO(String pId, String pOwner, double pSingleWithdrawLimit, double pBalance) {
		id = pId;
		owner = pOwner;
		singleWithdrawLimit = pSingleWithdrawLimit;
		balance = pBalance;
	}

	public String getId() {
		return id;
	}
}
