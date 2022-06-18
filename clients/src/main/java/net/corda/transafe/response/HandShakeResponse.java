package net.corda.transafe.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HandShakeResponse {

    private String message;
    private String error;

}
