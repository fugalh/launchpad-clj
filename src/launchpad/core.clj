;; Launchpad S and Launchpad Mini

(ns launchpad.core
  (:require [overtone.studio.midi :as midi]
            [overtone.midi :as midi0]))

(defn find-launchpad
  "Find the first connected Launchpad"
  []
  (midi/midi-find-connected-receiver "Launchpad"))

(defn mk-color
  "Make a color. red and green range from 0 to 3"
  [r g]
  (bit-or
   r
   0xC
   (bit-shift-left g 4)))

(defn light
  "light a pad at x,y (0,0 is top-left). Use mk-color for c"
  [m x y c]
  (midi/midi-note-on
   m
   (+ x (* 0x10 y))
   c))

(defn unlight
  "unlight a pad"
  [m x y]
  (light m x y 0))

(defn reset
  "reset the launchpad"
  [m] (midi/midi-control m 0 0))

(defn all-on
  "brightness is 1, 2 or 3"
  [m brightness]
  (midi/midi-control m 0 (+ 124 brightness)))

;; afaict this should work but I don't see any sysex sent with midi monitor,
;; so it might be an overtone bug.
(defn text
  [m color byte-seq]
  (midi/midi-sysex m [240 0 32 41 9 color byte-seq 0xf7]))

(defn clear-text
  "If text is looping (add 64 to color to loop), reset it"
  [m]
  (midi/midi-sysex m [240, 0, 32, 41, 9, 0, 247]))

(defn top-button
  "Light button 0-7"
  [m n col]
  (midi/midi-control m (+ 0x68 n) col))

(defn right-button
  "Light right button 0-7"
  [m n col]
  (light m 8 n col))
;; That should do it. There's more about intensity (wishlist) and about
;; double buffering and other tricks for more efficient updates, which I
;; may utilize when doing react-style
