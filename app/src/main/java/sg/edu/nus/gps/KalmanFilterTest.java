package sg.edu.nus.gps;

import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class KalmanFilterTest {
    //A - state transition matrix
    private RealMatrix A;
    //B - control input matrix
    private RealMatrix B;
    //H - measurement matrix
    private RealMatrix H;
    //Q - process noise covariance matrix
    private RealMatrix Q;
    //R - measurement noise covariance matrix
    private RealMatrix R;
    //x state
    private RealVector x;
    //P - error covariance
    private RealMatrix P0;

    //thread set for every second
    private static double dt = 1d;
    double measurementNoise;
    double processNoise;

    // control vector: accelerometer at time t
    private RealVector u;
    private KalmanFilter filter;

    public KalmanFilterTest(double mNoise, double pNoise){
        measurementNoise = mNoise;
        processNoise = pNoise;
        //2x2 State Transition matrix: This defines the next state given the current state
        A = new Array2DRowRealMatrix(new double[][] {
                { 1d, dt},
                { 0d, 1d }
        });
        //2x1 control matrix: This is what to multiply acceleration by
        B = new Array2DRowRealMatrix(new double[][] {
                { Math.pow(dt, 2d) / 2d },
                { dt }
        });
        //2x2 identity matrix for the experiment
        H = new Array2DRowRealMatrix(new double[][] {
                { 1d, 1d },
                { 1d, 1d }
        });
        //process covariance matrix
        Q = new Array2DRowRealMatrix(new double[][] {
                { processNoise, 0d },
                { 0d, processNoise }
        });
        //measurement covariance matrix
        R = new Array2DRowRealMatrix(new double[][] {
                { measurementNoise, 0d },
                { 0d, measurementNoise }
        });

        //1x1 vector which will be updated every second with new acceleration value.
        u = new ArrayRealVector(new double[] { 0 });

        //2x1 state vector that will be overridden at every step. can be initialized to 0
        x = new ArrayRealVector(new double[] { 0, 0 });

        P0 = new Array2DRowRealMatrix(new double[][] { { 0.001, 0.001 }, { 0.001, 0.001 } });

        //create the filter here
        ProcessModel pm = new DefaultProcessModel(A, B, Q, x, P0);
        MeasurementModel mm = new DefaultMeasurementModel(H, R);
        filter = new KalmanFilter(pm, mm);

    }

    public double[] getEstimate(double currentAcceleration, double currentPosition, double currentVelocity){
        u.setEntry(0,currentAcceleration);
        //prediction from Kalman Filter package
        filter.predict(u);

        x.setEntry(0,currentPosition);
        x.setEntry(1, currentVelocity);

        // x = A * x + B * u
        x = A.operate(x).add(B.operate(u));

        // z = H * x
        RealVector z = H.operate(x);

        //get the correct values
        filter.correct(z);

        double[] stateEstimate= filter.getStateEstimation();
        return stateEstimate;
    }


}
