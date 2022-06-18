package net.corda.transafe.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.transafe.accountUtilities.NewKeyForAccount;
import net.corda.transafe.contracts.TransferContract;
import net.corda.transafe.states.TransferState;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the IOU encapsulated
 * within an [IOUState].
 *
 * In our simple example, the [Acceptor] always accepts a valid IOU.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
public class ReceiveFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final Party fromParty;
        private final String sender;
        private final String receiver;
        private final String linearId;

        private final Step GENERATING_TRANSACTION = new Step("Generating transaction based on new IOU.");
        private final Step VERIFYING_TRANSACTION = new Step("Verifying contract constraints.");
        private final Step SIGNING_TRANSACTION = new Step("Signing transaction with our private key.");
        private final Step GATHERING_SIGS = new Step("Gathering the counterparty's signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING_TRANSACTION = new Step("Obtaining notary signature and recording transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        public Initiator(Party fromParty, String sender, String receiver, String linearId) {
            this.fromParty = fromParty;
            this.sender = sender;
            this.receiver = receiver;
            this.linearId = linearId;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // Stage 1.
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);

            AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);

            AccountInfo myAccount = accountService.accountInfo(receiver).stream()
                    .filter(account -> account.getState().getData().getHost().equals(getOurIdentity()))
                    .collect(Collectors.toList()).get(0).getState().getData();
            System.out.println("myAccount ---> "+myAccount);
            PublicKey myKey = subFlow(new NewKeyForAccount(myAccount.getIdentifier().getId())).getOwningKey();
            System.out.println("myKey ---> "+myKey);

            AccountInfo targetAccount = accountService.accountInfo(sender).stream()
                    .filter(account -> account.getState().getData().getHost().equals(fromParty))
                    .collect(Collectors.toList()).get(0).getState().getData();

            System.out.println("targetAccount ---> "+targetAccount);
            AnonymousParty targetAcctAnonymousParty = subFlow(new RequestKeyForAccount(targetAccount));
            System.out.println("targetAcctAnonymousParty ---> "+targetAcctAnonymousParty.toString());

            // Retrieve the transaction
            UUID id = UUID.fromString(linearId);
            QueryCriteria.LinearStateQueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria()
                    .withUuid(Collections.singletonList(id))
                    .withRelevancyStatus(Vault.RelevancyStatus.RELEVANT).withStatus(Vault.StateStatus.UNCONSUMED);

            System.out.println("queryCriteria: " + queryCriteria);
            System.out.println("queryCriteria: -->" + getServiceHub().getVaultService().queryBy(TransferState.class, queryCriteria).getStates());
            System.out.println("");
            if(getServiceHub().getVaultService().queryBy(TransferState.class, queryCriteria).getStates().size() == 0){
                throw new FlowException("Not found any states with given linear id and accounts");
            }
            StateAndRef<TransferState> inputTransferStateAndRef = getServiceHub().getVaultService().queryBy(TransferState.class, queryCriteria).getStates().get(0);
            TransferState inputTransferState = inputTransferStateAndRef.getState().getData();
            System.out.println("inputTransferState ---> "+inputTransferState);
            if(!inputTransferState.getSenderAccount().equals(sender)){
                throw new FlowException("The sender account does not match!");
            }if(!inputTransferState.getReceiverAccount().equals(receiver)){
                throw new FlowException("The receiver account does not match!");
            }
            /*List<TransferState> inputTransferStates = inputTransferStateAndRef.stream().map(state -> state.getState().getData())
                    .filter(state -> !state.isReceived() && state.getReceiverAccount().equals(receiver) && state.getSenderAccount().equals(sender)).collect(Collectors.toList());*/
            //change the receiving status
            TransferState outputTransferState = inputTransferState.receiveTransfer(targetAcctAnonymousParty, new AnonymousParty(myKey));
            // Obtain a reference to a notary we wish to use.
            Party notary = inputTransferStateAndRef.getState().getNotary();

            final Command<TransferContract.Commands.ReceiveFile> txCommand = new Command<>(
                    new TransferContract.Commands.ReceiveFile(),
                    Arrays.asList(myKey, targetAcctAnonymousParty.getOwningKey()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputTransferStateAndRef)
                    .addOutputState(outputTransferState)
                    .addCommand(txCommand);

            // Stage 2.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            System.out.println("VERIFYING_TRANSACTION");
            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Stage 3.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            System.out.println("SIGNING_TRANSACTION");
            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(
                    txBuilder, Arrays.asList(getOurIdentity().getOwningKey(), myKey));

            // Stage 4.
            progressTracker.setCurrentStep(GATHERING_SIGS);
            System.out.println("GATHERING_SIGS");
            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(targetAccount.getHost());

            List<TransactionSignature> accountToMoveToSignature = (List<TransactionSignature>) subFlow(new CollectSignatureFlow(partSignedTx,
                    otherPartySession, targetAcctAnonymousParty.getOwningKey()));

            System.out.println("--accountToMoveToSignature-- ");
            SignedTransaction signedByCounterParty = partSignedTx.withAdditionalSignatures(accountToMoveToSignature);
            System.out.println("--signedByCounterParty-- ");

            // Stage 5.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);

            System.out.println("** is legal: " + getServiceHub().getMyInfo().isLegalIdentity(targetAccount.getHost()));

            // Notarise and record the transaction in both parties' vaults.
            List<FlowSession> sessions = !getServiceHub().getMyInfo().isLegalIdentity(targetAccount.getHost())
                    ? Arrays.asList(otherPartySession).stream().filter(it -> it.getCounterparty() != getOurIdentity()).collect(Collectors.toList())
                    : Collections.emptyList();

            SignedTransaction fullySignedTx = subFlow(new FinalityFlow(signedByCounterParty, sessions, StatesToRecord.ALL_VISIBLE));
            System.out.println("\n--fullySignedTx--\n");
            subFlow(new SyncTransfers(outputTransferState.getLinearId().toString(),targetAccount.getHost()));
            System.out.println("\n--Synchronized--\n");
            return fullySignedTx;
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartySession;

        public Acceptor(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an Transfer transaction.", output instanceof TransferState);
                        return null;
                    });
                }
            }
            final SignTxFlow signTxFlow = new SignTxFlow(otherPartySession, SignTransactionFlow.Companion.tracker());
            final SecureHash txId = subFlow(signTxFlow).getId();

            return subFlow(new ReceiveFinalityFlow(otherPartySession, txId));
        }
    }
}
