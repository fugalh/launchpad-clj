(ns launchpad.core)
(use '[overtone.studio.midi] '[overtone.midi :only [midi-out]])

(defn find-launchpad []
  (midi-find-connected-receiver "Launchpad"))

(defn mk-color
  [r g]
  (bit-or
   r
   0xC
   (bit-shift-left g 4)))

(defn light
  [m x y c]
  (midi-note-on
   m
   (+ x (* 0x10 y))
   c))

(defn unlight [m x y]
  (light m x y 0))
