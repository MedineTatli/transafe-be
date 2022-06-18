package net.corda.transafe.accountUtilities;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.flows.StartableByService;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.transafe.states.TransferState;

import java.util.Arrays;
import java.util.List;

@StartableByRPC
@StartableByService
public class MyTransfer extends FlowLogic<List<StateAndRef<TransferState>>>{

    private String whoAmI;
    public MyTransfer(String whoAmI) {
        this.whoAmI = whoAmI;
    }

    @Override
    @Suspendable
    public List<StateAndRef<TransferState>> call() throws FlowException {
        AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
        AccountInfo myAccount = accountService.accountInfo(whoAmI).get(0).getState().getData();
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria().withExternalIds(Arrays.asList(myAccount.getIdentifier().getId()));
        List<StateAndRef<TransferState>> myTransferStates = getServiceHub().getVaultService().queryBy(TransferState.class, criteria).getStates();
        return myTransferStates;
    }
}