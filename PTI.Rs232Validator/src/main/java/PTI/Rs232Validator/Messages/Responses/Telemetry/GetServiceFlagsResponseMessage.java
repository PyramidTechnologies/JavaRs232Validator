package PTI.Rs232Validator.Messages.Responses.Telemetry;

import java.util.List;

public class GetServiceFlagsResponseMessage extends TelemetryResponseMessage{

    private final byte PayloadByteSize = 11;

    public GetServiceFlagsResponseMessage(List<Byte> payload) {
        super(payload);

        if(!PayloadIssues.isEmpty()){
            return;
        }

        if(payload.size() != PayloadByteSize){
            PayloadIssues.add(String.format("The payload size is %d bytes, but %d bytes are expected", payload.size(), PayloadByteSize));
            return;
        }

        TachSensorServiceSuggestor = Data.get(0);
        BillPathServiceSuggestor = Data.get(1);
        CashboxBeltServiceSuggestor = Data.get(2);
        CashboxMechanismServiceSuggestor = Data.get(3);
        MasServiceSuggestor = Data.get(4);
        SpringRollersuggestor = Data.get(5);

    }

    private Byte TachSensorServiceSuggestor;
    public Byte getTachSensorServiceSuggestor() {
        return TachSensorServiceSuggestor;
    }

    private Byte BillPathServiceSuggestor;
    public Byte getBillPathServiceSuggestor() {
        return BillPathServiceSuggestor;
    }

    private Byte CashboxBeltServiceSuggestor;
    public Byte getCashboxBeltServiceSuggestor() {
        return CashboxBeltServiceSuggestor;
    }

    private Byte CashboxMechanismServiceSuggestor;
    public Byte getCashboxMechanismServiceSuggestor() {
        return CashboxMechanismServiceSuggestor;
    }

    private Byte MasServiceSuggestor;
    public Byte getMasServiceSuggestor() {
        return MasServiceSuggestor;
    }

    private Byte SpringRollersuggestor;
    public Byte getSpringRollersuggestor() {
        return SpringRollersuggestor;
    }


    @Override
    public String toString() {
        return IsValid
                ? String.format("Tach Sensor Service Suggestor: %s | " +
                    "Bill Path Service Suggestor: %s | " +
                    "Cashbox Belt Service Suggestor: %s | " +
                    "Cashbox Mechanism Service Suggestor: %s | " +
                    "Mas Service Suggestor: %s | " +
                    "Spring Rollers Service Suggestor: %s",
                    ServiceSuggestor.toFlagString(TachSensorServiceSuggestor),
                    ServiceSuggestor.toFlagString(BillPathServiceSuggestor),
                    ServiceSuggestor.toFlagString(CashboxBeltServiceSuggestor),
                    ServiceSuggestor.toFlagString(CashboxMechanismServiceSuggestor),
                    ServiceSuggestor.toFlagString(MasServiceSuggestor),
                    ServiceSuggestor.toFlagString(SpringRollersuggestor))
                : super.toString();
    }

    public enum ServiceSuggestor {

        None((byte) 0),


        UsageMetrics((byte) (1 << 0)),


        DiagnosticsAndError((byte) (1 << 1));

        private final byte value;

        private ServiceSuggestor(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }

        public static ServiceSuggestor fromValue(byte value) {
            for(ServiceSuggestor suggestor : ServiceSuggestor.values()){
                if(suggestor.getValue() == value){
                    return suggestor;
                }
            }
            throw new IllegalArgumentException("Unknown ServiceSuggestor value: " + value);
        }

        public static String toFlagString(byte value) {
            StringBuilder builder = new StringBuilder();
            boolean first = true;

            for(ServiceSuggestor event : ServiceSuggestor.values()){
                if((value & event.getValue()) != 0){
                    if(!first){
                        builder.append(", ");
                    }
                    builder.append(event.name());
                    first = false;
                }
            }

            return builder.toString();
        }
    }
}
