package net.corda.transafe.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DocumentTransferResponse {

    private boolean isSuccess;
    private String error;
    private String transactionId;
}
