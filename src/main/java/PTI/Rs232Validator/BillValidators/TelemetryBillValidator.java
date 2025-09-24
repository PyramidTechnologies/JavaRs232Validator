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

    public CompletableFuture<GetServiceInfoResponseMessage> PingAsync(){
        return SendTelemetryMessageAsync(TelemetryCommand.Ping, new ArrayList<Byte>(), payload -> new GetServiceInfoResponseMessage(payload));
    }

    public CompletableFuture<GetServiceInfoResponseMessage> GetSerialNumberAsync(){
        return SendTelemetryMessageAsync(TelemetryCommand.GetSerialNumber, new ArrayList<Byte>(), payload -> new GetServiceInfoResponseMessage(payload));
    }

    public CompletableFuture<GetServiceInfoResponseMessage> GetCashboxMetrics(){
        return SendTelemetryMessageAsync(TelemetryCommand.GetCashboxMetrics, new ArrayList<Byte>(), payload -> new GetServiceInfoResponseMessage(payload));
    }

    public CompletableFuture<GetServiceInfoResponseMessage> ClearCashboxCount(){
        return SendTelemetryMessageAsync(TelemetryCommand.ClearCashboxCount, new ArrayList<Byte>(), payload -> new GetServiceInfoResponseMessage(payload));
    }

    public CompletableFuture<GetServiceInfoResponseMessage> GetUnitMetrics(){
        return SendTelemetryMessageAsync(TelemetryCommand.GetUnitMetrics, new ArrayList<Byte>(), payload -> new GetServiceInfoResponseMessage(payload));
    }

    public CompletableFuture<GetServiceInfoResponseMessage> GetServiceUsageCounters(){
        return SendTelemetryMessageAsync(TelemetryCommand.GetServiceUsageCounters, new ArrayList<Byte>(), payload -> new GetServiceInfoResponseMessage(payload));
    }

    public CompletableFuture<GetServiceInfoResponseMessage> GetServiceFlags(){
        return SendTelemetryMessageAsync(TelemetryCommand.GetServiceFlags, new ArrayList<Byte>(), payload -> new GetServiceInfoResponseMessage(payload));
    }

    public CompletableFuture<GetServiceInfoResponseMessage> ClearServiceFlags(){
        return SendTelemetryMessageAsync(TelemetryCommand.ClearServiceFlags, new ArrayList<Byte>(), payload -> new GetServiceInfoResponseMessage(payload));
    }

    public CompletableFuture<GetServiceInfoResponseMessage> GetServiceInfo(){
        return SendTelemetryMessageAsync(TelemetryCommand.GetServiceInfo, new ArrayList<Byte>(), payload -> new GetServiceInfoResponseMessage(payload));
    }

    public CompletableFuture<GetFirmwareMetricsResponseMessage> GetFirmwareMetrics() {
        return SendTelemetryMessageAsync(TelemetryCommand.GetFirmwareMetrics, new ArrayList<Byte>(), payload -> new GetFirmwareMetricsResponseMessage(payload));
    }

    public <TResponseMessage extends TelemetryResponseMessage> CompletableFuture<TResponseMessage> SendTelemetryMessageAsync(TelemetryCommand command,
                                                                                                                             List<Byte> requestData,
                                                                                                                             Function<List<Byte>, TResponseMessage> createResponseMessage){
        //return BillValidator.SendNonPollMessageAsync(ack -> new TelemetryRequestMessage(ack, command, requestData), createResponseMessage);
        return null;
    }
}
