package com.example.Rs232Validator.Screens

import PTI.Rs232Validator.Rs232Event
import PTI.Rs232Validator.Rs232State
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Rs232Validator.Constants
import com.example.Rs232Validator.ViewModel.ValidatorViewModel

@Composable
fun StatesScreen(viewModel: ValidatorViewModel){
    val states = Rs232State.entries.drop(1).toTypedArray()

    val eventFlags = listOf(
        Rs232Event.Stacked,
        Rs232Event.Returned,
        Rs232Event.Cheated,
        Rs232Event.BillRejected
    )

    val gridItems = listOf(
        listOf("s_1", "s_6", "e_1", "c_1"),
        listOf("s_2", "s_7", "e_2",  null),
        listOf("s_3", "s_8", "e_3",  null),
        listOf("s_4",  null, "e_4",  null),
        listOf("s_5",  null,  null,  null)
    )

    val currentState by viewModel.currentState.collectAsState()
    val currentEvent by viewModel.currentEvent.collectAsState()
    val cashboxAttached by viewModel.cashboxAttached.collectAsState()

    Column (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(gridItems.flatten()) { item ->
                when {
                    item == null -> Box(Modifier.aspectRatio(2f)) {}
                    item.startsWith("s_") -> {
                        val index = item.removePrefix("s_").toInt() - 1
                        val state = states.getOrNull(index)
                        if (state != null) {
                            val isCurrent = currentState == state

                            GridItem(
                                label = state.name,
                                color = if (isCurrent) Color(0xFFADD8E6) else Color.LightGray,
                            )
                        }
                    }

                    item.startsWith("e_") -> {
                        val index = item.removePrefix("e_").toInt() - 1
                        val flag = eventFlags.getOrNull(index)
                        if (flag != null) {
                            val event = Rs232Event(flag)
                            val active = currentEvent.hasFlag(flag)
                            GridItem(
                                label = event.Flags(),
                                color = if (active) Color(0xFF90EE90) else Color.LightGray
                            )
                        }
                    }

                    item.startsWith("c_") -> {
                        GridItem(
                            label = "Cashbox",
                            color = if (cashboxAttached) Color(0xFFFFFFE0) else Color.LightGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GridItem(label: String, color: Color) {
    Button(
        onClick = {},
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(2f),
        enabled = false,
        contentPadding = PaddingValues(
            top = 5.dp,
            bottom = 5.dp,
            start = 5.dp,
            end = 5.dp
        ),
        interactionSource = remember { MutableInteractionSource() },
        colors = ButtonColors(
            containerColor = color,
            contentColor = color,
            disabledContentColor = color,
            disabledContainerColor = color
        ),
        shape = RoundedCornerShape(size = Constants.Medium)
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight(400),
                color = Color.Black,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight(align = Alignment.CenterVertically)
        )
    }
}

