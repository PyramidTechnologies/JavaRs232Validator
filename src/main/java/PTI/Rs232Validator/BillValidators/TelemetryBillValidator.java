package PTI.Rs232Validator.BillValidators;

import PTI.Rs232Validator.Messages.Commands.TelemetryCommand;
import PTI.Rs232Validator.Messages.Requests.TelemetryRequestMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetFirmwareMetricsResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetServiceInfoResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.TelemetryResponseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class TelemetryBillValidator {

    public CompletableFuture<GetServiceInfoResponseMessage> GetServiceInfo(){
        return SendTelemetryMessageAsync(TelemetryCommand.GetServiceInfo, new ArrayList<Byte>(), payload -> new GetServiceInfoResponseMessage(payload));
    }

    public CompletableFuture<GetFirmwareMetricsResponseMessage> GetFirmwareMetrics() {
        return SendTelemetryMessageAsync(TelemetryCommand.GetFirmwareMetrics, new ArrayList<Byte>(), payload -> new GetFirmwareMetricsResponseMessage(payload));
    }

    public <TResponseMessage extends TelemetryResponseMessage> CompletableFuture<TResponseMessage> SendTelemetryMessageAsync(TelemetryCommand command,
                                                                                                                             List<Byte> requestData,
                                                                                                                             Function<List<Byte>, TResponseMessage> createResponseMessage){
        return BillValidator.SendNonPollMessageAsync(ack -> new TelemetryRequestMessage(ack, command, requestData), createResponseMessage);
    }
}
