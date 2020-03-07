package cryptah.trabladorr.cryptah

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.beust.klaxon.*
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import org.jetbrains.anko.*
import java.net.URL
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.charts.CombinedChart.DrawOrder
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.utils.ViewPortHandler
import com.github.mikephil.charting.formatter.IValueFormatter



val lowPos: Float = 1.003f

fun getJson(url: String): JsonObject? {
    return Parser().parse(StringBuilder(URL(url).readText())) as JsonObject
}

fun applyCandleSetParams(set: CandleDataSet): CandleDataSet{
    with(set) {
        setDrawIcons(false)
        axisDependency = AxisDependency.LEFT
        color = Color.rgb(80, 80, 80)
        setShadowColor(android.graphics.Color.DKGRAY)
        setShadowWidth(0.7f)
        setDecreasingColor(android.graphics.Color.RED)
        setDecreasingPaintStyle(android.graphics.Paint.Style.FILL)
        setIncreasingColor(android.graphics.Color.GREEN)
        setIncreasingPaintStyle(android.graphics.Paint.Style.FILL)
        barSpace = 0.05f
        valueTextColor = Color.rgb(60, 100, 60)
        neutralColor = Color.BLUE
        valueTextSize = 8f
        highLightColor = Color.TRANSPARENT
    }
    return set
}

fun applyBarSetParams(set: BarDataSet): BarDataSet{
    with(set) {
        color = Color.rgb(110, 120, 228)
        valueTextColor = Color.rgb(60, 78, 220)
        valueTextSize = 8f
        axisDependency = YAxis.AxisDependency.RIGHT
    }
    return set
}

