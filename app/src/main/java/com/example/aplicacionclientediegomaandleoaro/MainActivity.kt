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
            // Convertir las fechas en índices numéricos para el eje X
            val filteredData = exchangeRates.mapIndexed { index, rate ->
                Entry(index.toFloat(), rate.rate.toFloat())
            }
            val dates = exchangeRates.map { it.date } // Lista de fechas para el eje X

            CurrencyLineChart(data = filteredData, dates = dates, currency = currency, modifier = Modifier.fillMaxWidth().height(300.dp))
        } else {
            Text("No hay datos disponibles.")
        }
    }
}

@Composable
fun CurrencyLineChart(data: List<Entry>, dates: List<String>, currency: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false // Deshabilitar la descripción
                legend.isEnabled = true // Habilitar la leyenda
                setScaleEnabled(true) // Permitir zoom

                // Configuración del eje X
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM // Posición del eje X en la parte inferior
                    setDrawGridLines(false) // No dibujar líneas de la cuadrícula en el eje X
                    granularity = 1f // Espaciado entre etiquetas
                    labelRotationAngle = -45f // Rotar las etiquetas para evitar superposición
                    valueFormatter = IndexAxisValueFormatter(dates) // Usar las fechas como etiquetas del eje X
                    setLabelCount(dates.size, true) // Ajustar el número de etiquetas
                    spaceMin = 0.5f // Espacio mínimo entre etiquetas
                    spaceMax = 0.5f // Espacio máximo entre etiquetas
                }

                // Configuración del eje Y (izquierdo)
                axisLeft.apply {
                    granularity = 1f // Espaciado entre valores
                    axisMinimum = 0f // Valor mínimo del eje Y
                    // Calcular el valor máximo de los datos y agregar un margen del 10%
                    val maxValue = data.maxOfOrNull { it.y } ?: 0f
                    axisMaximum = maxValue * 1.1f // Añadir un 10% de espacio adicional
                }

                // Deshabilitar eje derecho
                axisRight.isEnabled = false

                // Ajustar el margen inferior para evitar que las etiquetas se corten
                setExtraOffsets(0f, 0f, 0f, 20f) // Margen inferior de 20f para las etiquetas
            }
        },
        modifier = modifier.fillMaxWidth().height(400.dp), // Tamaño del gráfico
        update = { lineChart ->
            if (data.isNotEmpty()) {
                // Crear un conjunto de datos para la gráfica
                val dataSet = LineDataSet(data, "Tipo de Cambio $currency/MXN").apply {
                    color = ColorTemplate.COLORFUL_COLORS[0] // Color de la línea
                    valueTextSize = 12f // Tamaño del texto de los valores
                    setDrawCircles(true) // Dibujar círculos en los puntos de datos
                    setDrawValues(true) // Mostrar valores en los puntos de datos
                    lineWidth = 2f // Grosor de la línea
                }

                // Configurar los datos de la gráfica
                val lineData = LineData(dataSet)
                lineChart.data = lineData

                // Actualizar la gráfica
                lineChart.notifyDataSetChanged()
                lineChart.invalidate()
                lineChart.requestLayout()
            }
        }
    )
}