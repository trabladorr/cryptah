package cryptah.trabladorr.cryptah

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.beust.klaxon.*
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import org.jetbrains.anko.*
import java.net.URL
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.charts.CombinedChart.DrawOrder
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.utils.ViewPortHandler
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*


const val lowPos: Float = 1.003f


fun getJson(url: String): JsonObject? {
    return Parser().parse(StringBuilder(URL(url).readText())) as JsonObject
}

fun applyCandleSetParams(context: Context, set: CandleDataSet): CandleDataSet{
    with(set) {
        axisDependency = AxisDependency.LEFT
        color = context.getColor(R.color.colorChartCandle)
        shadowColor = context.getColor(R.color.colorChartCandle)
        shadowWidth = 0.7f
        decreasingColor = context.getColor(R.color.colorChartDecreasing)
        decreasingPaintStyle = android.graphics.Paint.Style.FILL
        increasingColor = context.getColor(R.color.colorChartIncreasing)
        increasingPaintStyle = android.graphics.Paint.Style.FILL
        barSpace = 0.05f
        valueTextColor = context.getColor(R.color.colorChartIncreasing)
        neutralColor = context.getColor(R.color.colorChartNeutral)
        valueTextSize = 8f
        highLightColor = Color.TRANSPARENT
    }
    return set
}

fun applyBarSetParams(context: Context, set: BarDataSet): BarDataSet{
    with(set) {
        color = context.getColor(R.color.colorChartBar)
        valueTextColor = context.getColor(R.color.colorChartBarText)
        valueTextSize = 8f
        axisDependency = AxisDependency.RIGHT
    }
    return set
}

//this is unused
fun applyLineSetParams(set: LineDataSet): LineDataSet{
    with(set) {
        setDrawHighlightIndicators(false)
        color = Color.TRANSPARENT
        valueTextColor = Color.TRANSPARENT
        valueTextSize = 8f
        setDrawCircles(false)
        setDrawCircleHole(false)
        axisDependency = AxisDependency.LEFT
        valueFormatter = object: DefaultValueFormatter((set.valueFormatter as DefaultValueFormatter).decimalDigits) {
            override fun getFormattedValue(value: Float, entry: Entry, dataSetIndex: Int, viewPortHandler: ViewPortHandler): String {
                return super.getFormattedValue(value*lowPos, entry, dataSetIndex, viewPortHandler)
            }
        }
    }
    return set
}

class CandleActivity : FragmentActivity(), AnkoLogger, ChoiceDialogListener {

    private lateinit var coinSpinner : Spinner
    private lateinit var currencySpinner : Spinner
    private lateinit var intervalSpinner : Spinner
    private lateinit var exchangeSpinner : Spinner
    private lateinit var chart: CombinedChart
    private lateinit var latest: TextView

    var coin : String? = null
    var currency : String? = null
    var exchange : String? = null
    var interval : String? = null

    private val histoVals = 300

    private lateinit var coins : Array<String>
    private lateinit var currencies : Array<String>
    val intervals = arrayOf("Minute","Hour","Day")

    var exchanges = arrayOf("None")


    private var timestamps : List<Int>? = listOf()



