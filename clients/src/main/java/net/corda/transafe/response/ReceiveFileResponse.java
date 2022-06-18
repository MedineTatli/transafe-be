package net.corda.transafe.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiveFileResponse {

    private String file;
    private String error;
    private String transactionId;

}
