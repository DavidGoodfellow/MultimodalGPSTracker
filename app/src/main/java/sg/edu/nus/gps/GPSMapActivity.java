package sg.edu.nus.gps;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GPSMapActivity extends AppCompatActivity {

    PointsGraphSeries<DataPoint> series;
    List<DataPoint> xy;
    Number[] x,y;
    TextView mean, variance;
    XYSeries series1;
    XYPlot plot;
    double meanX, meanY, avgVarX, avgVarY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_map);

        Bundle extras = getIntent().getExtras();
        List<DataPoint> items =  (ArrayList<DataPoint>) extras.getSerializable("vector");

        double varianceSumX = 0.0, varianceSumY = 0.0, sumX=0.0, sumY=0.0;

        plot = (XYPlot) findViewById(R.id.gps_graphXY);

        xy = new ArrayList<>();
        x = new Number[items.size()];
        y = new Number[items.size()];

        //summing values together for mean
        for(int i = 0; i < items.size(); i++){
            xy.add(new DataPoint(items.get(i).getX(),items.get(i).getY()));
            sumX += items.get(i).getX();
            sumY += items.get(i).getY();
        }
        System.out.println("XY Size: " + xy.size());

        //finding the mean x and y
        meanX = sumX/items.size();
        meanY = sumY/items.size();

        //variance for each
        for(int i =0; i < xy.size(); i++){
            varianceSumX += ((xy.get(i).getX()-meanX)*(xy.get(i).getX()-meanX));
            varianceSumY += ((xy.get(i).getY()-meanY)*(xy.get(i).getY()-meanY));
        }
        avgVarX = varianceSumX/xy.size();
        avgVarY = varianceSumY/xy.size();

        //display variance and mean
        mean = (TextView) findViewById(R.id.mean_text);
        variance = (TextView) findViewById(R.id.variance_text);

        mean.setText("Mean: (" + (int) meanX +"m, " + (int) meanY + "m)");
        variance.setText("Variance: (" + avgVarX +"m, " + avgVarY + "m)");

        //points series
        int counter = 0;
        while(xy.size() != 0){
            int minX = 0;//index
            for(int i=1; i < xy.size(); i++){
                if(xy.get(minX).getX() > xy.get(i).getX()){
                    minX = i;
                }
            }

            x[counter] = xy.get(minX).getX();
            y[counter] = xy.get(minX).getY();

            System.out.println("XY Size: " + xy.size());
            xy.remove(minX);
            counter++;
        }

        /*Number[] series2Numbers = {0};
        XYSeries series2 = new SimpleXYSeries(Arrays.asList(series2Numbers),Arrays.asList(series2Numbers),  "Center");*/

        series1 = new SimpleXYSeries(Arrays.asList(x), Arrays.asList(y), "GPS Readings");
        LineAndPointFormatter formatter = new LineAndPointFormatter(null, Color.CYAN, null, null);
        formatter.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(20));

        Number[] mean_X = {meanX};
        Number[] mean_Y = {meanY};
        XYSeries series_mean = new SimpleXYSeries(Arrays.asList(mean_X), Arrays.asList(mean_Y), "Mean");
        LineAndPointFormatter formatter2 = new LineAndPointFormatter(null, Color.MAGENTA, null, null);
        formatter2.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(10));

        plot.addSeries(series1, formatter);
        plot.addSeries(series_mean, formatter2);

    }


    public void goToMainActivity(View view) {
        finish();
    }
}
