package PTI.Rs232Validator.EventListener;

import PTI.Rs232Validator.Rs232State;

public class StateChangedEventArgs {

    public StateChangedEventArgs(Rs232State oldState, Rs232State newState) {
        this.oldState = oldState;
        this.newState = newState;
    }

    private Rs232State oldState;
    public Rs232State getOldState() {
        return oldState;
    }


    private Rs232State newState;
    public Rs232State getNewState() {
        return newState;
    }
}
