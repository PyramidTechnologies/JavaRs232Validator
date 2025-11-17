package com.example.Rs232Validator.ViewModel

import PTI.Rs232Validator.BillValidators.BillValidator
import PTI.Rs232Validator.*
import PTI.Rs232Validator.Utility.ByteUtils
import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.Rs232Validator.Logger
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ValidatorViewModel(
    application: Application,
    private val sharedPreferences: SharedPreferences
) : AndroidViewModel(application){

    //Initial Parameters
    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    private var TimestampFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss a")
    private var billValidator: BillValidator? = null
    var escrow_mode = mutableStateOf(false)
        private set
    var detect_barcode = mutableStateOf(false)
        private set
    var enableMask = mutableStateListOf(true, true, true, true, true, true, true, true)
        private set
    fun GetMask(): Byte {
        var mask = 0;
        for(i in 0 .. enableMask.size-2){
            mask = mask or (if (enableMask[i]) 1 shl i else 0)
        }
        return mask.toByte()
    }

    private val _rs232Configuration = Rs232Configuration().apply{
        EnableMask = GetMask()
        ShouldEscrow = escrow_mode.value
        ShouldDetectBarcodes = detect_barcode.value
    }

    val logger : Logger = Logger()

    init {
        if(billValidator == null) {
            billValidator = BillValidator(
                logger,
                _rs232Configuration,
                context,
                sharedPreferences,
                "com.example.Rs232Validator.USB_PERMISSION"
            )

            billValidator?._serialProvider?.SetConfig(9600, 7, 1,2, 0)

            registerListeners()
        }
    }

    private fun registerListeners(){
        billValidator?.OnBillStacked?.addListener { args ->
            val billType = args.getOrNull(0) as? Byte ?:return@addListener
            updateBillValue(billType.toInt())
        }

        billValidator?.OnBillEscrowed?.addListener { args ->
            toggleIsBillInEscrow()
        }

        billValidator?.OnStateChanged?.addListener { args ->
            val oldState: Rs232State = args.getOrNull(0) as? Rs232State ?:return@addListener
            val newState: Rs232State = args.getOrNull(1) as? Rs232State ?:return@addListener

            logger.LogInfo("The state changed from %s to %s.", oldState.name, newState.name)

            _currentState.value = newState
        }

        billValidator?.OnBarcodeDetected?.addListener { args ->
            val barcode: String = args.getOrNull(0) as? String ?:return@addListener

            _lastBarcode.value = barcode
        }

        billValidator?.OnCashboxAttached?.addListener { args ->
            _cashboxAttached.value = true
        }

        billValidator?.OnCashboxRemoved?.addListener { args ->
            _cashboxAttached.value = false
        }

        billValidator?.OnEventReported?.addListener { args ->
            val event: Rs232Event = args.getOrNull(0) as? Rs232Event ?:return@addListener
            logger.LogInfo("Received event(s): %s.", event.Flags())
            _currentEvent.value = event
        }

        billValidator?.OnConnectionLost?.addListener { args ->
            isPolling.value = false
        }

        billValidator?.OnCommunicationAttempted?.addListener { args ->
            val entry = PayloadExchange(
                LocalDateTime.now().format(TimestampFormat),
                ByteUtils.ConvertToHexString(args.getOrNull(0) as? List<Byte> ?:return@addListener, false, true),
                args.getOrNull(1) as? String ?:return@addListener,
                ByteUtils.ConvertToHexString(args.getOrNull(2) as? List<Byte> ?:return@addListener, false, true),
                args.getOrNull(3) as? String ?:return@addListener
            )

            _PayloadExchanges.value = _PayloadExchanges.value + entry
        }
    }



    //Polling Screen Variables and Methods
    var isPolling = mutableStateOf(false)
        private set
    var billValues = mutableStateListOf(0,0,0,0,0,0,0,0)
        private set

    private val _IsBillInEscrow = MutableStateFlow(false)
    val IsBillInEscrow = _IsBillInEscrow.asStateFlow()

    fun onMaskChanged(type: Int, newMask: Boolean){
        enableMask[type - 1] = newMask
        _rs232Configuration.EnableMask = GetMask()
    }

    fun updateBillValue(billType: Int){
        billValues[billType-1]++

        when(billType) {
            1 -> billValues[7] += 1
            2 -> billValues[7] += 2
            3 -> billValues[7] += 5
            4 -> billValues[7] += 10
            5 -> billValues[7] += 20
            6 -> billValues[7] += 50
            7 -> billValues[7] += 100
            else -> {
                logger.LogInfo("Stacked an unknown bill type: %d", billType)
            }
        }
    }

    fun toggleEscrow(){
        escrow_mode.value = !escrow_mode.value
        _rs232Configuration.ShouldEscrow = escrow_mode.value
    }

    fun toggleBarcodeDetection(){
        detect_barcode.value = !detect_barcode.value
        _rs232Configuration.ShouldDetectBarcodes = detect_barcode.value
    }

    fun toggleIsBillInEscrow(){
        _IsBillInEscrow.value = !_IsBillInEscrow.value
    }

    fun OnPollingClicked(){
        if(isPolling.value){
            billValidator?.StopPollingLoop()
            isPolling.value = !isPolling.value
        } else {
            billValidator?.StartPollingLoop()
            isPolling.value = !isPolling.value
        }
    }

    fun OnStackClicked(){
        if(_IsBillInEscrow.value) {
            billValidator?.StackBill()
            _IsBillInEscrow.value = !_IsBillInEscrow.value
        }
    }

    fun OnReturnClicked(){
        if(_IsBillInEscrow.value) {
            billValidator?.ReturnBill()
            _IsBillInEscrow.value = !_IsBillInEscrow.value
        }
    }



    //States Screen Variables and Methods
    private val _currentState = MutableStateFlow(Rs232State.None)
    val currentState = _currentState.asStateFlow()

    private val _currentEvent = MutableStateFlow(Rs232Event(Rs232Event.None))
    val currentEvent = _currentEvent.asStateFlow()

    private val _cashboxAttached = MutableStateFlow(false)
    val cashboxAttached = _cashboxAttached.asStateFlow()



    //Telemetry Screen Variables and Methods
    var telemetryResponses = mutableStateListOf("N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A")
        private set
    private val ErrorMessage = "An error occurred"

    fun PingValidator() {
        val response = billValidator?.PingAsync()?.get()

        telemetryResponses[0] = response?.IsValid?.get().toString()
    }

    fun GetSerialNumber() {
        val response = billValidator?.GetSerialNumberAsync()?.get()

        var resultValue: String
        if (response?.IsValid?.get() == true && response.serialNumber.isNotEmpty()) {
            resultValue = response.serialNumber
        } else if (response?.IsValid?.get() == true && response.serialNumber.isEmpty()) {
            resultValue = "The acceptor was not assigned a serial number"
        } else {
            resultValue = ErrorMessage
        }

        telemetryResponses[1] = resultValue
    }

    fun GetCashboxMetrics() {
        val response = billValidator?.GetCashboxMetrics()?.get()
        val resultValue =
            if (response?.IsValid?.get() == true) response.toString() else ErrorMessage
        telemetryResponses[2] = resultValue
    }

    fun ClearCashboxCount() {
        val response = billValidator?.ClearCashboxCount()?.get()
        val resultValue =
            if (response?.IsValid?.get() == true) response.toString() else ErrorMessage
        telemetryResponses[3] = resultValue
    }

    fun GetUnitMetrics() {
        val response = billValidator?.GetUnitMetrics()?.get()
        val resultValue =
            if (response?.IsValid?.get() == true) response.toString() else ErrorMessage
        telemetryResponses[4] = resultValue
    }

    fun GetServiceUsageCounters() {
        val response = billValidator?.GetServiceUsageCounters()?.get()
        val resultValue =
            if (response?.IsValid?.get() == true) response.toString() else ErrorMessage
        telemetryResponses[5] = resultValue
    }

    fun GetServiceFlags() {
        val response = billValidator?.GetServiceFlags()?.get()
        val resultValue =
            if (response?.IsValid?.get() == true) response.toString() else ErrorMessage
        telemetryResponses[6] = resultValue
    }

    fun ClearServiceFlags() {
        val response = billValidator?.ClearServiceFlags()?.get()
        val resultValue = response?.IsValid?.get().toString()
        telemetryResponses[7] = resultValue
    }

    fun GetServiceInfo() {
        val response = billValidator?.GetServiceInfo()?.get()
        val resultValue =
            if (response?.IsValid?.get() == true) response.toString() else ErrorMessage
        telemetryResponses[8] = resultValue
    }

    fun GetFirmwareMetrics() {
        val response = billValidator?.GetFirmwareMetrics()?.get()
        val resultValue =
            if (response?.IsValid?.get() == true) response.toString() else ErrorMessage
        telemetryResponses[9] = resultValue
    }



    //Extended Screen Variables and Methods
    private val _lastBarcode = MutableStateFlow("N/A")
    val lastBarcode = _lastBarcode.asStateFlow()

    fun GetLastBarcode() {
        val response = billValidator?.GetDetectedBarcode()?.get()
        var resultValue: String
        if (response?.IsValid?.get() == true && response.barcode.length > 0) {
            resultValue = response.barcode
        } else if (response?.IsValid?.get() == true && response.barcode.length == 0) {
            resultValue = "No barcode was detected since the last power cycle"
        } else {
            resultValue = ErrorMessage
        }

        _lastBarcode.value = resultValue
    }



    //Logs Screen Variables and Methods
    private val _logsTab = MutableStateFlow(true)
    val logsTab = _logsTab.asStateFlow()

    private val _PayloadExchanges = MutableStateFlow<List<PayloadExchange>>(emptyList())
    val PayloadExchanges: StateFlow<List<PayloadExchange>> = _PayloadExchanges.asStateFlow()

    fun toggleLogTab(){
        _logsTab.value = !_logsTab.value
    }
}

data class PayloadExchange(
    val Timestamp: String,
    val RequestPayload: String,
    val RequestDecodedInfo: String,
    val ResponseString: String,
    val ResponseDecodedInfo: String
)