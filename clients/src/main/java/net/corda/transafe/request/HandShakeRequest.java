package net.corda.transafe.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HandShakeRequest {

    private String fromPerson;
    private String targetNodeName;

}
