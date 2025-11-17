package PTI.Rs232Validator.Messages.Responses.Telemetry;

import java.util.ArrayList;
import java.util.List;

/**
 * An RS-232 message from an acceptor to a host for {@link PTI.Rs232Validator.Messages.Commands.TelemetryCommand#GetServiceFlags GetServiceFlags}
 */
public class GetServiceFlagsResponseMessage extends TelemetryResponseMessage{

    private final byte PayloadByteSize = 11;

    /**
     * Initializes a new instance of {@link GetServiceFlagsResponseMessage}
     */
    public GetServiceFlagsResponseMessage(List<Byte> payload) {
        super(payload);

        if(!PayloadIssues.isEmpty()){
            return;
        }

        if(payload.size() != PayloadByteSize){
            PayloadIssues.add(String.format("The payload size is %d bytes, but %d bytes are expected", payload.size(), PayloadByteSize));
            return;
        }

        TachSensorServiceSuggestor = new ServiceSuggestor(Data.get(0));
        BillPathServiceSuggestor = new ServiceSuggestor(Data.get(1));
        CashboxBeltServiceSuggestor = new ServiceSuggestor(Data.get(2));
        CashboxMechanismServiceSuggestor = new ServiceSuggestor(Data.get(3));
        MasServiceSuggestor = new ServiceSuggestor(Data.get(4));
        SpringRollersuggestor = new ServiceSuggestor(Data.get(5));

    }

    private ServiceSuggestor TachSensorServiceSuggestor;

    /**
     * A byte flag value of {@link ServiceSuggestor ServiceSuggestor} for the tach sensor
     */
    public ServiceSuggestor getTachSensorServiceSuggestor() {
        return TachSensorServiceSuggestor;
    }

    private ServiceSuggestor BillPathServiceSuggestor;

    /**
     * A byte flag value of {@link ServiceSuggestor ServiceSuggestor} for the bill path
     */
    public ServiceSuggestor getBillPathServiceSuggestor() {
        return BillPathServiceSuggestor;
    }

    private ServiceSuggestor CashboxBeltServiceSuggestor;

    /**
     * A byte flag value of {@link ServiceSuggestor ServiceSuggestor} for the cash belt
     */
    public ServiceSuggestor getCashboxBeltServiceSuggestor() {
        return CashboxBeltServiceSuggestor;
    }

    private ServiceSuggestor CashboxMechanismServiceSuggestor;

    /**
     * A byte flag value of {@link ServiceSuggestor ServiceSuggestor} for the cashbox stacking mechanism
     */
    public ServiceSuggestor getCashboxMechanismServiceSuggestor() {
        return CashboxMechanismServiceSuggestor;
    }

    private ServiceSuggestor MasServiceSuggestor;

    /**
     * A byte flag value of {@link ServiceSuggestor ServiceSuggestor} for the mechanical anti-stringing level
     */
    public ServiceSuggestor getMasServiceSuggestor() {
        return MasServiceSuggestor;
    }

    private ServiceSuggestor SpringRollersuggestor;

    /**
     * A byte flag value of {@link ServiceSuggestor ServiceSuggestor} for the spring rollers
     */
    public ServiceSuggestor getSpringRollersuggestor() {
        return SpringRollersuggestor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return IsValid.get()
                ? String.format("Tach Sensor Service Suggestor: %s | " +
                    "Bill Path Service Suggestor: %s | " +
                    "Cashbox Belt Service Suggestor: %s | " +
                    "Cashbox Mechanism Service Suggestor: %s | " +
                    "Mas Service Suggestor: %s | " +
                    "Spring Rollers Service Suggestor: %s",
                    TachSensorServiceSuggestor.Flags(),
                    BillPathServiceSuggestor.Flags(),
                    CashboxBeltServiceSuggestor.Flags(),
                    CashboxMechanismServiceSuggestor.Flags(),
                    MasServiceSuggestor.Flags(),
                    SpringRollersuggestor.Flags())
                : super.toString();
    }

    /**
     * The entities that suggest a component requires service
     */
    public class ServiceSuggestor {

        /**
         * No entity suggests that a component requires service.
         */
        public static final byte None = 0;

        /**
         * The usage metrics suggest that a component requires service.
         */
        public static final byte UsageMetrics = 1 << 0;

        /**
         * The diagnostics and errors of the system suggest that a component requires service.
         */
        public static final byte DiagnosticsAndError = 1 << 1;

        public byte flags = 0;

        public ServiceSuggestor(byte flags){
            this.flags = flags;
        }

        public boolean hasFlag(byte flag){
            return (flags & flag) == flag;
        }

        public void setFlag(byte flag){
            flags = (byte) (flags | flag);
        }

        public String Flags(){
            List<String> setFlags = new ArrayList<>();

            if((flags & UsageMetrics) == UsageMetrics)
                setFlags.add("UsageMetrics");
            if((flags & DiagnosticsAndError) == DiagnosticsAndError)
                setFlags.add("DiagnosticsAndError");

            if(setFlags.isEmpty())
                setFlags.add("None");

            return String.join(", ", setFlags);
        }
    }
}
