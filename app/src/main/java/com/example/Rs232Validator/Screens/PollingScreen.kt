package com.example.Rs232Validator.Screens

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Rs232Validator.ViewModel.ValidatorViewModel
import com.example.Rs232Validator.Constants


@Composable
fun PollingScreen(viewModel: ValidatorViewModel){
    //The Overall page container
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFFFFFFF))
            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        // Bill Masking and Value area
        LazyVerticalGrid(
            columns = GridCells.Adaptive(180.dp),
            contentPadding = PaddingValues(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp),
            userScrollEnabled = false
        ) {
            val numbers = mutableStateListOf<Int>()
            for (i in 1..8){
                numbers.add(i)
            }

            items(numbers){
                BillValueRow(billType = it, viewModel = viewModel)
            }
        }

        HorizontalDivider(thickness = 1.dp, color = Constants.NeutralStrokeDisabledRest)

        ControlSection(viewModel)
    }
}

@Composable
fun BillValueRow(billType : Int, viewModel: ValidatorViewModel) {
    Row (
        modifier = Modifier
            .height(72.dp)
            .padding(start = 10.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BillValueComponent(billType, viewModel = viewModel)
    }
}

@Composable
fun BillValueComponent(billType: Int, viewModel: ValidatorViewModel){
    val billVal by remember {
        derivedStateOf { viewModel.billValues[billType - 1] }
    }

    val maskBill by remember {
        derivedStateOf { viewModel.enableMask[billType - 1] }
    }

    Row(
        modifier = Modifier
            .width(82.dp)
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        //Bill 1 Masking checkbox
        if(billType != 8){
            Checkbox(
                checked = maskBill,
                onCheckedChange = { newMask ->
                    viewModel.onMaskChanged(billType, newMask)
                },
                enabled = true,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.Blue,
                    uncheckedColor = Color.DarkGray,
                    checkmarkColor = Color.White
                ),
                interactionSource = remember { MutableInteractionSource() }
            )
        }

        //Bill 1 Label
        Text(
            text = if(billType == 8) "Total" else "Bill $billType",
            style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight(400)
            )
        )
    }

    //Bill 1 TextField
    Column(
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(60.dp)
            .fillMaxHeight()
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
                .width(60.dp)
                .height(32.dp)
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
                    .width(50.dp)
                    .height(32.dp)
                    .padding(start = 2.dp, top = 5.dp, end = 2.dp, bottom = 7.dp)
            ) {
                Text(
                    text = billVal.toString(),
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight(400),
                        textAlign = TextAlign.Right,
                    ),
                    modifier = Modifier
                        .width(46.dp)
                        .height(20.dp)
                )
            }
        }
    }
}

@Composable
fun ControlSection(viewModel: ValidatorViewModel){
    val escrow_mode by remember {
        derivedStateOf { viewModel.escrow_mode }
    }

    val detect_barcodes by remember {
        derivedStateOf { viewModel.detect_barcode }
    }

    val isPolling by remember {
        derivedStateOf { viewModel.isPolling }
    }

    val IsBillInEscrow by viewModel.IsBillInEscrow.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(200.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .heightIn(max = 350.dp),
        userScrollEnabled = false
    ) {
        item {
            Button(
                onClick = {
                    viewModel.OnPollingClicked()
                },
                modifier = Modifier
                    .width(130.dp)
                    .height(91.75.dp)
                    .padding(start = 12.dp, top = 5.dp, end = 12.dp, bottom = 5.dp),
                enabled = true,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = 30.dp,
                    bottom = 30.dp,
                    end = 12.dp
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
                    text = if(isPolling.value) "Stop Polling" else "Start Polling",
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight(600),
                    )
                )
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.OnStackClicked()
                },
                modifier = Modifier
                    .width(130.dp)
                    .height(91.75.dp)
                    .padding(start = 12.dp, top = 5.dp, end = 12.dp, bottom = 5.dp),
                enabled = IsBillInEscrow,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = 30.dp,
                    bottom = 30.dp,
                    end = 12.dp
                ),
                interactionSource = remember { MutableInteractionSource() },
                colors = ButtonColors(
                    containerColor = Color(0xFF48C748),
                    contentColor = Color.White,
                    disabledContentColor = Color.White,
                    disabledContainerColor = Color(0xFF48C748)
                ),
                shape = RoundedCornerShape(size = Constants.Medium)
            ) {
                Text(
                    text = "Stack",
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight(600),
                    )
                )
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.OnReturnClicked()
                },
                modifier = Modifier
                    .width(130.dp)
                    .height(91.75.dp)
                    .padding(start = 12.dp, top = 5.dp, end = 12.dp, bottom = 5.dp),
                enabled = IsBillInEscrow,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = 30.dp,
                    bottom = 30.dp,
                    end = 12.dp
                ),
                interactionSource = remember { MutableInteractionSource() },
                colors = ButtonColors(
                    containerColor = Color(0xFFDA4A4A),
                    contentColor = Color.White,
                    disabledContentColor = Color.White,
                    disabledContainerColor = Color(0xFFDA4A4A)
                ),
                shape = RoundedCornerShape(size = Constants.Medium)
            ) {
                Text(
                    text = "Return",
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight(600),
                    )
                )
            }
        }

        item {
            LazyHorizontalGrid(
                rows = GridCells.Adaptive(40.75.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                userScrollEnabled = false
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(111.75.dp)
                            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
                            .clickable {
                                viewModel.toggleEscrow()
                            },
                        horizontalArrangement = Arrangement.spacedBy(
                            10.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = escrow_mode.value,
                            onCheckedChange = {
                                viewModel.toggleEscrow()
                            },
                            enabled = true,
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.Blue,
                                uncheckedColor = Color.DarkGray,
                                checkmarkColor = Color.White
                            ),
                            interactionSource = remember { MutableInteractionSource() }
                        )

                        Text(
                            text = "Escrow Mode",
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight(400)
                            )
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(111.75.dp)
                            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
                            .clickable {
                                viewModel.toggleBarcodeDetection()
                            },
                        horizontalArrangement = Arrangement.spacedBy(
                            10.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = detect_barcodes.value,
                            onCheckedChange = {
                                viewModel.toggleBarcodeDetection()
                            },
                            enabled = true,
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.Blue,
                                uncheckedColor = Color.DarkGray,
                                checkmarkColor = Color.White
                            ),
                            interactionSource = remember { MutableInteractionSource() }
                        )

                        Text(
                            text = "Barcode Detection",
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight(400)
                            )
                        )
                    }
                }
            }
        }
    }
}