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


public class TimestampAxisValueFormatter implements IAxisValueFormatter {

    public enum Formatting {
            Minute ("HH:mm"),
            Hour   ("EEE:HH'h'"),
            Day   ("MMM dd");

        private final String format;
            Formatting(String format) {
            this.format = format;
        }
    }

    private final List<Integer> timestamps;
    private final DateFormat mDataFormat;
    private final Date mDate;

    public TimestampAxisValueFormatter(List<Integer> timestamps, String interval) {
        this.timestamps = timestamps;
        this.mDataFormat = new SimpleDateFormat(Formatting.valueOf(interval).format, Locale.ENGLISH);
        this.mDate = new Date();
    }

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