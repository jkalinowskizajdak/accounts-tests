package accounts;

import accounts.dto.AccountCreateDTO;
import accounts.dto.AccountDTO;
import accounts.dto.AccountHistoryDTO;
import accounts.dto.OperationDTO;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;


/**
 * @author Jakub Kalinowski-Zajdak
 */
public class AccountsTest {

    private static final int OK = 200;
    private static final int NO_CONTENT = 204;
    private static final int ERROR = 500;
    private static final int NOT_FOUND = 404;
    private static final int BAD_REQUEST = 400;
    private static final String HOST = System.getProperty("HOST", "localhost");
    private static final int PORT = Integer.parseInt(System.getProperty("PORT", "8080"));
    private static final String ACCOUNTS_PATH = "/rest/accounts";
    private static final double balance_1 = 50000;
    private static final double balance_2 = 80000;
    private static final double limit_1 = 500;
    private static final double limit_2 = 1000;
    private static final String ACCOUNT_ID_PARAM = "accountId";
    private static final String OWNER_PARAM = "accountId";
    private static final String owner_1 = UUID.randomUUID().toString();
    private static final String owner_2 = UUID.randomUUID().toString();
    private static final AccountCreateDTO testAccount_1 = new AccountCreateDTO(owner_1, limit_1, balance_1);
    private static final AccountCreateDTO testAccount_2 = new AccountCreateDTO(owner_2, limit_2, balance_2);


