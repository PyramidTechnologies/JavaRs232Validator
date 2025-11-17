package PTI.Rs232Validator;

import PTI.Rs232Validator.Utility.Action;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A handler for bill validator events
 */
public class ValidatorEvent {

    public final int EventCode;

    /**
     * Initializes a new instance of {@link ValidatorEvent}
     * @param eventCode An {@code int} to identify the type of event the instance handles
     */
    public ValidatorEvent(int eventCode){
        this.EventCode = eventCode;
    }

    protected List<Action> listeners = new CopyOnWriteArrayList<>();

    /**
     * Adds a new listener Action to the event handler
     * @param arg0 A listener for the event
     */
    public void addListener(Action arg0) {
        listeners.add(arg0);
    }

    /**
     * Removes a listener from the event handler
     * @param arg0 The listener to be removed
     */
    public void removeListener(Action arg0) {
        listeners.remove(arg0);
    }

    /**
     * Invokes the actions of the listeners
     * @param args Parameters to be passed to the listeners
     */
    public void Invoke(Object... args) {
        for (Action a : listeners) {
            a.accept(args);
        }
    }
}
