package com.example.Rs232Validator.Screens

import com.example.Rs232Validator.R
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Rs232Validator.Constants
import com.example.Rs232Validator.LogEntry
import com.example.Rs232Validator.ViewModel.PayloadExchange
import com.example.Rs232Validator.ViewModel.ValidatorViewModel

@Composable
fun LogsScreen(viewModel: ValidatorViewModel){
    val logsTab by viewModel.logsTab.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
            ) {
                //Logs Tab
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(98.dp)
                        .fillMaxHeight()
                        .background(
                            color = Constants.NeutralBackgroundTransparentRest,
                            shape = RoundedCornerShape(size = 4.dp)
                        )
                        .padding(start = 10.dp, top = 12.dp, end = 10.dp, bottom = 12.dp)
                        .clickable {
                            if (!logsTab) {
                                viewModel.toggleLogTab()
                            }
                        }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.Start),
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .padding(start = 2.dp, end = 2.dp)
                    ) {
                        Text(
                            text = "Logs",
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight(400),
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .fillMaxSize(),
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.selection_indicator),
                        contentDescription = "image description",
                        contentScale = ContentScale.None,
                        modifier = Modifier
                            .padding(1.dp)
                            .fillMaxSize()
                            .alpha(if (logsTab) 1f else 0f)
                    )
                }

                //Payloads Tab
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(98.dp)
                        .fillMaxHeight()
                        .background(
                            color = Constants.NeutralBackgroundTransparentRest,
                            shape = RoundedCornerShape(size = 4.dp)
                        )
                        .padding(start = 10.dp, top = 12.dp, end = 10.dp, bottom = 12.dp)
                        .clickable {
                            if (logsTab) {
                                viewModel.toggleLogTab()
                            }
                        }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.Start),
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .padding(start = 2.dp, end = 2.dp)
                    ) {
                        Text(
                            text = "Payloads",
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight(400),
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.selection_indicator),
                        contentDescription = "image description",
                        contentScale = ContentScale.None,
                        modifier = Modifier
                            .padding(1.dp)
                            .fillMaxSize()
                            .alpha(if (!logsTab) 1f else 0f)
                    )
                }
            }

            if(logsTab) {
                LogTable(viewModel = viewModel)
            } else {
                PayloadTable(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun LogTable(viewModel: ValidatorViewModel){
    val LogEntries by viewModel.logger.logEntries.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(LogEntries.size) {
        if(LogEntries.isNotEmpty()) listState.animateScrollToItem(LogEntries.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
        ){
            Text(
                "Level",
                modifier = Modifier.weight(0.15f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            VerticalDivider(thickness = 1.dp)

            Text(
                "Timestamp",
                modifier = Modifier.weight(0.3f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            VerticalDivider(thickness = 1.dp)

            Text(
                "Message",
                modifier = Modifier.weight(0.55f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = Constants.NeutralStrokeDisabledRest,
                    shape = RoundedCornerShape(size = 4.dp)
                ),
            state = listState
        ){
            items(LogEntries){ entry ->
                LogRow(entry)
            }
        }
    }
}

@Composable
fun LogRow(entry: LogEntry){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ){
        Text(
            text = entry.Level.name,
            modifier = Modifier.weight(0.15f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = entry.Timestamp,
            modifier = Modifier.weight(0.3f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = entry.Message,
            modifier = Modifier.weight(0.55f),
            fontSize = 14.sp
        )
    }
}

@Composable
fun PayloadTable(viewModel: ValidatorViewModel){
    val PayloadExchanges by viewModel.PayloadExchanges.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(PayloadExchanges.size) {
        if(PayloadExchanges.isNotEmpty()) listState.animateScrollToItem(PayloadExchanges.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
        ){
            Text(
                "Timestamp",
                modifier = Modifier.weight(0.3f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            VerticalDivider(thickness = 1.dp)

            Text(
                "Exchange",
                modifier = Modifier.weight(0.7f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = Constants.NeutralStrokeDisabledRest,
                    shape = RoundedCornerShape(size = 4.dp)
                ),
            state = listState
        ){
            items(PayloadExchanges){ exchange ->
                PayloadRow(exchange)
            }
        }
    }
}

@Composable
fun PayloadRow(exchange: PayloadExchange){

    val Message =
        "Request Payload - ${exchange.RequestPayload}\n" +
        "Request Decoded Info - ${exchange.RequestDecodedInfo}\n" +
        "Response Payload - ${exchange.ResponseString}\n" +
        "Response Decoded Info - ${exchange.ResponseDecodedInfo}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ){
        Text(
            text = exchange.Timestamp,
            modifier = Modifier.weight(0.3f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = Message,
            modifier = Modifier.weight(0.7f),
            fontSize = 14.sp
        )
    }
}