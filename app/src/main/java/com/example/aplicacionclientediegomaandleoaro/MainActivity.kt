package com.example.aplicacionclientediegomaandleoaro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.aplicacionclientediegomaandleoaro.ui.theme.AplicacionClienteDiegoMAandLeoAROTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AplicacionClienteDiegoMAandLeoAROTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CurrencyGraphScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CurrencyGraphScreen(viewModel: CurrencyClientViewModel = viewModel(), modifier: Modifier = Modifier) {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("") }
    val exchangeRates by viewModel.exchangeRates.observeAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Consulta el tipo de cambio", fontSize = 20.sp)

        OutlinedTextField(
            value = startDate,
            onValueChange = { startDate = it },
            label = { Text("Fecha inicio (YYYY-MM-DD)") }
        )

        OutlinedTextField(
            value = endDate,
            onValueChange = { endDate = it },
            label = { Text("Fecha fin (YYYY-MM-DD)") }
        )

        OutlinedTextField(
            value = currency,
            onValueChange = { currency = it.uppercase() },
            label = { Text("Divisa (Ejemplo: USD)") }
        )

        Button(onClick = { viewModel.getExchangeRates(startDate, endDate, currency) }) {
            Text("Consultar")
        }

        if (exchangeRates.isNotEmpty() && currency.isNotEmpty()) {
            val allDates = generateDateRange(startDate, endDate)
            val rateMap = exchangeRates.associateBy { it.date }
            var lastValidRate: Float? = null
            val completeData = allDates.mapIndexed { index, date ->
                val rate = rateMap[date]?.rate?.toFloat() ?: lastValidRate
                lastValidRate = rate
                Entry(index.toFloat(), rate ?: 0f)
            }

            CurrencyLineChart(data = completeData, dates = allDates, currency = currency, modifier = Modifier.fillMaxWidth().height(300.dp))
        } else {
            Text("No hay datos disponibles.")
        }
    }
}

fun generateDateRange(start: String, end: String): List<String> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val startDate = dateFormat.parse(start)
    val endDate = dateFormat.parse(end)
    val calendar = Calendar.getInstance()
    val dateList = mutableListOf<String>()

    if (startDate != null && endDate != null) {
        calendar.time = startDate
        while (!calendar.time.after(endDate)) {
            dateList.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
    return dateList
}

@Composable
fun CurrencyLineChart(data: List<Entry>, dates: List<String>, currency: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = true
                setScaleEnabled(true)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    labelRotationAngle = -45f
                    valueFormatter = IndexAxisValueFormatter(dates)
                    setLabelCount(dates.size, true)
                    spaceMin = 0.5f
                    spaceMax = 0.5f
                }
                axisLeft.apply {
                    granularity = 0.1f
                    val minValue = data.minOfOrNull { it.y } ?: 0f
                    val maxValue = data.maxOfOrNull { it.y } ?: 0f
                    axisMinimum = (minValue - 2f).coerceAtLeast(0f)
                    axisMaximum = maxValue + 2f
                    setLabelCount(10, true)
                }
                axisRight.isEnabled = false
                setExtraOffsets(0f, 0f, 0f, 20f)
            }
        },
        modifier = modifier.fillMaxWidth().height(400.dp),
        update = { lineChart ->
            if (data.isNotEmpty()) {
                val dataSet = LineDataSet(data, "Tipo de Cambio $currency/MXN").apply {
                    color = ColorTemplate.MATERIAL_COLORS[0]
                    valueTextSize = 14f
                    setDrawCircles(true)
                    setDrawValues(true)
                    lineWidth = 3f
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                }
                val lineData = LineData(dataSet)
                lineChart.data = lineData
                lineChart.notifyDataSetChanged()
                lineChart.invalidate()
                lineChart.requestLayout()
            }
        }
    )
}