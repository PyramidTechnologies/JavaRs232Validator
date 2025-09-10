package PTI.Rs232Validator.BillValidators;

import PTI.Rs232Validator.Messages.Commands.ExtendedCommand;
import PTI.Rs232Validator.Messages.Requests.ExtendedRequestMessage;
import PTI.Rs232Validator.Messages.Responses.Extended.BarcodeDetectedResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Extended.ExtendedResponseMessage;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ExtendedBillValidator {

    public CompletableFuture<BarcodeDetectedResponseMessage> GetDetectedBarcode(){
        return SendExtendedMessageAsync(ExtendedCommand.BarcodeDetected, new ArrayList<Byte>(), payload -> new BarcodeDetectedResponseMessage(payload));
    }

    public <TResponseMessage extends ExtendedResponseMessage> CompletableFuture<TResponseMessage> SendExtendedMessageAsync(ExtendedCommand command,
                                                                                                                                      List<Byte> requestData,
                                                                                                                                      Function<List<Byte>, TResponseMessage> createResponseMessage){
        return BillValidator.SendNonPollMessageAsync(ack -> new ExtendedRequestMessage(ack, command, requestData), createResponseMessage);
    }
}
