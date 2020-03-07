package cryptah.trabladorr.cryptah;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by trabladorr on 10/26/17.
 */


public class TimestampAxisValueFormatter implements IAxisValueFormatter
{
    public enum Formatting {
            Minute ("HH:mm"),
            Hour   ("dd HH:00"),
            Day   ("dd MMM");

        private final String format;
            Formatting(String format) {
            this.format = format;
        }
    }

    private List<Integer> timestamps = null;
    private DateFormat mDataFormat;
    private Date mDate;

    public TimestampAxisValueFormatter(List<Integer> timestamps, String interval) {
        this.timestamps = timestamps;
        this.mDataFormat = new SimpleDateFormat(Formatting.valueOf(interval).format, Locale.ENGLISH);
        this.mDate = new Date();
    }

    /**
     * Called when a value from an axis is to be formatted
     * before being drawn. For performance reasons, avoid excessive calculations
     * and memory allocations inside this method.
     *
     * @param value the value to be formatted
     * @param axis  the axis the value belongs to
     * @return
     */
    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        long timestamp = timestamps.get((int) value);

        try{
            mDate.setTime(timestamp*1000);
            return mDataFormat.format(mDate);
        }
        catch(Exception ex){
            return "xx";
        }
    }

}