package PTI.Rs232Validator.EventListener;

import PTI.Rs232Validator.Rs232State;

/**
 * Event arguments for the State Changed Event event
 */
public class StateChangedEventArgs {

    /**
     * Initializes a new instance of {@link StateChangedEventArgs}
     * @param oldState {@link StateChangedEventArgs#OldState}
     * @param newState {@link StateChangedEventArgs#NewState}
     */
    public StateChangedEventArgs(Rs232State oldState, Rs232State newState) {
        this.OldState = oldState;
        this.NewState = newState;
    }

    /**
     * The previous state of the device
     */
    public final Rs232State OldState;


    /**
     * The new state of the device
     */
    public final Rs232State NewState;
}
