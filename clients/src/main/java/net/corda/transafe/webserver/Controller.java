package net.corda.transafe.webserver;


import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.transafe.accountUtilities.CreateNewAccount;
import net.corda.transafe.accountUtilities.MyTransfer;
import net.corda.transafe.flows.GetAllTransactionsFlow;
import net.corda.transafe.request.*;
import net.corda.transafe.response.DocumentTransferResponse;
import net.corda.transafe.response.GetMyTransfersResponse;
import net.corda.transafe.response.HandShakeResponse;
import net.corda.transafe.response.ReceiveFileResponse;
import net.corda.transafe.service.AccountManagementService;
import net.corda.transafe.service.DocumentTransferService;
import net.corda.transafe.service.IAccountManagementService;
import net.corda.transafe.states.TransferState;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.TransformedMultiValuedMap;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private static final Logger logger = LoggerFactory.getLogger(RestController.class);
    private final CordaRPCOps proxy;
    private final CordaX500Name me;
    private final DocumentTransferService documentTransferService;
    private final IAccountManagementService accountManagementService;

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();
        documentTransferService = new DocumentTransferService(this.proxy);
        accountManagementService = new AccountManagementService(this.proxy);
    }

    /** Helpers for filtering the network map cache. */
    public String toDisplayString(X500Name name){
        return BCStyle.INSTANCE.toString(name);
    }

    private boolean isNotary(NodeInfo nodeInfo) {
        return !proxy.notaryIdentities()
                .stream().filter(el -> nodeInfo.isLegalIdentity(el))
                .collect(Collectors.toList()).isEmpty();
    }

    private boolean isMe(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().equals(me);
    }

    private boolean isNetworkMap(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().getOrganisation().equals("Network Map Service");
    }

    @Configuration
    class Plugin {
        @Bean
        public ObjectMapper registerModule() {
            return JacksonSupport.createNonRpcMapper();
        }
    }

    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
    @GetMapping(value = "/me",produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> whoami(){
        HashMap<String, String> myMap = new HashMap<>();
        myMap.put("me", me.toString());
        return myMap;
    }

    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
    @PostMapping(value =  "createAccount/{acctName}")
    private ResponseEntity<String> createAccount(@PathVariable String acctName){
        try{
            String result = proxy.startTrackedFlowDynamic(CreateNewAccount.class,acctName)
                    .getReturnValue()
                    .get();
            return ResponseEntity.status(HttpStatus.CREATED).body("Account "+acctName+" Created");

        }catch (Exception e) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(e.getMessage());
        }
    }

    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
    @PostMapping(value = "handShake", produces = APPLICATION_JSON_VALUE)
    private ResponseEntity<HandShakeResponse> handShake(@RequestBody HandShakeRequest request){
        HandShakeResponse result = null;
        try{
            result = accountManagementService.handShake(request);
            return ResponseEntity.status(HttpStatus.OK).body(result);

        }catch (Exception e) {
            if(result == null){
                result = new HandShakeResponse();
            }
            result.setMessage(e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(result);
        }
    }

    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
    @PostMapping(value = "sendFile", produces = APPLICATION_JSON_VALUE)
    private ResponseEntity<DocumentTransferResponse> sendFile(@RequestBody DocumentTransferRequest request){
        try{
            DocumentTransferResponse responseBody = documentTransferService.sendFile(request);
            return ResponseEntity.ok(responseBody);
        }catch (Exception e){
            DocumentTransferResponse response = new DocumentTransferResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
    }

    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
    @GetMapping(value = "getTransfers",produces = APPLICATION_JSON_VALUE)
    public List<StateAndRef<TransferState>> getIOUs() {
        // Filter by state type: IOU.
        return proxy.vaultQuery(TransferState.class).getStates();
    }

    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
    @PostMapping(value = "getHistoricDataByLinearId",produces = APPLICATION_JSON_VALUE)
    public List<StateAndRef<TransferState>> getTransfers(@RequestBody GetAllTransfersRequest request) throws ExecutionException, InterruptedException {
        // Filter by state type: IOU.
        List<StateAndRef<TransferState>> auditTrail = proxy.startFlowDynamic(GetAllTransactionsFlow.Initiator.class,
                request.getLinearId()).getReturnValue().get();
        return auditTrail;
    }

    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
    @PostMapping(value = "getMyTransfers", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<GetMyTransfersResponse> getMyTransfer(@RequestBody GetMyTransfersRequest request){
        logger.info("accountName: {}", request.getAccountName());
        GetMyTransfersResponse response = new GetMyTransfersResponse();

        try{
            List<StateAndRef<TransferState>> myTransferStates = proxy.startTrackedFlowDynamic(MyTransfer.class, request.getAccountName()).getReturnValue().get();
            response.setMyTransfers(myTransferStates);
            return ResponseEntity.ok(response);
        }catch (Exception e){
            response.setError(e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
    }

    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
    @PostMapping(value = "receiveFile", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ReceiveFileResponse> receiveFile(@RequestBody ReceiveFileRequest request){
        try{
            ReceiveFileResponse response = documentTransferService.receiveFile(request);
            return ResponseEntity.ok(response);
        }catch (Exception e){
            ReceiveFileResponse response = new ReceiveFileResponse();
            response.setError(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}