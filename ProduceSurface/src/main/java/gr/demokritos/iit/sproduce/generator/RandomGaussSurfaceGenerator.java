/** 
* Copyright 2018 Antonia Tsili NCSR Demokritos
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package gr.demokritos.iit.sproduce.generator;

import java.util.*;
import java.io.*;
import java.lang.Math;
import java.math.BigDecimal;
import java.math.RoundingMode;

import edu.princeton.cs.algs4.Complex;

import gr.demokritos.iit.sproduce.utils.FastFourier;
import gr.demokritos.iit.sproduce.utils.Linspace;


/**
 * <p>This class generates a square 2-dimensional random rough surface f(x,y) with NxN
 * surface points. The surface has a Gaussian height distribution and
 * exponential autocovariance functions (in both x and y), where rL is the
 * length of the surface side, h is the RMS height and clx and cly are the
 * correlation lengths in x and y. Omitting cly makes the surface isotropic.</p>
 *
 */
public class RandomGaussSurfaceGenerator {

    /**
     * Number of surface points (along square side)
     */
    Integer N;
    /**
     * Length of surface (along square side)
     */
    double rL;
    /**
     * RMS height
     */
    double H;
    /**
     * Correlation length x axis
     */
    double clx;
    /**
     * Correlation length y axis
     */
    double cly;

    /**
     * Heights in real numbers
     */
    protected double[][] RandomRoughSurf;
    /**
     * All rows of same column with the same point (needed for transformation)
     */
    protected double[][] meshGridX;
    /**
     * All columns of same row with the same point (needed for transformation)
     */
    protected double[][] meshGridY;
    /**
     * Height results
     */
    public double[][] Surf;

    /**
     * <p>For a non-isotropic surface</p>
     *
     * @param args      Passed from input
     * @param cly       Correlation length in y
     * @throws ImError  If Fourier transformation did not succeed
     */
    RandomGaussSurfaceGenerator(double[] args, double cly) throws ImError{
        this.N   = (int)args[0];
        this.rL  = args[1];
        this.H   = args[2];
        this.clx = args[3];
        this.cly = cly;

        meshGrid();		  // init members meshGridX, meshGridY
        RandomSurfaceH(); // init member RandomRoughSurf
        double[][] GF = GaussianFilter(cly);

        /*
         * correlation of surface including convolution (faltung), inverse
         * Fourier transform and normalizing prefactors
         */

        FastFourier fft2 = new FastFourier(N,N); // NxM matrix Fourier Transform

        // implementing ifft2(fft2(GF).*fft2(RRS)
        Complex[][] GF_cox = fft2.double2Complex(GF);
        Complex[][] GF_Fourier = fft2.FTransform(GF_cox);

        Complex[][] RRS_cox = fft2.double2Complex(RandomRoughSurf);
        Complex[][] RRS_Fourier = fft2.FTransform(RRS_cox);

        Complex[][] MultOut = new Complex[N][N];
        fft2.ComplexArray_mult(GF_Fourier,RRS_Fourier,MultOut);
        Complex[][] Res = fft2.iFTransform(MultOut);
        // end

        Surf = new double[N][N];

        for (int i=0 ; i<N ; i++) {
            for (int j=0 ; j<N ; j++) {
                if( round(Res[i][j].im(),10)!=0 ) // must be real number after rounding
                    throw new ImError();
                Surf[i][j] = 2 * rL / N / Math.sqrt(clx * cly) * Res[i][j].re();
            }
        }

    }


    /**
     * <p>For an isotropic surface</p>
     *
     * @param args      Passed from input
     * @throws ImError  If Fourier transformation did not succeed
     */
    RandomGaussSurfaceGenerator(double[] args) throws ImError{
        this.N   = (int)args[0];
        this.rL  = args[1];
        this.H   = args[2];
        this.clx = args[3];
        this.cly = 0.0;

        meshGrid();       // init members meshGridX, meshGridY
        RandomSurfaceH(); // init member RandomRoughSurf
        double[][] GF = GaussianFilter();

        /*
         * correlation of surface including convolution (faltung), inverse
         * Fourier transform and normalizing prefactors
         */

        FastFourier fft2 = new FastFourier(N,N); // NxM matrix Fourier Transform

        // implementing ifft2(fft2(GF).*fft2(RRS)
        Complex[][] GF_cox = fft2.double2Complex(GF);
        Complex[][] GF_Fourier = fft2.FTransform(GF_cox);

        Complex[][] RRS_cox = fft2.double2Complex(RandomRoughSurf);
        Complex[][] RRS_Fourier = fft2.FTransform(RRS_cox);

        Complex[][] MultOut = new Complex[N][N];
        fft2.ComplexArray_mult(GF_Fourier,RRS_Fourier,MultOut);
        Complex[][] Res = fft2.iFTransform(MultOut);
        // end

        Surf = new double[N][N];

        for (int i=0 ; i<N ; i++) {
            for (int j=0 ; j<N ; j++) {
                if( round(Res[i][j].im(),10)!=0 ) // must be real number after rounding
                    throw new ImError();
                Surf[i][j] = 2 * rL / N / clx * Res[i][j].re();
            }
        }

    }

