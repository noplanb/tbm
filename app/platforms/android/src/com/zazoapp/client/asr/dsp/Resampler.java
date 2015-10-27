package com.zazoapp.client.asr.dsp;

/**
 * Copyright (C) 2011 Jacquet Wong
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Resample signal data (base on bytes)
 *
 * @author jacquet
 *
 */
public class Resampler {

    public Resampler() {
    }

    /**
     * Do resampling. Currently the amplitude is stored by short such that maximum bitsPerSample is 16 (bytePerSample is 2)
     *
     * @param sourceData    The source data in bytes
     * @param bitsPerSample How many bits represents one sample (currently supports max. bitsPerSample=16)
     * @param sourceRate    Sample rate of the source data
     * @param targetRate    Sample rate of the target data
     * @param sourceChannels
     * @param targetChannels
     * @return re-sampled data
     */
    public byte[] reSample(byte[] sourceData, int bitsPerSample, int sourceRate, int targetRate, int sourceChannels, int targetChannels) {

        // make the bytes to amplitudes first
        int bytePerSample = bitsPerSample / 8;
        int numSamples = sourceData.length / bytePerSample;
        short[] amplitudes = new short[numSamples];     // 16 bit, use a short to store

        int pointer = 0;
        for (int i = 0; i < numSamples; i++) {
            short amplitude = 0;
            for (int byteNumber = 0; byteNumber < bytePerSample; byteNumber++) {
                // little endian
                amplitude |= (short) ((sourceData[pointer++] & 0xFF) << (byteNumber * 8));
            }
            amplitudes[i] = amplitude;
        }
        // end make the amplitudes
        // convert to mono if necessary
        if (sourceChannels == 2 && targetChannels == 1) {
            short[] mono = new short[amplitudes.length / 2];
            for (int i = 0; i < mono.length; i++) {
                mono[i] = (short) ((amplitudes[i * 2] + amplitudes[i * 2 + 1]) / 2);
            }
            amplitudes = mono;
        }
        // do interpolation
        LinearInterpolation reSample = new LinearInterpolation();
        short[] targetSample = reSample.interpolate(sourceRate, targetRate, amplitudes);
        int targetLength = targetSample.length;
        // end do interpolation

        // TODO: Remove the high frequency signals with a digital filter, leaving a signal containing only half-sample-rated frequency information, but still sampled at a rate of target sample rate. Usually FIR is used

        // end resample the amplitudes

        // convert the amplitude to bytes
        byte[] bytes;
        if (bytePerSample == 1) {
            bytes = new byte[targetLength];
            for (int i = 0; i < targetLength; i++) {
                bytes[i] = (byte) targetSample[i];
            }
        } else {
            // suppose bytePerSample==2
            bytes = new byte[targetLength * 2];
            for (int i = 0; i < targetSample.length; i++) {
                // little endian
                bytes[i * 2] = (byte) (targetSample[i] & 0xff);
                bytes[i * 2 + 1] = (byte) ((targetSample[i] >> 8) & 0xff);
            }
        }
        // end convert the amplitude to bytes

        return bytes;
    }

    public byte[] stereo2mono(byte[] sourceData, int bitsPerSample) {
        byte[] mono = new byte[sourceData.length / 2];
        int bytePerSample = bitsPerSample / 8;
        if (bytePerSample == 1) {
            for (int i = 0; i < mono.length - 2; i++) {
                mono[i] = (byte) ((sourceData[i * 2] + sourceData[i * 2 + 2]) / 2);
                mono[i + 1] = (byte) ((sourceData[i * 2 + 1] + sourceData[i * 2 + 3]) / 2);
            }
        } else {
            for (int i = 0; i < mono.length - 2; i += 2) {
                mono[i] = (byte) ((sourceData[i * 2] + sourceData[i * 2 + 2]) / 2);
                mono[i + 1] = (byte) ((sourceData[i * 2 + 1] + sourceData[i * 2 + 3]) / 2);
            }
        }

        return mono;
    }
}

