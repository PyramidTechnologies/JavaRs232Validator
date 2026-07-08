package com.example.Rs232Validator.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
fun ExtendedScreen(viewModel: ValidatorViewModel){
    val lastBarcode by viewModel.lastBarcode.collectAsState()
    val detectBarcode by remember {
        derivedStateOf { viewModel.detect_barcode }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        Button(
            onClick = {
                viewModel.GetLastBarcode()
            },
            modifier = Modifier
                .widthIn(min = 150.dp)
                .heightIn(min = 96.dp),
            enabled = detectBarcode.value,
            contentPadding = PaddingValues(bottom = 2.dp),
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
                text = "Get Detected Barcode",
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight(400),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.width(134.dp).height(20.dp)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .width(372.dp)
                .height(100.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .border(width = 1.dp, color = Constants.NeutralStrokeDisabledRest, shape = RoundedCornerShape(size = 4.dp))
                    .width(372.dp)
                    .height(100.dp)
                    .background(color = Constants.NeutralBackgroundTransparentRest, shape = RoundedCornerShape(size = 4.dp))
                    .padding(start = 10.dp, end = 10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.Start),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .width(352.dp)
                        .wrapContentHeight()
                        .defaultMinSize(minHeight = 100.dp)
                        .padding(start = 2.dp, top = 5.dp, end = 2.dp, bottom = 7.dp)
                ) {
                    Text(
                        text = lastBarcode,
                        style = TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight(400),
                        ),
                        modifier = Modifier
                            .width(348.dp)
                            .height(88.dp)
                    )
                }
            }
        }
    }
}