    fun updateExchanges () {
        doAsync{

            val jsonObj = getJson("https://min-api.cryptocompare.com/data/top/exchanges?fsym=$coin&tsym=$currency&limit=50")

            if (jsonObj?.string("Response") == "Success"){
                exchanges = (jsonObj.array<JsonObject>("Data")?.string("exchange")?.value as List<String>).toTypedArray()
                if (exchanges.isNotEmpty())
                    uiThread {
                        exchangeSpinner.adapter = ArrayAdapter<String>(this@CandleActivity,android.R.layout.simple_list_item_1,exchanges)
                        exchange = exchanges[0]
                        loadChart()
                    }
                else{
                    uiThread {
                        chart.clear()
                    }
                }
            }
            else{
                uiThread {
                    chart.clear()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun loadLatest(){
        doAsync{
            val jsonObj = getJson("https://min-api.cryptocompare.com/data/price?fsym=$coin&tsyms=$currency&e=$exchange")
            if (jsonObj?.string("Response") == "Error")
                return@doAsync
            val last= jsonObj?.get(currency!!)

            uiThread {
                // latest.text = getString(R.string.text_latest,last,currency) //bad formatting, not enough formatting options
                latest.text = getString(R.string.text_latest)+" $last $currency"
            }
        }
    }

    fun loadChart(){
        if (coin != null && currency!= null && exchange!= null && interval!= null)
            doAsync{

                val jsonObj = getJson("https://min-api.cryptocompare.com/data/histo${interval?.toLowerCase(Locale.ROOT)}?fsym=$coin&tsym=$currency&e=$exchange&limit=$histoVals")

                if (jsonObj?.string("Response") == "Success"){
                    val arr = (jsonObj.array<JsonObject>("Data") as JsonArray<JsonObject>)
//                    Log.d("DBG",arr.toString())

                    val barValues = List(arr.size) { i ->
                        BarEntry(
                                i.toFloat(),
                                (arr[i]["volumeto"] as Number).toFloat())
                    }


                    val candleValues = List(arr.size) { i ->
                        CandleEntry(
                                i.toFloat(),
                                (arr[i]["high"] as Number).toFloat(),
                                (arr[i]["low"] as Number).toFloat(),
                                (arr[i]["open"] as Number).toFloat(),
                                (arr[i]["close"] as Number).toFloat())
                    }

                    val lineValues = List(arr.size) { i ->
                        Entry(
                                i.toFloat(),
                                (arr[i]["low"] as Number).toFloat() / lowPos)
                    }

                    timestamps = List(arr.size) { i -> (arr[i]["time"] as Int) }

                    val data = CombinedData()
                    data.setData(BarData(applyBarSetParams(applicationContext,BarDataSet(barValues, getString(R.string.legendVolume)))))
                    data.setData(CandleData(applyCandleSetParams(applicationContext,CandleDataSet(candleValues, getString(R.string.legendOHLC)))))
                    data.setData(LineData(applyLineSetParams(LineDataSet(lineValues, ""))))

                    loadLatest()

                    uiThread {
                        chart.resetTracking()
                        chart.xAxis.valueFormatter = TimestampAxisValueFormatter(timestamps,interval)
                        chart.xAxis.granularity = 1f
                        chart.axisRight.axisMaximum = data.barData.yMax *3
                        chart.data = data
                        chart.invalidate()
                    }
                }
                else{
                    uiThread {
                        chart.clear()
                    }
                }
            }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_candle)

        findViewById<FloatingActionButton>(R.id.fab_refresh).setOnClickListener {
            loadChart()
        }

        coinSpinner = findViewById(R.id.spinner_coin)
        currencySpinner = findViewById(R.id.spinner_currency)
        intervalSpinner = findViewById(R.id.spinner_interval)
        exchangeSpinner = findViewById(R.id.spinner_exchange)

        latest = findViewById(R.id.text_latest)

        coins = getCoinChoiceArray(this)
        currencies = resources.getStringArray(R.array.currencies)

        val prefs = getSharedPreferences(getString(R.string.pref_file_key), Context.MODE_PRIVATE)
        coin = prefs.getString(getString(R.string.pref_coin) , coins[0])
        currency = prefs.getString(getString(R.string.pref_currency) , currencies[0])
        interval = prefs.getString(getString(R.string.pref_interval) , intervals[0])
        Log.d("DBG:","$coin $currency $interval")

        chart = findViewById(R.id.chart_candle)

        chart.setBackgroundColor(this.getColor(R.color.colorBackground))
        chart.setMaxVisibleValueCount(30)
        chart.description.isEnabled = false
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.axisLeft.gridColor = this.getColor(R.color.colorChartCandleText)
        chart.axisLeft.spaceBottom = 50f
        chart.axisLeft.textColor = this.getColor(R.color.colorChartCandleText)
        chart.axisRight.gridColor = this.getColor(R.color.colorChartBarText)
        chart.axisRight.textColor = this.getColor(R.color.colorChartBarText)
        chart.axisRight.spaceBottom = 0f
        chart.axisRight.spaceTop = 20f
        chart.legend.textColor =this.getColor(R.color.colorOnBackground)
        chart.drawOrder = arrayOf(DrawOrder.BAR, DrawOrder.BUBBLE, DrawOrder.CANDLE, DrawOrder.LINE, DrawOrder.SCATTER)

        coinSpinner.adapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,coins)
        currencySpinner.adapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,currencies)
        intervalSpinner.adapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,intervals)
        exchangeSpinner.adapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,exchanges)

        coinSpinner.setSelection(coins.indexOf(coin))
        currencySpinner.setSelection(currencies.indexOf(currency))
        intervalSpinner.setSelection(intervals.indexOf(interval))

        coinSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                if (coins[p2] == getString(R.string.coin_edit)){
                    CoinChoiceDialog().show(supportFragmentManager, "coin")
                    return
                }
                if (coin != coins[p2]) {
                    coin = coins[p2]
                    with(getSharedPreferences(getString(R.string.pref_file_key), Context.MODE_PRIVATE)?.edit()!!){
                        putString(getString(R.string.pref_coin), coin)
                        apply()
                    }
                    if (currency != null)
                        updateExchanges()
                }
            }
        }


        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                if (currency != currencies[p2]) {
                    currency = currencies[p2]
                    with(getSharedPreferences(getString(R.string.pref_file_key), Context.MODE_PRIVATE)?.edit()!!){
                        putString(getString(R.string.pref_currency), currency)
                        apply()
                    }
                    if (coin != null)
                        updateExchanges()
                }
            }
        }

        intervalSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                if (interval != intervals[p2]) {
                    interval = intervals[p2]
                    with(getSharedPreferences(getString(R.string.pref_file_key), Context.MODE_PRIVATE)?.edit()!!){
                        putString(getString(R.string.pref_interval), interval)
                        apply()
                    }
                    loadChart()
                }
            }
        }

        exchangeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                exchange = exchanges[p2]
                loadChart()
            }
        }

        updateExchanges()
    }

    private fun getCoinChoiceArray (context: Context): Array<String> {
        val coinSet: Set<String> = context.getSharedPreferences(getString(R.string.pref_file_key), Context.MODE_PRIVATE).getStringSet(getString(R.string.pref_coins) , setOf()) as Set<String>
        val coinArray : Array<String> = context.resources.getStringArray(R.array.coins_100)
        val coinList : MutableList<String> = coinArray.toMutableList()
        coinList.retainAll { it in coinSet }
        coinList.add(getString(R.string.coin_edit))
        return coinList.toTypedArray()
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        if (dialog::class == CoinChoiceDialog::class){
            coins = getCoinChoiceArray(this)
            coinSpinner.adapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,coins)
        }
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
    }
}

interface ChoiceDialogListener {
    fun onDialogPositiveClick(dialog: DialogFragment)
    fun onDialogNegativeClick(dialog: DialogFragment)
}
