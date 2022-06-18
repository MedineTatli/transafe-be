package net.corda.transafe.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetAllTransfersRequest {
    private String linearId;
}
