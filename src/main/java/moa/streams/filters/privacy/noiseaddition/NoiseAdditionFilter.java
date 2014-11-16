/*
 *    AddNoiseFilter.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.streams.filters.privacy.noiseaddition;

import java.util.Random;

import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.GaussianEstimator;
import moa.core.InstancesHeader;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.streams.filters.AbstractStreamFilter;
import weka.core.Instance;

/**
 * Filter for adding random noise to examples in a stream.
 * Noise can be added to attribute values or to class labels.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class NoiseAdditionFilter extends AbstractStreamFilter {

    @Override
    public String getPurposeString() {
        return "Protects the stream adding correlated noise in a stream.";
    }

    private static final long serialVersionUID = 1L;

    public IntOption randomSeedOption = new IntOption("randomSeed", 'r',
            "Seed for random noise.", 1);

    public FloatOption SigmaOption = new FloatOption("Sigma",
            's', "The sigma value for the noise.", 0.1, 0.0, 1.0);

    protected Random random;

    protected AutoExpandVector<Object> attValObservers;

    @Override
    protected void restartImpl() {
        this.random = new Random(this.randomSeedOption.getValue());
        this.attValObservers = new AutoExpandVector<Object>();
    }

    @Override
    public InstancesHeader getHeader() {
        return this.inputStream.getHeader();
    }

    @Override
    public Instance nextInstance() {
        Instance inst = (Instance) this.inputStream.nextInstance().copy();
        for (int i = 0; i < inst.numAttributes()-1; i++) {
        	GaussianEstimator obs = (GaussianEstimator) this.attValObservers.get(i);
            if (obs == null) {
            	obs = new GaussianEstimator();
                this.attValObservers.set(i, obs);
            }
            obs.addObservation(inst.value(i), inst.weight());
           // System.out.println(inst.value(i) + " " + this.random.nextGaussian()  + " " + obs.getStdDev()  + " " +  this.SigmaOption.getValue() + " pos:" + i + " valor:" + (inst.value(i) + this.random.nextGaussian() * obs.getStdDev() * this.SigmaOption.getValue()));
            inst.setValue(i, inst.value(i) + this.random.nextGaussian() * obs.getStdDev() * this.SigmaOption.getValue());
        }
       // System.out.println("Despres: " + inst.toString());
        return inst;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
