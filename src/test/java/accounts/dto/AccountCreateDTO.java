package accounts.dto;


/**
 * @author Jakub Kalinowski-Zajdak
 */
public class AccountCreateDTO {

	public String owner;
	public double singleWithdrawLimit;
	public double balance;
	
	public AccountCreateDTO() {
		
	}
	
	public AccountCreateDTO(String pOwner, double pSingleWithdrawLimit, double pBalance) {
		owner = pOwner;
		singleWithdrawLimit = pSingleWithdrawLimit;
		balance = pBalance;
	}
}
