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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AccGraphActivity extends AppCompatActivity {

    PointsGraphSeries<DataPoint> series_gps, series_fixed;
    TextView avg_acc_error, avg_mean_error;
    XYPlot graph1;
    List<DataPoint> gps1, fixed, xy;
    Double x,y,x1,y1;
    double meanX, meanY, varianceSumX, varianceSumY, sumX, sumY;
    double avgVarX, avgVarY;

    Thread t1;

    Number[] x_gps;
    Number[] y_gps;
    Number[] x_fixed;
    Number[] y_fixed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acc_graph);

        graph1 = (XYPlot) findViewById(R.id.acc_graphXY);
        //graph1 = (GraphView) findViewById(R.id.acc_graph1);

        avg_acc_error = (TextView) findViewById(R.id.acc_variance_text1);
        avg_acc_error.setText("Fixed Variance: ");
        avg_mean_error = (TextView) findViewById(R.id.acc_mean_text1);
        avg_mean_error.setText("Avg Mean Variance: ");

        //get bundle extras
        Bundle extras = getIntent().getExtras();
        gps1 =  (ArrayList<DataPoint>) extras.getSerializable("gps");
        fixed =  (ArrayList<DataPoint>) extras.getSerializable("fixed");

        x_gps = new Number[gps1.size()];
        y_gps = new Number[gps1.size()];
        x_fixed = new Number[fixed.size()];
        y_fixed = new Number[fixed.size()];

        //calculate error
        varianceSumX = 0.0;
        varianceSumY = 0.0;
        sumX = 0.0;
        sumY = 0.0;
        xy = new ArrayList<>();

        for(int i = 1; i < gps1.size(); i++){
            x = gps1.get(i).getX() - gps1.get(i-1).getX();
            y = gps1.get(i).getY() - gps1.get(i-1).getY();
            x1 = fixed.get(i).getX() - fixed.get(i-1).getX();
            y1 = fixed.get(i).getY() - fixed.get(i-1).getY();

            xy.add(new DataPoint(x1,y1));
            sumX += x-x1;
            sumY += y-y1;
        }

        //finding the mean x and y
        meanX = sumX/xy.size();
        meanY = sumY/xy.size();

        //variance for each
        for(int i =0; i < xy.size(); i++){
            varianceSumX += ((xy.get(i).getX()-meanX)*(xy.get(i).getX()-meanX));
            varianceSumY += ((xy.get(i).getY()-meanY)*(xy.get(i).getY()-meanY));
        }
        avgVarX = varianceSumX/xy.size();
        avgVarY = varianceSumY/xy.size();

        avg_mean_error.setText("Mean: (" + Double.parseDouble(new DecimalFormat("##.###").format(meanX)) +"m/s^2, " +
                Double.parseDouble(new DecimalFormat("##.###").format(meanY)) + "m/s^2)");
        avg_acc_error.setText("Variance: (" + Double.parseDouble(new DecimalFormat("##.#####").format(avgVarX)) +"m/s^2, " +
                Double.parseDouble(new DecimalFormat("##.###").format(avgVarY)) + "m/s^2)");

        t1 = new Thread(new Runnable() {

            @Override
            public void run() {
                try{
                    //set up gps series
                    int counter_gps = 0;
                    while(gps1.size() != 0){
                        x_gps[counter_gps] = gps1.get(0).getX();
                        y_gps[counter_gps] = gps1.get(0).getY();

                        gps1.remove(0);
                        counter_gps++;
                    }
                    //set up acceleration displacement series
                    int counter_fixed = 0;
                    while(fixed.size() != 0){
                        y_fixed[counter_fixed] = fixed.get(0).getY();
                        x_fixed[counter_fixed] = fixed.get(0).getX();

                        fixed.remove(0);
                        counter_fixed++;
                    }

                    //creating the series
                    Number[] series2Numbers = {0};
                    XYSeries series_gps = new SimpleXYSeries(Arrays.asList(x_gps), Arrays.asList(y_gps), "GPS");
                    XYSeries series_fixed = new SimpleXYSeries(Arrays.asList(x_fixed), Arrays.asList(y_fixed), "Acceleration");
                    LineAndPointFormatter formatter = new LineAndPointFormatter(Color.GREEN, Color.CYAN, null, null);
                    formatter.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(4));
                    LineAndPointFormatter formatter2 = new LineAndPointFormatter(Color.RED, Color.YELLOW, null, null);
                    formatter2.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(4));
                    LineAndPointFormatter formatter3 = new LineAndPointFormatter(null, Color.MAGENTA, null, null);
                    formatter2.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(4));

                    graph1.addSeries(series_gps, formatter);
                    graph1.addSeries(series_fixed, formatter2);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t1.start();
    }

    public void goToMainActivity(View view){
        finish();
    }
}
