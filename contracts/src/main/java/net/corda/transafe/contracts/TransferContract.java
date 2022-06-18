package net.corda.transafe.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.transafe.states.TransferState;

import java.util.Date;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

// ************
// * Contract *
// ************
public class TransferContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "net.corda.transafe.contracts.TransferContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<TransferContract.Commands> command = requireSingleCommand(tx.getCommands(), TransferContract.Commands.class);

        List<ContractState> inputs = tx.getInputStates();
        List<ContractState> outputs = tx.getOutputStates();

        if (command.getValue() instanceof TransferContract.Commands.SendFile) {

            // Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("No inputs should be consumed when creating a new Invoice State.", inputs.isEmpty());
                require.using("Transaction must have exactly one output.", outputs.size() == 1);
                TransferState output = (TransferState) outputs.get(0);
                require.using("End date should be set now date or after today.", output.getEndDate().compareTo(new Date()) >= 0);
                require.using("End date should be set after startDate.", output.getEndDate().compareTo(output.getStartDate()) > 0);
                return null;
            });

        } else if (command.getValue() instanceof TransferContract.Commands.ReceiveFile){
            requireThat(require -> {
                require.using("Transaction must have exactly one input.", inputs.size() == 1);
                require.using("Transaction must have exactly one output.", outputs.size() == 1);
                TransferState output = (TransferState) outputs.get(0);
                TransferState input = (TransferState) inputs.get(0);
                require.using("Expire check: Input Start date should occurs before now date", input.getStartDate().compareTo(new Date()) <= 0);
                require.using("Expire check: End date should occurs after now date.", input.getEndDate().compareTo(new Date()) >= 0);
                require.using("The input should not be received before.", !input.isReceived());
                require.using("The output receive status should be changed.", output.isReceived());
                return null;
            });
        }else{
            throw new IllegalArgumentException("Command not found!");
        }

    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        //In our hello-world app, We will only have one command.
        class SendFile implements Commands {}
        class ReceiveFile implements Commands {}
    }
}