    static {
        RestAssured.config = RestAssuredConfig.config()
                .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory((aClass, s) -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper;
                }));
    }

    public static RequestSpecification given() {
        return RestAssured.given().baseUri("http://" + HOST).port(PORT).contentType(ContentType.JSON);
    }

    private static String accountId_1;
    private static String accountId_2;

    @BeforeClass
    public static void addAccounts() {
        accountId_1 = addAccount(testAccount_1);
        accountId_2 = addAccount(testAccount_2);
    }

    @AfterClass
    public static void deleteAccounts() {
        deleteAccount(accountId_1);
        deleteAccount(accountId_2);
    }

    @Test
    public void checkEmptyOwnerCreation() {
        Response response = getResultCreateAccount(new AccountCreateDTO("", balance_1, limit_1));
        Assertions.assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void checkLessThenZeroBalanceCreation() {
        Response response = getResultCreateAccount(new AccountCreateDTO(owner_1, -10d, limit_1));
        Assertions.assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void checkLessThenZeroLimitCreation() {
        Response response = getResultCreateAccount(new AccountCreateDTO(owner_1, balance_1, -10d));
        Assertions.assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void checkAccounts() {
        AccountDTO[] accounts = getAccounts();
        Assertions.assertThat(accounts).extracting(AccountDTO::getId).contains(accountId_1, accountId_2);
    }

    @Test
    public void checkOwnerAccounts() {
        AccountDTO[] accounts = getOwnerAccounts(owner_1);
        Assertions.assertThat(accounts).extracting(AccountDTO::getId).contains(accountId_1);
        Assertions.assertThat(accounts).extracting(AccountDTO::getId).doesNotContain(accountId_2);
    }

    @Test
    public void checkLimits() {
        AccountDTO accountTest1 = getAccount(accountId_1);
        Assertions.assertThat(accountTest1.singleWithdrawLimit).isEqualTo(limit_1);

        AccountDTO accountTest2 = getAccount(accountId_2);
        Assertions.assertThat(accountTest2.singleWithdrawLimit).isEqualTo(limit_2);
    }

    @Test
    public void checkEmptyTargetAccountOperation() {
        double value = balance_1 + 100;
        Response response = performOperation(accountId_1, new OperationDTO("", value));
        Assertions.assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void checkNotFoundOperation() {
        double value = balance_1 + 100;
        Response response = performOperation("test", new OperationDTO(accountId_2, value));
        Assertions.assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }


    @Test
    public void checkLessThenZeroValueOperation() {
        double value = -10d;
        Response response = performOperation(accountId_1, new OperationDTO(accountId_2, value));
        Assertions.assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void checkFoundsExceededOperation() {
        double value = balance_1 + 100;
        Response response = performOperation(accountId_1, new OperationDTO(accountId_2, value));
        Assertions.assertThat(response.getStatusCode()).isEqualTo(ERROR);
    }

    @Test
    public void checkLimitExceededOperation() {
        double value = limit_1 + 100;
        Response response = performOperation(accountId_1, new OperationDTO(accountId_2, value));
        Assertions.assertThat(response.getStatusCode()).isEqualTo(ERROR);
    }

    @Test
    public void checkWithdrawOperation() {
        double value = limit_1 - 100;

        double beforeBalanceTest1 = getBalance(accountId_1);
        double beforeBalanceTest2 = getBalance(accountId_2);

        Response response = performOperation(accountId_1, new OperationDTO(accountId_2, value));
        Assertions.assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT);

        double balanceTest1 = getBalance(accountId_1);
        Assertions.assertThat(balanceTest1).isEqualTo(beforeBalanceTest1 - value);

        double balanceTest2 = getBalance(accountId_2);
        Assertions.assertThat(balanceTest2).isEqualTo(beforeBalanceTest2 + value);
    }


    @Test
    public void checkHistories() {
        double beforeBalanceTest1 = getBalance(accountId_1);
        double beforeBalanceTest2 = getBalance(accountId_2);

        double value = limit_1 - 100;
        double afterBalanceTest1 = beforeBalanceTest1 - value;
        double afterBalanceTest2 = beforeBalanceTest2 + value;

        performOperation(accountId_1, new OperationDTO(accountId_2, value));

        AccountHistoryDTO[] histories1 = getAccountHistories(accountId_1);
        Optional<AccountHistoryDTO> history1 = Arrays.stream(histories1).filter(history ->
                historyFilter(history, beforeBalanceTest1, afterBalanceTest1, "withdraw")).findAny();
        Assertions.assertThat(history1).isPresent();
        Assertions.assertThat(history1.get().fromTo).isEqualTo(owner_2);

        AccountHistoryDTO[] histories2 = getAccountHistories(accountId_2);
        Optional<AccountHistoryDTO> history2 = Arrays.stream(histories2).filter(history ->
                historyFilter(history, beforeBalanceTest2, afterBalanceTest2, "deposit")).findAny();
        Assertions.assertThat(history2).isPresent();
        Assertions.assertThat(history2.get().fromTo).isEqualTo(owner_1);
    }

    private boolean historyFilter(AccountHistoryDTO history, double beforeBalance, double afterBalance, String operationType) {
        return history.operationType.equals(operationType)
                && history.beforeBalance == beforeBalance
                && history.afterBalance == afterBalance;
    }

    private static AccountDTO[] getAccounts() {
        return given()
                .get(ACCOUNTS_PATH + "/all")
                .then()
                .extract()
                .as(AccountDTO[].class);
    }

    private static AccountDTO[] getOwnerAccounts(String owner) {
        return given()
                .pathParam(OWNER_PARAM, owner)
                .get(ACCOUNTS_PATH + "/owner/{" + OWNER_PARAM + "}")
                .then()
                .extract()
                .as(AccountDTO[].class);
    }

    private static AccountHistoryDTO[] getAccountHistories(String accountId) {
        return given()
                .pathParam(ACCOUNT_ID_PARAM, accountId)
                .get(ACCOUNTS_PATH + "/{" + ACCOUNT_ID_PARAM + "}/history")
                .then()
                .extract()
                .as(AccountHistoryDTO[].class);
    }

    private static Response getResultCreateAccount(AccountCreateDTO account) {
        return given()
                .body(account)
                .post(ACCOUNTS_PATH + "/add");
    }

    private static String addAccount(AccountCreateDTO account) {
        return getResultCreateAccount(account)
                .then()
                .assertThat()
                .statusCode(OK)
                .extract()
                .asString();
    }

    private static void deleteAccount(String accountId) {
        given()
                .pathParam(ACCOUNT_ID_PARAM, accountId)
                .delete(ACCOUNTS_PATH + "/{" + ACCOUNT_ID_PARAM + "}")
                .then()
                .assertThat()
                .statusCode(NO_CONTENT);
    }

    private static Response performOperation(String accountId, OperationDTO operationDTO) {
        return given()
                .body(operationDTO)
                .pathParam(ACCOUNT_ID_PARAM, accountId)
                .put(ACCOUNTS_PATH + "/{" + ACCOUNT_ID_PARAM + "}");
    }

    private static double getBalance(String accountId) {
        return given()
                .pathParam(ACCOUNT_ID_PARAM, accountId)
                .get(ACCOUNTS_PATH + "/{" + ACCOUNT_ID_PARAM + "}/balance")
                .then()
                .extract()
                .as(Double.class);
    }

    private static AccountDTO getAccount(String accountId) {
        return given()
                .pathParam(ACCOUNT_ID_PARAM, accountId)
                .get(ACCOUNTS_PATH + "/{" + ACCOUNT_ID_PARAM + "}")
                .then()
                .extract()
                .as(AccountDTO.class);
    }

}
