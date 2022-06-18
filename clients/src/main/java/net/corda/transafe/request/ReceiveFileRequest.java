package net.corda.transafe.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiveFileRequest {

    private String senderHost;
    private String sender;
    private String receiver;
    private String linearId;

}
