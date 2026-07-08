package com.example.Rs232Validator.ViewModel

import PTI.Rs232Validator.BillValidators.BillValidator
import PTI.Rs232Validator.*
import PTI.Rs232Validator.SerialProviders.ISerialProvider
import PTI.Rs232Validator.Utility.ByteUtils
import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.Rs232Validator.Logger
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ValidatorViewModel(application: Application) : AndroidViewModel(application){

    //Initial Parameters
    private var TimestampFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss a")
    @SuppressLint("StaticFieldLeak")
    private lateinit var billValidator: BillValidator
    var escrow_mode = mutableStateOf(false)
        private set
    var detect_barcode = mutableStateOf(false)
        private set
    var enableMask = mutableStateListOf(true, true, true, true, true, true, true)
        private set
    fun GetMask(): Byte {
        var mask = 0
        for(i in 0 .. enableMask.size-1){
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
    
    fun initializeValidator(serialProvider: ISerialProvider){
        billValidator = BillValidator(logger, serialProvider, _rs232Configuration)
        registerListeners()
    }

    private fun registerListeners(){
        billValidator.OnBillStacked?.addListener { args ->
            val billType = args.getOrNull(0) as? Byte ?:return@addListener
            updateBillValue(billType.toInt())
        }

        billValidator.OnBillEscrowed?.addListener {
            toggleIsBillInEscrow()
        }

        billValidator.OnStateChanged?.addListener { args ->
            val oldState: Rs232State = args.getOrNull(0) as? Rs232State ?:return@addListener
            val newState: Rs232State = args.getOrNull(1) as? Rs232State ?:return@addListener

            logger.LogInfo("The state changed from %s to %s.", oldState.name, newState.name)

            viewModelScope.launch {
                _currentState.value = newState
            }
        }

        billValidator.OnBarcodeDetected?.addListener { args ->
            val barcode: String = args.getOrNull(0) as? String ?:return@addListener

            viewModelScope.launch {
                _lastBarcode.value = barcode
            }
        }

        billValidator.OnCashboxAttached?.addListener {
            viewModelScope.launch {
                _cashboxAttached.value = true
            }
        }

        billValidator.OnCashboxRemoved?.addListener {
            viewModelScope.launch {
                _cashboxAttached.value = false
            }
        }

        billValidator.OnEventReported?.addListener { args ->
            val event: Rs232Event = args.getOrNull(0) as? Rs232Event ?:return@addListener
            viewModelScope.launch {
                logger.LogInfo("Received event(s): %s.", event.Flags())
                _currentEvent.value = event
            }
        }

        billValidator.OnConnectionLost?.addListener {
            viewModelScope.launch {
                isPolling.value = false
            }
        }

        billValidator.OnCommunicationAttempted?.addListener { args ->
            val entry = PayloadExchange(
                LocalDateTime.now().format(TimestampFormat),
                ByteUtils.ConvertToHexString(args.getOrNull(0) as? List<Byte> ?:return@addListener, false, true),
                "",
                ByteUtils.ConvertToHexString(args.getOrNull(1) as? List<Byte> ?:return@addListener, false, true),
                ""
            )

            viewModelScope.launch {
                _PayloadExchanges.update { it + entry }
            }
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
        if (type < 1 || type > enableMask.size) {
            logger.LogInfo("onMaskChanged called with invalid type: %d", type)
            return
        }
        enableMask[type - 1] = newMask
        _rs232Configuration.EnableMask = GetMask()
    }

    fun updateBillValue(billType: Int){
        viewModelScope.launch {
            billValues[billType - 1]++

            when (billType) {
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
        viewModelScope.launch {
            _IsBillInEscrow.value = !_IsBillInEscrow.value
        }
    }

    fun OnPollingClicked(){
        if(isPolling.value){
            billValidator.StopPollingLoop()
            viewModelScope.launch {
                isPolling.value = !isPolling.value
            }
        } else {
            viewModelScope.launch {
                withContext(Dispatchers.IO){
                    billValidator.StartPollingLoop()
                    isPolling.value = billValidator._isPolling
                }
            }
        }
    }

    fun OnStackClicked(){
        if(_IsBillInEscrow.value) {

            viewModelScope.launch {
                withContext(Dispatchers.IO){
                     billValidator.StackBill()
                }
                _IsBillInEscrow.value = !_IsBillInEscrow.value
            }
        }
    }

    fun OnReturnClicked(){
        if(_IsBillInEscrow.value) {
            viewModelScope.launch {
                withContext(Dispatchers.IO){
                     billValidator.ReturnBill()
                }
                _IsBillInEscrow.value = !_IsBillInEscrow.value
            }
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
        viewModelScope.launch {
            val response = billValidator.PingAsync()?.await()
            telemetryResponses[0] = response.toString()
        }
    }

    fun GetSerialNumber() {
        viewModelScope.launch {
            val response = billValidator.GetSerialNumberAsync()?.await()

            val resultValue: String
            /*if (response?.IsValid?.get().toString() == "true" && response?.serialNumber?.isNotEmpty() == true) {
                    resultValue = response.serialNumber ?: ""
                } else if (response?.IsValid?.get() == true && response.serialNumber.isEmpty()) {
                    resultValue = "The acceptor was not assigned a serial number"
                } else {
                    resultValue = ErrorMessage
                }*/
            telemetryResponses[1] = response.toString()
        }
    }

    fun GetCashboxMetrics() {
        viewModelScope.launch {
            val response = billValidator.GetCashboxMetrics()?.await()
            val resultValue =
                if (response?.IsValid?.get() == true) response.toString() else ErrorMessage
            telemetryResponses[2] = resultValue
        }
    }

    fun ClearCashboxCount() {
        viewModelScope.launch {
            val response = billValidator.ClearCashboxCount()?.await()
            val resultValue =
                if (response?.IsValid?.get() == true) response.toString() else ErrorMessage
            telemetryResponses[3] = resultValue
        }
    }

    fun GetUnitMetrics() {
        viewModelScope.launch {
            val response = billValidator.GetUnitMetrics()?.await()
            val resultValue =
                if (response?.IsValid?.get() == true) response.toString() else ErrorMessage
            telemetryResponses[4] = resultValue
        }
    }

    fun GetServiceUsageCounters() {
        viewModelScope.launch {
            val response = billValidator.GetServiceUsageCounters()?.await()
            val resultValue =
                if (response?.IsValid?.get() == true) response.toString() else ErrorMessage
            telemetryResponses[5] = resultValue
        }
    }

    fun GetServiceFlags() {
        viewModelScope.launch {
            val response = billValidator.GetServiceFlags()?.await()
            val resultValue =
                if (response?.IsValid?.get() == true) response.toString() else ErrorMessage
            telemetryResponses[6] = resultValue
        }
    }

    fun ClearServiceFlags() {
        //viewModelScope.launch {
        //    val response = billValidator.ClearServiceFlags()?.await()
        //    val resultValue = response?.IsValid.toString()
        //    telemetryResponses[7] = resultValue
        //}
    }

    fun GetServiceInfo() {
        viewModelScope.launch {
            val response = billValidator.GetServiceInfo()?.await()
            val resultValue =
                if (response?.IsValid?.get() == true) response.toString() else ErrorMessage
            telemetryResponses[8] = resultValue
        }
    }

    fun GetFirmwareMetrics() {
        viewModelScope.launch {
            val response = billValidator.GetFirmwareMetrics()?.await()
            val resultValue =
                if (response?.IsValid?.get() == true) response.toString() else ErrorMessage
            telemetryResponses[9] = resultValue
        }
    }



    //Extended Screen Variables and Methods
    private val _lastBarcode = MutableStateFlow("N/A")
    val lastBarcode = _lastBarcode.asStateFlow()

    fun GetLastBarcode() {
        viewModelScope.launch {
            val response = billValidator.GetDetectedBarcode()?.await()
            val resultValue: String
            if (response?.IsValid?.get() == true && response.barcode.isNotEmpty()) {
                resultValue = response.barcode
            } else if (response?.IsValid?.get() == true && response.barcode.isEmpty()) {
                resultValue = "No barcode was detected since the last power cycle"
            } else {
                resultValue = ErrorMessage
            }

            _lastBarcode.value = resultValue
        }
    }



    //Logs Screen Variables and Methods
    private val _logsTab = MutableStateFlow(true)
    val logsTab = _logsTab.asStateFlow()

    private val _PayloadExchanges = MutableStateFlow<List<PayloadExchange>>(emptyList())
    val PayloadExchanges: StateFlow<List<PayloadExchange>> = _PayloadExchanges.asStateFlow()

    fun toggleLogTab(){
        viewModelScope.launch {
            _logsTab.value = !_logsTab.value
        }
    }
}
