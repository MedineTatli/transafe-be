package net.corda.transafe.response;

import lombok.Getter;
import lombok.Setter;
import net.corda.core.contracts.StateAndRef;
import net.corda.transafe.states.TransferState;

import java.util.List;

@Getter
@Setter
public class GetMyTransfersResponse {

    private List<StateAndRef<TransferState>> myTransfers;
    private String error;

}
