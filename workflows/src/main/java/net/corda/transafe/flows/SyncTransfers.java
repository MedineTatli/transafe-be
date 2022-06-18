package net.corda.transafe.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.workflows.flows.ShareStateAndSyncAccounts;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.flows.StartableByService;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.transafe.states.TransferState;

import java.util.Arrays;
import java.util.UUID;

@StartableByRPC
@StartableByService
public class SyncTransfers extends FlowLogic<String>{
    private String transferId;
    private Party party;

    public SyncTransfers(String transferId, Party party) {
        this.transferId = transferId;
        this.party = party;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        UUID id = UUID.fromString(transferId);
        QueryCriteria.LinearStateQueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria()
                .withUuid(Arrays.asList(id)).withStatus(Vault.StateStatus.UNCONSUMED);
        try {
            StateAndRef<TransferState> inputTransferStateAndRef = getServiceHub().getVaultService().queryBy(TransferState.class,queryCriteria).getStates().get(0);
            subFlow(new ShareStateAndSyncAccounts(inputTransferStateAndRef,party));

        }catch (Exception e){
            throw new FlowException("TransferState with id "+ transferId +" not found.");
        }
        return "Game synced";
    }
}