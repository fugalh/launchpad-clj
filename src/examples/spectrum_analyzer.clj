;;; Display a spectrum analysis, a la ye olde WinAmp
;;; We read a block of audio from the default system input, do an FFT,
;;; bin by octaves, and display octaves 1-8 (which corresponds roughly
;;; to the piano keyboard range)


;;; OSX users: you can hear and watch at the same time without using the lame
;;; webcam mic, if you want. Get soundflower, make a multi-output device in Audio
;;; Midi Setup that combines both soundflower 2ch and built-in output, and set the
;;; default input to soundflower 2ch, and the default output to the multi-output
;;; device. Then start the spectrum analyzer and you should see and hear your
;;; music.

(ns examples.spectrum_analyzer
  (:require [launchpad.core :as lp])
  (:import (javax.sound.sampled AudioFormat
                                AudioSystem))
  (:gen-class))

;;; The FFT stuff. (Could I use a library? Of course, but there wasn't
;;; a simple nice one I could find in a quick search, and I hadn't
;;; implemented an FFT in awhile...)

;;; 2048 samples gives a bin width
;;; and frame rate of a little over 20 Hz (but we overlap to display
;;; at 40 Hz)
(def N 2048)

(def lg
  "Log base 2"
  (memoize
   (fn [x] (/ (Math/log x) (Math/log 2)))))

(def octave-bins
  "Map the FFT bins to Octave bins. For N=2048 there are 11 bins."
  (let [o (+ 1 (lg N))
        octaves (map #(Math/pow 2 %) (range o))]
    (map vector
         (drop-last octaves)
         (drop 1 octaves))))

;;; Thanks for not having a complex type, Java. wtf
;;; In this file, a complex number is represented as (a + bi) = [a b]

(defn expi
  "e^(i*x) with kudos to Euler"
  [x]
  [(Math/cos x) (Math/sin x)])

(defn +i
  [[a b] [c d]]
  [(+ a c) (+ b d)])

(defn -i
  [[a b] [c d]]
  [(- a c) (- b d)])

(defn *i
  [[a b] [c d]]
  [(- (* a c)
      (* b d))
   (- (* a d)
      (* b c))])

(def twiddle
  "FFT twiddle factors"
  (memoize (fn [k n]
             (expi (* -2 Math/PI (/ k n))))))

(defn fft
  "Return the FFT of the input block, whose length (N) must be a power
  of two. The resulting sequence is complex."
  ([block]
   (let [n (count block)
         n2 (/ n 2)]
     (if (= 1 n)
       [[(first block) 0]] ; base case
       (let [block1 (fft (take-nth 2 block)) ; evens
             block2 (fft (take-nth 2 (rest block)))] ; odds
         (concat (map (fn [k x1 x2]
                        (+i x1
                            (*i (twiddle k n)
                                x2)))
                      (range n2)
                      block1
                      block2)
                 (map (fn [k x1 x2]
                        (-i x1
                            (*i (twiddle k n)
                                x2)))
                      (range n2)
                      block1
                      block2)))))))

(defn magnitude
  "Convert the sequence of complex to magnitude"
  [spectrum]
  (map (fn [[a b]]
         (Math/sqrt (+ (* a a)
                       (* b b))))
       spectrum))

(defn normalize-to
  "Normalize magnitude to the given maximum."
  [spectrum max-mag]
  (if (= 0.0 max-mag) ; NB 0 doesn't work, only 0.0!
    spectrum
    (map #(/ % max-mag) spectrum)))

(defn bin-by-octave
  [spectrum]
  (let [spectrum (vec spectrum)]
    (map (fn [[a b]]
           (/ (reduce + (subvec spectrum a b)) (- b a)))
         octave-bins)))

(defn dBFS
  [x]
  (* 20 (Math/log10 (Math/abs x))))

(defn ->dBFS
  "collection to dBFS"
  [coll]
  (map dBFS coll))

(defn calculate-hann-window
  [n]
  (map #(Math/sin (/ (* Math/PI %)
                     (dec n)))
       (range n)))

(defn window-by
  [block window]
  (map * block window))

(defn open-audio
  []
  (let [rate 44100
        bits 16
        channels 1
        signed true
        bigendian false
        af (AudioFormat. rate bits channels signed bigendian)
        tdl (AudioSystem/getTargetDataLine af)]
    (.open tdl af)
    (.start tdl)
    tdl))

(defn bytes-to-shorts
  "little-endian"
  [bytes]
  (map #(+ %1 (bit-shift-left %2 8))
       (take-nth 2 bytes)
       (take-nth 2 (rest bytes))))

(defn read-audio
  [tdl n]
  (let [n (* 2 n) ; 16-bit
        buf (byte-array n)]
    (.read tdl buf 0 n)
    (vec (bytes-to-shorts buf))))

;;; The Launchpad stuff
(defn colorize
  "The color for a cell"
  [x y]
  ([[3 0]
    [3 1]
    [3 2]
    [3 3]
    [2 3]
    [1 3]
    [0 3]
    [0 3]] y))

(defn render-column
  "h is height [0.0-1.0), we will flip and scale it"
  [x h]
  (let [y (max h 0.0) ; avoid -Infinity
        y (int (* 8 y)) ; scale
        y (- 8 y)] ; flip
    (when (<= 0 y 7)
      (reduce merge {}
              (for [yy (range y 8)] {[x yy]
                                     (colorize x yy)})))))

(defn render-spectrum
  "Render the spectrum [0.0-1.0) to a Launchpad state"
  [spectrum]
  (assoc lp/initial-state :grid
         (reduce merge {}
                 (map render-column
                      (range 8)
                      spectrum))))

(defn render-spectrum-db
  "Render spectrum [0.0-1.0) with decibel scale"
  [spectrum]
  (let [db-range 96 ; 16-bit resolution (ignore dithering)
        db-range 48 ; in practice this is enough, even for soft jazz
        spectrum (->> spectrum
                      ;; convert to decibels
                      (->dBFS)
                      ;; scale and shift the range we want to [0.0-1.0)
                      (map #(+ 1.0 (/ % db-range))))]
    (render-spectrum spectrum)))

(defn render-spectrum-linear
  "Render spectrum [0.0-1.0) with linear scale"
  [spectrum]
  (render-spectrum spectrum))


;;; run
(defn run
  [kind]
  (let [tdl (open-audio)
        pad (lp/get-launchpad)
        hann-window (calculate-hann-window N)
        render-fn (get {:db render-spectrum-db
                        :linear render-spectrum-linear}
                       kind
                       render-spectrum-db)]
    (.reset pad)
    ;; We adapt the range of the spectrum by the maximum observed
    ;; magnitude and the dynamic range (in render-spectrum-db). In this
    ;; way we completely punt on the question of "what does it mean to
    ;; normalize an FFT?", and it's pretty much self-adjusting.
    ;;
    ;; We also overlap two N/2 reads to double the framerate
    (loop [max-mag 0
           xs1 (read-audio tdl (/ N 2))]
      (let [xs2 (read-audio tdl (/ N 2))
            xs (concat xs1 xs2)
            spectrum (-> xs
                         (window-by hann-window)
                         (fft) ; N wide
                         (magnitude)
                         (bin-by-octave)) ; 11 wide
            max-mag (max max-mag (reduce max spectrum))
            spectrum (normalize-to spectrum max-mag)]
        (.render pad (render-fn spectrum))
        (recur max-mag xs2)))))

(defn -main
  ([]
   (run :db))
  ([kind]
   (run (keyword kind))))

;;; TODO
;;; falling peaks
;;; dynamic range zoom in/out (/8 or *8)
