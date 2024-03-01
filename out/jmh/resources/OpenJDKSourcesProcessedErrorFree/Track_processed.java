/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javax.sound.midi;

import java.util.ArrayList;
import java.util.HashSet;

import com.sun.media.sound.MidiUtils;

/**
 * A MIDI track is an independent stream of MIDI events (time-stamped MIDI data)
 * that can be stored along with other tracks in a standard MIDI file. The MIDI
 * specification allows only 16 channels of MIDI data, but tracks are a way to
 * get around this limitation. A MIDI file can contain any number of tracks,
 * each containing its own stream of up to 16 channels of MIDI data.
 * <p>
 * A {@code Track} occupies a middle level in the hierarchy of data played by a
 * {@link Sequencer}: sequencers play sequences, which contain tracks, which
 * contain MIDI events. A sequencer may provide controls that mute or solo
 * individual tracks.
 * <p>
 * The timing information and resolution for a track is controlled by and stored
 * in the sequence containing the track. A given {@code Track} is considered to
 * belong to the particular {@link Sequence} that maintains its timing. For this
 * reason, a new (empty) track is created by calling the
 * {@link Sequence#createTrack} method, rather than by directly invoking a
 * {@code Track} constructor.
 * <p>
 * The {@code Track} class provides methods to edit the track by adding or
 * removing {@code MidiEvent} objects from it. These operations keep the event
 * list in the correct time order. Methods are also included to obtain the
 * track's size, in terms of either the number of events it contains or its
 * duration in ticks.
 *
 * @author Kara Kytle
 * @author Florian Bomers
 * @see Sequencer#setTrackMute
 * @see Sequencer#setTrackSolo
 */
public final class Track {


    /**
     * The list containing the events.
     */
    private final ArrayList<MidiEvent> eventsList = new ArrayList<>();

    /**
     * Use a hashset to detect duplicate events in add(MidiEvent).
     */
    private final HashSet<MidiEvent> set = new HashSet<>();

    private final MidiEvent eotEvent;

    /**
     * Package-private constructor. Constructs a new, empty Track object, which
     * initially contains one event, the meta-event End of Track.
     */
    Track() {
        MetaMessage eot = new ImmutableEndOfTrack();
        eotEvent = new MidiEvent(eot, 0);
        eventsList.add(eotEvent);
        set.add(eotEvent);
    }

    /**
     * Adds a new event to the track. However, if the event is already contained
     * in the track, it is not added again. The list of events is kept in time
     * order, meaning that this event inserted at the appropriate place in the
     * list, not necessarily at the end.
     *
     * @param  event the event to add
     * @return {@code true} if the event did not already exist in the track and
     *         was added, otherwise {@code false}
     */
    public boolean add(MidiEvent event) {
        if (event == null) {
            return false;
        }
        synchronized(eventsList) {

            if (!set.contains(event)) {
                int eventsCount = eventsList.size();

                MidiEvent lastEvent = null;
                if (eventsCount > 0) {
                    lastEvent = eventsList.get(eventsCount - 1);
                }
                if (lastEvent != eotEvent) {
                    if (lastEvent != null) {
                        eotEvent.setTick(lastEvent.getTick());
                    } else {
                        eotEvent.setTick(0);
                    }
                    eventsList.add(eotEvent);
                    set.add(eotEvent);
                    eventsCount = eventsList.size();
                }

                if (MidiUtils.isMetaEndOfTrack(event.getMessage())) {
                    if (event.getTick() > eotEvent.getTick()) {
                        eotEvent.setTick(event.getTick());
                    }
                    return true;
                }

                set.add(event);

                int i = eventsCount;
                for ( ; i > 0; i--) {
                    if (event.getTick() >= (eventsList.get(i-1)).getTick()) {
                        break;
                    }
                }
                if (i == eventsCount) {

                    eventsList.set(eventsCount - 1, event);
                    if (eotEvent.getTick() < event.getTick()) {
                        eotEvent.setTick(event.getTick());
                    }
                    eventsList.add(eotEvent);
                } else {
                    eventsList.add(i, event);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Removes the specified event from the track.
     *
     * @param  event the event to remove
     * @return {@code true} if the event existed in the track and was removed,
     *         otherwise {@code false}
     */
    public boolean remove(MidiEvent event) {


        synchronized(eventsList) {
            if (set.remove(event)) {
                int i = eventsList.indexOf(event);
                if (i >= 0) {
                    eventsList.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Obtains the event at the specified index.
     *
     * @param  index the location of the desired event in the event vector
     * @return the event at the specified index
     * @throws ArrayIndexOutOfBoundsException if the specified index is negative
     *         or not less than the current size of this track
     * @see #size
     */
    public MidiEvent get(int index) throws ArrayIndexOutOfBoundsException {
        try {
            synchronized(eventsList) {
                return eventsList.get(index);
            }
        } catch (IndexOutOfBoundsException ioobe) {
            throw new ArrayIndexOutOfBoundsException(ioobe.getMessage());
        }
    }

    /**
     * Obtains the number of events in this track.
     *
     * @return the size of the track's event vector
     */
    public int size() {
        synchronized(eventsList) {
            return eventsList.size();
        }
    }

    /**
     * Obtains the length of the track, expressed in MIDI ticks. (The duration
     * of a tick in seconds is determined by the timing resolution of the
     * {@code Sequence} containing this track, and also by the tempo of the
     * music as set by the sequencer.)
     *
     * @return the duration, in ticks
     * @see Sequence#Sequence(float, int)
     * @see Sequencer#setTempoInBPM(float)
     * @see Sequencer#getTickPosition()
     */
    public long ticks() {
        long ret = 0;
        synchronized (eventsList) {
            if (eventsList.size() > 0) {
                ret = (eventsList.get(eventsList.size() - 1)).getTick();
            }
        }
        return ret;
    }

    private static class ImmutableEndOfTrack extends MetaMessage {
        private ImmutableEndOfTrack() {
            super(new byte[3]);
            data[0] = (byte) META;
            data[1] = MidiUtils.META_END_OF_TRACK_TYPE;
            data[2] = 0;
        }

        @Override
        public void setMessage(int type, byte[] data, int length) throws InvalidMidiDataException {
            throw new InvalidMidiDataException("cannot modify end of track message");
        }
    }
}