    /**
     * <p>Function to create matrices X,Y of absolute values where
     * <ul>
     * <li>X: same vector in each row</li>
     * <li>Y: same vector in each column</li>
     * </ul>
     * <br>of evenly spaced points between -rL/2 and rL/2</p>
     */
    protected void meshGrid() {
        double begin = -rL/2;
        double end = rL/2;

        meshGridX = new double[N][N];
        meshGridY = new double[N][N];

        Linspace linspace = new Linspace(begin,end,N);
        double[] L = linspace.op();

        for (int j=0 ; j<N ; j++) {
            for (int i=0 ; i<N ; i++) {
                meshGridX[i][j] = Math.abs(L[j]);
            }
        }
        for (int i=0 ; i<N ; i++) {
            for (int j=0 ; j<N ; j++) {
                meshGridY[i][j] = Math.abs(L[i]);
            }
        }

    }

    /**
     *
     * <p>Create matrix NxN of random normal distributed values
     * multiplied by h (rms height)</p>
     *
     */
    protected void RandomSurfaceH() {
        RandomRoughSurf = new double[N][N];
        Random rand = new Random();
        for (int i=0 ; i<N ; i++) {
            for (int j=0 ; j<N ; j++) {
                RandomRoughSurf[i][j] = H*rand.nextGaussian(); //standard normal distribution
            }
        }

    }

    /**
     * <p>Compute the Gaussian filter
     * of non-isotropic</p>
     *
     * <p>F is size N*N</p>
     */
    protected double[][] GaussianFilter(double arg) {
        double[][] F = new double[N][N];
        for (int i=0 ; i<N ; i++) {
            for (int j=0 ; j<N ; j++) {
                F[i][j] = Math.exp( -( meshGridX[i][j]/(clx/2) + meshGridY[i][j]/(cly/2)) );
            }
        }
        return F;

    }

    /**
     * <p>Compute the Gaussian filter
     * of isotropic</p>
     *
     * <p>F is size N*N</p>
     */
    protected double[][] GaussianFilter() {
        double[][] F = new double[N][N];
        for (int i=0 ; i<N ; i++) {
            for (int j=0 ; j<N ; j++) {
                F[i][j] = Math.exp( -( (meshGridX[i][j] + meshGridY[i][j])/(clx/2) ) );
            }
        }
        return F;

    }

    /**
     * <p>Print results to file</p>
     *
     * @param writer        Writer class
     * @param X             Array of height values
     * @throws IOException
     * @see  java.io.FileWriter
     */
    void printArray(FileWriter writer, double[][] X) throws IOException { // one surface per line, height per column
        StringBuilder sb = new StringBuilder();
        sb.append("rms:").append(String.valueOf((this.H))); // printing parameters in first column as: <param_name>:<param_value>
        sb.append(":clx:").append(String.valueOf(this.clx));
        sb.append(":cly:").append(String.valueOf(this.cly));
        sb.append(":N:").append(String.valueOf(this.N));
        sb.append(',');
        for (int i=0 ; i<N ; i++) {
            for (int j=0 ; j<N ; j++) {
                if( i==0 && j==0 ) {
                    sb.append(String.valueOf(X[0][0]));
                } else {
                    sb.append(',').append(String.valueOf(X[i][j]));
                }
            }
        }
        sb.append("\n");
        writer.append(sb.toString());
        writer.close();
    }

    /**
     * <p>Print results to standard output</p>
     *
     * @param X             Array of height values
     * @throws IOException
     * @see                 java.io.FileWriter
     */
    void printArray(double[][] X) throws IOException { // one line of surface per line of output
        System.out.println("rms:"+this.H+" clx:"+this.clx+" cly:"+this.cly+" N:"+this.N);
        for (int i=0 ; i<N ; i++) {
            for (int j=0 ; j<N ; j++) {
                if( j==0 ) {
                    System.out.print(X[i][0]);
                } else {
                    System.out.print(","+X[i][j]);
                }
            }
            System.out.println();
        }
        System.out.println();
    }


    /**
     * <p>Round double value to n decimals</p>
     *
     */
    protected static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}