package net.corda.transafe.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetMyTransfersRequest {
    private String accountName;
}
