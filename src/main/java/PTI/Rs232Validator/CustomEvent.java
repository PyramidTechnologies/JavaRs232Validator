package PTI.Rs232Validator;

import PTI.Rs232Validator.Utility.Action;

import java.util.ArrayList;
import java.util.List;

public class CustomEvent {


    protected List<Action> listeners = new ArrayList<Action>();

    public void addListener(Action arg0) {
        listeners.add(arg0);
    }

    public void removeListener(Action arg0) {
        listeners.remove(arg0);
    }

    public void Invoke(Object... args) {
        for (Action a : listeners) {
            a.accept(args);
        }
    }

    /*public StateChangedEvent(Rs232State oldState, Rs232State newState) {
        OldState = oldState;
        NewState = newState;
    }

    private final Rs232State OldState;
    private final Rs232State NewState;

    public Rs232State getOldState() {
        return OldState;
    }

    public Rs232State getNewState() {
        return NewState;
    }*/
}
