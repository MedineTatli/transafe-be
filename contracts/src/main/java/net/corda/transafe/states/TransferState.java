package net.corda.transafe.states;

import lombok.Getter;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.transafe.contracts.TransferContract;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Getter
@BelongsToContract(TransferContract.class)
public class TransferState implements LinearState {
    private final String file;
    private final AnonymousParty sender;
    private final String senderAccount;
    private final AnonymousParty receiver;
    private final String receiverAccount;
    private final Date startDate;
    private final Date endDate;
    private final UniqueIdentifier linearId;
    private boolean isReceived;

    public TransferState(String file, AnonymousParty sender, String senderAccount, AnonymousParty receiver, String receiverAccount,
                         Date startDate, Date endDate, UniqueIdentifier linearId, boolean isReceived) {
        this.file = file;
        this.sender = sender;
        this.senderAccount = senderAccount;
        this.receiver = receiver;
        this.receiverAccount = receiverAccount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.linearId = linearId;
        this.isReceived = isReceived;
    }

    @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public List<AbstractParty> getParticipants() {
        return Arrays.asList(sender, receiver);
    }

    public TransferState receiveTransfer(AnonymousParty sender, AnonymousParty receiver){
        return new TransferState(this.file, sender, this.senderAccount, receiver, this.receiverAccount, this.startDate, this.endDate, this.linearId, true);
    }

    /*@Override
    public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof TransferSchemaV1) {
            return new TransferSchemaV1.PersistentIOU(
                    this.sender.nameOrNull() == null ? null : this.sender.nameOrNull().toString(),
                    this.senderAccount,
                    this.receiver.nameOrNull() == null ? null : this.receiver.nameOrNull().toString(),
                    this.receiverAccount,
                    this.file,
                    this.startDate,
                    this.endDate,
                    this.linearId.getId());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override public Iterable<MappedSchema> supportedSchemas() {
        return Arrays.asList(new TransferSchemaV1());
    }*/

    @Override
    public String toString() {
        return "TransferState{" +
                "file='" + file + '\'' +
                ", sender=" + sender +
                ", senderAccount='" + senderAccount + '\'' +
                ", receiver=" + receiver +
                ", receiverAccount='" + receiverAccount + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", linearId=" + linearId +
                ", isReceived=" + isReceived +
                '}';
    }
}
