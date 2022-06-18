package net.corda.transafe.service;

import lombok.RequiredArgsConstructor;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.transafe.accountUtilities.ShareAccountTo;
import net.corda.transafe.request.HandShakeRequest;
import net.corda.transafe.response.HandShakeResponse;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class AccountManagementService implements IAccountManagementService{

    private final CordaRPCOps proxy;

    @Override
    public HandShakeResponse handShake(HandShakeRequest request) throws ExecutionException, InterruptedException {
        HandShakeResponse response = new HandShakeResponse();
        Set<Party> matchingPasties = proxy.partiesFromName(request.getTargetNodeName(),false);
        Iterator iter = matchingPasties.iterator();
        String result = proxy.startTrackedFlowDynamic(ShareAccountTo.class,request.getFromPerson(),iter.next()).getReturnValue().get();
        response.setMessage(result);
        return response;
    }
}
