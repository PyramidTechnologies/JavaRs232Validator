package PTI.Rs232Validator.EventListener;

import PTI.Rs232Validator.Messages.Requests.Rs232RequestMessage;
import PTI.Rs232Validator.Messages.Responses.Rs232ResponseMessage;

/**
 * Event arguments for the Communication Attempted event
 */
public class CommunicationAttemptedEventArgs {

    /**
     * Initializes a new instance of {@link CommunicationAttemptedEventArgs}
     * @param requestMessage {@link CommunicationAttemptedEventArgs#RequestMessage}
     * @param responseMessage {@link CommunicationAttemptedEventArgs#ResponseMessage}
     */
    public CommunicationAttemptedEventArgs(Rs232RequestMessage requestMessage, Rs232ResponseMessage responseMessage) {
        this.RequestMessage = requestMessage;
        this.ResponseMessage = responseMessage;
    }

    /**
     * An instance of {@link Rs232RequestMessage}, the {@link Rs232RequestMessage#Payload} of which was
     * sent to the acceptor
     */
    public final Rs232RequestMessage RequestMessage;

    /**
     * An instance of {@link Rs232ResponseMessage}, the {@link Rs232ResponseMessage#Payload} of which was
     * either received from the acceptor or created as an empty collection due to a timeout.
     * @implNote Consider checking {@link Rs232ResponseMessage#IsValid} of the instance.
     */
    public final Rs232ResponseMessage ResponseMessage;
}
