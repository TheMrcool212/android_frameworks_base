/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.media.audiopolicy;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.audiopolicy.AudioMixingRule.AttributeMatchCriterion;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;

/**
 * @hide
 * Internal storage class for AudioPolicy configuration.
 */
public class AudioPolicyConfig implements Parcelable {

    private static final String TAG = "AudioPolicyConfig";

    ArrayList<AudioMix> mMixes;

    AudioPolicyConfig(ArrayList<AudioMix> mixes) {
        mMixes = mixes;
    }

    /**
     * Add an {@link AudioMix} to be part of the audio policy being built.
     * @param mix a non-null {@link AudioMix} to be part of the audio policy.
     * @return the same Builder instance.
     * @throws IllegalArgumentException
     */
    public void addMix(AudioMix mix) throws IllegalArgumentException {
        if (mix == null) {
            throw new IllegalArgumentException("Illegal null AudioMix argument");
        }
        mMixes.add(mix);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mMixes.size());
        for (AudioMix mix : mMixes) {
            // write mix route flags
            dest.writeInt(mix.getRouteFlags());
            // write mix format
            dest.writeInt(mix.getFormat().getSampleRate());
            dest.writeInt(mix.getFormat().getEncoding());
            dest.writeInt(mix.getFormat().getChannelMask());
            // write mix rules
            final ArrayList<AttributeMatchCriterion> criteria = mix.getRule().getCriteria();
            dest.writeInt(criteria.size());
            for (AttributeMatchCriterion criterion : criteria) {
                dest.writeInt(criterion.mRule);
                dest.writeInt(criterion.mAttr.getUsage());
            }
        }
    }

    private AudioPolicyConfig(Parcel in) {
        mMixes = new ArrayList<AudioMix>();
        int nbMixes = in.readInt();
        for (int i = 0 ; i < nbMixes ; i++) {
            final AudioMix.Builder mixBuilder = new AudioMix.Builder();
            // read mix route flags
            int routeFlags = in.readInt();
            mixBuilder.setRouteFlags(routeFlags);
            // read mix format
            int sampleRate = in.readInt();
            int encoding = in.readInt();
            int channelMask = in.readInt();
            final AudioFormat format = new AudioFormat.Builder().setSampleRate(sampleRate)
                    .setChannelMask(channelMask).setEncoding(encoding).build();
            mixBuilder.setFormat(format);
            // read mix rules
            int nbRules = in.readInt();
            AudioMixingRule.Builder ruleBuilder = new AudioMixingRule.Builder();
            for (int j = 0 ; j < nbRules ; j++) {
                // read the matching rules
                int matchRule = in.readInt();
                if ((matchRule == AudioMixingRule.RULE_EXCLUDE_ATTRIBUTE_USAGE)
                    || (matchRule == AudioMixingRule.RULE_MATCH_ATTRIBUTE_USAGE)) {
                    int usage = in.readInt();
                    final AudioAttributes attr = new AudioAttributes.Builder()
                            .setUsage(usage).build();
                    ruleBuilder.addRule(attr, matchRule);
                } else {
                    Log.w(TAG, "Encountered unsupported rule, skipping");
                    in.readInt();
                }
            }
            mixBuilder.setMixingRule(ruleBuilder.build());
            mMixes.add(mixBuilder.build());
        }
    }

    /** @hide */
    public static final Parcelable.Creator<AudioPolicyConfig> CREATOR
            = new Parcelable.Creator<AudioPolicyConfig>() {
        /**
         * Rebuilds an AudioPolicyConfig previously stored with writeToParcel().
         * @param p Parcel object to read the AudioPolicyConfig from
         * @return a new AudioPolicyConfig created from the data in the parcel
         */
        public AudioPolicyConfig createFromParcel(Parcel p) {
            return new AudioPolicyConfig(p);
        }
        public AudioPolicyConfig[] newArray(int size) {
            return new AudioPolicyConfig[size];
        }
    };

    /** @hide */
    @Override
    public String toString () {
        String textDump = new String("android.media.audiopolicy.AudioPolicyConfig:\n");
        textDump += mMixes.size() + " AudioMix:\n";
        for(AudioMix mix : mMixes) {
            // write mix route flags
            textDump += "* route flags=0x" + Integer.toHexString(mix.getRouteFlags()) + "\n";
            // write mix format
            textDump += "  rate=" + mix.getFormat().getSampleRate() + "Hz\n";
            textDump += "  encoding=" + mix.getFormat().getEncoding() + "\n";
            textDump += "  channels=0x";
            textDump += Integer.toHexString(mix.getFormat().getChannelMask()).toUpperCase() +"\n";
            // write mix rules
            final ArrayList<AttributeMatchCriterion> criteria = mix.getRule().getCriteria();
            for (AttributeMatchCriterion criterion : criteria) {
                switch(criterion.mRule) {
                    case AudioMixingRule.RULE_EXCLUDE_ATTRIBUTE_USAGE:
                        textDump += "  exclude usage ";
                        textDump += criterion.mAttr.usageToString();
                        break;
                    case AudioMixingRule.RULE_MATCH_ATTRIBUTE_USAGE:
                        textDump += "  match usage ";
                        textDump += criterion.mAttr.usageToString();
                        break;
                    default:
                        textDump += "invalid rule!";
                }
                textDump += "\n";
            }
        }
        return textDump;
    }
}
