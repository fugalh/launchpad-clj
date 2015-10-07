(ns midi
  (:import (javax.sound.midi MidiSystem
                             ShortMessage))
  (:refer-clojure :exclude [send]))

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

(defn find-devices
  "Return the devices whose names match the regex.
   They may already be open, but this code doesn't open them."
  [re]
  (let [infos (MidiSystem/getMidiDeviceInfo)]
    (map #(MidiSystem/getMidiDevice %)
         (filter #(re-find (re-pattern re) (.getName %))
                 infos))))

(defn get-receiver
  "Return a new opened receiver for the first device whose name matches the
  given regex. The device will also be opened, and they should both be closed
  when you're done with them. (But if you only open one receiver and don't
  close it and its device, I won't tell.)"
  [re]
  (let [devices (find-devices re)
        devices (filter #(not (= 0 (.getMaxReceivers %))) devices)
        device (first devices)]
    (do
      (when (not (.isOpen device))
        (.open device))
      (.getReceiver device))))

(defn send
  "Send a message to a receiver"
  [rx msg] (.send rx msg -1))


;; TODO
;; Looks like this works great in linux! test in windows
;; Linux even sees the new device when you plug in. sweet. guess that's an osx bug
;; Has a different name in linux, but "Launchpad" is still in the description. so probably need to do the "name or description" thing.
