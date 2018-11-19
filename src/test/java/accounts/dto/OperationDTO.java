package accounts.dto;


/**
 * @author Jakub Kalinowski-Zajdak
 */
public class OperationDTO {
	
    public String targetAccountId;
    public double value;
    
    public OperationDTO() {
    	
    }
    
    public OperationDTO(String pTargetAccountId, double pAmount) {
        targetAccountId = pTargetAccountId;
    	value = pAmount;
    }
}
