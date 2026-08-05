package com.example.Rs232Validator.Screens

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Rs232Validator.Constants
import com.example.Rs232Validator.ViewModel.ValidatorViewModel

@Composable
fun TelemetryScreen(viewModel: ValidatorViewModel){
    //The Overall page container
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFFFFFFF))
            .verticalScroll(rememberScrollState())
            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        TelemetryControls(viewModel = viewModel)
    }
}

@Composable
fun TelemetryControls(viewModel: ValidatorViewModel){
    val selectedOption by viewModel.correctableComponent.collectAsState()

    val telemetryResponses by remember {
        derivedStateOf {  viewModel.telemetryResponses }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TelemetryControl("Ping", telemetryResponses[0]) {
            viewModel.PingValidator()
        }
        TelemetryControl("Get\nSerial Number", telemetryResponses[1]) {
            viewModel.GetSerialNumber()
        }
        TelemetryControl("Get\nCashbox Metrics", telemetryResponses[2]) {
            viewModel.GetCashboxMetrics()
        }
        TelemetryControl("Clear\nCashbox", telemetryResponses[3]) {
            viewModel.ClearCashboxCount()
        }
        TelemetryControl("Get\nUnit Metrics", telemetryResponses[4]) {
            viewModel.GetUnitMetrics()
        }
        TelemetryControl("Get Service\nUsage Counters", telemetryResponses[5]) {
            viewModel.GetServiceUsageCounters()
        }
        TelemetryControl("Get\nService Flags", telemetryResponses[6]) {
            viewModel.GetServiceFlags()
        }
        TelemetryControl("Clear\nService Flags", telemetryResponses[7]) {
            viewModel.ClearServiceFlags()
        }
        CorrectableComponentMenu(
            label = "Correctable Component",
            options = listOf("Tach Sensor", "Bill Path", "Cashbox Belt", "Cashbox Mechanism", "MAS", "Spring Rollers", "All"),
            selectedOption = selectedOption,
            onOptionSelected = { newSelection ->
                viewModel.onCorrectableComponentChanged(newSelection)
            }
        )
        TelemetryControl("Get\nService Info", telemetryResponses[8]) {
            viewModel.GetServiceInfo()
        }
        TelemetryControl("Get\nFirmware Metrics", telemetryResponses[9]) {
            viewModel.GetFirmwareMetrics()
        }
    }
}


@Composable
fun TelemetryControl(name: String, response: String, onClick: () -> Unit){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = {
                onClick()
            },
            modifier = Modifier
                .width(134.dp)
                .fillMaxHeight(),
            enabled = true,
            contentPadding = PaddingValues(
                top = 5.dp,
                bottom = 5.dp,
                start = 5.dp,
                end = 5.dp
            ),
            interactionSource = remember { MutableInteractionSource() },
            colors = ButtonColors(
                containerColor = Color(0xFF0F6CBD),
                contentColor = Color.White,
                disabledContentColor = Color.White,
                disabledContainerColor = Color(0xFF0F6CBD)
            ),
            shape = RoundedCornerShape(size = Constants.Medium)
        ) {
            Text(
                text = name,
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight(400),
                    color = Constants.NeutralForegroundOnBrandRest,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight(align = Alignment.CenterVertically)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = Constants.NeutralStrokeDisabledRest,
                        shape = RoundedCornerShape(size = 4.dp)
                    )
                    .fillMaxSize()
                    .background(
                        color = Constants.NeutralBackgroundTransparentRest,
                        shape = RoundedCornerShape(size = 4.dp)
                    )
                    .padding(start = 10.dp, end = 10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.Start),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 2.dp, top = 5.dp, end = 2.dp, bottom = 7.dp)
                ) {
                    Text(
                        text = response,
                        style = TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight(400),
                            textAlign = TextAlign.Right,
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorrectableComponentMenu(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
){
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}