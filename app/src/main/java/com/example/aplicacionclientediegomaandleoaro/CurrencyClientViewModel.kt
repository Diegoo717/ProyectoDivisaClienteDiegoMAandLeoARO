package com.example.aplicacionclientediegomaandleoaro

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ExchangeRate(
    val date: String, // Fecha del registro (YYYY-MM-DD)
    val rate: Double  // Tasa de cambio para la divisa seleccionada
)

class CurrencyClientViewModel(application: Application) : AndroidViewModel(application) {

    private val _exchangeRates = MutableLiveData<List<ExchangeRate>>()
    val exchangeRates: LiveData<List<ExchangeRate>> get() = _exchangeRates

    /**
     * Convierte un timestamp en milisegundos a una fecha real en formato YYYY-MM-DD
     */
    private fun convertTimestampToDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault() // Usa la zona horaria del dispositivo
        return sdf.format(Date(timestamp))
    }

    fun getExchangeRates(startDate: String, endDate: String, currency: String) {
        Log.d("CurrencyClientViewModel", "getExchangeRates llamado con startDate=$startDate, endDate=$endDate, currency=$currency")

        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val contentResolver = context.contentResolver

            val uri = Uri.parse("content://com.example.exchangerate.provider/currency_table/$startDate/$endDate")
            Log.d("CurrencyClientViewModel", "URI construida: $uri")

            val cursor: Cursor? = contentResolver.query(uri, null, null, null, "timestamp DESC")
            Log.d("CurrencyClientViewModel", "Cursor obtenido: ${cursor != null}")

            val exchangeMap = mutableMapOf<String, ExchangeRate>() // Mapa para almacenar solo el último valor por día

            cursor?.use {
                val timestampIndex = it.getColumnIndex("timestamp")
                val rateIndex = it.getColumnIndex("conversionRates")

                while (it.moveToNext()) {
                    // Convertir el timestamp a fecha real
                    val timestamp = it.getLong(timestampIndex) // Timestamp en milisegundos
                    val date = convertTimestampToDate(timestamp) // Convertir a YYYY-MM-DD
                    Log.d("CurrencyClientViewModel", "Timestamp: $timestamp, Fecha convertida: $date")

                    // Obtener las tasas de cambio en formato JSON
                    val ratesJson = it.getString(rateIndex)
                    val rates: Map<String, Double> = Gson().fromJson(ratesJson, object : TypeToken<Map<String, Double>>() {}.type)

                    if (rates.containsKey(currency)) { // Filtrar por divisa
                        val exchangeRate = ExchangeRate(date, rates[currency]!!)

                        // Solo guardar el último valor de cada día
                        exchangeMap[date] = exchangeRate
                        Log.d("CurrencyClientViewModel", "Registro actualizado para $date -> ${rates[currency]}")
                    }
                }
            }

            // Ordenar los datos por fecha ascendente
            val sortedList = exchangeMap.values.sortedBy { it.date }
            Log.d("CurrencyClientViewModel", "Datos filtrados y ordenados: $sortedList")

            // Actualizar la UI
            _exchangeRates.postValue(sortedList)
        }
    }
}