fun applyLineSetParams(set: LineDataSet): LineDataSet{
    with(set) {
        setDrawHighlightIndicators(false)
        color = Color.TRANSPARENT
//        valueTextColor = Color.rgb(100, 60, 60)
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

class CandleActivity : AppCompatActivity(), AnkoLogger {

    lateinit var coin_spinner : Spinner
    lateinit var currency_spinner : Spinner
    lateinit var interval_spinner : Spinner
    lateinit var exchange_spinner : Spinner
    lateinit var chart: CombinedChart

    var coin : String? = null
    var currency : String? = null
    var exchange : String? = null
    var interval : String? = null

    val histoVals = 300

    val coins = arrayOf("BTC","XMR","ETH","ZEC","XCP","BCH","PEPECASH")
    val currencies = arrayOf("EUR","BTC","USD")
    val intervals = arrayOf("Minute","Hour","Day")
    var exchanges = arrayOf("None")

    var timestamps : List<Int>? = listOf()

    fun updateExchanges () {
        doAsync{

            val jsonObj = getJson("https://min-api.cryptocompare.com/data/top/exchanges?fsym=$coin&tsym=$currency&limit=50")

            if (jsonObj?.string("Response") == "Success"){
                exchanges = (jsonObj?.array<JsonObject>("Data")?.string("exchange")?.value as List<String>).toTypedArray()
                uiThread {
                    exchange_spinner.adapter = ArrayAdapter<String>(this@CandleActivity,android.R.layout.simple_list_item_1,exchanges)
                }
            }
        }
    }

    fun loadChart(){
        if (coin != null && currency!= null && exchange!= null && interval!= null)
            doAsync{

                val jsonObj = getJson("https://min-api.cryptocompare.com/data/histo${interval?.toLowerCase()}?fsym=$coin&tsym=$currency&e=$exchange&limit=$histoVals")
                warn(jsonObj)
                if (jsonObj?.string("Response") == "Success"){
                    val arr = (jsonObj.array<JsonObject>("Data") as JsonArray<JsonObject>)

                    val barVals = List<BarEntry>(arr.size,{i -> BarEntry(
                            i.toFloat(),
                            (arr[i]["volumeto"] as Number).toFloat())})


                    val candleVals = List<CandleEntry>(arr.size,{i -> CandleEntry(
                            i.toFloat(),
                            (arr[i]["high"] as Number).toFloat(),
                            (arr[i]["low"] as Number).toFloat(),
                            (arr[i]["open"] as Number).toFloat(),
                            (arr[i]["close"] as Number).toFloat())})

                    val lineVals = List<Entry>(arr.size,{i -> Entry(
                            i.toFloat(),
                            (arr[i]["low"] as Number).toFloat()/lowPos)})

                    timestamps = List<Int>(arr.size,{i -> (arr[i]["time"] as Int)})

                    val data = CombinedData()
                    data.setData(BarData(applyBarSetParams(BarDataSet(barVals, "Volume"))))
                    data.setData(CandleData(applyCandleSetParams(CandleDataSet(candleVals, "OHLC"))))
                    data.setData(LineData(applyLineSetParams(LineDataSet(lineVals, ""))))

                    uiThread {
                        chart.resetTracking()
                        chart.getXAxis().setValueFormatter(TimestampAxisValueFormatter(timestamps,interval))
                        chart.getAxisRight().setAxisMaximum(data.barData.getYMax()*3);
                        chart.setData(data)
                        chart.invalidate()
                    }
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_candle)


        coin_spinner = findViewById<Spinner>(R.id.spinner_coin)
        currency_spinner = findViewById<Spinner>(R.id.spinner_currency)
        interval_spinner = findViewById<Spinner>(R.id.spinner_interval)
        exchange_spinner = findViewById<Spinner>(R.id.spinner_exchange)

        chart = findViewById<CombinedChart>(R.id.chart_candle)

        chart.setBackgroundColor(Color.WHITE)
        chart.setMaxVisibleValueCount(30)
        chart.getDescription().setEnabled(false)
        chart.xAxis.setPosition(XAxis.XAxisPosition.BOTTOM)
        chart.axisLeft.gridColor = Color.RED
        chart.axisLeft.spaceBottom = 50f
        chart.axisLeft.textColor = Color.RED
        chart.axisRight.gridColor = Color.BLUE
        chart.axisRight.textColor = Color.BLUE
        chart.axisRight.spaceBottom = 0f
        chart.axisRight.spaceTop = 20f

        chart.setDrawOrder(arrayOf(DrawOrder.BAR, DrawOrder.BUBBLE, DrawOrder.CANDLE, DrawOrder.LINE, DrawOrder.SCATTER))

        coin_spinner.adapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,coins)
        currency_spinner.adapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,currencies)
        interval_spinner.adapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,intervals)
        exchange_spinner.adapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,exchanges)


        coin_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                coin = coins.get(p2)
                if (currency != null){
                    updateExchanges()
                }
            }
        }

        currency_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                currency = currencies.get(p2)
                if (coin != null){
                    updateExchanges()
                }
            }
        }

        interval_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                interval = intervals.get(p2)
                loadChart()
            }
        }

        exchange_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                exchange = exchanges.get(p2)
                loadChart()
            }
        }
    }

    fun testChart(){
        chart.resetTracking()

        val yVals1 = ArrayList<CandleEntry>()

        for (i in 0 until 1) {
            val mult = 5
            val j = (Math.random() * 40).toFloat() + mult

            val high = (Math.random() * 9).toFloat() + 8f
            val low = (Math.random() * 9).toFloat() + 8f

            val open = (Math.random() * 6).toFloat() + 1f
            val close = (Math.random() * 6).toFloat() + 1f

            val even = i % 2 == 0

            yVals1.add(CandleEntry(
                    i.toFloat(),
                    j + high,
                    j - low,
                    if (even) j + open else j - open,
                    if (even) j - close else j + close
            ))
        }

        val set1 = CandleDataSet(yVals1, "Data Set")

        applyCandleSetParams(set1)

        val data = CandleData(set1)

        val dataC = CombinedData()

        dataC.setData(data)

        chart.setData(dataC)
        chart.invalidate()
    }

}
