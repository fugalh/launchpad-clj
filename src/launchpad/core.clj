(ns launchpad.core)
(use '[overtone.studio.midi] '[overtone.midi :only [midi-out]])

(defn find-launchpad
  "Find the first connected Launchpad"
  []
  (midi-find-connected-receiver "Launchpad"))

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
  (midi-note-on
   m
   (+ x (* 0x10 y))
   c))

(defn unlight
  "unlight a pad"
  [m x y]
  (light m x y 0))

(defn reset
  "reset the launchpad"
  [m] (midi-control m 0 0))

(defn all-on
  "brightness is 1, 2 or 3"
  [m brightness]
  (midi-control m 0 (+ 124 brightness)))


