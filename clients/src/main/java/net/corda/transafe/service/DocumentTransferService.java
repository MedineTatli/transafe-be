package net.corda.transafe.service;

import lombok.RequiredArgsConstructor;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import net.corda.transafe.flows.ReceiveFlow;
import net.corda.transafe.flows.TransferFlow;
import net.corda.transafe.request.DocumentTransferRequest;
import net.corda.transafe.request.ReceiveFileRequest;
import net.corda.transafe.response.DocumentTransferResponse;
import net.corda.transafe.response.ReceiveFileResponse;
import net.corda.transafe.states.TransferState;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class DocumentTransferService implements IDocumentTransferService {

    private final CordaRPCOps proxy;

    @Override
    public DocumentTransferResponse sendFile(DocumentTransferRequest request) throws ExecutionException, InterruptedException {
        CordaX500Name receiver500Name = CordaX500Name.parse(request.getReceiverHost());
        Party receiver = proxy.wellKnownPartyFromX500Name(receiver500Name);

        DocumentTransferResponse response = new DocumentTransferResponse();

        SignedTransaction result = proxy.startTrackedFlowDynamic(TransferFlow.Initiator.class,
                request.getFile(), receiver, request.getSender(), request.getReceiver(), request.getStartDate(), request.getEndDate())
                .getReturnValue().get();

        response.setTransactionId(result.getId().toString());
        response.setSuccess(true);
        return response;
    }

    @Override
    public ReceiveFileResponse receiveFile(ReceiveFileRequest request) throws ExecutionException, InterruptedException {
        CordaX500Name sender500Name = CordaX500Name.parse(request.getSenderHost());
        Party senderParty = proxy.wellKnownPartyFromX500Name(sender500Name);

        ReceiveFileResponse response = new ReceiveFileResponse();

        SignedTransaction result = proxy.startTrackedFlowDynamic(ReceiveFlow.Initiator.class,
                senderParty, request.getSender(), request.getReceiver(), request.getLinearId()).getReturnValue().get();
        TransferState output = (TransferState) result.getTx().getOutputs().get(0).getData();
        response.setTransactionId(result.getId().toString());
        response.setFile(output.getFile());

        return response;
    }
}
