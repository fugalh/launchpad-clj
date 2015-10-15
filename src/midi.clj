;; There's overtone.midi but I had trouble with it not finding devices on
;; non-OSX platforms. So I decided to build this from Java first-principles
;; while debugging (I thought it was a Java thing generally, but apparently
;; it's an overtone.midi bug)
;;
;; Also, I didn't like the inconsistent naming (midi- prefix or not?). I
;; like namespaces to do the work of namespacing, I'm kind of weird that way.
;;
;; On OSX, Java permanently caches the results of the midi scan the first
;; time MidiSystem/getMidiDeviceInfo is called, so there is no way for us to
;; get an updated list of MIDI devices after the JVM starts. Which means
;; you have to restart the program/repl whenever you change MIDI
;; configuration. :-/

(ns midi
  (:import (javax.sound.midi MidiSystem
                             ShortMessage
                             SysexMessage
                             MidiUnavailableException)))

(defn note-on
  "Make a note-on midi message, suitable to send to a receiver."
  [note vel]
  (ShortMessage. ShortMessage/NOTE_ON note vel))

(defn note-off
  "Make a note-off midi message, suitable to send to a receiver.
   (when you're feeling too pure to just do (note-on note 0))"
  [note]
  (ShortMessage. ShortMessage/NOTE_OFF note 0))

(defn control-change
  "Make a control-change midi message, suitable to send to a receiver."
  [controller value]
  (ShortMessage. ShortMessage/CONTROL_CHANGE controller value))

;; TODO look at https://github.com/locurasoft/osxmidi4j which may also fix the
;; rescan problem? but maybe not a great lib:
;; https://groups.google.com/forum/#!topic/overtone/Q0hLAoOfjEc
;; might have to implement my own java/coremidi bridge. :-P
;; http://docs.oracle.com/javase/tutorial/sound/SPI-intro.html
(defn sysex
  "Make a sysex message (we add status 0xf0 and the terminating 0xf7).
  Looks like this is another thing broken on OSX. :/"
  [bytes]
  (SysexMessage. 0xf0 (byte-array bytes) (count bytes)))

(defn find-infos
  "Return the device info for devices whose names match the regex."
  [pattern]
  (let [infos (MidiSystem/getMidiDeviceInfo)]
    (filter #(or (re-find pattern (.getName %))
                 (re-find pattern (.getDescription %)))
            infos)))
  
(defn get-devices
  [infos]
  (map #(MidiSystem/getMidiDevice %) infos))

(defn find-devices
  [pattern]
  (get-devices (find-infos pattern)))

(defn get-receiver
  "Return a new opened receiver for the first device whose name matches the
  given regex. The device will also be opened, and they should both be closed
  when you're done with them. (But if you only open one receiver and don't
  close it and its device, I won't tell.)"
  [re]
  (let [devices (find-devices re)
        devices (filter #(not (= 0 (.getMaxReceivers %))) devices)
        device (first devices)]
    (when-not device
      (throw (MidiUnavailableException.
              (str "No MIDI receiver matching /" re "/"))))
    (when-not (.isOpen device)
      (.open device))
    (.getReceiver device)))

(defn receives? [dev]
  (not (zero? (.getMaxReceivers dev))))

(defn transmits? [dev]
  (not (zero? (.getMaxTransmitters dev))))

(defn open-device-duplex
  "Return a pair of [^Transmitter midi-in, ^Receiver midi-out] devices where the name or
  description matches the given regular expression pattern."
  [pattern]
  (let [devices (find-devices (re-pattern pattern))
        ins (filter #(not= 0 (.getMaxTransmitters %)) devices)
        outs (filter #(not= 0 (.getMaxReceivers %)) devices)
        in (last ins)
        out (last outs)]
    (when (and in out)
      (when-not (.isOpen in) (.open in))
      (when-not (.isOpen out) (.open out))
      [(.getTransmitter in) (.getReceiver out)])))
