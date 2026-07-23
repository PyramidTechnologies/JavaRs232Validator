package PTI.Rs232Validator.EventListener;

import PTI.Rs232Validator.Messages.Requests.Rs232RequestMessage;
import PTI.Rs232Validator.Messages.Responses.Rs232ResponseMessage;

public class CommunicationAttemptedEventArgs {

    public CommunicationAttemptedEventArgs(Rs232RequestMessage requestMessage, Rs232ResponseMessage responseMessage) {
        this.requestMessage = requestMessage;
        this.responseMessage = responseMessage;
    }

    private Rs232RequestMessage requestMessage;
    public Rs232RequestMessage getRequestMessage() {
        return requestMessage;
    }

    private Rs232ResponseMessage responseMessage;
    public Rs232ResponseMessage getResponseMessage() {
        return responseMessage;
    }
}
